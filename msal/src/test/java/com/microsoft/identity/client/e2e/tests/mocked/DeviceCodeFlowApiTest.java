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
import com.microsoft.identity.client.e2e.shadows.ShadowHttpRequestForMockedTest;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;

/**
 * Testing class for the device code flow protocol. Currently only supporting testing for the API-side
 * of the protocol. Will be extended to test individual aspects of the flow.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowMsalUtils.class})
public class DeviceCodeFlowApiTest extends PublicClientApplicationAbstractTest {

    private MicrosoftStsAuthorizationRequest.Builder mBuilder;
    private String mUrlBody;
    private OAuth2Strategy mStrategy;
    private MicrosoftStsTokenRequest mTokenRequest;
    private boolean mUserCodeReceived;

    @Before
    public void setup() {
        super.setup();

        // getDeviceCode() testing variables
        final PublicClientApplicationConfiguration config = mApplication.getConfiguration();
        mUrlBody = ((AzureActiveDirectoryAuthority) config.getAuthorities().get(0)).getAudience().getCloudUrl();
        mBuilder = new MicrosoftStsAuthorizationRequest.Builder();
        mBuilder.setClientId(config.getClientId())
                .setScope("user.read")
                .setState("State!");

        final OAuth2StrategyParameters options = new OAuth2StrategyParameters();
        mStrategy = new AzureActiveDirectoryOAuth2Strategy(
                new AzureActiveDirectoryOAuth2Configuration(),
                options
        );

        // token request testing variable
        mTokenRequest = new MicrosoftStsTokenRequest();
        mTokenRequest.setCodeVerifier("");
        mTokenRequest.setCorrelationId(UUID.fromString("a-b-c-d-e"));
        mTokenRequest.setClientId(config.getClientId());
        mTokenRequest.setGrantType(TokenRequest.GrantTypes.DEVICE_CODE);
        mTokenRequest.setRedirectUri(config.getRedirectUri());
    }

    @Override
    public String getConfigFilePath() {
        return SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;
    }

    //===========================================================================================================
    // getDeviceCode() Testing
    //===========================================================================================================
    @Test
    public void testGetDeviceCodeSuccessResult() throws IOException {
        final MicrosoftStsAuthorizationRequest authorizationRequest = mBuilder.build();
        final AuthorizationResult authorizationResult = mStrategy.getDeviceCode(authorizationRequest, mUrlBody);
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
    public void testGetDeviceCodeFailureNoClientId() throws IOException {
        final MicrosoftStsAuthorizationRequest authorizationRequest = mBuilder.setClientId(null).build();
        final AuthorizationResult authorizationResult = mStrategy.getDeviceCode(authorizationRequest, mUrlBody);
        final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse = (MicrosoftStsAuthorizationErrorResponse) authorizationResult.getAuthorizationErrorResponse();

        Assert.assertFalse(authorizationResult.getSuccess());
        Assert.assertNull(authorizationResult.getAuthorizationResponse());

        Assert.assertNotNull(authorizationErrorResponse);
        Assert.assertEquals(ErrorStrings.INVALID_REQUEST, authorizationErrorResponse.getError());
    }

    @Test
    public void testGetDeviceCodeFailureNoScope() throws IOException {
        final MicrosoftStsAuthorizationRequest authorizationRequest = mBuilder.setScope(null).build();
        final AuthorizationResult authorizationResult = mStrategy.getDeviceCode(authorizationRequest, mUrlBody);
        final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse = (MicrosoftStsAuthorizationErrorResponse) authorizationResult.getAuthorizationErrorResponse();

        Assert.assertFalse(authorizationResult.getSuccess());
        Assert.assertNull(authorizationResult.getAuthorizationResponse());

        Assert.assertNotNull(authorizationErrorResponse);
        Assert.assertEquals(ErrorStrings.INVALID_REQUEST, authorizationErrorResponse.getError());
    }

    @Test
    public void testGetDeviceCodeFailureBadScope() throws IOException {
        final MicrosoftStsAuthorizationRequest authorizationRequest = mBuilder.setScope("/").build();
        final AuthorizationResult authorizationResult = mStrategy.getDeviceCode(authorizationRequest, mUrlBody);
        final MicrosoftStsAuthorizationErrorResponse authorizationErrorResponse = (MicrosoftStsAuthorizationErrorResponse) authorizationResult.getAuthorizationErrorResponse();

        Assert.assertFalse(authorizationResult.getSuccess());
        Assert.assertNull(authorizationResult.getAuthorizationResponse());

        Assert.assertNotNull(authorizationErrorResponse);
        Assert.assertEquals(ErrorStrings.INVALID_SCOPE, authorizationErrorResponse.getError());
    }

    //===========================================================================================================
    // Token Request Testing
    //===========================================================================================================x
    @Test
    public void testDeviceCodeFlowTokenInvalidRequest() throws IOException, ClientException {
        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setAuthorityUrl(new URL(mUrlBody));
        final OAuth2StrategyParameters options = new OAuth2StrategyParameters();
        final OAuth2Strategy strategy = new MicrosoftStsOAuth2Strategy(config, options);

        final TokenResult tokenResult = strategy.requestToken(mTokenRequest);
        Assert.assertNull(tokenResult.getTokenResponse());
        Assert.assertNotNull(tokenResult.getErrorResponse());
        Assert.assertEquals(ErrorStrings.INVALID_REQUEST, tokenResult.getErrorResponse().getError());
    }

    @Test
    public void testDeviceCodeFlowTokenExpiredToken() throws IOException, ClientException {
        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setAuthorityUrl(new URL(mUrlBody));
        final OAuth2StrategyParameters options = new OAuth2StrategyParameters();
        final OAuth2Strategy strategy = new MicrosoftStsOAuth2Strategy(config, options);

        // Previously authenticated code
        mTokenRequest.setDeviceCode(
                "AAQABAAEAAAAm-06blBE1TpVMil8KPQ41e5vDLI7te0y-3XHYO_uurPryAiyBiPiKnjEVzAQZQzCyGZERne4a" +
                        "IwYAiBlQ8an93ENYuVOO-vEAt48FEJSEMQqq-zHZVD59bkc6eYIAViZKVvTv5_qilKj4uEjVE9BGkIxY5B6Uq1K8oWHEqzH-w6CiWjC8vQc6mSV_FPCbnAggAA");

        final TokenResult tokenResult = strategy.requestToken(mTokenRequest);
        Assert.assertNull(tokenResult.getTokenResponse());
        Assert.assertNotNull(tokenResult.getErrorResponse());
        Assert.assertEquals(ErrorStrings.DEVICE_CODE_FLOW_EXPIRED_TOKEN_CODE, tokenResult.getErrorResponse().getError());
    }

    // A device code that has not yet been registered leads to invalid_grant, not bad_verification_code
//    @Test
//    public void testDeviceCodeFlowTokenBadVerificationCode() throws IOException, ClientException {
//        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
//        config.setAuthorityUrl(new URL(mUrlBody));
//        final OAuth2StrategyParameters options = new OAuth2StrategyParameters();
//        final OAuth2Strategy strategy = new MicrosoftStsOAuth2Strategy(config, options);
//        mTokenRequest.setDeviceCode(
//                "AAQABAAEAAAAm-06blBE1TpVMil8KPQ41e5vDLI7te0y-3XHYO_uurPryAiyBiPiKnjEVzAQZQzCyGZERne4a" +
//                        "IwYAiBlQ8an93ENYuVOO-vEAt48FEJSEMQqq-zHZVD59bkc6eYIAViZKVvTv5_qilKj4uEjVE9BGkIxY5B6Uq1K8oWHEqzH-w6CiWjC8vQc6mSV_FPCbnAggBA");
//
//        final TokenResult tokenResult = strategy.requestToken(mTokenRequest);
//        Assert.assertNull(tokenResult.getTokenResponse());
//        Assert.assertNotNull(tokenResult.getErrorResponse());
//        Assert.assertEquals(ErrorStrings.DEVICE_CODE_FLOW_BAD_VERIFICATION_CODE, tokenResult.getErrorResponse().getError());
//    }

    // authorization_declined is triggered in the actual auth side
//    @Test
//    public void testDeviceCodeFlowTokenAuthorizationDeclined() throws IOException, ClientException {
//        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
//        config.setAuthorityUrl(new URL(mUrlBody));
//        final OAuth2StrategyParameters options = new OAuth2StrategyParameters();
//        final OAuth2Strategy strategy = new MicrosoftStsOAuth2Strategy(config, options);
//
//        final TokenResult tokenResult = strategy.requestToken(mTokenRequest);
//        Assert.assertNull(tokenResult.getTokenResponse());
//        Assert.assertNotNull(tokenResult.getErrorResponse());
//        Assert.assertEquals(ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_DECLINED_CODE, tokenResult.getErrorResponse().getError());
//    }

    //===========================================================================================================
    // API-Side Testing
    //===========================================================================================================
    @Test
    @Config(shadows = {ShadowDeviceCodeFlowCommandAuthError.class})
    public void testDeviceCodeFlowAuthFailure() {
        String[] scope = {"user.read"};
        mApplication.acquireTokenWithDeviceCode(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void onUserCodeReceived(@NonNull String vUri, @NonNull String userCode, @NonNull String message) {
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
            public void onUserCodeReceived(@NonNull String vUri, @NonNull String userCode, @NonNull String message) {
                // Assert that the protocol returns the userCode and others after successful authorization
                Assert.assertNotNull(vUri);
                Assert.assertNotNull(userCode);
                Assert.assertNotNull(message);

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
                Assert.assertEquals(ErrorStrings.DEVICE_CODE_FLOW_EXPIRED_TOKEN_CODE, error.getErrorCode());
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
            public void onUserCodeReceived(@NonNull String vUri, @NonNull String userCode, @NonNull String message) {
                // Assert that the protocol returns the userCode and others after successful authorization
                Assert.assertNotNull(vUri);
                Assert.assertNotNull(userCode);
                Assert.assertNotNull(message);

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
