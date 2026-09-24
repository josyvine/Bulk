package com.bulk.app;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import dalvik.system.InMemoryDexClassLoader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class StubApplication extends Application {

    private static final String PAYLOAD_ASSET_NAME = "payload.bin";
    private static final String OPENSSL_MAGIC_HEADER = "Salted__";
    private static final String ENCRYPTION_PASSPHRASE = "Bulk_Master_Secret_2026";
    private static final int PBKDF2_ITERATION_COUNT = 10000;
    private static final int DERIVED_KEY_AND_IV_LENGTH_BITS = 384; // 32 bytes Key + 16 bytes IV = 48 bytes * 8

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        loadAndInjectEncryptedDex(base);
    }

    private void loadAndInjectEncryptedDex(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            String[] assets = assetManager.list("");
            if (assets == null) {
                return;
            }

            boolean hasPayload = false;
            for (String asset : assets) {
                if (PAYLOAD_ASSET_NAME.equals(asset)) {
                    hasPayload = true;
                    break;
                }
            }

            if (!hasPayload) {
                // If payload.bin is absent (e.g. running unencrypted debug build), do not interrupt
                return;
            }

            byte[] rawBytes;
            try (InputStream inputStream = assetManager.open(PAYLOAD_ASSET_NAME);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                rawBytes = outputStream.toByteArray();
            }

            // Minimum length check: 8 bytes OpenSSL header + 8 bytes salt + at least 16 bytes AES block
            if (rawBytes == null || rawBytes.length <= 32) {
                return;
            }

            // Verify OpenSSL magic header "Salted__"
            String magicHeader = new String(rawBytes, 0, 8, StandardCharsets.US_ASCII);
            if (!OPENSSL_MAGIC_HEADER.equals(magicHeader)) {
                return;
            }

            // Extract 8-byte salt
            byte[] salt = new byte[8];
            System.arraycopy(rawBytes, 8, salt, 0, 8);

            // Extract ciphertext
            int cipherTextLength = rawBytes.length - 16;
            byte[] cipherText = new byte[cipherTextLength];
            System.arraycopy(rawBytes, 16, cipherText, 0, cipherTextLength);

            // PBKDF2WithHmacSHA256 key derivation matching OpenSSL -pbkdf2
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(
                    ENCRYPTION_PASSPHRASE.toCharArray(),
                    salt,
                    PBKDF2_ITERATION_COUNT,
                    DERIVED_KEY_AND_IV_LENGTH_BITS
            );
            byte[] derivedBytes = factory.generateSecret(spec).getEncoded();

            byte[] aesKeyBytes = new byte[32];
            byte[] ivBytes = new byte[16];
            System.arraycopy(derivedBytes, 0, aesKeyBytes, 0, 32);
            System.arraycopy(derivedBytes, 32, ivBytes, 0, 16);

            SecretKeySpec secretKey = new SecretKeySpec(aesKeyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            // In-memory decryption (never written to disk or storage)
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decryptedDexBytes = cipher.doFinal(cipherText);

            // Load decrypted DEX directly from RAM
            ByteBuffer dexByteBuffer = ByteBuffer.wrap(decryptedDexBytes);

            ClassLoader baseClassLoader = context.getClassLoader();
            Field parentField = ClassLoader.class.getDeclaredField("parent");
            parentField.setAccessible(true);

            ClassLoader originalParent = (ClassLoader) parentField.get(baseClassLoader);
            InMemoryDexClassLoader inMemoryDexClassLoader = new InMemoryDexClassLoader(dexByteBuffer, originalParent);

            // Inject InMemoryDexClassLoader into parent chain
            parentField.set(baseClassLoader, inMemoryDexClassLoader);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}