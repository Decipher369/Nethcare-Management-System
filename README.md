# Nethcare Management System

> A web-based management system for **Neth Opticians**, Kolonnawa — an optometrist clinic offering eye examinations, prescription glasses, contact lenses, and repairs. Currently all operations run on paper registers; Nethcare digitises the entire workflow.

**SLIIT · SE2012 Object Oriented Analysis & Design · Group Project · Semester 2 2026**

## Project Structure

```
nethcare-management-system/
├── src/
│   ├── main/
│   │   ├── java/com/nethcare/
│   │   │   ├── config/           # Spring Security, CORS, JPA auditing
│   │   │   ├── controller/       # REST controllers (by module)
│   │   │   ├── dto/              # Data transfer objects & API response wrapper
│   │   │   ├── exception/        # Global exception handling
│   │   │   ├── model/            # JPA entities & enums (by module)
│   │   │   ├── repository/       # Spring Data JPA repositories
│   │   │   └── service/          # Business logic layer
│   │   └── resources/
│   │       ├── application.properties       # Base config
│   │       ├── application-dev.properties   # Dev profile — seeded accounts
│   │       ├── application-prod.properties  # Production config
│   │       └── templates/                   # login.html, landing.html, error.html
│   └── test/
│       └── java/com/nethcare/    # Unit & integration tests
├── docker-compose.yml            # MySQL 8.0 database container
├── pom.xml                       # Maven build (Spring Boot 3.2.5, Java 17)
└── README.md
```

## Client

| Detail | Information |
|---|---|
| Business | Neth Opticians — optical retail & eye-care services |
| Client contact | Ms. Udeni Gurusinghe (owner) |
| Location | Kolonnawa, Sri Lanka |
| Premises | Single branch: reception, examination room, dispensing counter |
| Current system | Paper registers and handwritten prescription cards |

## Team

| Member | Module | Responsibility |
|---|---|---|
| Gayathri | M1 — Patient Registration & History | Patient records, portal, user & role management |
| Akshai | M2 — Examination, Prescription & Referral | Clinical workflow, prescription lifecycle, surgeon hand-off |
| Shagash | M3 — Order, Billing & Stock | Order pipeline, pricing, payments, inventory control |
| Edwien | M4 — Follow-up, Reporting & Audit | Reminders, notifications, management reports, audit trail |

## Modules

### M1 — Patient Registration & History
- Register new patients with contact, age, and medical notes
- Search and open any existing patient record instantly
- Patient self-service portal (view own profile, prescriptions, order status)
- Admin console: create users, assign roles, deactivate accounts
- Chronological visit history timeline (append-only)
- Mandatory-field validation on registration; patient consent recorded at sign-up

### M2 — Examination, Prescription & Referral
- Structured eye-examination form (VA, SPH, CYL, AXIS, ADD, IPD)
- Prescription generated from examination, stored permanently
- Full prescription history with year-over-year change comparison
- Referral to eye surgeons with reason and attached history
- Surgeon feedback returned into the patient file

### M3 — Order, Billing & Stock
- Create orders from prescriptions (frame, lens type, coatings)
- Automatic pricing, discount, and advance-payment handling
- Printable bills and receipts with outstanding balance
- Order status tracking: placed → lab → ready → collected
- Stock deducted on issue with low-stock alerts for frames and lenses

**Module 3 is on the `m3-stock-billing` branch.** It builds the stock catalogue
first, since the frame gallery and the order screen both read from it.

| Entity | What it holds |
|---|---|
| `StockItem` | One product — code, brand, category, price, quantity, reserved, reorder level, expiry, image |
| `Order` | Order number, customer, lens spec, priority, status, promised date |
| `OrderItem` | One line — stock item, quantity, and the unit price *as it was on the day* |

Categories are the five the client asked to see on the dashboard: `FRAME`,
`SINGLE_VISION_LENS`, `BIFOCAL_LENS`, `CONTACT_LENS`, `CASE`.

`quantity` is what is on the shelf. `reserved` is what an open order has
claimed but not yet taken. The counter works from `available()`, which is the
difference — a frame that is on the shelf but already spoken for must not be
offered to the next customer.

`StockService` keeps the three movements apart, because merging them is how a
shop ends up selling the same frame twice:

| Method | Shelf count | Reserved | When |
|---|---|---|---|
| `reserve` | unchanged | up | order placed — set aside, still on the shelf |
| `deduct` | down | down | customer collects — the item actually leaves |
| `release` | unchanged | down | order cancelled — the reservation goes back |

`adjustQuantity` is the counter recount. The staff member is stating what is
really there, so their number wins — but a count *below* what is already
reserved is refused, since that would hand an open order's frame to somebody
else without anyone noticing.

Low stock is `available() <= reorderLevel`, so an item flagged while fully in
stock still shows once somebody claims the last one.

An order runs `PLACED → LAB → READY → COLLECTED`, with `CANCELLED` reachable
from anywhere before collection. `OrderStatus.next()` is the only place that
knows which step follows which — the tracker reads from it rather than
hard-coding the chain a second time.

`OrderItem.unitPrice` is written once and never re-read from the price list.
The admin can change a frame's price next month, but this order still cost
what it cost on the day; otherwise an old bill would quietly stop adding up
and the customer's receipt would disagree with our records.

The lens specification (type, coating) is copied onto the order rather than
looked up through the prescription, for the same reason. A prescription gets
reissued next year — the glasses already made to last year's spec must not
change with it.

`Order` carries `customerName` and `customerPhone` instead of a patient id,
because M3 branches from `main` and no patient table exists there yet. That
link is added when M3 merges with M1.

A bill is `INV-xxxx` and stores its own totals — subtotal, discount, urgent
surcharge, total. Same reasoning as the order line price: a bill is a document
the customer keeps, so reprinting it next year must show what they actually
owed. The arithmetic is worked out once, when the bill is raised, and the
figures sit on the row.

Payments are a separate append-only table. A bill's paid amount is the sum of
its payments rather than a column that gets overwritten, so the advance taken
before the lab and the balance settled on collection stay as two entries —
which is what M4's financial report reads.

`PaymentMethod` is cash or card, recorded by staff. There is no gateway and no
card details are stored anywhere; the row records that money was taken, not
how it moved.

`OrderService` is where the client's rules are actually enforced — each one is
a decision the system makes, so none of them are left to a screen:

| Rule | Where it lives |
|---|---|
| Price frozen on the day | `place` copies the catalogue price onto each line |
| 40% advance before the lab | `advance` checks the bill and says how much is short |
| Stock reserved at order, deducted at collection | `place` reserves, `advance` deducts on `COLLECTED` |
| Low stock flags itself | `StockItem.isLow()` — `available() <= reorderLevel` |
| Cancel releases stock, writes a credit note | `cancel` releases, then raises `CN-xxxx` if money was taken |

The 40% comes from `nethcare.order.advance-percentage` in
`application.properties`, not a number typed into a screen, so changing the
client's terms is a config change. Urgent orders carry a 15% surcharge on top
of the discount, per FR-3.3.

Money rounds to two decimals at every step, so the customer adding up the
printed bill gets the same figure we hold.

Overpayment is refused rather than absorbed. If somebody hands over a note for
a 500 balance, the counter needs to know before the change is given, not after.

Cancelling an order that took money raises a credit note. It is marked, not
deleted — the money genuinely moved, and M4 reports on it.

## M3 — public shop front

The public side needs no login. Four pages, all reading from the same
catalogue the counter works from — so the gallery is not a hand-kept list
that drifts out of date.

| Page | Path | Shows |
|---|---|---|
| Home | `/` | Shop name, location, services, link to the gallery |
| About | `/about` | Business details and the full service list |
| Frames | `/frames` | Live gallery, filterable by category, searchable |
| Contact | `/contact` | Contact details |

`/` used to be the post-login redirect. It is now the shop front, and
`/dashboard` does the role routing instead — a signed-in user opening the home
page sees the shop, not a redirect loop. All five roles still land correctly
(`/dashboard` → `/admin`, `/patients`, `/orders`, `/referrals`, `/portal`).

**The gallery shows "Available" or "Ask us — on order", never the exact count.**
Printing "2 left" on a public page is a countdown for somebody else to beat us
to. The staff screen in the next commit is where the numbers live.

**`BusinessProfile` holds only what the client actually told us** — the shop
name, Kolonnawa, the owner's name and the three premises. Phone, street
address, email and opening hours appear nowhere in the proposal or the deck, so
those fields are `null` and the page prints "To be confirmed" instead. An
invented phone number on a real business is worse than a blank.

If the client supplies the missing details, they go in `BusinessProfile` and
every page updates. The templates already read every value from there rather
than hard-coding text, so promoting it to an editable settings screen later
means changing one class, not five templates.

Stock items with no photo render a neutral "No photo yet" tile. Real frame
photographs go in `src/main/resources/static/images/frames/` and are picked up
by `imageName` — no template change needed.

### M4 — Follow-up, Reporting & Audit
- Weekly follow-up list of patients due for a re-check (12 months / 6 months for contact lens users)
- SMS / email notifications: order ready, appointment, reminder
- Management reports: monthly sales, patients attended, order status, stock summary
- Immutable audit log of every create, update, and delete

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2.5, Spring MVC, Spring Security, Spring Data JPA
- **Frontend:** Thymeleaf templates
- **Database:** MySQL 8 (3NF schema, surrogate PKs, FK enforced, soft-delete only)
- **Build:** Maven
- **Version Control:** Git + GitHub

## Key Design Principles

- Role-based access control on every screen and query
- Soft-delete only — records are never hard-deleted
- Append-only audit log that no role can modify
- Patient data encrypted at rest and in transit
- Local-first design with sync; core screens work offline

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+ (or Docker for containerised database)

### Setup
```bash
# Option A — Docker (recommended)
docker compose up -d              # starts MySQL on localhost:3306

# Option B — Manual MySQL
# Create the database
mysql -u root -p -e "CREATE DATABASE nethcare; CREATE USER 'nethcare_user'@'localhost' IDENTIFIED BY 'nethcare_pass'; GRANT ALL ON nethcare.* TO 'nethcare_user'@'localhost';"

# Build and run (dev profile seeds one account per role)
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Verify
curl http://localhost:8080/api/health
```

### Login

Sign in at `http://localhost:8080/login`. The `dev` profile creates one account
per role on first run, and prints the list on the login page itself:

| Username | Password | Role |
|---|---|---|
| admin | admin123 | ADMIN |
| optician | optician123 | OPTICIAN |
| staff | staff123 | STAFF_NURSE |
| surgeon | surgeon123 | SURGEON |
| patient | patient123 | PATIENT |

Without the `dev` profile no accounts are created and there is nothing to log in with.
Change the passwords in `application-dev.properties` before using this anywhere shared.

### Where each role lands after login

Sign-in redirects by role, so nobody reaches a page they cannot use:

| Role | Lands on | Owns |
|---|---|---|
| ADMIN | `/admin` | Users, roles, pricing, stock, reports, audit |
| OPTICIAN | `/patients` | Patient records, examinations, prescriptions, referrals |
| STAFF_NURSE | `/orders` | Orders, bills, order status, stock |
| SURGEON | `/referrals` | Referred patients, surgical notes |
| PATIENT | `/portal` | Own profile, prescriptions, order status |

These pages currently list what each role can do. The real screens are still
being built under their module issues.

### If you get Spring's whitelabel error page

That fallback shows when something fails and nothing handles the resulting
`/error` request. Restart the app and read the message on the error page; it
names the URL that failed. If `/login` is the one failing, check the run
configuration has `-Dspring-boot.run.profiles=dev`.

## Project Timeline

| Phase | Period | Deliverables |
|---|---|---|
| Requirements | Aug – 2 Sep 2026 | Interviews, actors, use cases, client presentation |
| Analysis & Design | Sep 2026 | Class diagrams, ERD, sequence diagrams, UI design |
| Implementation | Sep – Oct 2026 | Module build by owner, weekly integration |
| Testing & Handover | Oct 2026 | Integration testing, client UAT, documentation |

## Milestones

- **2 Sep 2026** — Requirements sign-off (initial presentation)
- **30 Sep 2026** — Progress review (design pack + 50% module implementation)
- **7 Oct 2026** — Group report submitted to Turnitin
- **21 Oct 2026** — Final viva, demo, and code submission (Gradescope)
