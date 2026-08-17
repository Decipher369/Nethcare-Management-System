package com.nethcare.model;

/**
 * Roles that can log in. Each one gets a different slice of the system.
 *
 * ADMIN       everything
 * OPTICIAN    patients, exams, prescriptions, referrals
 * STAFF_NURSE orders, bills, stock
 * SURGEON     referrals and their own notes
 * PATIENT     own records only
 */
public enum Role {
    ADMIN,
    OPTICIAN,
    STAFF_NURSE,
    SURGEON,
    PATIENT
}
