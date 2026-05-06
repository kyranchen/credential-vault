package com.credentialvault.crypto;

import javax.crypto.SecretKey;

/**
 * Abstraction over the key store. MVP implementation uses a local in-memory key.
 * Swap to AzureKeyProvider when ready (see vault.local.* config for the MVP version).
 */
public interface KeyProvider {
    SecretKey getKey(String keyReference);
    String getLatestKeyReference();
}
