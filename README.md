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

### Patient registration (M1)

Opticians and admins work the register at `/patients`:

| Page | What it does |
|---|---|
| `/patients` | The list, with a search box over name, phone and patient number |
| `/patients/new` | Registration form |
| `/patients/{id}` | One patient's record and visit history |

A patient row is separate from their login. The `users` entry is the account
and password, the `patients` entry is the clinical record, and `user_id` links
them — so closing an account leaves the visit history intact. Tick "also create
a login" during registration and a `PATIENT` account is made with a random
password that the front desk writes on a slip.

The visit history on a patient's record is empty for now. It fills in when
examinations and prescriptions land with M2.

### Patient API (M1)

The same register over JSON, for anything that is not a browser form. Every
path is behind the login, and the role rules are the same ones the HTML pages
use — `OPTICIAN` and `ADMIN` on patients, `ADMIN` only on users.

| Method | Path | Does |
|---|---|---|
| GET | `/api/patients?q=` | List, searchable by name, phone or patient number |
| GET | `/api/patients/{id}` | One patient |
| POST | `/api/patients` | Register a patient |
| PUT | `/api/patients/{id}` | Correct a patient's details |
| GET | `/api/patients/{id}/history` | Visit history — empty until M2 |
| GET | `/api/users` | List staff accounts (admin) |
| POST | `/api/users` | Create a staff account (admin) |
| PUT | `/api/users/{id}/role` | Change someone's role (admin) |
| PATCH | `/api/users/{id}/deactivate` | Close an account (admin) |

**Registration calls the same `PatientService` the form uses**, so the API and
the browser cannot disagree about what makes a valid patient.

**`PUT` only accepts the fields a person can correct.** `patient_no`, `user_id`
and `registered_on` are set at creation and stay put, so a `PUT` cannot quietly
renumber a patient or repoint their portal login.

**Deactivating is not deleting.** It sets `status = INACTIVE` and leaves the row
in place, because registrations point at `user_id` and removing the account
would orphan everything they touched.

**Responses are DTOs, not entities.** `User` has a `getPasswordHash()`, and
Jackson would happily serialise it — returning the entity from `/api/users`
put every account's BCrypt hash in the response. `UserDto` never reads the
field off the entity, so there is nothing to leak.

`/api/**` skips CSRF (`SecurityConfig`) because it is stateless and may get a
non-browser client. Everything the browser submits still goes through a
checked form post.

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
