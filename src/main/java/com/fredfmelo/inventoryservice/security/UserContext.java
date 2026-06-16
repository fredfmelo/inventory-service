package com.fredfmelo.inventoryservice.security;

import java.util.List;
import java.util.UUID;

public record UserContext(UUID userId, List<String> roles) {

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }

    public boolean isSeller() {
        return roles.contains("SELLER");
    }
}
