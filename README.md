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

**Status: screens built, data pending.** This module is on its own branch off
`main`, so the tables these screens read — patients from M1, examinations from
M2, orders and bills from M3 — are not in the tree yet. The layouts, columns,
filters and buttons are finished; the figures are placeholders from the deck's
own mock-up, and every screen says so at the top. The live queries get wired
up once M1–M3 are merged into `main`.

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

#### What waits for the merge

Building the follow-up *list* from real examinations, and the four report
aggregates, need the patients, examinations, orders and bills tables. Those
belong to M1, M2 and M3 and are not in this branch — M4 was started from
`main`, before any of them existed. The rules above are all testable against
hand-seeded rows now, and the live queries drop in at the merge.

### Where each role lands after login

Sign-in redirects by role, so nobody reaches a page they cannot use:

| Role | Lands on | Owns |
|---|---|---|
| ADMIN | `/dashboard/console` | Users, roles, pricing, stock, reports, audit |
| OPTICIAN | `/patients` | Patient records, examinations, prescriptions, referrals |
| STAFF_NURSE | `/dashboard/console` | Orders, bills, order status, stock, follow-up |
| SURGEON | `/referrals` | Referred patients, surgical notes |
| PATIENT | `/portal` | Own profile, prescriptions, order status |

The M4 console screens are built — see the table above. The other modules'
screens are still being built under their module issues.

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

### Referral worklists and validity

The worklists and the validity calls. These are the endpoints a surgeon opens
their day with, and they are the only place the 12-month and 90-day rules are
enforced in code — `Prescription.isValidOn` and `Referral.isOpenForAccess` both
existed on the entities with nothing calling them, so a client had to re-derive
the rules for itself.

| Method | Path | Does |
|---|---|---|
| GET | `/api/referrals` | Worklist, `?status=PENDING` by default, `?status=all` for everything |
| GET | `/api/referrals/open` | Only the ones still inside the 90-day window |
| GET | `/api/referrals/patient/{id}` | One patient's referral history |
| PATCH | `/api/referrals/{id}/feedback` | Surgeon records the operation notes |
| GET | `/api/patients/{id}/prescriptions/validity` | History, each row saying if it is still valid |
| GET | `/api/patients/{id}/prescriptions/expired` | Just the ones that have run out |

**Closed referrals are reported, not hidden.** A referral past its 90 days comes
back with `accessible: false` and a summary saying when access closed. Silently
dropping it would leave a surgeon chasing an old case with no idea why the
patient disappeared from their list.

**Feedback needs the surgeon's role.** The service already refuses a second
write; the endpoint also refuses a caller who is not the surgeon, so an optician
cannot record an operation that did not happen through their account.

`/api/prescriptions/compare` moved into `ClinicalService.compareRx` so the
controller and any later view describe a change the same way.

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
