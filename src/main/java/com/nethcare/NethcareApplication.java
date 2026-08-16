package com.nethcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nethcare Management System — SE2012 OOAD Group Project
 * Clinic management software for Neth Opticians, Kolonnawa.
 *
 * Modules:
 *   M1 — Patient Registration & History      (Gayathri)
 *   M2 — Examination, Prescription & Referral (Akshai)
 *   M3 — Order, Billing & Stock               (Shagash)
 *   M4 — Follow-up, Reporting & Audit         (Edwien)
 */
@SpringBootApplication
public class NethcareApplication {

    public static void main(String[] args) {
        SpringApplication.run(NethcareApplication.class, args);
    }
}
