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
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Set;

public class NimbusPoPUtils {

    public static final int MINIMUM_RSA_COMPLEXITY = 2048;

    /**
     * Generates an RSA KeyPair of the specified length (complexity).
     *
     * @param length   The RSA key size, in bits. Minimum 2048 bits.
     * @param kid      Optional identifier, GUID is suggested.
     * @param keystore Optional reference to the underlying KeyStore.
     * @param ops      Optional key operations parameter, maps to key_ops in RFC-7517.
     * @param use      Optional use operations parameter, maps to use in RFC-7517.
     * @return A newly generated JWK according to the supplied criteria.
     * @throws JOSEException If the key generation failed.
     */
    public static RSAKey generateRsaKeyPair(final int length,
                                            @NonNull final String kid,
                                            @Nullable final KeyStore keystore,
                                            @Nullable final Set<KeyOperation> ops,
                                            @Nullable final KeyUse use) throws JOSEException {
        if (MINIMUM_RSA_COMPLEXITY > length) {
            throw new IllegalArgumentException("Key length must be >= 2048");
        }

        return new RSAKeyGenerator(length)
                .keyID(kid)
                .keyStore(keystore)
                .keyOperations(ops)
                .keyUse(use)
                .generate();
    }

    /**
     * Gets the JWSSigner associated with the private key of the supplied keypair.
     *
     * @param rsaJwk The RSA JWK to use for signing. Note that key length must be >=
     *               {@link #MINIMUM_RSA_COMPLEXITY}.
     * @return A JWSSigner bound to the supplied keypair.
     * @throws JOSEException If the supplied RSA keypair does not contain private key material or
     *                       if it cannot be accessed/extracted.
     */
    public static JWSSigner getSigner(@NonNull final RSAKey rsaJwk) throws JOSEException {
        return new RSASSASigner(rsaJwk);
    }

    /**
     * @param signer    The signer with which to sign this JWT.
     * @param claimsSet The claims to sign.
     * @param kid       The key identifier of this JWT.
     * @return The SignedJWT.
     * @throws JOSEException If the JWS object couldn't be signed.
     */
    public static SignedJWT getSignedJwt(@NonNull final JWSSigner signer,
                                         @NonNull final JWTClaimsSet claimsSet,
                                         @Nullable final String kid) throws JOSEException {
        final SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(kid)
                        .build(),
                claimsSet
        );

        signedJWT.sign(signer);

        return signedJWT;
    }

    private static final String ENCRYPTED_SHARED_PREFS = "SecurePrefs";

    public static void writeSecurePref(@NonNull final Context context,
                           @NonNull final String key,
                           @NonNull final String value) {
        try {
            SharedPreferences prefs = getSecurePrefs(context);

            prefs.edit().putString(key, value).apply();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readSecurePref(@NonNull final Context context,
                                      @NonNull final String key) {
        try {
            SharedPreferences prefs = getSecurePrefs(context);
            return prefs.getString(key, null);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SharedPreferences getSecurePrefs(@NonNull Context context) throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        return EncryptedSharedPreferences.create(
                ENCRYPTED_SHARED_PREFS,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}
