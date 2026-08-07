package com.bnfix.ubm.domains.auth;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException() {
        super("Invalid email or password");
    }
}
