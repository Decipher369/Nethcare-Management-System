-- Apply after m4_schema.sql. These database guards make both evidence tables append-only.
DELIMITER $$
DROP TRIGGER IF EXISTS clinical_audit_log_no_update$$
DROP TRIGGER IF EXISTS clinical_audit_log_no_delete$$
DROP TRIGGER IF EXISTS clinical_advice_log_no_update$$
DROP TRIGGER IF EXISTS clinical_advice_log_no_delete$$
CREATE TRIGGER clinical_audit_log_no_update BEFORE UPDATE ON clinical_audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'clinical_audit_log is append-only'$$
CREATE TRIGGER clinical_audit_log_no_delete BEFORE DELETE ON clinical_audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'clinical_audit_log is append-only'$$
CREATE TRIGGER clinical_advice_log_no_update BEFORE UPDATE ON clinical_advice_logs
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'clinical_advice_logs is append-only'$$
CREATE TRIGGER clinical_advice_log_no_delete BEFORE DELETE ON clinical_advice_logs
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'clinical_advice_logs is append-only'$$
DELIMITER ;
