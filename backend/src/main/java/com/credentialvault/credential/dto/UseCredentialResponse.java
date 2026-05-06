package com.credentialvault.credential.dto;

/**
 * Contains plaintext credentials. This object must NEVER be logged.
 * The extension clears the password from memory immediately after autofill.
 */
public record UseCredentialResponse(String username, String password) {}
