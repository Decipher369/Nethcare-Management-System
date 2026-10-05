-- Complete the application schema from the JPA entity mappings.
-- V1 already owns roles, users, patients, login_events, and number_sequences.
-- Hibernate is validation-only; all DDL must be versioned here.

create table bills (
        cancelled bit not null,
        discount decimal(12,2) not null,
        follow_up_on date,
        frame_charges decimal(12,2) not null,
        is_active bit not null,
        is_credit_note bit not null,
        lens_charges decimal(12,2) not null,
        other_charges decimal(12,2) not null,
        paid decimal(12,2) not null,
        subtotal decimal(12,2) not null,
        surcharge decimal(12,2) not null,
        total decimal(12,2) not null,
        created_at datetime(6) not null,
        examination_id bigint,
        id bigint not null auto_increment,
        order_id bigint not null,
        patient_id bigint not null,
        updated_at datetime(6),
        bill_no varchar(30) not null,
        credit_note_no varchar(30),
        payment_status enum ('PENDING','PARTIALLY_PAID','COMPLETED','CANCELLED') not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table clinical_advice_logs (
        high_risk bit not null,
        clinician_id bigint not null,
        id bigint not null auto_increment,
        patient_id bigint not null,
        recorded_at_utc datetime(6) not null,
        visit_id bigint not null,
        previous_hash varchar(64),
        record_hash varchar(64) not null,
        caution_advice varchar(1000),
        advice_text tinytext not null,
        diagnostic_inputs tinytext not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table clinical_audit_log (
        actor_id bigint,
        id bigint not null auto_increment,
        occurred_at datetime(6) not null,
        entity_id varchar(40),
        actor varchar(50) not null,
        entity_name varchar(60) not null,
        previous_hash varchar(64),
        record_hash varchar(64) not null,
        note varchar(200),
        new_value varchar(500),
        old_value varchar(500),
        action VARCHAR(10) not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table clinical_symptoms (
        blur_vision bit not null,
        burning bit not null,
        discharge bit not null,
        distortion bit not null,
        double_vision bit not null,
        dry bit not null,
        flashes bit not null,
        floaters bit not null,
        glare bit not null,
        headache bit not null,
        is_active bit not null,
        itching bit not null,
        pain bit not null,
        photophobia bit not null,
        redness bit not null,
        squint bit not null,
        strain bit not null,
        tearing bit not null,
        created_at datetime(6) not null,
        examination_id bigint not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        additional_symptoms varchar(2000),
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table contact_lens_trackers (
        alert_sent bit not null,
        dispensed_date date not null,
        is_active bit not null,
        lens_modality_days integer not null,
        replacement_date date not null,
        bill_id bigint not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        patient_id bigint not null,
        updated_at datetime(6),
        lens_brand varchar(100) not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table examinations (
        exam_date date not null,
        is_active bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        patient_id bigint not null,
        updated_at datetime(6),
        base_curve varchar(10),
        diameter varchar(10),
        ipd varchar(10),
        od_add varchar(10),
        od_axis varchar(10),
        od_cyl varchar(10),
        od_sph varchar(10),
        od_va varchar(10),
        os_add varchar(10),
        os_axis varchar(10),
        os_cyl varchar(10),
        os_sph varchar(10),
        os_va varchar(10),
        examined_by varchar(100) not null,
        findings varchar(1000),
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table frames (
        is_active bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        stock_item_id bigint not null,
        updated_at datetime(6),
        size varchar(20),
        colour varchar(30),
        brand varchar(50),
        model varchar(50) not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table lenses (
        anti_reflective_coat bit not null,
        diameter decimal(4,1),
        is_active bit not null,
        uv_coat bit not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        stock_item_id bigint not null,
        updated_at datetime(6),
        colour varchar(30),
        lens_type varchar(50) not null,
        segment varchar(50),
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table notification_dispatches (
        is_active bit not null,
        retry_count integer not null,
        created_at datetime(6) not null,
        delivered_at datetime(6),
        follow_up_id bigint,
        id bigint not null auto_increment,
        last_attempt_at datetime(6),
        order_id bigint,
        patient_id bigint,
        recipient_user_id bigint,
        scheduled_for datetime(6),
        sent_at datetime(6),
        updated_at datetime(6),
        reference varchar(20) not null,
        gateway_receipt_id varchar(100),
        destination varchar(120),
        patient_name varchar(120),
        failure_reason varchar(200),
        skipped_reason varchar(200),
        message varchar(1000) not null,
        channel VARCHAR(10) not null,
        notification_type enum ('WEEKLY_PATIENT_LIST','PATIENT_SMS','VISIT_REMINDER','SPECIAL_CASE_FOLLOWUP','ORDER_READY') not null,
        recipient_role enum ('PATIENT','OPTICIAN','BOTH') not null,
        status VARCHAR(20) not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table order_items (
        is_active bit not null,
        quantity integer not null,
        unit_price decimal(12,2) not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        order_id bigint not null,
        stock_item_id bigint not null,
        updated_at datetime(6),
        item_code varchar(40) not null,
        description varchar(200) not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table order_status_history (
        is_active bit not null,
        changed_at datetime(6) not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        order_id bigint not null,
        updated_at datetime(6),
        changed_by varchar(80) not null,
        note varchar(500),
        new_status enum ('PLACED','LAB','READY','COLLECTED','CANCELLED') not null,
        previous_status enum ('PLACED','LAB','READY','COLLECTED','CANCELLED'),
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table orders (
        collected_on date,
        is_active bit not null,
        ordered_on date not null,
        promised_on date,
        ready_on date,
        created_at datetime(6) not null,
        examination_id bigint,
        frame_id bigint,
        id bigint not null auto_increment,
        lens_id bigint,
        patient_id bigint not null,
        prescription_id bigint,
        updated_at datetime(6),
        od_add varchar(10),
        od_axis varchar(10),
        od_cyl varchar(10),
        od_sph varchar(10),
        os_add varchar(10),
        os_axis varchar(10),
        os_cyl varchar(10),
        os_sph varchar(10),
        pupillary_distance varchar(10),
        patient_no_snapshot varchar(20) not null,
        customer_phone varchar(30),
        lens_type varchar(30),
        order_no varchar(30) not null,
        prescription_no_snapshot varchar(30),
        coating varchar(60),
        collected_by varchar(80),
        placed_by varchar(80),
        customer_name varchar(120) not null,
        remarks varchar(500),
        priority enum ('NORMAL','URGENT') not null,
        status enum ('PLACED','LAB','READY','COLLECTED','CANCELLED') not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table patient_followup_cases (
        booked_on date,
        is_active bit not null,
        is_high_risk bit not null,
        last_exam_on date,
        opt_out bit not null,
        responded_on date,
        target_review_date date not null,
        assigned_optician_id bigint not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        originating_visit_id bigint not null,
        patient_id bigint not null,
        updated_at datetime(6),
        phone varchar(30),
        email varchar(120),
        patient_name varchar(120),
        clinical_notes varchar(1000),
        case_category enum ('ROUTINE_REVIEW','REASSESSMENT','POST_OPERATIVE','CORNEAL_ULCER','PEDIATRIC_AMBLYOPIA') not null,
        outcome VARCHAR(20),
        status VARCHAR(20) not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table patient_medical_history (
        arthritis bit not null,
        asthma bit not null,
        bronchitis bit not null,
        cancer bit not null,
        cardiac bit not null,
        cholesterol bit not null,
        diabetic bit not null,
        hypertension bit not null,
        is_active bit not null,
        migraine bit not null,
        recorded_on date not null,
        renal bit not null,
        sle bit not null,
        syphilis bit not null,
        tb bit not null,
        thyroid bit not null,
        version_number integer not null,
        created_at datetime(6) not null,
        examination_id bigint not null,
        id bigint not null auto_increment,
        patient_id bigint not null,
        updated_at datetime(6),
        recorded_by varchar(100) not null,
        ocular_history varchar(1000),
        other_conditions varchar(2000),
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table payments (
        amount decimal(12,2) not null,
        is_active bit not null,
        is_advance bit not null,
        bill_id bigint not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        receipt_no varchar(30) not null,
        taken_by varchar(80),
        note varchar(200),
        method enum ('CASH','CARD') not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table prescriptions (
        is_active bit not null,
        issued_on date not null,
        created_at datetime(6) not null,
        examination_id bigint not null,
        id bigint not null auto_increment,
        patient_id bigint not null,
        updated_at datetime(6),
        ipd varchar(10),
        od_add varchar(10),
        od_axis varchar(10),
        od_cyl varchar(10),
        od_sph varchar(10),
        os_add varchar(10),
        os_axis varchar(10),
        os_cyl varchar(10),
        os_sph varchar(10),
        rx_no varchar(20) not null,
        issued_by varchar(100) not null,
        notes varchar(500),
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table referrals (
        feedback_on date,
        is_active bit not null,
        referred_on date not null,
        created_at datetime(6) not null,
        examination_id bigint,
        id bigint not null auto_increment,
        patient_id bigint not null,
        updated_at datetime(6),
        urgency varchar(10) not null,
        ref_no varchar(20) not null,
        status varchar(20) not null,
        referred_by varchar(100) not null,
        surgeon_name varchar(100) not null,
        diagnosis varchar(500),
        reason varchar(1000) not null,
        feedback varchar(2000),
        follow_up_instructions varchar(2000),
        tests varchar(2000),
        treatment varchar(2000),
        attached_history varchar(4000),
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table stock_items (
        expires_on date,
        is_active bit not null,
        quantity integer not null,
        reorder_level integer not null,
        reserved integer not null,
        unit_price decimal(12,2) not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        updated_at datetime(6),
        item_code varchar(40) not null,
        brand varchar(80),
        name varchar(120) not null,
        image_name varchar(200),
        description varchar(500),
        category enum ('FRAME','SINGLE_VISION_LENS','BIFOCAL_LENS','CONTACT_LENS','CASE') not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table stock_movements (
        is_active bit not null,
        quantity_after integer not null,
        quantity_change integer not null,
        reserved_after integer not null,
        created_at datetime(6) not null,
        id bigint not null auto_increment,
        order_id bigint,
        recorded_at datetime(6) not null,
        stock_item_id bigint not null,
        updated_at datetime(6),
        recorded_by varchar(80) not null,
        reason varchar(500),
        movement_type enum ('RECEIPT','RESERVATION','RELEASE','DISPENSING','ADJUSTMENT') not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create table system_summary_reports (
        is_active bit not null,
        period_end date not null,
        period_start date not null,
        total_collected decimal(12,2) not null,
        total_outstanding decimal(12,2) not null,
        total_revenue decimal(12,2) not null,
        total_stock_value decimal(12,2) not null,
        created_at datetime(6) not null,
        generated_by_staff_id bigint not null,
        id bigint not null auto_increment,
        total_attended_patients bigint not null,
        total_orders_pending bigint not null,
        updated_at datetime(6),
        breakdown_payload tinytext,
        report_type enum ('SALES','ATTENDED_PATIENTS','ORDER_STATUS','STOCK_DETAILS') not null,
        primary key (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

create index idx_bill_patient
       on bills (patient_id);

create index idx_bill_payment_status
       on bills (payment_status);

alter table bills
       add constraint UK_575bqi2iwedxvaiwo0mbg60cp unique (order_id);

alter table bills
       add constraint UK_q4wh447kcm2egle401dysyfwh unique (bill_no);

alter table clinical_advice_logs
       add constraint UK_4g7cpf6b6rgflthkil086805w unique (record_hash);

alter table clinical_audit_log
       add constraint UK_87is5y8doa91osdth2sabjp9x unique (record_hash);

alter table clinical_symptoms
       add constraint UK_ni483vedl3gbjwuqou15dc07t unique (examination_id);

create index idx_contact_tracker_patient
       on contact_lens_trackers (patient_id);

create index idx_contact_tracker_replacement
       on contact_lens_trackers (alert_sent, replacement_date);

alter table frames
       add constraint UK_9evmr3y7bh9f2kbl4eoac9imt unique (stock_item_id);

alter table lenses
       add constraint UK_exkx9f9td97jus7vkw7le6i4t unique (stock_item_id);

create index idx_order_history_order_time
       on order_status_history (order_id, changed_at);

create index idx_order_patient
       on orders (patient_id);

create index idx_order_status
       on orders (status);

create index idx_order_prescription
       on orders (prescription_id);

alter table orders
       add constraint UK_g8pohnngqi5x1nask7nff2u7w unique (order_no);

alter table patient_medical_history
       add constraint UK_3sd6n14vt7xwblod6kqrb7g8k unique (examination_id);

alter table prescriptions
       add constraint UK_eu718d0jtc9387g5pdmgejh2n unique (rx_no);

alter table referrals
       add constraint UK_9f9g3v2lcp9gli5a1g74hpi2v unique (ref_no);

alter table stock_items
       add constraint UK_cwr2wuya1xvjjd4i7tv1ooyxt unique (item_code);

create index idx_stock_movement_item_time
       on stock_movements (stock_item_id, recorded_at);

create index idx_stock_movement_order
       on stock_movements (order_id);

alter table bills
       add constraint FKcw75i5slwydtokrrqg2lmckvd
       foreign key (examination_id)
       references examinations (id);

alter table bills
       add constraint FK2s1iwv6bgsmh8u9awhdd1aela
       foreign key (order_id)
       references orders (id);

alter table bills
       add constraint FKiklkhnj1odoll0m9otela7gb9
       foreign key (patient_id)
       references patients (id);

alter table contact_lens_trackers
       add constraint FK4mbilslfc4mrcpr1tohnujfpn
       foreign key (bill_id)
       references bills (id);

alter table contact_lens_trackers
       add constraint FKba0gx1bdmsos6i3obwver9d3p
       foreign key (patient_id)
       references patients (id);

alter table frames
       add constraint FKn5s6uiglt4qcvj5fvtbhbmkjh
       foreign key (stock_item_id)
       references stock_items (id);

alter table lenses
       add constraint FKt8fhbxrp3dnlvqoyg2nhwdp5v
       foreign key (stock_item_id)
       references stock_items (id);

alter table order_items
       add constraint FKbioxgbv59vetrxe0ejfubep1w
       foreign key (order_id)
       references orders (id);

alter table order_status_history
       add constraint FKnmcbg3mmbt8wfva97ra40nmp3
       foreign key (order_id)
       references orders (id);

alter table orders
       add constraint FKa6ka3f5bv5xc6op7r34mb997p
       foreign key (examination_id)
       references examinations (id);

alter table orders
       add constraint FKem58elw9nqqjjr71dfslyucux
       foreign key (frame_id)
       references frames (id);

alter table orders
       add constraint FKtpmfqq54c83nhx3127cammlya
       foreign key (lens_id)
       references lenses (id);

alter table orders
       add constraint FK5mb8btcy61iok4soyik0tqenj
       foreign key (patient_id)
       references patients (id);

alter table orders
       add constraint FK6yximlmk5fny8k1y7nqrrkkjj
       foreign key (prescription_id)
       references prescriptions (id);

alter table stock_movements
       add constraint FK82mrlg9h36kaw5kn90fliqu0b
       foreign key (order_id)
       references orders (id);

alter table stock_movements
       add constraint FKnpt24ujdtowfdohxkvop7mewp
       foreign key (stock_item_id)
       references stock_items (id);
-- M4 indexes and relationships represented by scalar IDs in the JPA entities.
CREATE INDEX idx_followup_status_date ON patient_followup_cases (status, target_review_date);
CREATE INDEX idx_followup_patient ON patient_followup_cases (patient_id);
CREATE INDEX idx_dispatch_queue ON notification_dispatches (status, scheduled_for);
CREATE INDEX idx_dispatch_patient ON notification_dispatches (patient_id);
CREATE INDEX idx_audit_occurred ON clinical_audit_log (occurred_at);
CREATE INDEX idx_audit_entity ON clinical_audit_log (entity_name, entity_id);
CREATE INDEX idx_advice_patient_time ON clinical_advice_logs (patient_id, recorded_at_utc);

ALTER TABLE patient_followup_cases
    ADD CONSTRAINT fk_followup_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_followup_optician FOREIGN KEY (assigned_optician_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_followup_visit FOREIGN KEY (originating_visit_id) REFERENCES examinations(id) ON UPDATE CASCADE ON DELETE RESTRICT;
ALTER TABLE notification_dispatches
    ADD CONSTRAINT fk_dispatch_followup FOREIGN KEY (follow_up_id) REFERENCES patient_followup_cases(id) ON UPDATE CASCADE ON DELETE CASCADE,
    ADD CONSTRAINT fk_dispatch_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_dispatch_user FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE SET NULL,
    ADD CONSTRAINT fk_dispatch_order FOREIGN KEY (order_id) REFERENCES orders(id) ON UPDATE CASCADE ON DELETE SET NULL;
ALTER TABLE clinical_audit_log
    ADD CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT;
ALTER TABLE clinical_advice_logs
    ADD CONSTRAINT fk_advice_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_advice_clinician FOREIGN KEY (clinician_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_advice_visit FOREIGN KEY (visit_id) REFERENCES examinations(id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT chk_high_risk_caution CHECK (high_risk = FALSE OR caution_advice IS NOT NULL);

-- Append-only evidence tables.
CREATE TRIGGER clinical_audit_log_no_update BEFORE UPDATE ON clinical_audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'clinical_audit_log is append-only';
CREATE TRIGGER clinical_audit_log_no_delete BEFORE DELETE ON clinical_audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'clinical_audit_log is append-only';
CREATE TRIGGER clinical_advice_log_no_update BEFORE UPDATE ON clinical_advice_logs
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'clinical_advice_logs is append-only';
CREATE TRIGGER clinical_advice_log_no_delete BEFORE DELETE ON clinical_advice_logs
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'clinical_advice_logs is append-only';
