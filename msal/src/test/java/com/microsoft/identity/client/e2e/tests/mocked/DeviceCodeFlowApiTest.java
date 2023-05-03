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

import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_AAD_USGOV_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.SINGLE_ACCOUNT_DCF_TEST_CONFIG_FILE_PATH;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowDeviceCodeFlowCommandAuthError;
import com.microsoft.identity.client.e2e.shadows.ShadowDeviceCodeFlowCommandSuccessful;
import com.microsoft.identity.client.e2e.shadows.ShadowDeviceCodeFlowCommandTokenError;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationErrorResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.java.util.StringUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Testing class for the device code flow protocol. Currently only supporting testing for the API-side
 * of the protocol. Will be extended to test individual aspects of the flow.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowPublicClientApplicationConfiguration.class})
@SuppressWarnings("unchecked")
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
        final OAuth2StrategyParameters strategyParameters = OAuth2StrategyParameters.builder().build();
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
                Assert.assertFalse(StringUtil.isNullOrEmpty(vUri));
                Assert.assertFalse(StringUtil.isNullOrEmpty(userCode));
                Assert.assertFalse(StringUtil.isNullOrEmpty(message));
                Assert.assertNotNull(sessionExpirationDate);

                Assert.assertFalse(mUserCodeReceived);
                mUserCodeReceived = true;
            }

            @Override
            public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                Assert.assertTrue(mUserCodeReceived);
                Assert.assertNotNull(authResult);
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                // This shouldn't run
                throw new AssertionError(exception);
            }
        });

        RoboTestUtils.flushScheduler();
    }

    @Test
    @Config(shadows = {ShadowDeviceCodeFlowCommandSuccessful.class})
    public void testDeviceCodeFlowSuccessWithList() {
        List<String> scope = new ArrayList<>();
        scope.add("user.read");
        mApplication.acquireTokenWithDeviceCode(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void onUserCodeReceived(@NonNull String vUri,
                                           @NonNull String userCode,
                                           @NonNull String message,
                                           @NonNull Date sessionExpirationDate) {
                // Assert that the protocol returns the userCode and others after successful authorization
                Assert.assertFalse(StringUtil.isNullOrEmpty(vUri));
                Assert.assertFalse(StringUtil.isNullOrEmpty(userCode));
                Assert.assertFalse(StringUtil.isNullOrEmpty(message));
                Assert.assertNotNull(sessionExpirationDate);

                Assert.assertFalse(mUserCodeReceived);
                mUserCodeReceived = true;
            }

            @Override
            public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                Assert.assertTrue(mUserCodeReceived);
                Assert.assertNotNull(authResult);
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                // This shouldn't run
                throw new AssertionError(exception);
            }
        });

        RoboTestUtils.flushScheduler();
    }

    // With 2 PCA objects initalized with different clouds, make sure that each clouds are
    // returning the correct URI from each endpoints.
    // https://portal.microsofticm.com/imp/v3/incidents/details/325344544/home
    @Test
    public void testInitializingMultiplePCAFromDifferentClouds() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();

        final IPublicClientApplication[] apps = new IPublicClientApplication[2];
        PublicClientApplication.create(context, new File(MULTIPLE_ACCOUNT_MODE_AAD_USGOV_CONFIG_FILE_PATH),
                new PublicClientApplication.ApplicationCreatedListener() {
                    @Override
                    public void onCreated(IPublicClientApplication application) {
                        apps[0] = application;
                    }

                    @Override
                    public void onError(MsalException exception) {
                        // This shouldn't run
                        Assert.fail();
                    }
                });

        RoboTestUtils.flushScheduler();

        PublicClientApplication.create(context, new File(MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH),
                new PublicClientApplication.ApplicationCreatedListener() {
                    @Override
                    public void onCreated(IPublicClientApplication application) {
                        apps[1] = application;
                    }

                    @Override
                    public void onError(MsalException exception) {
                        // This shouldn't run
                        Assert.fail();
                    }
                });

        RoboTestUtils.flushScheduler();

        final IPublicClientApplication usGovApp = apps[0];
        final IPublicClientApplication wwApp = apps[1];

        final List<String> scope = new ArrayList<>();
        scope.add("user.read");

        // Note: we can use resultFuture here because this method is not dispatched back to main thread
        // via AndroidPlatformUtil.postCommandResult()
        final ResultFuture<String> wwUri = new ResultFuture<String>();
        final ResultFuture<String> usGovUri = new ResultFuture<String>();

        final String[] uris = new String[2];
        wwApp.acquireTokenWithDeviceCode(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void onUserCodeReceived(@NonNull String vUri, @NonNull String userCode, @NonNull String message, @NonNull Date sessionExpirationDate) {
                wwUri.setResult(vUri);
            }

            @Override
            public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                // This shouldn't run
                Assert.fail();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                // This shouldn't run
                wwUri.setException(exception);
            }
        });

        usGovApp.acquireTokenWithDeviceCode(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void onUserCodeReceived(@NonNull String vUri, @NonNull String userCode, @NonNull String message, @NonNull Date sessionExpirationDate) {
                usGovUri.setResult(vUri);
            }

            @Override
            public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                // This shouldn't run
                Assert.fail();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                // This shouldn't run
                usGovUri.setException(exception);
            }
        });

        final String ww = wwUri.get(10, TimeUnit.SECONDS);
        final String usgov = usGovUri.get(10, TimeUnit.SECONDS);

        Assert.assertEquals(ww, "https://microsoft.com/devicelogin");
        Assert.assertEquals(usgov, "https://microsoft.com/deviceloginus");
    }

    // The same device code url shall be the same for 2 PCA objects with the same configuration (pointing to USGov)
    // Even if the 1st PCA was used to invoke DCF prior to the 2nd one.
    // https://portal.microsofticm.com/imp/v3/incidents/details/325344544/home
    // NOTE: This one FAILS.
    @Test
    public void testInitializingMultiplePCAWithSameUsGovConfig_OnlyDeviceCodeFlowUSGovURLShouldBeReturned() throws Exception {
        final Context context = ApplicationProvider.getApplicationContext();

        final List<String> scope = new ArrayList<>();
        scope.add("user.read");

        // 1. created one public client application with a us gov configuration file.
        final IPublicClientApplication[] apps = new IPublicClientApplication[2];
        PublicClientApplication.create(context,
                new File(MULTIPLE_ACCOUNT_MODE_AAD_USGOV_CONFIG_FILE_PATH),
                new IPublicClientApplication.ApplicationCreatedListener() {
                    @Override
                    public void onCreated(IPublicClientApplication application) {
                        apps[0] = application;
                    }

                    @Override
                    public void onError(MsalException exception) {
                        // This shouldn't run
                        Assert.fail();
                    }
                });

        RoboTestUtils.flushScheduler();

        // 2. use this public client application to acquire the user code, this time the returned user code is correct.
        final IPublicClientApplication pcaApp1 = apps[0];
        Assert.assertEquals("https://login.microsoftonline.us/common", pcaApp1.getConfiguration().getAuthorities().get(0).getAuthorityURL().toString());

        final ResultFuture<String> uri1 = new ResultFuture<String>();
        pcaApp1.acquireTokenWithDeviceCode(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void onUserCodeReceived(@NonNull String vUri, @NonNull String userCode, @NonNull String message, @NonNull Date sessionExpirationDate) {
                uri1.setResult(vUri);
            }

            @Override
            public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                // This shouldn't run
                Assert.fail();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                // This shouldn't run
                uri1.setException(exception);
            }
        });

        Assert.assertEquals("https://microsoft.com/deviceloginus", uri1.get(10, TimeUnit.SECONDS));

        //3. then create another public client application with the same configuration file in step 1.
        PublicClientApplication.create(context, new File(MULTIPLE_ACCOUNT_MODE_AAD_USGOV_CONFIG_FILE_PATH),
                new IPublicClientApplication.ApplicationCreatedListener() {
                    @Override
                    public void onCreated(IPublicClientApplication application) {
                        apps[1] = application;
                    }

                    @Override
                    public void onError(MsalException exception) {
                        // This shouldn't run
                        Assert.fail();
                    }
                });

        RoboTestUtils.flushScheduler();

        final IPublicClientApplication pcaApp2 = apps[1];
        Assert.assertEquals("https://login.microsoftonline.us/common", pcaApp2.getConfiguration().getAuthorities().get(0).getAuthorityURL().toString());

        // Note: we can use resultFuture here because this method is not dispatched back to main thread
        // via AndroidPlatformUtil.postCommandResult()
        final ResultFuture<String> uri2 = new ResultFuture<String>();
        pcaApp2.acquireTokenWithDeviceCode(scope, new IPublicClientApplication.DeviceCodeFlowCallback() {
            @Override
            public void onUserCodeReceived(@NonNull String vUri, @NonNull String userCode, @NonNull String message, @NonNull Date sessionExpirationDate) {
                uri2.setResult(vUri);
            }

            @Override
            public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                // This shouldn't run
                Assert.fail();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                // This shouldn't run
                uri2.setException(exception);
            }
        });

        // Should still get USGOV back.
        Assert.assertEquals("https://microsoft.com/deviceloginus", uri2.get(10, TimeUnit.SECONDS));
    }
}
