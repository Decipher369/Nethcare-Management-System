-- NethCare report demonstration data for the local development database.
-- Run with: MYSQL_PWD=nethcare_pass mariadb -h 127.0.0.1 -P 3307 -u nethcare_user nethcare < scripts/seed-report-demo.sql
-- Every inserted business key starts with RPT-. Re-running inserts missing rows only.
-- This is sample data for the Sales, Orders and Stock reports, not production data.

START TRANSACTION;

CREATE TEMPORARY TABLE report_seed_patients (
    seq INT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL
);
INSERT INTO report_seed_patients VALUES
    (1, 'Demo Patient Amara Perera'), (2, 'Demo Patient Nimal Silva'),
    (3, 'Demo Patient Fathima Rahman'), (4, 'Demo Patient Kavindu Fernando'),
    (5, 'Demo Patient Malini Jayasinghe'), (6, 'Demo Patient Arjun Kumar'),
    (7, 'Demo Patient Tharushi De Silva'), (8, 'Demo Patient Ruwan Senanayake'),
    (9, 'Demo Patient Ishara Wijesinghe'), (10, 'Demo Patient Hiran Perera'),
    (11, 'Demo Patient Anjali Mendis'), (12, 'Demo Patient Sahan Gunasekara');

INSERT INTO patients
    (created_at, is_active, dob, full_name, patient_no, phone, registered_on,
     consent_given, registration_notes)
SELECT NOW(6), b'1', DATE_SUB(CURDATE(), INTERVAL (22 + p.seq * 3) YEAR),
       p.full_name, CONCAT('RPT-P-', LPAD(p.seq, 3, '0')),
       CONCAT('077900', LPAD(p.seq, 4, '0')), DATE_SUB(CURDATE(), INTERVAL 5 MONTH),
       b'1', 'DEMO REPORT DATA - synthetic patient'
FROM report_seed_patients p
WHERE NOT EXISTS (
    SELECT 1 FROM patients existing
    WHERE existing.patient_no = CONCAT('RPT-P-', LPAD(p.seq, 3, '0'))
);

CREATE TEMPORARY TABLE report_seed_stock (
    seq INT PRIMARY KEY,
    item_code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(30) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    reserved INT NOT NULL,
    reorder_level INT NOT NULL
);
INSERT INTO report_seed_stock VALUES
    (1, 'RPT-FR-01', 'Demo Classic Frame', 'FRAME', 12500.00, 18, 2, 5),
    (2, 'RPT-FR-02', 'Demo Titanium Frame', 'FRAME', 22500.00, 3, 1, 4),
    (3, 'RPT-FR-03', 'Demo Junior Frame', 'FRAME', 8500.00, 14, 1, 4),
    (4, 'RPT-SV-01', 'Demo Single Vision Lenses', 'SINGLE_VISION_LENS', 7500.00, 24, 3, 6),
    (5, 'RPT-SV-02', 'Demo Blue Filter Lenses', 'SINGLE_VISION_LENS', 11200.00, 5, 2, 5),
    (6, 'RPT-BF-01', 'Demo Bifocal Lenses', 'BIFOCAL_LENS', 18500.00, 9, 1, 3),
    (7, 'RPT-CL-01', 'Demo Monthly Contacts', 'CONTACT_LENS', 9200.00, 4, 1, 5),
    (8, 'RPT-CL-02', 'Demo Daily Contacts', 'CONTACT_LENS', 6800.00, 16, 2, 5),
    (9, 'RPT-CS-01', 'Demo Protective Case', 'CASE', 1800.00, 30, 0, 8),
    (10, 'RPT-CS-02', 'Demo Travel Case', 'CASE', 2500.00, 2, 0, 4);

INSERT INTO stock_items
    (created_at, is_active, item_code, name, brand, category, unit_price,
     quantity, reserved, reorder_level, description)
SELECT NOW(6), b'1', s.item_code, s.name, 'NethCare Demo', s.category,
       s.unit_price, s.quantity, s.reserved, s.reorder_level,
       'DEMO REPORT DATA - synthetic catalogue item'
FROM report_seed_stock s
WHERE NOT EXISTS (SELECT 1 FROM stock_items existing WHERE existing.item_code = s.item_code);

CREATE TEMPORARY TABLE report_seed_orders (
    seq INT PRIMARY KEY,
    patient_seq INT NOT NULL,
    stock_seq INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    month_offset INT NOT NULL,
    day_offset INT NOT NULL,
    quantity INT NOT NULL,
    payment_ratio DECIMAL(3,2) NOT NULL
);
INSERT INTO report_seed_orders VALUES
    (1, 1, 1, 'COLLECTED', 'NORMAL', 4, 4, 1, 1.00),
    (2, 2, 4, 'COLLECTED', 'NORMAL', 4, 15, 2, 1.00),
    (3, 3, 2, 'COLLECTED', 'URGENT', 3, 6, 1, 1.00),
    (4, 4, 9, 'COLLECTED', 'NORMAL', 3, 18, 2, 1.00),
    (5, 5, 6, 'COLLECTED', 'NORMAL', 2, 3, 1, 1.00),
    (6, 6, 3, 'COLLECTED', 'NORMAL', 2, 20, 1, 1.00),
    (7, 7, 7, 'COLLECTED', 'URGENT', 1, 7, 1, 1.00),
    (8, 8, 5, 'READY', 'NORMAL', 1, 16, 1, 0.40),
    (9, 9, 8, 'LAB', 'NORMAL', 1, 23, 2, 0.40),
    (10, 10, 1, 'PLACED', 'NORMAL', 0, 0, 1, 0.00),
    (11, 11, 4, 'LAB', 'NORMAL', 0, 1, 2, 0.40),
    (12, 12, 2, 'READY', 'URGENT', 0, 2, 1, 0.40),
    (13, 1, 3, 'COLLECTED', 'NORMAL', 0, 0, 1, 1.00),
    (14, 2, 6, 'COLLECTED', 'NORMAL', 0, 1, 1, 1.00),
    (15, 3, 9, 'CANCELLED', 'NORMAL', 0, 2, 1, 0.00),
    (16, 4, 7, 'PLACED', 'URGENT', 0, 0, 1, 0.00),
    (17, 5, 10, 'READY', 'NORMAL', 0, 1, 2, 0.40),
    (18, 6, 5, 'COLLECTED', 'NORMAL', 0, 2, 1, 1.00);

-- Current-month dates are capped at today, so the dashboard sees only elapsed dates.
INSERT INTO orders
    (created_at, is_active, order_no, patient_id, patient_no_snapshot,
     customer_name, customer_phone, priority, status, ordered_on,
     promised_on, ready_on, collected_on, collected_by, placed_by, remarks)
SELECT DATE_ADD(DATE_ADD(CAST(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL d.month_offset MONTH), '%Y-%m-01') AS DATE),
                     INTERVAL IF(d.month_offset = 0, LEAST(d.day_offset, DAY(CURDATE()) - 1), d.day_offset) DAY),
                INTERVAL 10 HOUR),
       b'1', CONCAT('RPT-ORD-', LPAD(d.seq, 3, '0')),
       p.id, p.patient_no, p.full_name, p.phone, d.priority, d.status,
       DATE_ADD(CAST(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL d.month_offset MONTH), '%Y-%m-01') AS DATE),
                INTERVAL IF(d.month_offset = 0, LEAST(d.day_offset, DAY(CURDATE()) - 1), d.day_offset) DAY),
       CASE WHEN d.status IN ('LAB','READY') AND d.seq IN (8,9,12) THEN DATE_SUB(CURDATE(), INTERVAL 2 DAY)
            ELSE DATE_ADD(CURDATE(), INTERVAL 7 DAY) END,
       CASE WHEN d.status IN ('READY','COLLECTED') THEN CURDATE() ELSE NULL END,
       CASE WHEN d.status = 'COLLECTED' THEN CURDATE() ELSE NULL END,
       CASE WHEN d.status = 'COLLECTED' THEN 'Demo staff' ELSE NULL END,
       'Demo staff', 'DEMO REPORT DATA - synthetic order'
FROM report_seed_orders d
JOIN patients p ON p.patient_no = CONCAT('RPT-P-', LPAD(d.patient_seq, 3, '0'))
WHERE NOT EXISTS (SELECT 1 FROM orders existing WHERE existing.order_no = CONCAT('RPT-ORD-', LPAD(d.seq, 3, '0')));

INSERT INTO order_items
    (created_at, is_active, order_id, stock_item_id, item_code, description, quantity, unit_price)
SELECT o.created_at, b'1', o.id, s.id, s.item_code, s.name, d.quantity, s.unit_price
FROM report_seed_orders d
JOIN orders o ON o.order_no = CONCAT('RPT-ORD-', LPAD(d.seq, 3, '0'))
JOIN report_seed_stock rs ON rs.seq = d.stock_seq
JOIN stock_items s ON s.item_code = rs.item_code
WHERE NOT EXISTS (SELECT 1 FROM order_items existing WHERE existing.order_id = o.id);

INSERT INTO bills
    (created_at, is_active, bill_no, cancelled, is_credit_note, discount,
     order_id, patient_id, frame_charges, lens_charges, other_charges,
     subtotal, surcharge, total, paid, payment_status)
SELECT DATE_ADD(o.created_at, INTERVAL 3 HOUR), b'1',
       CONCAT('RPT-INV-', LPAD(d.seq, 3, '0')),
       IF(d.status = 'CANCELLED', b'1', b'0'), b'0', 0.00,
       o.id, o.patient_id,
       IF(s.category = 'FRAME', s.unit_price * d.quantity, 0.00),
       IF(s.category IN ('SINGLE_VISION_LENS','BIFOCAL_LENS'), s.unit_price * d.quantity, 0.00),
       IF(s.category IN ('CONTACT_LENS','CASE'), s.unit_price * d.quantity, 0.00),
       s.unit_price * d.quantity,
       IF(d.priority = 'URGENT', ROUND(s.unit_price * d.quantity * 0.15, 2), 0.00),
       s.unit_price * d.quantity + IF(d.priority = 'URGENT', ROUND(s.unit_price * d.quantity * 0.15, 2), 0.00),
       ROUND((s.unit_price * d.quantity + IF(d.priority = 'URGENT', ROUND(s.unit_price * d.quantity * 0.15, 2), 0.00)) * d.payment_ratio, 2),
       CASE WHEN d.status = 'CANCELLED' THEN 'CANCELLED'
            WHEN d.payment_ratio = 1.00 THEN 'COMPLETED'
            WHEN d.payment_ratio > 0 THEN 'PARTIALLY_PAID'
            ELSE 'PENDING' END
FROM report_seed_orders d
JOIN orders o ON o.order_no = CONCAT('RPT-ORD-', LPAD(d.seq, 3, '0'))
JOIN report_seed_stock rs ON rs.seq = d.stock_seq
JOIN stock_items s ON s.item_code = rs.item_code
WHERE NOT EXISTS (SELECT 1 FROM bills existing WHERE existing.bill_no = CONCAT('RPT-INV-', LPAD(d.seq, 3, '0')));

INSERT INTO payments
    (created_at, is_active, bill_id, receipt_no, amount, method, is_advance, taken_by, note)
SELECT DATE_ADD(b.created_at, INTERVAL 1 HOUR), b'1', b.id,
       CONCAT('RPT-RCP-A-', LPAD(d.seq, 3, '0')),
       ROUND(b.total * 0.40, 2),
       IF(MOD(d.seq, 2) = 0, 'CARD', 'CASH'), b'1', 'Demo staff',
       'DEMO REPORT DATA - advance'
FROM report_seed_orders d
JOIN bills b ON b.bill_no = CONCAT('RPT-INV-', LPAD(d.seq, 3, '0'))
WHERE d.payment_ratio >= 0.40
  AND NOT EXISTS (SELECT 1 FROM payments existing WHERE existing.receipt_no = CONCAT('RPT-RCP-A-', LPAD(d.seq, 3, '0')));

INSERT INTO payments
    (created_at, is_active, bill_id, receipt_no, amount, method, is_advance, taken_by, note)
SELECT DATE_ADD(b.created_at, INTERVAL 2 HOUR), b'1', b.id,
       CONCAT('RPT-RCP-B-', LPAD(d.seq, 3, '0')),
       b.total - ROUND(b.total * 0.40, 2),
       IF(MOD(d.seq, 2) = 0, 'CASH', 'CARD'), b'0', 'Demo staff',
       'DEMO REPORT DATA - final balance'
FROM report_seed_orders d
JOIN bills b ON b.bill_no = CONCAT('RPT-INV-', LPAD(d.seq, 3, '0'))
WHERE d.payment_ratio = 1.00
  AND NOT EXISTS (SELECT 1 FROM payments existing WHERE existing.receipt_no = CONCAT('RPT-RCP-B-', LPAD(d.seq, 3, '0')));

DROP TEMPORARY TABLE report_seed_orders;
DROP TEMPORARY TABLE report_seed_stock;
DROP TEMPORARY TABLE report_seed_patients;

COMMIT;

SELECT 'demo patients' AS metric, COUNT(*) AS total FROM patients WHERE patient_no LIKE 'RPT-P-%'
UNION ALL SELECT 'demo stock items', COUNT(*) FROM stock_items WHERE item_code LIKE 'RPT-%'
UNION ALL SELECT 'demo orders', COUNT(*) FROM orders WHERE order_no LIKE 'RPT-ORD-%'
UNION ALL SELECT 'demo bills', COUNT(*) FROM bills WHERE bill_no LIKE 'RPT-INV-%'
UNION ALL SELECT 'demo payments', COUNT(*) FROM payments WHERE receipt_no LIKE 'RPT-RCP-%';
