package com.bnfix.ubm.shared.security;

public final class TenantContext {
    private static final ThreadLocal<Values> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long companyId, String email, String accountId) {
        CURRENT.set(new Values(companyId, email, accountId));
    }

    public static Values require() {
        Values value = CURRENT.get();
        if (value == null) throw new IllegalStateException("No authenticated tenant context");
        return value;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Values(Long companyId, String email, String accountId) {}
}
