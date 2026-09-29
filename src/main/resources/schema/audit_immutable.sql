-- Module 4 — the audit trail has to be unchangeable by anyone, including the
-- admin. JPA alone cannot do that: @Updatable stops the application issuing an
-- UPDATE, but it says nothing about a script in a terminal, another app on the
-- same database, or a mistake in a client console.
--
-- So the second half of the lock lives here, in the database. The triggers
-- raise on any UPDATE or DELETE against audit_log, which is the same table the
-- app appends to — INSERT is the only thing it allows.
--
-- Apply once, after the app has created the table. Needs a user with the
-- SUPER privilege, because MySQL refuses to create a trigger when binary
-- logging is on and log_bin_trust_function_creators is not set (error 1419).
-- The app's own nethcare_user is not enough:
--
--   mysql -u root -p nethcare < src/main/resources/schema/audit_immutable.sql
--
-- If the MySQL server is one you control and this is a development box, the
-- one-time alternative is:
--
--   SET GLOBAL log_bin_trust_function_creators = 1;
--
-- Do not run that on a production server: it lets anyone with CREATE TRIGGER
-- write code the server will log, which is the hole the setting exists to
-- close.
--
-- Check whether it is in place:
--
--   SELECT TRIGGER_NAME FROM information_schema.TRIGGERS
--   WHERE TRIGGER_SCHEMA = 'nethcare' AND EVENT_OBJECT_TABLE = 'audit_log';
--
-- To prove it holds, try to change a row and expect to be refused:
--
--   UPDATE audit_log SET actor = 'tampered' WHERE id = 1;
--   DELETE FROM audit_log WHERE id = 1;
--
-- Until this is applied the table is append-only by convention only.

-- Each trigger is a single statement on purpose. A BEGIN..END block needs a
-- DELIMITER change, which is a mysql-client feature and not SQL, so it cannot
-- be run through JDBC or any other tool. One SIGNAL does the same job without
-- the delimiter trouble.

DROP TRIGGER IF EXISTS audit_log_no_update;
DROP TRIGGER IF EXISTS audit_log_no_delete;

CREATE TRIGGER audit_log_no_update BEFORE UPDATE ON audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'audit_log is append-only: rows cannot be updated';

CREATE TRIGGER audit_log_no_delete BEFORE DELETE ON audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'audit_log is append-only: rows cannot be deleted';
