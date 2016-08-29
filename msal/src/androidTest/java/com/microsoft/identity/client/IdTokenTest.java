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

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;

/**
 * Instrumentation tests for {@link IdToken}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class IdTokenTest {

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyIdToken() throws AuthenticationException {
        new IdToken("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullIdToken() throws AuthenticationException {
        new IdToken(null);
    }

    @Test
    public void testIdTokenInvalidJWT() {
        try {
            new IdToken("..");
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode() == MSALError.IDTOKEN_PARSING_FAILURE);
        }

        try {
            new IdToken("test.ab.b.");
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode() == MSALError.IDTOKEN_PARSING_FAILURE);
        }

        try {
            new IdToken(".....");
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode() == MSALError.IDTOKEN_PARSING_FAILURE);
        }

        try {
            new IdToken("test");
            Assert.fail();
        } catch (final AuthenticationException e) {
            Assert.assertTrue(e.getErrorCode() == MSALError.IDTOKEN_PARSING_FAILURE);
        }
    }

    @Test
    public void testIdTokenWithInvalidPayload() {
        // If the id token contains the empty payload, it's not in the correct JSON format, JsonException will be thrown.
        try {
            new IdToken("test..");
            Assert.fail("Expect exceptions");
        } catch (final AuthenticationException e) {
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof JSONException);
        }

        try {
            new IdToken("test.test.test");
        } catch (final AuthenticationException e) {
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof JSONException);
        }
    }

    @Test
    public void testIncompleteIdTokenPayload() {
        /**
         * Header:
         * "typ": "JWT",
         * "alg": "none"
         *
         * Payload:
         * "aud":"e70b115e-ac0a-4823-85da-8f4b7b4f00e6",
         * "iss":"https://sts.windows.net/30baa666-8df8-48e7-97e6-77cfd0995963/",
         * "nbf":1376428310,
         * "exp":1376457110,
         * "ver":"1.0",
         * "tid":"30baa666-8df8-48e7-97e6-77cfd0995963",
         * "oid":"4f859989-a2ff-411e-9048-c322247ac62c",
         * "upn":"admin@aaltests.onmicrosoft.com",
         * "unique_name":"admin@aaltests.onmicrosoft.com",
         * "sub":"T54WhFGTglBL7UVake879RGadENiHy-sczsXNaqD_c4",
         * "family_name":"S
         */
        final String rawIdToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2.";
        try {
            new IdToken(rawIdToken);
            Assert.fail("Expect exceptions.");
        } catch (final AuthenticationException e) {
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof JSONException);
        }
    }

    @Test
    public void testIdTokenHappyPath() throws UnsupportedEncodingException {
        // Id token with invalid signature part, we don't do any signature validation today. As long as the id token
        // contains a valid payload, we'll successfully parse it.
        final String rawIdToken = AndroidTestUtil.createIdToken(
                AndroidTestUtil.AUDIENCE, AndroidTestUtil.ISSUER, AndroidTestUtil.NAME,
                AndroidTestUtil.OBJECT_ID, AndroidTestUtil.PREFERRED_USERNAME,
                AndroidTestUtil.SUBJECT, AndroidTestUtil.TENANT_ID, AndroidTestUtil.VERSION,
                AndroidTestUtil.HOME_OBJECT_ID);
        try {
            final IdToken idToken = new IdToken(rawIdToken);
            Assert.assertTrue(idToken.getIssuer().equals(AndroidTestUtil.ISSUER));
            Assert.assertTrue(idToken.getName().equals(AndroidTestUtil.NAME));
            Assert.assertTrue(idToken.getObjectId().equals(AndroidTestUtil.OBJECT_ID));
            Assert.assertTrue(idToken.getPreferredName().equals(AndroidTestUtil.PREFERRED_USERNAME));
            Assert.assertTrue(idToken.getVersion().equals(AndroidTestUtil.VERSION));
            Assert.assertTrue(idToken.getTenantId().equals(AndroidTestUtil.TENANT_ID));
            Assert.assertTrue(idToken.getHomeObjectId().equals(AndroidTestUtil.HOME_OBJECT_ID));
            Assert.assertTrue(idToken.getSubject().equals(AndroidTestUtil.SUBJECT));
        } catch (final AuthenticationException e) {
            Assert.fail("unexpected exception");
        }
    }
}
