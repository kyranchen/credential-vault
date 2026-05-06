package com.credentialvault.unit.crypto;

import com.credentialvault.crypto.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;
    private SecretKey key;

    @BeforeEach
    void setUp() throws Exception {
        encryptionService = new EncryptionService();
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        key = kg.generateKey();
    }

    @Test
    void roundtrip_produces_original_plaintext() {
        String plaintext = "super-secret-password-123!";

        EncryptionService.EncryptedData encrypted = encryptionService.encrypt(plaintext, key);
        String decrypted = encryptionService.decrypt(encrypted.ciphertext(), encrypted.iv(), key);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void ciphertext_is_not_plaintext() {
        String plaintext = "password";
        EncryptionService.EncryptedData encrypted = encryptionService.encrypt(plaintext, key);

        byte[] ciphertextBytes = Base64.getDecoder().decode(encrypted.ciphertext());
        assertThat(ciphertextBytes).doesNotContain(plaintext.getBytes());
    }

    @Test
    void each_encryption_produces_unique_iv() {
        EncryptionService.EncryptedData first = encryptionService.encrypt("password", key);
        EncryptionService.EncryptedData second = encryptionService.encrypt("password", key);

        assertThat(first.iv()).isNotEqualTo(second.iv());
    }

    @Test
    void each_encryption_produces_unique_ciphertext_for_same_plaintext() {
        EncryptionService.EncryptedData first = encryptionService.encrypt("password", key);
        EncryptionService.EncryptedData second = encryptionService.encrypt("password", key);

        // Same plaintext but different IVs must yield different ciphertexts
        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
    }

    @Test
    void tampered_ciphertext_throws_on_decrypt() {
        EncryptionService.EncryptedData encrypted = encryptionService.encrypt("password", key);

        // Flip the last byte of the ciphertext (corrupts the GCM auth tag)
        byte[] bytes = Base64.getDecoder().decode(encrypted.ciphertext());
        bytes[bytes.length - 1] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> encryptionService.decrypt(tampered, encrypted.iv(), key))
            .isInstanceOf(com.credentialvault.crypto.CryptoException.class)
            .hasMessageContaining("Decryption failed");
    }

    @Test
    void wrong_key_throws_on_decrypt() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey wrongKey = kg.generateKey();

        EncryptionService.EncryptedData encrypted = encryptionService.encrypt("password", key);

        assertThatThrownBy(() -> encryptionService.decrypt(encrypted.ciphertext(), encrypted.iv(), wrongKey))
            .isInstanceOf(com.credentialvault.crypto.CryptoException.class);
    }

    @Test
    void iv_is_96_bits() {
        EncryptionService.EncryptedData encrypted = encryptionService.encrypt("password", key);
        byte[] iv = Base64.getDecoder().decode(encrypted.iv());
        assertThat(iv).hasSize(12); // 96 bits = 12 bytes
    }
}
