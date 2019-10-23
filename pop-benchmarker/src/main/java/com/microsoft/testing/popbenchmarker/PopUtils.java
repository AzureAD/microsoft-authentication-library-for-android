//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.testing.popbenchmarker;

import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.nimbusds.jose.jwk.RSAKey;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.security.auth.x500.X500Principal;

public class PopUtils {

    public static class KeyPairGeneratorAlgorithms {
        static final String
                DH = "DiffieHellman", // note: key.getAlgorithm() returns 'DH'
                DSA = "DSA", // Generates keypairs for the Digital Signature Algorithm.
                RSA = "RSA", // Generates keypairs for the RSA algorithm (Signature/Cipher).
                EC = "EC"; // Generates keypairs for the Elliptic Curve algorithm.
    }

    static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    static final String KEYSTORE_ALIAS = "Device-PoP";

    // Self-signed certificate properties
    static final int CERTIFICATE_VALIDITY_YEARS = 99;
    static final BigInteger SERIAL_NUMBER = BigInteger.ONE;
    static final String COMMON_NAME = "CN=PoP";

    public static KeyPairGenerator getInitializedRsaKeyPairGenerator(final Context context)
            throws UnsupportedOperationException {
        final String alg = KeyPairGeneratorAlgorithms.RSA;
        final int keySize = 2048;

        KeyPairGenerator keyPairGenerator;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                keyPairGenerator = getKeyPairGenerator(alg);
            } catch (final NoSuchProviderException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException(
                        "Failed to locate Provider: "
                                + ANDROID_KEYSTORE
                                + " for alg: " + alg
                );
            } catch (final NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException(
                        "Failed to locate algorithm: "
                                + alg
                                + " for Provider: "
                                + ANDROID_KEYSTORE
                );
            }
        } else {
            throw new UnsupportedOperationException("API 18 is required for PoP usage.");
        }

        try {
            initialize(context, keyPairGenerator, keySize);
        } catch (final InvalidAlgorithmParameterException e) {
            e.printStackTrace();
            // TODO catch / handle
        }

        return keyPairGenerator;
    }

    private static void initialize(@NonNull final Context context,
                                   @NonNull final KeyPairGenerator keyPairGenerator,
                                   final int keySize) throws InvalidAlgorithmParameterException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            initializePre23(context, keyPairGenerator, keySize);
        } else {
            initialize23(keyPairGenerator, keySize);
        }
    }

    private static void initializePre23(@NonNull final Context context,
                                        @NonNull final KeyPairGenerator keyPairGenerator,
                                        final int keySize) throws InvalidAlgorithmParameterException {
        final Calendar calendar = Calendar.getInstance();
        final Date start = getNow(calendar);
        calendar.add(Calendar.YEAR, CERTIFICATE_VALIDITY_YEARS);
        final Date end = calendar.getTime();

        final KeyPairGeneratorSpec.Builder specBuilder = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEYSTORE_ALIAS)
                .setStartDate(start)
                .setEndDate(end)
                .setSerialNumber(SERIAL_NUMBER)
                .setSubject(new X500Principal(COMMON_NAME));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            specBuilder.setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(keySize, RSAKeyGenParameterSpec.F4));
        }

        final KeyPairGeneratorSpec spec = specBuilder.build();
        keyPairGenerator.initialize(spec);
    }

    private static Date getNow(@NonNull final Calendar calendar) {
        return calendar.getTime();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void initialize23(KeyPairGenerator keyPairGenerator, int keySize)
            throws InvalidAlgorithmParameterException {
        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_SIGN
                        | KeyProperties.PURPOSE_VERIFY
                        | KeyProperties.PURPOSE_ENCRYPT
                        | KeyProperties.PURPOSE_DECRYPT
        )
                .setKeySize(keySize)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setDigests(KeyProperties.DIGEST_SHA256);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder = applyHardwareIsolation(builder);
        }

        final AlgorithmParameterSpec spec = builder.build();
        keyPairGenerator.initialize(spec);
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @NonNull
    private static KeyGenParameterSpec.Builder applyHardwareIsolation(
            @NonNull final KeyGenParameterSpec.Builder builder) {
        return builder.setIsStrongBoxBacked(true);
    }

    private static KeyPairGenerator getKeyPairGenerator(@NonNull final String alg)
            throws NoSuchProviderException, NoSuchAlgorithmException {
        return KeyPairGenerator.getInstance(alg, ANDROID_KEYSTORE);
    }

    static boolean isInsideSecureHardware(@NonNull final PrivateKey privateKey) {
        boolean isInsideSecureHardware = false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                final KeyFactory keyFactory = KeyFactory.getInstance(
                        privateKey.getAlgorithm(),
                        ANDROID_KEYSTORE
                );

                final KeyInfo keyInfo = keyFactory.getKeySpec(privateKey, KeyInfo.class);
                isInsideSecureHardware = keyInfo.isInsideSecureHardware();
            } catch (final NoSuchAlgorithmException
                    | NoSuchProviderException
                    | InvalidKeySpecException e) {
                e.printStackTrace();

                // TODO log a warning
                isInsideSecureHardware = false;
            }
        }

        return isInsideSecureHardware;
    }

    static RSAKey getRsaKey(@NonNull final KeyPair keyPair) {
        final RSAKey rsaKey =
                new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                        .privateKey(keyPair.getPrivate())
                        .keyUse(null)
                        .keyID(UUID.randomUUID().toString())
                        .build();
        return rsaKey;
    }
}
