package com.credentialvault.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MVP key provider backed by an env-var-supplied AES key.
 * Replace with AzureKeyVaultKeyProvider for production.
 *
 * Required env var: VAULT_MASTER_KEY — base64-encoded 32-byte AES-256 key.
 * Generate: openssl rand -base64 32
 */
@Service
public class LocalKeyProvider implements KeyProvider {

    @Value("${vault.local.master-key}")
    private String masterKeyBase64;

    @Value("${vault.local.key-reference}")
    private String keyReference;

    private final Map<String, SecretKey> keyStore = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                "VAULT_MASTER_KEY must be a 32-byte (256-bit) AES key. Got " + keyBytes.length + " bytes."
            );
        }
        keyStore.put(keyReference, new SecretKeySpec(keyBytes, "AES"));
    }

    @Override
    public SecretKey getKey(String ref) {
        SecretKey key = keyStore.get(ref);
        if (key == null) {
            throw new CryptoException("Unknown key reference: " + ref);
        }
        return key;
    }

    @Override
    public String getLatestKeyReference() {
        return keyReference;
    }
}
