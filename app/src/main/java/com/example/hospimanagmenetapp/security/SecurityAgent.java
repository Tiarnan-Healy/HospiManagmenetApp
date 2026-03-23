package com.example.hospimanagmenetapp.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;


// Agentic Security class providing AES-256/GCM encryption and decryption
// for Protected Health Information (PHI)
// COMPLIANCE:- AES-256/GCM satisfies NHS DSP Toolkit encryption requirements
// - Android Keystore key storage satisfies UK GDPR Article 32

public class SecurityAgent {

    private static final String TAG              = "SecurityAgent";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS        = "PatientDataKey";
    private static final String TRANSFORMATION   = "AES/GCM/NoPadding";

//     Constructor generates the AES key in the Android Keystore if it
//     does not already exist. Safe to call multiple times — key is only
//     generated once and persisted across app restarts.

    public SecurityAgent() throws Exception {
        generateKeyIfNeeded();
    }

//     Generates an AES-256 key in the Android Keystore.
//     setRandomizedEncryptionRequired(true) forces a new random IV for
//     every encryption — identical plaintexts produce different ciphertexts.

    private void generateKeyIfNeeded() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build();

            keyGenerator.init(keySpec);
            keyGenerator.generateKey();
            Log.i(TAG, "AES-256 key generated and stored in Android Keystore.");
        }
    }


//    Retrieves the AES key from the Android Keystore.
//    The raw key bytes are never accessible — only the key object is returned.

    private SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return ((KeyStore.SecretKeyEntry)
                keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

//     Encrypts a plaintext string using AES-256/GCM.
//     The IV is prepended to the ciphertext and the combined bytes are
//     Base64-encoded for safe string storage in Room.

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;

        try {
            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] iv          = cipher.getIV();
            byte[] cipherBytes = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext — needed for decryption
            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.encodeToString(combined, Base64.DEFAULT);

        } catch (Exception e) {
            // AGENTIC: Log alert — do not expose exception detail
            Log.e(TAG, "SECURITY ALERT: Encryption failed — PHI not written.");
            return null;
        }
    }

//     Decrypts a Base64-encoded AES-256/GCM ciphertext string.
//     AGENTIC TAMPERING DETECTION:
//     GCM appends an authentication tag during encryption. If the stored
//     ciphertext has been modified, decryption throws AEADBadTagException —
//     caught separately so tampering is logged as a distinct security alert.
//     @param base64CipherText the encrypted string from Room
//     @return decrypted plaintext, or null if decryption or auth fails

    public String decrypt(String base64CipherText) {
        if (base64CipherText == null || base64CipherText.isEmpty())
            return base64CipherText;

        try {
            byte[] combined    = Base64.decode(base64CipherText, Base64.DEFAULT);
            byte[] iv          = Arrays.copyOfRange(combined, 0, 12);
            byte[] cipherBytes = Arrays.copyOfRange(combined, 12, combined.length);

            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);

        } catch (AEADBadTagException authEx) {
            // AGENTIC ALERT: GCM tag mismatch — ciphertext has been modified
            Log.w(TAG, "TAMPERING ALERT: GCM authentication tag invalid — "
                    + "possible data tampering detected.");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "SECURITY ALERT: Decryption failed.");
            return null;
        }
    }
}
