package com.credentialvault.crypto;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;    // 96-bit IV — NIST recommendation for GCM
    private static final int TAG_LENGTH_BITS = 128;   // 128-bit authentication tag

    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptedData encrypt(String plaintext, SecretKey key) {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            // doFinal returns ciphertext || 16-byte GCM auth tag (appended automatically by Java)
            byte[] ciphertextWithTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptedData(
                Base64.getEncoder().encodeToString(ciphertextWithTag),
                Base64.getEncoder().encodeToString(iv)
            );
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    public String decrypt(String encryptedBase64, String ivBase64, SecretKey key) {
        try {
            byte[] ciphertextWithTag = Base64.getDecoder().decode(encryptedBase64);
            byte[] iv = Base64.getDecoder().decode(ivBase64);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            // AEADBadTagException is thrown here if the ciphertext was tampered with
            byte[] plaintext = cipher.doFinal(ciphertextWithTag);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoException("Decryption failed", e);
        }
    }

    public record EncryptedData(String ciphertext, String iv) {}
}
