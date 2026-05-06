package com.credentialvault.config;

/**
 * Always maps to HTTP 403. Used for: credential not found, wrong org, no team access.
 * Deliberately opaque — callers cannot distinguish "does not exist" from "exists but denied."
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException() {
        super("Access denied");
    }
}
