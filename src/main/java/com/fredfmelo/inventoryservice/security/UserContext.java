package com.fredfmelo.inventoryservice.security;

import java.util.UUID;

public record UserContext(UUID userId, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isSeller() {
        return role == Role.SELLER;
    }

    public boolean hasRole(Role role) {
        return this.role == role;
    }
}
