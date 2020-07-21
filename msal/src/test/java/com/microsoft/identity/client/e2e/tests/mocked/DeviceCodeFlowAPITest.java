// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.tests.mocked;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.e2e.shadows.ShadowDeviceCodeFlowCommandAuthError;
import com.microsoft.identity.client.e2e.shadows.ShadowDeviceCodeFlowCommandSuccessful;
import com.microsoft.identity.client.e2e.shadows.ShadowDeviceCodeFlowCommandTokenError;
import com.microsoft.identity.client.e2e.shadows.ShadowHttpRequestForMockedTest;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.exception.MsalException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;

/**
 * Testing class for the device code flow protocol. Currently only supporting testing for the API-side
 * of the protocol. Will be extended to test individual aspects of the flow.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowMsalUtils.class, ShadowHttpRequestForMockedTest.class})
public class DeviceCodeFlowAPITest extends PublicClientApplicationAbstractTest {

    private static Boolean uCode;

    @Before
    public void setup() {
        super.setup();
        uCode = false;
    }

    @Override
    public String getConfigFilePath() {
        return SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;
    }

    @Test
    @Config(shadows = {ShadowDeviceCodeFlowCommandAuthError.class})
    public void testDeviceCodeFlowAuthFailure() {
        String[] scope = {"user.read"};
        mApplication.deviceCodeFlow(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void getUserCode(@NonNull String vUri, @NonNull String user_code, @NonNull String message) {
                // This shouldn't run if authorization step fails
                Assert.fail();
            }
            @Override
            public void getToken(AuthenticationResult authResult) {
                // This shouldn't run if authorization step fails
                Assert.fail();
            }
            @Override
            public void onError(MsalException error) {
                // Handle exception when authorization fails
                Assert.assertFalse(uCode);
                Assert.assertTrue(error.getErrorCode().equals("invalid_scope"));
            }
        });

        RoboTestUtils.flushScheduler();
    }

    @Test
    @Config(shadows = {ShadowDeviceCodeFlowCommandTokenError.class})
    public void testDeviceCodeFlowTokenFailure() {
        String[] scope = {"user.read"};
        mApplication.deviceCodeFlow(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void getUserCode(@NonNull String vUri, @NonNull String user_code, @NonNull String message) {
                // Assert that the protocol returns the user_code and others after successful authorization
                Assert.assertNotNull(vUri);
                Assert.assertNotNull(user_code);
                Assert.assertNotNull(message);

                Assert.assertFalse(uCode);
                uCode = true;
            }
            @Override
            public void getToken(AuthenticationResult authResult) {
                // This shouldn't run
                Assert.fail();
            }
            @Override
            public void onError(MsalException error) {
                // Handle Exception
                Assert.assertTrue(uCode);
                Assert.assertTrue(error.getErrorCode().equals("expired_token"));
            }
        });

        RoboTestUtils.flushScheduler();
    }

    @Test
    @Config(shadows = {ShadowDeviceCodeFlowCommandSuccessful.class})
    public void testDeviceCodeFlowSuccess() {
        String[] scope = {"user.read"};
        mApplication.deviceCodeFlow(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void getUserCode(@NonNull String vUri, @NonNull String user_code, @NonNull String message) {
                // Assert that the protocol returns the user_code and others after successful authorization
                Assert.assertNotNull(vUri);
                Assert.assertNotNull(user_code);
                Assert.assertNotNull(message);

                Assert.assertFalse(uCode);
                uCode = true;
            }
            @Override
            public void getToken(AuthenticationResult authResult) {
                Assert.assertTrue(uCode);
                Assert.assertNotNull(authResult);
            }
            @Override
            public void onError(MsalException error) {
                // This shouldn't run
                Assert.fail();
            }
        });

        RoboTestUtils.flushScheduler();
    }
}
