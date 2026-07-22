package com.medibridge.common.enums;

/**
 * Doctors self-register into PENDING and stay unusable until an admin approves
 * them - the admin dashboard's "Doctor account approved" activity entry.
 * Patients and admins never use PENDING.
 */
public enum AccountStatus {
    PENDING("pending"),
    ACTIVE("active"),
    INACTIVE("inactive"),
    SUSPENDED("suspended");

    private final String dbValue;

    AccountStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public boolean canLogIn() {
        return this == ACTIVE;
    }
}
