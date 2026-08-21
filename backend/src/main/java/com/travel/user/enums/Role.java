package com.travel.user.enums;

public enum Role {
    GUEST,
    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
