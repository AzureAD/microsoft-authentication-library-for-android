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
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowDeviceCodeFlowCommandAuthError;
import com.microsoft.identity.client.e2e.shadows.ShadowDeviceCodeFlowCommandSuccessful;
import com.microsoft.identity.client.e2e.shadows.ShadowDeviceCodeFlowCommandTokenError;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;

/**
 * Testing class for the device code flow protocol. Currently only supporting testing for the API-side
 * of the protocol. Will be extended to test individual aspects of the flow.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowPublicClientApplicationConfiguration.class})
public class DeviceCodeFlowApiTest extends PublicClientApplicationAbstractTest {

    private boolean mUserCodeReceived;

    @Before
    public void setup() {
        super.setup();
    }

    @Override
    public String getConfigFilePath() {
        return SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;
    }

    //===========================================================================================================
    // getDeviceCode() Testing
    //===========================================================================================================
    @Test
    public void testGetDeviceCodeSuccessResult() throws IOException, ClientException {
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        final OAuth2Strategy strategy = config.getDefaultAuthority().createOAuth2Strategy(strategyParameters);

        final MicrosoftStsAuthorizationRequest.Builder builder = createMockAuthorizationRequestBuilder();
        final MicrosoftStsAuthorizationRequest authorizationRequest = builder.build();
        final AuthorizationResult authorizationResult = strategy.getDeviceCode(authorizationRequest);
        final MicrosoftStsAuthorizationResponse authorizationResponse = (MicrosoftStsAuthorizationResponse) authorizationResult.getAuthorizationResponse();

        Assert.assertTrue(authorizationResult.getSuccess());
        Assert.assertNotNull(authorizationResponse);

        Assert.assertNotNull(authorizationResponse.getDeviceCode());
        Assert.assertNotNull(authorizationResponse.getUserCode());
        Assert.assertNotNull(authorizationResponse.getMessage());
        Assert.assertNotNull(authorizationResponse.getInterval());
        Assert.assertNotNull(authorizationResponse.getExpiresIn());
        Assert.assertNotNull(authorizationResponse.getVerificationUri());

        Assert.assertNull(authorizationResult.getAuthorizationErrorResponse());
    }

    @Test
    public void testGetDeviceCodeFailureNoClientId() throws IOException, ClientException {
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        final OAuth2Strategy strategy = config.getDefaultAuthority().createOAuth2Strategy(strategyParameters);

        final MicrosoftStsAuthorizationRequest.Builder builder = createMockAuthorizationRequestBuilder();
        final MicrosoftStsAuthorizationRequest authorizationRequest = builder.setClientId(null).build();
        final AuthorizationResult authorizationResult = strategy.getDeviceCode(authorizationRequest);
        final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse = (MicrosoftStsAuthorizationErrorResponse) authorizationResult.getAuthorizationErrorResponse();

        Assert.assertFalse(authorizationResult.getSuccess());
        Assert.assertNull(authorizationResult.getAuthorizationResponse());

        Assert.assertNotNull(authorizationErrorResponse);
        Assert.assertEquals(ErrorStrings.INVALID_REQUEST, authorizationErrorResponse.getError());
    }

    @Test
    public void testGetDeviceCodeFailureNoScope() throws IOException, ClientException {
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        final OAuth2Strategy strategy = config.getDefaultAuthority().createOAuth2Strategy(strategyParameters);

        final MicrosoftStsAuthorizationRequest.Builder builder = createMockAuthorizationRequestBuilder();
        final MicrosoftStsAuthorizationRequest authorizationRequest = builder.setScope(null).build();
        final AuthorizationResult authorizationResult = strategy.getDeviceCode(authorizationRequest);
        final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse = (MicrosoftStsAuthorizationErrorResponse) authorizationResult.getAuthorizationErrorResponse();

        Assert.assertFalse(authorizationResult.getSuccess());
        Assert.assertNull(authorizationResult.getAuthorizationResponse());

        Assert.assertNotNull(authorizationErrorResponse);
        Assert.assertEquals(ErrorStrings.INVALID_REQUEST, authorizationErrorResponse.getError());
    }

    @Test
    public void testGetDeviceCodeFailureBadScope() throws IOException, ClientException {
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        final OAuth2Strategy strategy = config.getDefaultAuthority().createOAuth2Strategy(strategyParameters);

        final MicrosoftStsAuthorizationRequest.Builder builder = createMockAuthorizationRequestBuilder();
        final MicrosoftStsAuthorizationRequest authorizationRequest = builder.setScope("/").build();
        final AuthorizationResult authorizationResult = strategy.getDeviceCode(authorizationRequest);
        final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse = (MicrosoftStsAuthorizationErrorResponse) authorizationResult.getAuthorizationErrorResponse();

        Assert.assertFalse(authorizationResult.getSuccess());
        Assert.assertNull(authorizationResult.getAuthorizationResponse());

        Assert.assertNotNull(authorizationErrorResponse);
        Assert.assertEquals(ErrorStrings.INVALID_SCOPE, authorizationErrorResponse.getError());
    }

    /**
     * Helper function to create a mock authorization request builder
     * @return builder object
     */
    private MicrosoftStsAuthorizationRequest.Builder createMockAuthorizationRequestBuilder() {
        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        final MicrosoftStsAuthorizationRequest.Builder builder = new MicrosoftStsAuthorizationRequest.Builder();
        builder.setClientId(config.getClientId())
                .setScope("user.read")
                .setState("State!");

        return builder;
    }

    //===========================================================================================================
    // Token Request Testing
    //===========================================================================================================x
    @Test
    public void testDeviceCodeFlowTokenInvalidRequest() throws IOException, ClientException {
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        final Authority defaultAuthority = config.getDefaultAuthority();
        Assert.assertNotNull("Default authority should not be null", defaultAuthority);
        final OAuth2Strategy strategy = defaultAuthority.createOAuth2Strategy(strategyParameters);

        final MicrosoftStsTokenRequest tokenRequest = createMockTokenRequest();

        final TokenResult tokenResult = strategy.requestToken(tokenRequest);
        Assert.assertNull(tokenResult.getTokenResponse());
        Assert.assertNotNull(tokenResult.getErrorResponse());
        Assert.assertEquals(ErrorStrings.INVALID_REQUEST, tokenResult.getErrorResponse().getError());
    }

    @Test
    public void testDeviceCodeFlowTokenExpiredToken() throws IOException, ClientException {
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        final Authority defaultAuthority = config.getDefaultAuthority();
        Assert.assertNotNull("Default authority should not be null", defaultAuthority);
        final OAuth2Strategy strategy = defaultAuthority.createOAuth2Strategy(strategyParameters);
        Assert.assertNotNull("Strategy should not be null", strategy);

        final MicrosoftStsTokenRequest tokenRequest = createMockTokenRequest();

        // Previously authenticated code
        tokenRequest.setDeviceCode(
                "AAQABAAEAAAAm-06blBE1TpVMil8KPQ41e5vDLI7te0y-3XHYO_uurPryAiyBiPiKnjEVzAQZQzCyGZERne4a" +
                        "IwYAiBlQ8an93ENYuVOO-vEAt48FEJSEMQqq-zHZVD59bkc6eYIAViZKVvTv5_qilKj4uEjVE9BGkIxY5B6Uq1K8oWHEqzH-w6CiWjC8vQc6mSV_FPCbnAggAA");

        final TokenResult tokenResult = strategy.requestToken(tokenRequest);
        Assert.assertNull(tokenResult.getTokenResponse());
        Assert.assertNotNull(tokenResult.getErrorResponse());
        Assert.assertEquals(ErrorStrings.DEVICE_CODE_FLOW_EXPIRED_TOKEN_ERROR_CODE, tokenResult.getErrorResponse().getError());
    }

    /**
     * Helper function to create a mock token request.
     * @return a token request.
     */
    private MicrosoftStsTokenRequest createMockTokenRequest() {
        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();

        final MicrosoftStsTokenRequest tokenRequest = new MicrosoftStsTokenRequest();
        tokenRequest.setCodeVerifier("");
        tokenRequest.setCorrelationId(UUID.randomUUID());
        tokenRequest.setClientId(config.getClientId());
        tokenRequest.setGrantType(TokenRequest.GrantTypes.DEVICE_CODE);
        tokenRequest.setRedirectUri(config.getRedirectUri());
        tokenRequest.setClientAppName("TestApp");
        tokenRequest.setClientAppVersion("1.0");

        return tokenRequest;
    }

    //===========================================================================================================
    // API-Side Testing
    //===========================================================================================================
    @Test
    @Config(shadows = {ShadowDeviceCodeFlowCommandAuthError.class})
    public void testDeviceCodeFlowAuthFailure() {
        String[] scope = {"user.read"};
        mApplication.acquireTokenWithDeviceCode(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void onUserCodeReceived(@NonNull String vUri,
                                           @NonNull String userCode,
                                           @NonNull String message,
                                           @NonNull Date sessionExpirationDate) {
                // This shouldn't run if authorization step fails
                Assert.fail();
            }
            @Override
            public void onTokenReceived(@NonNull AuthenticationResult authResult) {
                // This shouldn't run if authorization step fails
                Assert.fail();
            }
            @Override
            public void onError(@NonNull MsalException error) {
                // Handle exception when authorization fails
                Assert.assertFalse(mUserCodeReceived);
                Assert.assertEquals(ErrorStrings.INVALID_SCOPE, error.getErrorCode());
            }
        });

        RoboTestUtils.flushScheduler();
    }

    @Test
    @Config(shadows = {ShadowDeviceCodeFlowCommandTokenError.class})
    public void testDeviceCodeFlowTokenFailure() {
        String[] scope = {"user.read"};
        mApplication.acquireTokenWithDeviceCode(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void onUserCodeReceived(@NonNull String vUri,
                                           @NonNull String userCode,
                                           @NonNull String message,
                                           @NonNull Date sessionExpirationDate) {
                // Assert that the protocol returns the userCode and others after successful authorization
                Assert.assertNotNull(vUri);
                Assert.assertNotNull(userCode);
                Assert.assertNotNull(message);
                Assert.assertNotNull(sessionExpirationDate);

                Assert.assertFalse(mUserCodeReceived);
                mUserCodeReceived = true;
            }
            @Override
            public void onTokenReceived(@NonNull AuthenticationResult authResult) {
                // This shouldn't run
                Assert.fail();
            }
            @Override
            public void onError(@NonNull MsalException error) {
                // Handle Exception
                Assert.assertTrue(mUserCodeReceived);
                Assert.assertEquals(ErrorStrings.DEVICE_CODE_FLOW_EXPIRED_TOKEN_ERROR_CODE, error.getErrorCode());
            }
        });

        RoboTestUtils.flushScheduler();
    }

    @Test
    @Config(shadows = {ShadowDeviceCodeFlowCommandSuccessful.class})
    public void testDeviceCodeFlowSuccess() {
        String[] scope = {"user.read"};
        mApplication.acquireTokenWithDeviceCode(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void onUserCodeReceived(@NonNull String vUri,
                                           @NonNull String userCode,
                                           @NonNull String message,
                                           @NonNull Date sessionExpirationDate) {
                // Assert that the protocol returns the userCode and others after successful authorization
                Assert.assertNotNull(vUri);
                Assert.assertNotNull(userCode);
                Assert.assertNotNull(message);
                Assert.assertNotNull(sessionExpirationDate);

                Assert.assertFalse(mUserCodeReceived);
                mUserCodeReceived = true;
            }
            @Override
            public void onTokenReceived(@NonNull AuthenticationResult authResult) {
                Assert.assertTrue(mUserCodeReceived);
                Assert.assertNotNull(authResult);
            }
            @Override
            public void onError(@NonNull MsalException error) {
                // This shouldn't run
                Assert.fail();
            }
        });

        RoboTestUtils.flushScheduler();
    }
}
