# Module 4 workflow

## 1. Create a follow-up case

An optician or administrator records the patient, originating examination,
assigned optician, category, risk flag, clinical notes, and an explicit target
review date. The application verifies that the examination belongs to the
patient. It does not derive the date from a 12-month prescription-validity rule.

`POST /api/followups` creates the case and appends a hashed audit event in the
same transaction. If the audit write fails, the case creation is rolled back.

## 2. Generate and queue a review cohort

`GET /followups?from=YYYY-MM-DD&to=YYYY-MM-DD` loads active cases in a selected
date range, ordered by the target date. Staff can queue one case, while
`POST /api/followups/notifications/queue` queues the full cohort without adding
a second open dispatch for the same case.

An opted-out patient receives an `EXCLUDED` dispatch record. A missing or invalid
Sri Lankan mobile number produces a `FAILED` record. Both stay visible for audit
and exception handling.

## 3. Dispatch and receipt handling

A valid reminder enters `PENDING`. The SMS adapter or staff callback supplies a
gateway receipt to mark it `SENT`; a delivery receipt then marks it `DELIVERED`.
Timeouts and gateway-credit failures use `RETRY_PENDING`, increment the attempt
count, and retain the failure reason. Permanent failures use `FAILED`.

The scheduler runs daily at 08:00 Asia/Colombo. Set `nethcare.sms.enabled=true`,
`nethcare.sms.endpoint`, and `nethcare.sms.token` in the production environment
to activate the HTTP gateway adapter.

## 4. Clinical advice evidence

`POST /api/clinical-advice` stores the patient, clinician, visit, full advice,
diagnostic inputs, and UTC timestamp. High-risk records require caution advice.
Each record contains its predecessor hash and a SHA-256 hash of its canonical
content. MySQL triggers reject updates and deletes.

## 5. Reports

Sales figures come from persisted bills and payments for any selected date
range (daily, monthly, quarterly, or annual); order status comes from orders;
stock value and reorder rows come from stock. `/reports/sales.csv`
exports the selected period and persists a `SystemSummaryReport` snapshot with an
audit entry. Browser print provides the PDF workflow. Only `ADMIN` and `AUDITOR`
can access financial reports and the audit trail.

## Database installation

For a managed MySQL database, apply `src/main/resources/schema/m4_schema.sql`
after the M1-M3 tables exist, then apply
`src/main/resources/schema/audit_immutable.sql`. Hibernate can create the mapped
tables in development, but the trigger script is required for database-level
append-only enforcement.

## 6. Role-scoped console and responsive navigation

The console rail (`console.css`, `staff.css`, and `admin.css`) provides unified vertical navigation
styled in enterprise dark navy blue (`#174c70`):
- **OPTICIAN**: Patient care group (`Patients`, `Referrals`, `Follow-ups`, `Notifications`).
- **STAFF_NURSE**: Store operations group (`Stock`, `Orders`, `Bills`).
- **SURGEON**: Consultations group (`Referrals`).
- **AUDITOR**: Reports and accountability groups (`Sales report`, `Order fulfillment`, `Stock valuation`, `Audit trail`).
- **ADMIN**: Unrestricted administrative shell with all workspaces.

On mobile and narrow viewports (`<= 900px`), navigation links automatically format as compact,
touch-friendly chips with `flex-wrap: wrap`, preventing overflow clipping and ensuring all role-scoped
routes remain accessible without blocking viewport real estate.
