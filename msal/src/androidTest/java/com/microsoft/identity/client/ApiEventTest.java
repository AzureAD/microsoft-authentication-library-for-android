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

import androidx.test.runner.AndroidJUnit4;

import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.client.internal.telemetry.ApiEvent;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import static com.microsoft.identity.client.internal.telemetry.EventConstants.EventProperty;

@RunWith(AndroidJUnit4.class)
public class ApiEventTest {

    private static final String TEST_IDP = "https://sts.windows.net/30baa666-8df8-48e7-97e6-77cfd0995963/";
    private static final String TEST_TENANT_ID = "cDlznUzXvRPmsu0nwRE5iZ4/mbYap0jgmkpxSnZzRQY=";
    private static final String TEST_USER_ID = "admin@aaltests.onmicrosoft.com"; // test token does not contain id

    static final String TEST_AUTHORITY = HttpEventTest.TEST_HTTP_PATH.toString();
    static final String TEST_UI_BEHAVIOR = "FORCE_LOGIN";
    static final String TEST_API_ID = "12345";
    static final String TEST_ID_TOKEN;

    static {
        TEST_ID_TOKEN =
                AndroidTestUtil.createIdToken(
                        "e70b115e-ac0a-4823-85da-8f4b7b4f00e6",
                        "https://sts.windows.net/30baa666-8df8-48e7-97e6-77cfd0995963/",
                        "John Doe",
                        "4f859989-a2ff-411e-9048-c322247ac62c",
                        "admin@aaltests.onmicrosoft.com",
                        "T54WhFGTglBL7UVake879RGadENiHy-sczsXNaqD_c4",
                        "30baa666-8df8-48e7-97e6-77cfd0995963",
                        "1.0",
                        new HashMap<String, Object>() {{
                            put("nbf", 1376428310);
                            put("exp", 1376457110);
                            put("upn", "admin@aaltests.onmicrosoft.com");
                            put("unique_name", "admin@aaltests.onmicrosoft.com");
                        }}
                );
    }

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
    public void testAuthorityWithIdentifierScrubbed() {
        final ApiEvent apiEvent = getTestApiEvent(Telemetry.generateNewRequestId(), TEST_AUTHORITY_WITH_IDENTIFIER);
        Assert.assertEquals("https://login.microsoftonline.com/", apiEvent.getAuthority());
    }

    @Test
    @Ignore
    public void testApiEventInitializes() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Telemetry.setAllowPii(true);
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        final ApiEvent apiEvent = getTestApiEvent(telemetryRequestId, TEST_AUTHORITY);
        Assert.assertEquals(telemetryRequestId, apiEvent.getRequestId());
        Assert.assertEquals(TEST_START_TIME, apiEvent.getStartTime());
        Assert.assertEquals(TEST_STOP_TIME, apiEvent.getStopTime());
        Assert.assertEquals(TEST_ELAPSED_TIME, apiEvent.getElapsedTime());
        Assert.assertEquals(TEST_UI_BEHAVIOR, apiEvent.getUiBehavior());
        Assert.assertEquals(TEST_API_ID, apiEvent.getApiId());
        Assert.assertEquals(TEST_VALIDATION_STATUS, apiEvent.getValidationStatus());
        // Testing token parsing in another test....
        Assert.assertEquals(MsalUtils.createHash(TEST_LOGIN_HINT), apiEvent.getLoginHint());
        Assert.assertEquals(Boolean.valueOf(TEST_API_CALL_WAS_SUCCESSFUL), apiEvent.wasSuccessful());
        Assert.assertEquals(TEST_API_ERROR_CODE, apiEvent.getApiErrorCode());
        Telemetry.setAllowPii(false);
    }

}
