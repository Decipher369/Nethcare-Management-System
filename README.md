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
│   │       └── templates/                   # login.html, landing.html, error.html,
│   │                                        # patients/
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

Module 3 builds the stock catalogue first, since the frame gallery and the
order screen both read from it.

| Entity | What it holds |
|---|---|
| `StockItem` | One product — code, brand, category, price, quantity, reserved, reorder level, expiry, image |
| `Order` | Patient, visit/prescription snapshot, selected frame/lens, status and dispensing dates |
| `OrderItem` | One line — stock item, quantity, and the unit price *as it was on the day* |
| `Frame` / `Lens` | Category-specific details linked one-to-one with the shared stock row |
| `OrderStatusHistory` | Append-only order workflow timeline |
| `StockMovement` | Append-only reservation, release, dispensing and adjustment ledger |
| `ContactLensTracker` | Patient and bill-linked contact-lens replacement record |

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

Orders are created only for a registered patient selected by staff. If a
prescription is selected, the service verifies that it belongs to that patient
and copies its powers into the order. The copied patient and prescription data
keeps the historical sale readable after either source record changes.

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
page sees the shop, not a redirect loop. `/dashboard` routes admin and staff to
the management console, the optician to patients, the surgeon to referrals,
and the patient to their portal.

**The gallery shows "Available" or "Ask us — on order", never the exact count.**
Printing "2 left" on a public page is a countdown for somebody else to beat us
to. The staff screen is where the numbers live.

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

## M3 — staff stock screen

`/stock`, behind the login. This is the screen the counter actually works on,
and it writes to the same catalogue the public gallery reads.

| Page | Path | Does |
|---|---|---|
| Catalogue | `/stock` | Every item, filterable by state, category or search |
| Add / edit | `/stock/new`, `/stock/{id}/edit` | Price, reorder level, expiry, image file |
| Stock count | `/stock/{id}/count` | Records what is on the shelf, with a reason |

Open to `ADMIN` and `STAFF_NURSE`. The optician, surgeon and patient get 403.

**A stock count does not touch reserved units.** Reserved stock belongs to an
open order, so a recount of the shelf must not silently hand that frame to
somebody else. If the count lands below what is already reserved the service
refuses it rather than papering over the conflict.

**Adding an item lists it for sale immediately** — `is_active` starts true, so a
newly added frame appears on the public gallery. That is the point of the shared
catalogue, but it means a frame cannot be stocked before it has physically
arrived.

## M3 — order and bill screens

`/orders` and `/bills`, behind the login. These drive the order pipeline that
`OrderService` already enforced, so the counter can work an order through
placed → lab → ready → collected without touching the console.

| Page | Path | Does |
|---|---|---|
| Order list | `/orders` | Filter by Open / Overdue / All / Collected / Cancelled, or search the order number and customer |
| New order | `/orders/new` | Lens type, coating, promised date, priority, and a quantity against each catalogue item |
| Order detail | `/orders/{id}` | The lines, the next step, raise the bill, cancel with a reason |
| Bill list | `/bills` | Filter by Outstanding / Settled / Cancelled / All |
| Bill detail | `/bills/{id}` | Itemised subtotal, discount, urgent surcharge, total, balance, and the receipts taken |

Open to `ADMIN` and `STAFF_NURSE` — the same two roles as the stock screen. The
optician, surgeon and patient get 403.

**The 40% advance is enforced at the lab step.** An order cannot go from placed
to the lab until 40% of the bill is paid, and the page says how much is still
short. Urgent orders add a 15% surcharge, so the advance is calculated on the
surcharged total rather than the subtotal.

**Collection is stricter than the advance — the bill must be settled in full.**
The glasses do not leave the shop while money is still owing, so the counter
cannot hand over an unpaid order. The order page says what is outstanding and
why the step is refused, rather than making staff click through to find out.

**Stock is reserved when the order is placed, and deducted when it is
collected.** A reserved unit cannot be sold twice, and it comes back to the
shelf if the order is cancelled.

**Nothing here stores a card number.** A `CARD` payment only records that
someone paid by card at the counter.

### M4 — Follow-up, Reporting & Audit
- Weekly follow-up list of patients due for a re-check (12 months / 6 months for contact lens users)
- SMS / email notifications: order ready, appointment, reminder
- Management reports: monthly sales, patients attended, order status, stock summary
- Immutable audit log of every create, update, and delete

**Status: screens built, live reporting pending.** The layouts, columns,
filters and buttons are finished, but the figures are still placeholders from
the deck's mock-up. The merged patient, examination, order and billing tables
now provide the data sources needed to replace them with live queries.

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
docker compose up -d              # starts MySQL on localhost:3307

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

### Deploy on Railway

1. Add a **MySQL** service to the Railway project and wait for it to become available.
2. Create a service from this GitHub repository. The `pom.xml` is at the
   repository root, so leave Railway's **Root Directory** unset.
3. In the app service's **Variables** tab, set the following values. Replace `MySQL`
   with the exact name of your Railway database service if it differs:

   ```text
   SPRING_PROFILES_ACTIVE=prod
   DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?serverTimezone=Asia/Colombo
   DB_USER=${{MySQL.MYSQLUSER}}
   DB_PASS=${{MySQL.MYSQLPASSWORD}}
   ```

   `DB_URL` must start with `jdbc:mysql://`; Railway's `MYSQL_URL` is not a JDBC URL.
   Use the database's private host and port for the app in the same Railway project.
   Review and deploy the staged variable changes, then redeploy the app.
4. In the app service's **Settings → Networking**, generate a public domain. The
   app listens on Railway's `PORT` variable. Set `/api/health` as the health check
   path if you enable Railway health checks.

Flyway V1 and V2 create the full application schema during startup. Hibernate
is validation-only (`ddl-auto=validate`); it does not create or update tables.
Flyway requires a working database connection. A
`Connection refused` error means the configured host and port are not accepting
connections; check that MySQL is running and inspect the rendered `DB_URL` host
and port in the app's Railway variables. The `prod` profile does not create demo
accounts, so an empty production database will need accounts provisioned separately.

### Console screens (M4)

Admin and staff land straight on the dashboard.

| Screen | URL |
|---|---|
| Management dashboard | `/dashboard/console` |
| Patients due for review | `/followups` |
| Reminder queue | `/notifications` |
| Sales report | `/reports/sales` |
| Order status | `/reports/orders` |
| Stock summary | `/reports/stock` |
| Audit trail | `/audit` |

Open to `ADMIN`, `OPTICIAN` and `STAFF_NURSE`. The surgeon and the patient get
403 on all of them — the audit trail and the money figures are not the
clinical role's business.

### M4 — the follow-up and audit tables

The console screens above are still wireframes with the client's own numbers on
them. Underneath, the tables and rules now exist.

| Table | Holds |
|---|---|
| `follow_ups` | A patient due back, when they were last seen, and what they said |
| `notifications` | The reminder queue, one row per attempt |
| `audit_log` | One row per create / update / delete, in the whole system |

**The follow-up rule is 12 months, or 6 for a contact lens patient.** Both live
in `nethcare.followup.regular-months` and `contact-lens-months` rather than in
the code, and the service works out the due date from the last examination.
`due_for_who` records which rule applied, so nobody has to reverse the
arithmetic to know why somebody is on the list.

**A reminder is SMS first, email as the fallback.** A patient with no number
gets the email channel; a patient who has opted out gets nothing queued at all,
and a patient with no contact details is recorded as failed rather than left
sitting in the queue with nowhere to go.

**A no-answer stays on the worklist.** Recording that nobody picked up leaves
the row PENDING, because a customer who did not answer has not been told no.
Only a booking or a decline closes it.

**Nothing is sent from this table.** The shop has no SMS gateway, so a row
being SENT means a staff member recorded handing it over — the app does not
claim a message left the building on its own.

#### The audit trail is append-only, with one caveat

The trigger that enforces this is in
`src/main/resources/schema/audit_immutable.sql`:

```bash
mysql -u root -p nethcare < src/main/resources/schema/audit_immutable.sql
```

It needs a user with the `SUPER` privilege — the app's own `nethcare_user` gets
MySQL error 1419, because binary logging is on and
`log_bin_trust_function_creators` is not set. Check it is in place with:

```sql
SELECT TRIGGER_NAME FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = 'nethcare' AND EVENT_OBJECT_TABLE = 'audit_log';
```

**Until that has been applied, the table is append-only by agreement, not by
enforcement.** `@Immutable` on the entity stops Hibernate issuing an UPDATE,
but that is not enough on its own — measured on this schema, it leaves the
update blocked and `deleteById()` still removes the row. The trigger is what
closes that, and it applies to anything reaching the database, not just this
application.

Audit rows are written by `AuditService.record(...)`, which the other modules
call on every create, update and delete. It uses `REQUIRES_NEW` so an entry
survives even when the operation it describes rolls back — a failed create is
still worth having on record.

#### Integration still required

Building the follow-up *list* from real examinations and calculating the four
report aggregates still need to be wired to the merged patient, examination,
order and billing repositories. The rules above can currently be exercised
against hand-seeded rows.

### Where each role lands after login

Sign-in redirects by role, so nobody reaches a page they cannot use:

| Role | Lands on | Owns |
|---|---|---|
| ADMIN | `/dashboard/console` | Users, roles, pricing, stock, reports, audit |
| OPTICIAN | `/patients` | Patient records, examinations, prescriptions, referrals |
| STAFF_NURSE | `/dashboard/console` | Orders, bills, order status, stock, follow-up |
| SURGEON | `/referrals` | Referred patients, surgical notes |
| PATIENT | `/portal` | Own profile, prescriptions, order status |

The patient, clinical, order, billing, stock, portal and M4 console screens are built.

### Patient registration (M1)

Opticians and admins work the register at `/patients`:

| Page | What it does |
|---|---|
| `/patients` | Active/inactive register, searchable by name, NIC, phone and patient number |
| `/patients/new` | Registration form |
| `/patients/{id}` | One patient's record, visit history and prescriptions |
| `/patients/{id}/edit` | Correct contact, guardian and demographic details |
| `/portal` | Patient's own read-only profile, prescriptions and linked orders |

A patient row is separate from their login. The `users` entry is the account
and password, the `patients` entry is the clinical record, and `user_id` links
them — so closing an account leaves the visit history intact. Tick "also create
a login" during registration and a `PATIENT` account is made. Its one-time
password combines a recognizable name/DOB prefix with a random suffix, is
shown once to staff, and must be changed at first login. Only its BCrypt hash
is stored.

Adults require a unique Sri Lankan NIC and phone number. Patients under 18
require a guardian name and phone, and duplicate minor registrations are
checked against name, DOB and guardian phone. Consent is recorded with its
time and staff member. Deactivation is reversible and preserves every clinical
record.

The patient detail page reads its examination and prescription history from
M2.

### Clinical workflow (M2)

The browser workflow starts from a patient record. An optician opens
`/examinations/new?patientId={id}`, confirms the current medical and ocular
history, records symptoms and measurements, and saves the visit. From the saved
examination they can issue one prescription or send one referral to an active
surgeon account.

| Page | What it does |
|---|---|
| `/examinations/new?patientId={id}` | Structured symptoms, history and eye examination form |
| `/examinations/{id}` | Visit detail, permanent prescription and referral actions |
| `/referrals` | Pending, completed or full role-aware referral worklist |
| `/referrals/{id}` | Referral context and surgeon consultation form |

Each examination writes a new `patient_medical_history` snapshot instead of
editing the previous row. That preserves what the clinician knew at every
visit. Symptoms are stored once against their examination. Axis, sphere,
cylinder, ADD and PD values are checked by the service before anything is
saved.

### Clinical API (M2)

Examinations, prescriptions and referrals over JSON. `OPTICIAN`, `SURGEON` and
`ADMIN` reach these; patients get 403.

| Method | Path | Does |
|---|---|---|
| POST | `/api/examinations` | Record an examination (VA, SPH, CYL, AXIS, ADD, IPD) |
| GET | `/api/examinations/{id}` | One examination |
| GET | `/api/patients/{id}/examinations` | A patient's examinations, newest first |
| POST | `/api/examinations/{id}/prescription` | Issue the glasses from that examination |
| GET | `/api/prescriptions/{id}` | One prescription |
| GET | `/api/patients/{id}/prescriptions` | Full prescription history |
| GET | `/api/prescriptions/compare?rx1=&rx2=` | What changed between two prescriptions |
| POST | `/api/examinations/{id}/referral` | Refer to a surgeon |
| GET | `/api/referrals/{id}` | One referral |

**Prescriptions are immutable.** A wrong prescription stays on the record;
changing it means a new examination and a new one alongside it. There is no
update endpoint, on purpose.

**A referral carries the last four prescriptions with it**, copied in as text
when the referral is made rather than looked up live. A referral is a record of
what the surgeon was actually handed over.

### Referral worklists and permanent records

Prescriptions and referrals do not expire automatically. Both remain available
as clinical history. Referral status represents whether the assigned surgeon
has completed the consultation.

| Method | Path | Does |
|---|---|---|
| GET | `/api/referrals` | Worklist, `?status=PENDING` by default, `?status=all` for everything |
| GET | `/api/referrals/open` | Pending referrals without an age cutoff |
| GET | `/api/referrals/patient/{id}` | One patient's referral history |
| PATCH | `/api/referrals/{id}/feedback` | Assigned surgeon records tests, diagnosis, treatment and follow-up |

**Consultation results are append-only.** The assigned surgeon, or an admin,
can complete a pending referral once. The service requires a diagnosis and
treatment and returns those findings to the permanent patient record.

`/api/prescriptions/compare` moved into `ClinicalService.compareRx` so the
controller and any later view describe a change the same way.

### Patient API (M1)

The same register over JSON, for anything that is not a browser form. Every
path is behind the login, and the role rules are the same ones the HTML pages
use — `OPTICIAN` and `ADMIN` on patients, `ADMIN` only on users.

| Method | Path | Does |
|---|---|---|
| GET | `/api/patients?q=` | List, searchable by name, NIC, phone or patient number |
| GET | `/api/patients/{id}` | One patient |
| POST | `/api/patients` | Register a patient |
| PUT | `/api/patients/{id}` | Correct a patient's details |
| GET | `/api/patients/{id}/history` | Visits and prescriptions from M2 |
| GET | `/api/portal` | Signed-in patient's own read-only record |
| GET | `/api/users` | List staff accounts (admin) |
| POST | `/api/users` | Create a staff account (admin) |
| PUT | `/api/users/{id}/role` | Change someone's role (admin) |
| PATCH | `/api/users/{id}/deactivate` | Close an account (admin) |
| PATCH | `/api/users/{id}/reactivate` | Reopen an account (admin) |
| POST | `/api/users/{id}/reset-password` | Issue a one-time password (admin) |

**Registration calls the same `PatientService` the form uses**, so the API and
the browser cannot disagree about what makes a valid patient.

**`PUT` only accepts the fields a person can correct.** `patient_no`, `user_id`
and `registered_on` are set at creation and stay put, so a `PUT` cannot quietly
renumber a patient or repoint their portal login.

**Deactivating is not deleting.** It sets `is_active = false` and leaves the row
in place, because registrations point at `user_id` and removing the account
would orphan everything they touched. The final active administrator and the
currently signed-in administrator are protected from deactivation.

**Responses are DTOs, not entities.** `User` has a `getPasswordHash()`, and
Jackson would happily serialise it — returning the entity from `/api/users`
put every account's BCrypt hash in the response. `UserDto` never reads the
field off the entity, so there is nothing to leak.

Session-authenticated API writes require CSRF protection too. Successful and
failed login attempts are stored in `login_events`, and the admin page shows
the latest activity without exposing password hashes.

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
