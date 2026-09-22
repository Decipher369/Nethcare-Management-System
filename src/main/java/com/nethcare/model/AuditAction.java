package com.nethcare.model;

/**
 * What a person did to a record.
 *
 * DELETE is here for completeness of the trail even though nothing in the
 * system hard-deletes — the soft-delete flag is a DELETE action in the audit
 * sense, and having the value there means the report does not need a special
 * case for it.
 */
public enum AuditAction {

    CREATE,
    UPDATE,
    DELETE;

    public String label() {
        return this == CREATE ? "Create" : this == UPDATE ? "Update" : "Delete";
    }
}
