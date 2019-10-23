package com.microsoft.testing.popbenchmarker;


import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyInfo;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

public class PoPUtils {

    private static final String LOG_TAG = PoPUtils.class.getSimpleName();
    static final String API_18_PROVIDER = "AndroidKeyStore";
    static final String KEYSTORE_ALIAS = "Device-PoP";

    public static class KeyPairGeneratorAlgorithms {
        static final String
                DH = "DiffieHellman", // note: key.getAlgorithm() returns 'DH'
                DSA = "DSA", // Generates keypairs for the Digital Signature Algorithm.
                RSA = "RSA", // Generates keypairs for the RSA algorithm (Signature/Cipher).
                EC = "EC"; // Generates keypairs for the Elliptic Curve algorithm.
    }

//    /**
//     * Gets an RSA KeyPairGenerator.
//     *
//     * @return The generator.
//     * @throws NoSuchAlgorithmException If RSA is not supported.
//     */
//    @NonNull
//    public static KeyPairGenerator getInitializedRsaKeyPairGenerator() throws NoSuchAlgorithmException {
//        final String alg = KeyPairGeneratorAlgorithms.RSA;
//        final int keySize = 2048;
//
//        KeyPairGenerator keyPairGenerator;
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            try {
//                keyPairGenerator = getKeyPairGenerator18(alg);
//            } catch (final NoSuchProviderException e) {
//                // TODO Log a warning
//                keyPairGenerator = getKeyPairGeneratorPre18(alg);
//            }
//        } else {
//            throw new UnsupportedOperationException("No PoP support API 18 atm");
//        }
//
//        initialize(keyPairGenerator, keySize);
//
//        return keyPairGenerator;
//    }
//
//    private static void initialize(@NonNull final KeyPairGenerator keyPairGenerator,
//                                   final int keySize) {
//
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            initializePre23(keyPairGenerator, keySize);
//        } else {
//            try {
//                initialize23(keyPairGenerator, keySize);
//            } catch (InvalidAlgorithmParameterException e) {
//                // TODO Log a warning
//                initializePre23(keyPairGenerator, keySize);
//            }
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.M)
//    private static void initialize23(@NonNull final KeyPairGenerator keyPairGenerator,
//                                     final int keySize) throws InvalidAlgorithmParameterException {
//        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
//                KEYSTORE_ALIAS,
//                KeyProperties.PURPOSE_SIGN
//                        | KeyProperties.PURPOSE_VERIFY
//                        | KeyProperties.PURPOSE_ENCRYPT
//                        | KeyProperties.PURPOSE_DECRYPT
//        )
//                .setKeySize(keySize)
//                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
//                .setDigests(KeyProperties.DIGEST_SHA256);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            builder = applyHardwareIsolation(builder);
//        }
//
//        final AlgorithmParameterSpec spec = builder.build();
//        keyPairGenerator.initialize(spec);
//    }
//
//    @RequiresApi(Build.VERSION_CODES.P)
//    @NonNull
//    private static KeyGenParameterSpec.Builder applyHardwareIsolation(
//            @NonNull final KeyGenParameterSpec.Builder builder) {
//        return builder.setIsStrongBoxBacked(true);
//    }
//
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//    private static void initializePre23(@NonNull final KeyPairGenerator keyPairGenerator,
//                                        final int keySize) {
//        keyPairGenerator.initialize(keySize);
//    }
//
//    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
//    @NonNull
//    private static KeyPairGenerator getKeyPairGenerator18(@NonNull final String alg)
//            throws NoSuchProviderException, NoSuchAlgorithmException {
//        return KeyPairGenerator.getInstance(alg, API_18_PROVIDER);
//    }
//
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//    @NonNull
//    private static KeyPairGenerator getKeyPairGeneratorPre18(@NonNull final String alg)
//            throws NoSuchAlgorithmException {
//        return KeyPairGenerator.getInstance(alg);
//    }
//
//    static void doStuff() {
//        // TODO
//        // Figure out how to store the keystore permanently when AndroidKeyStore isn't used
//        // ----> I am thinking that I'll just say PoP isn't available below API 18
//
//        // Figure out how to create a JWT that is signed with this key
//        // Figure out how to create a JWK from the RSA keypair we generated
//
//        try {
//            final KeyPairGenerator kpg = getInitializedRsaKeyPairGenerator();
//            final KeyPair keyPair = kpg.generateKeyPair();
//            final boolean isInsideSecureHardware = isInsideSecureHardware(keyPair.getPrivate());
//            final RSAKey rsaKey = getRsaKey(keyPair);
//            final RSAKey pubJwk = rsaKey.toPublicJWK();
//            final String jwkJson = pubJwk.toJSONString();
//            final Base64URL base64UrlEncodedPublicJwk = Base64URL.encode(jwkJson);
//
//            Log.d(
//                    LOG_TAG,
//                    "Key is hardware protected? " + isInsideSecureHardware
//            );
//            Log.d(
//                    LOG_TAG,
//                    "Jwk String: " + base64UrlEncodedPublicJwk.toString()
//            );
//
//            // Test recovering this key from the store....
//            final KeyStore keyStore = KeyStore.getInstance(API_18_PROVIDER);
//            keyStore.load(null);
//            final KeyStore.Entry entry = keyStore.getEntry(KEYSTORE_ALIAS, null);
//            final PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
//            final PublicKey publicKey = keyStore.getCertificate(KEYSTORE_ALIAS).getPublicKey();
//            final KeyPair restoredKeyPair = new KeyPair(publicKey, privateKey);
//            final int i = 0 + 2;
//        } catch (NoSuchAlgorithmException e) {
//            // RSA not supported!
//        } catch (KeyStoreException e) {
//            e.printStackTrace();
//        } catch (UnrecoverableEntryException e) {
//            e.printStackTrace();
//        } catch (CertificateException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    static boolean isInsideSecureHardware(@NonNull final PrivateKey privateKey) {
//        boolean isInsideSecureHardware = false;
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//            try {
//                final KeyFactory keyFactory = KeyFactory.getInstance(
//                        privateKey.getAlgorithm(),
//                        API_18_PROVIDER
//                );
//
//                final KeyInfo keyInfo = keyFactory.getKeySpec(privateKey, KeyInfo.class);
//                isInsideSecureHardware = keyInfo.isInsideSecureHardware();
//            } catch (final NoSuchAlgorithmException
//                    | NoSuchProviderException
//                    | InvalidKeySpecException e) {
//                e.printStackTrace();
//
//                // TODO log a warning
//                isInsideSecureHardware = false;
//            }
//        }
//
//        return isInsideSecureHardware;
//    }
//
//    static RSAKey getRsaKey(@NonNull final KeyPair keyPair) {
//        final RSAKey rsaKey =
//                new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
//                        .privateKey(keyPair.getPrivate())
//                        .keyUse(null)
//                        .keyID(UUID.randomUUID().toString())
//                        .build();
//        return rsaKey;
//    }
}
