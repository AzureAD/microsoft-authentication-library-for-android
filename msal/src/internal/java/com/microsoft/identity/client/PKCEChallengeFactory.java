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

package com.microsoft.identity.client;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Factory class for PKCE Verification
 */
final class PKCEChallengeFactory {

    private static final int CODE_VERIFIER_BYTE_SIZE = 32;
    private static final int CODE_VERIFIER_LENGTH = 128;
    private static final int ENCODE_MASK = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;
    private static final String DIGEST_ALGORITHM = "SHA-256";
    private static final String ISO_8859_1 = "ISO_8859_1";

    /**
     *
     */
    static class PKCEChallenge {

        /**
         *
         */
        enum ChallengeMethod {
            PLAIN,
            S256
        }

        final String codeVerifier;

        /**
         *
         */
        final String codeChallenge;

        /**
         *
         */
        final ChallengeMethod method;

        PKCEChallenge(String codeVerifier, String codeChallenge, ChallengeMethod method) {
            this.codeVerifier = codeVerifier;
            this.codeChallenge = codeChallenge;
            this.method = method;
        }
    }

    /**
     * @return
     */
    static PKCEChallenge newVerifier() {
        // Generate the code_verifier as a high-entropy cryptographic random String
        final String codeVerifier = generateCodeVerifier();

        // Create a code_challenge derived from the code_verifier
        final String codeChallenge = generateCodeVerifierChallenge(codeVerifier);

        // Set the challenge_method - if plain, the code_verifier and code_challenge will match
        final PKCEChallenge.ChallengeMethod challengeMethod =
                codeVerifier
                        .equals(codeChallenge) ?
                        PKCEChallenge.ChallengeMethod.PLAIN :
                        PKCEChallenge.ChallengeMethod.S256;

        return new PKCEChallenge(codeVerifier, codeChallenge, challengeMethod);
    }

    /**
     * @return
     */
    private static String generateCodeVerifier() {
        final byte[] verifierBytes = new byte[CODE_VERIFIER_BYTE_SIZE];
        new SecureRandom().nextBytes(verifierBytes);
        return Base64.encodeToString(verifierBytes, ENCODE_MASK);
    }

    /**
     * @param verifier
     * @return
     */
    private static String generateCodeVerifierChallenge(final String verifier) {
        try {
            MessageDigest digester = MessageDigest.getInstance(DIGEST_ALGORITHM);
            digester.update(verifier.getBytes(ISO_8859_1));
            byte[] digestBytes = digester.digest();
            return Base64.encodeToString(digestBytes, ENCODE_MASK);
        } catch (NoSuchAlgorithmException e) {
            // TODO log warning
            return verifier;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(
                    "Every implementation of the Java platform is required to support ISO-8859-1."
                            + "Consult the release documentation for your implementation.", e
            );
        }
    }
}
