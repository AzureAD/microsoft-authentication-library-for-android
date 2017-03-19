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

    private static final String sTestIdp = "https://sts.windows.net/30baa666-8df8-48e7-97e6-77cfd0995963/";
    private static final String sTestTenantId = "cDlznUzXvRPmsu0nwRE5iZ4/mbYap0jgmkpxSnZzRQY=";
    private static final String sTestUserId = null; // test token does not contain id

    static final String sTestAuthority = HttpEventTest.sTestHttpPath.toString();
    static final String sTestUiBehavior = "FORCE_LOGIN";
    static final String sTestApiId = "12345";
    static final String sTestValidationStatus = EventProperty.Value.AUTHORITY_VALIDATION_SUCCESS;
    static final String sTestIdToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.eyJhdWQiOiJlNzBiMTE1ZS1hYzBhLTQ4MjMtODVkYS04ZjRiN2I0ZjAwZTYiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwibmJmIjoxMzc2NDI4MzEwLCJleHAiOjEzNzY0NTcxMTAsInZlciI6IjEuMCIsInRpZCI6IjMwYmFhNjY2LThkZjgtNDhlNy05N2U2LTc3Y2ZkMDk5NTk2MyIsIm9pZCI6IjRmODU5OTg5LWEyZmYtNDExZS05MDQ4LWMzMjIyNDdhYzYyYyIsInVwbiI6ImFkbWluQGFhbHRlc3RzLm9ubWljcm9zb2Z0LmNvbSIsInVuaXF1ZV9uYW1lIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwic3ViIjoiVDU0V2hGR1RnbEJMN1VWYWtlODc5UkdhZEVOaUh5LXNjenNYTmFxRF9jNCIsImZhbWlseV9uYW1lIjoiU2VwZWhyaSIsImdpdmVuX25hbWUiOiJBZnNoaW4ifQ.";
    static final String sTestLoginHint = "user@contoso.com";
    static final boolean sTestIsDeprecated = false;
    static final boolean sTestHasExtendedExpiresStatus = false;
    static final boolean sTestApiCallWasSuccessful = true;

    static IApiEvent getTestApiEvent(final Telemetry.RequestId requestId) {
        return new ApiEvent.Builder()
                .requestId(requestId)
                .authority(sTestAuthority)
                .uiBehavior(sTestUiBehavior)
                .apiId(sTestApiId)
                .validationStatus(sTestValidationStatus)
                .rawIdToken(sTestIdToken)
                .loginHint(sTestLoginHint)
                .isDeprecated(sTestIsDeprecated)
                .hasExtendedExpiresOnStatus(sTestHasExtendedExpiresStatus)
                .apiCallWasSuccessful(sTestApiCallWasSuccessful)
                .build();
    }

    @Test
    public void testApiEventInitializes() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // TODO
        final Telemetry.RequestId requestId = Telemetry.generateNewRequestId();
        IApiEvent apiEvent = getTestApiEvent(requestId);
        Assert.assertEquals(requestId.value, apiEvent.getRequestId());
        Assert.assertEquals(sTestAuthority, apiEvent.getAuthority());
        Assert.assertEquals(sTestUiBehavior, apiEvent.getUiBehavior());
        Assert.assertEquals(sTestApiId, apiEvent.getApiId());
        Assert.assertEquals(sTestValidationStatus, apiEvent.getValidationStatus());
        // Testing token parsing in another test....
        Assert.assertEquals(MSALUtils.createHash(sTestLoginHint), apiEvent.getLoginHint());
        Assert.assertEquals(Boolean.valueOf(sTestIsDeprecated), apiEvent.isDeprecated());
        Assert.assertEquals(Boolean.valueOf(sTestHasExtendedExpiresStatus), apiEvent.hasExtendedExpiresOnStatus());
        Assert.assertEquals(Boolean.valueOf(sTestApiCallWasSuccessful), apiEvent.wasSuccessful());
    }

    @Test
    public void testIdTokenParsing() {
        final Telemetry.RequestId requestId = Telemetry.generateNewRequestId();
        IApiEvent apiEvent = getTestApiEvent(requestId);
        Assert.assertEquals(sTestIdp, apiEvent.getIdpName());
        Assert.assertEquals(sTestTenantId, apiEvent.getTenantId());
        Assert.assertEquals(sTestUserId, apiEvent.getUserId());
    }
}
