CREATE TABLE IF NOT EXISTS roles (
    role_name VARCHAR(20) NOT NULL PRIMARY KEY,
    description VARCHAR(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (role_name, description) VALUES
    ('ADMIN', 'System administration and full management access'),
    ('OPTICIAN', 'Patient registration and clinical care'),
    ('STAFF_NURSE', 'Reception, orders, billing and stock'),
    ('SURGEON', 'Assigned specialist referral access'),
    ('PATIENT', 'Read-only access to the linked patient record'),
    ('AUDITOR', 'Read-only reporting and audit access')
ON DUPLICATE KEY UPDATE description = VALUES(description);

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    full_name VARCHAR(100) NULL,
    email VARCHAR(120) NULL,
    phone VARCHAR(20) NULL,
    must_change_password BIT(1) NOT NULL DEFAULT b'0',
    last_login_at DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS must_change_password BIT(1) NOT NULL DEFAULT b'0';

UPDATE users SET is_active = b'0' WHERE status = 'INACTIVE';
ALTER TABLE users DROP COLUMN IF EXISTS status;

ALTER TABLE users
    ADD CONSTRAINT fk_users_role FOREIGN KEY (role) REFERENCES roles(role_name)
    ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE TABLE IF NOT EXISTS patients (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    patient_no VARCHAR(20) NOT NULL,
    user_id BIGINT NULL,
    nic VARCHAR(20) NULL,
    full_name VARCHAR(100) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(10) NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(120) NULL,
    address VARCHAR(200) NULL,
    blood_group VARCHAR(5) NULL,
    guardian_name VARCHAR(100) NULL,
    guardian_phone VARCHAR(20) NULL,
    registration_notes VARCHAR(1000) NULL,
    consent_given BIT(1) NOT NULL DEFAULT b'0',
    consent_recorded_at DATETIME(6) NULL,
    consent_recorded_by VARCHAR(50) NULL,
    created_by_user_id BIGINT NULL,
    registered_on DATE NOT NULL,
    deactivated_at DATETIME(6) NULL,
    deactivated_by VARCHAR(50) NULL,
    deactivation_reason VARCHAR(300) NULL,
    CONSTRAINT uq_patients_patient_no UNIQUE (patient_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE patients
    ADD COLUMN IF NOT EXISTS nic VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS guardian_name VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS guardian_phone VARCHAR(20) NULL,
    ADD COLUMN IF NOT EXISTS registration_notes VARCHAR(1000) NULL,
    ADD COLUMN IF NOT EXISTS consent_given BIT(1) NOT NULL DEFAULT b'0',
    ADD COLUMN IF NOT EXISTS consent_recorded_at DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS consent_recorded_by VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS deactivated_at DATETIME(6) NULL,
    ADD COLUMN IF NOT EXISTS deactivated_by VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(300) NULL;

CREATE UNIQUE INDEX uq_patients_user ON patients(user_id);
CREATE UNIQUE INDEX uq_patients_nic ON patients(nic);
CREATE INDEX idx_patient_full_name ON patients(full_name);
CREATE INDEX idx_patient_phone ON patients(phone);
CREATE INDEX idx_patient_guardian_phone ON patients(guardian_phone);

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    ADD CONSTRAINT fk_patients_created_by FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE TABLE IF NOT EXISTS login_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(50) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    successful BIT(1) NOT NULL,
    failure_reason VARCHAR(100) NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(300) NULL,
    CONSTRAINT fk_login_event_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    INDEX idx_login_event_time (occurred_at),
    INDEX idx_login_event_user (user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS number_sequences (
    sequence_name VARCHAR(40) NOT NULL PRIMARY KEY,
    next_value BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO number_sequences (sequence_name, next_value)
SELECT 'PATIENT', COALESCE(MAX(CAST(SUBSTRING(patient_no, 3) AS UNSIGNED)), 0) + 1
FROM patients
ON DUPLICATE KEY UPDATE next_value = GREATEST(next_value, VALUES(next_value));
