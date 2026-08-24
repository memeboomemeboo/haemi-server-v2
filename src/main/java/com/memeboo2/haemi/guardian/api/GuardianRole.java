package com.memeboo2.haemi.guardian.api;

public enum GuardianRole {
    GUARDIAN("보호자"),
    DAUGHTER("딸"),
    SON("아들"),
    GRANDDAUGHTER("손녀"),
    GRANDSON("손자"),
    OTHER("기타");

    private final String label;

    GuardianRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
