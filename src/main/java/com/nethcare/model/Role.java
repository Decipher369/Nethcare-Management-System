package com.nethcare.model;

/**
 * User roles for Nethcare role-based access control.
 *
 * Access rules from requirements deck:
 *   ADMIN    — Users, roles, pricing, stock, reports, settings (all modules)
 *   OPTICIAN — Register patients, examinations, prescriptions, referrals (M1, M2)
 *   STAFF    — Orders, bills, order status, stock (M3)
 *   SURGEON  — Referred patients, surgical notes (M2)
 *   PATIENT  — Own profile, prescriptions, order status (M1, M4 — read-only)
 */
public enum Role {
    ADMIN,
    OPTICIAN,
    STAFF,
    SURGEON,
    PATIENT
}
