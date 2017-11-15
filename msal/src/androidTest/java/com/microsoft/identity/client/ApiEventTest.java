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

    private static final String TEST_IDP = "https://sts.windows.net/30baa666-8df8-48e7-97e6-77cfd0995963/";
    private static final String TEST_TENANT_ID = "cDlznUzXvRPmsu0nwRE5iZ4/mbYap0jgmkpxSnZzRQY=";
    private static final String TEST_USER_ID = null; // test token does not contain id

    static final String TEST_AUTHORITY = HttpEventTest.TEST_HTTP_PATH.toString();
    static final Authority.AuthorityType TEST_AUTHORITY_TYPE = Authority.AuthorityType.AAD;
    static final String TEST_UI_BEHAVIOR = "FORCE_LOGIN";
    static final String TEST_API_ID = "12345";
    static final String TEST_ID_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";
    static final String TEST_VALIDATION_STATUS = EventProperty.Value.AUTHORITY_VALIDATION_SUCCESS;
    static final String TEST_LOGIN_HINT = "user@contoso.com";
    static final boolean TEST_API_CALL_WAS_SUCCESSFUL = true;
    static final String TEST_API_ERROR_CODE = "test_error_code";
    static final Long TEST_START_TIME = 0L;
    static final Long TEST_STOP_TIME = 1L;
    static final Long TEST_ELAPSED_TIME = TEST_STOP_TIME - TEST_START_TIME;

    // Authorities
    private static final String TEST_AUTHORITY_WITH_IDENTIFIER = AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT;
    static final String TEST_AUTHORITY_COMMON = "https://login.microsoftonline.com/common";
    private static final String TEST_AUTHORITY_B2C = "https://login.microsoftonline.com/tfp/tenant/policy";

    static ApiEvent.Builder getRandomTestApiEventBuilder() {
        return getTestApiEventBuilder(Telemetry.generateNewRequestId(), TEST_AUTHORITY);
    }

    static ApiEvent.Builder getTestApiEventBuilder(final String requestId, final String authority) {
        return new ApiEvent.Builder(requestId)
                .setStartTime(0L)
                .setStopTime(1L)
                .setElapsedTime(1L)
                .setAuthority(authority)
                .setAuthorityType(TEST_AUTHORITY_TYPE)
                .setUiBehavior(TEST_UI_BEHAVIOR)
                .setApiId(TEST_API_ID)
                .setValidationStatus(TEST_VALIDATION_STATUS)
                .setRawIdToken(TEST_ID_TOKEN)
                .setLoginHint(TEST_LOGIN_HINT)
                .setApiErrorCode(TEST_API_ERROR_CODE)
                .setApiCallWasSuccessful(TEST_API_CALL_WAS_SUCCESSFUL);
    }

    static ApiEvent getTestApiEvent(final String requestId, final String authority) {
        return getTestApiEventBuilder(requestId, authority).build();
    }

    @Test
    public void testCommonAuthorityPresent() {
        final ApiEvent apiEvent = getTestApiEvent(Telemetry.generateNewRequestId(), TEST_AUTHORITY_COMMON);
        Assert.assertEquals("https://login.microsoftonline.com/", apiEvent.getAuthority());
    }

    @Test
    public void testAuthorityB2cOmitted() {
        final ApiEvent apiEvent = getTestApiEvent(Telemetry.generateNewRequestId(), TEST_AUTHORITY_B2C);
        Assert.assertEquals(null, apiEvent.getAuthority());
    }

    @Test
    public void testAuthorityWithIdentifierScrubbed() {
        final ApiEvent apiEvent = getTestApiEvent(Telemetry.generateNewRequestId(), TEST_AUTHORITY_WITH_IDENTIFIER);
        Assert.assertEquals("https://login.microsoftonline.com/", apiEvent.getAuthority());
    }

    @Test
    public void testApiEventInitializes() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Telemetry.setAllowPii(true);
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        final ApiEvent apiEvent = getTestApiEvent(telemetryRequestId, TEST_AUTHORITY);
        Assert.assertEquals(telemetryRequestId, apiEvent.getRequestId());
        Assert.assertEquals(TEST_START_TIME, apiEvent.getStartTime());
        Assert.assertEquals(TEST_STOP_TIME, apiEvent.getStopTime());
        Assert.assertEquals(TEST_ELAPSED_TIME, apiEvent.getElapsedTime());
        Assert.assertEquals(TEST_AUTHORITY, apiEvent.getAuthority());
        Assert.assertEquals(EventProperty.Value.AUTHORITY_TYPE_AAD, apiEvent.getAuthorityType());
        Assert.assertEquals(TEST_UI_BEHAVIOR, apiEvent.getUiBehavior());
        Assert.assertEquals(TEST_API_ID, apiEvent.getApiId());
        Assert.assertEquals(TEST_VALIDATION_STATUS, apiEvent.getValidationStatus());
        // Testing token parsing in another test....
        Assert.assertEquals(MsalUtils.createHash(TEST_LOGIN_HINT), apiEvent.getLoginHint());
        Assert.assertEquals(Boolean.valueOf(TEST_API_CALL_WAS_SUCCESSFUL), apiEvent.wasSuccessful());
        Assert.assertEquals(TEST_API_ERROR_CODE, apiEvent.getApiErrorCode());
        Telemetry.setAllowPii(false);
    }

    @Test
    public void testIdTokenParsing() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Telemetry.setAllowPii(true);
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        final ApiEvent apiEvent = getTestApiEvent(telemetryRequestId, TEST_AUTHORITY);
        Assert.assertEquals(TEST_IDP, apiEvent.getIdpName());
        Assert.assertEquals(TEST_TENANT_ID, apiEvent.getTenantId());
        Assert.assertEquals(MsalUtils.createHash(TEST_USER_ID), apiEvent.getUserId());
        Telemetry.setAllowPii(false);
    }
}
