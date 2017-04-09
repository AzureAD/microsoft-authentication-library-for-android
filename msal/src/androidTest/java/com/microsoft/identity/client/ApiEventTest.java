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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static com.microsoft.identity.client.EventConstants.EventProperty;

@RunWith(AndroidJUnit4.class)
public class ApiEventTest {

    private static final String TEST_IDP = AndroidTestUtil.ISSUER;
    private static final String TEST_TENANT_ID = "SSK/3ighb7Ip0yNFWm6rjYaAEAqp3/7F9/26jGxcYAQ=";
    private static final String TEST_USER_ID = "H2oDBkBpuQubaCLk0W2P+bSganOO7XBJ9o/+iHQSbo0="; // test token does not contain id

    static final String TEST_AUTHORITY = HttpEventTest.TEST_HTTP_PATH.toString();
    static final Authority.AuthorityType TEST_AUTHORITY_TYPE = Authority.AuthorityType.AAD;
    static final String TEST_UI_BEHAVIOR = "FORCE_LOGIN";
    static final String TEST_API_ID = "12345";
    static final String TEST_VALIDATION_STATUS = EventProperty.Value.AUTHORITY_VALIDATION_SUCCESS;
    static final String TEST_LOGIN_HINT = "user@contoso.com";
    static final boolean TEST_API_CALL_WAS_SUCCESSFUL = true;

    static ApiEvent.Builder getRandomTestApiEventBuilder() {
        return getTestApiEventBuilder(Telemetry.generateNewRequestId());
    }

    private static User getTestUser() {
        final String displayable = "test@contoso.onmicrosoft.com";
        return new User(displayable, displayable, AndroidTestUtil.ISSUER, AndroidTestUtil.UID, AndroidTestUtil.UTID);
    }

    static ApiEvent.Builder getTestApiEventBuilder(Telemetry.RequestId requestId) {
        return new ApiEvent.Builder(requestId)
                .setAuthority(TEST_AUTHORITY)
                .setAuthorityType(TEST_AUTHORITY_TYPE)
                .setUiBehavior(TEST_UI_BEHAVIOR)
                .setApiId(TEST_API_ID)
                .setValidationStatus(TEST_VALIDATION_STATUS)
                .setIdToken(getTestUser())
                .setLoginHint(TEST_LOGIN_HINT)
                .setApiCallWasSuccessful(TEST_API_CALL_WAS_SUCCESSFUL);
    }

    static ApiEvent getTestApiEvent(final Telemetry.RequestId requestId) {
        return getTestApiEventBuilder(requestId).build();
    }

    @Test
    public void testApiEventInitializes() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        final Telemetry.RequestId requestId = Telemetry.generateNewRequestId();
        final ApiEvent apiEvent = getTestApiEvent(requestId);
        Assert.assertEquals(requestId, apiEvent.getRequestId());
        Assert.assertEquals(TEST_AUTHORITY, apiEvent.getAuthority());
        Assert.assertEquals(EventProperty.Value.AUTHORITY_TYPE_AAD, apiEvent.getAuthorityType());
        Assert.assertEquals(TEST_UI_BEHAVIOR, apiEvent.getUiBehavior());
        Assert.assertEquals(TEST_API_ID, apiEvent.getApiId());
        Assert.assertEquals(TEST_VALIDATION_STATUS, apiEvent.getValidationStatus());
        // Testing token parsing in another test....
        Assert.assertEquals(MSALUtils.createHash(TEST_LOGIN_HINT), apiEvent.getLoginHint());
        Assert.assertEquals(Boolean.valueOf(TEST_API_CALL_WAS_SUCCESSFUL), apiEvent.wasSuccessful());
    }

    @Test
    public void testIdTokenParsing() {
        final Telemetry.RequestId requestId = Telemetry.generateNewRequestId();
        final ApiEvent apiEvent = getTestApiEvent(requestId);
        Assert.assertEquals(TEST_IDP, apiEvent.getIdpName());
        Assert.assertEquals(TEST_TENANT_ID, apiEvent.getTenantId());
        Assert.assertEquals(TEST_USER_ID, apiEvent.getUserId());
    }
}
