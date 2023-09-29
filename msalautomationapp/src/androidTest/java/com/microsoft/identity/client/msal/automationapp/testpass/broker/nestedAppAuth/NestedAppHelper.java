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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.nestedAppAuth;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadLoginComponentHandler;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.broker.BrokerData;
import com.microsoft.identity.common.internal.commands.parameters.AndroidActivityInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NestedAppHelper {

    private AndroidActivityInteractiveTokenCommandParameters mInteractiveParameters;

    private final ILabAccount mLabAccount;
    private final BrokerMsalController mController;

    private final IPlatformComponents mPlatformComponents;

    private final Activity mActivity;

    private final String mAuthorityUrl;

    private static final String HUB_APP_CLIENT_ID = "1fec8e78-bce4-4aaf-ab1b-5451cc387264";

    private static final String NESTED_APP_CLIENT_ID = "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0";

    private static final String NESTED_APP_US_GOV_CLIENT_ID = "cb7faed4-b8c0-49ee-b421-f5ed16894c83";

    private static final String NESTED_APP_REDIRECT_URI = "brk-multihub://www.msaltest.com";

    private static final String HUB_APP_REDIRECT_URI = "msauth://com.microsoft.teams/VCpKgbYCXucoq1mZ4BZPsh5taNE=";

    private static final String HUB_APP_US_GOV_REDIRECT_URI = "https://login.microsoftonline.com/common/oauth2/nativeclient";

    private static final String US_GOV_AUTHORITY = "https://login.microsoftonline.us/common";

    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";

    private static final String APP_ID = "appid";

    private static final String REQUIRED_PROTOCOL_VERSION_FIFTEEN = "15.0";

    private static final String DEVICE_ID_CLAIM = "{\"access_token\":{\"deviceid\":{\"essential\":true}}}";

    private static final int TIME_OUT_IN_SECONDS = 120;

    private static final String CLIENT_TEST_SDK_VERSION = "1.0.0";

    public NestedAppHelper(Activity activity, ILabAccount labAccount) {
        mPlatformComponents = AndroidPlatformComponentsFactory.createFromActivity(
                activity, null /*fragment*/);
        mController = new BrokerMsalController(
                activity.getApplicationContext(),
                mPlatformComponents,
                BrokerData.getDebugBrokerHost().getPackageName()
        );

       mAuthorityUrl = "https://login.microsoftonline.com/common";
        mInteractiveParameters = AndroidActivityInteractiveTokenCommandParameters
                .builder()
                .platformComponents(mPlatformComponents)
                .sdkType(SdkType.MSAL_CPP)
                .callerPackageName(activity.getPackageName())
                .callerSignature(activity.getPackageName())
                .applicationName(activity.getPackageName())
                .applicationVersion(CLIENT_TEST_SDK_VERSION)
                .sdkVersion(CLIENT_TEST_SDK_VERSION)
                .authority(Authority.getAuthorityFromAuthorityUrl(mAuthorityUrl))
                .scopes(Collections.singleton(GRAPH_SCOPE))
                .redirectUri(HUB_APP_REDIRECT_URI)
                .clientId(HUB_APP_CLIENT_ID)
                .requiredBrokerProtocolVersion(REQUIRED_PROTOCOL_VERSION_FIFTEEN)
                .loginHint(labAccount.getUsername())
                .authenticationScheme(new BearerAuthenticationSchemeInternal())
                .correlationId(UUID.randomUUID().toString())
                .prompt(OpenIdConnectPromptParameter.UNSET)
                .authorizationAgent(AuthorizationAgent.WEBVIEW)
                .build();
        mLabAccount = labAccount;
        mActivity = activity;
    }

    protected void performATForHubApp() {
        final CompletableFuture<Void> handlePromptFuture = CompletableFuture.runAsync(() -> {
            handlePromptAsync(this::handlePrompt);
        });
        final CompletableFuture<AcquireTokenResult> acquireTokenFuture = CompletableFuture.supplyAsync(this::acquireTokenAsync);

        try {
            CompletableFuture.allOf(handlePromptFuture, acquireTokenFuture).get(TIME_OUT_IN_SECONDS, TimeUnit.SECONDS);
            final AcquireTokenResult tokenResult = acquireTokenFuture.get();
            Assert.assertNotNull(tokenResult);
            Assert.assertTrue(tokenResult.getSucceeded());
            final String appId =
                    (String) IDToken.parseJWT(tokenResult.getLocalAuthenticationResult().getAccessToken()).get(APP_ID);
            Assert.assertEquals(HUB_APP_CLIENT_ID, appId);

        } catch (InterruptedException | ExecutionException | TimeoutException |
                 ServiceException e) {
            throw new AssertionError(e);
        }
    }

    protected void  performATForHubAppInUSGovCloud() {
        mInteractiveParameters = AndroidActivityInteractiveTokenCommandParameters
                .builder()
                .platformComponents(mPlatformComponents)
                .sdkType(SdkType.MSAL_CPP)
                .callerPackageName(mActivity.getPackageName())
                .callerSignature(mActivity.getPackageName())
                .applicationName(mActivity.getPackageName())
                .applicationVersion(CLIENT_TEST_SDK_VERSION)
                .sdkVersion(CLIENT_TEST_SDK_VERSION)
                .authority(Authority.getAuthorityFromAuthorityUrl(US_GOV_AUTHORITY))
                .scopes(Collections.singleton(GRAPH_SCOPE))
                .redirectUri(HUB_APP_US_GOV_REDIRECT_URI)
                .clientId(HUB_APP_CLIENT_ID)
                .requiredBrokerProtocolVersion(REQUIRED_PROTOCOL_VERSION_FIFTEEN)
                .loginHint(mLabAccount.getUsername())
                .authenticationScheme(new BearerAuthenticationSchemeInternal())
                .correlationId(UUID.randomUUID().toString())
                .prompt(OpenIdConnectPromptParameter.UNSET)
                .authorizationAgent(AuthorizationAgent.WEBVIEW)
                .build();
        performATForHubApp();
    }

    protected void performATSilentForNestedApp(AccountRecord accountRecord, boolean shouldAddDeviceIdClaim) throws BaseException {
        String claimsJsonString = "";
        if (shouldAddDeviceIdClaim) {
            claimsJsonString = DEVICE_ID_CLAIM;
        }
        final SilentTokenCommandParameters mSilentTokenCommandParameters =
                SilentTokenCommandParameters
                        .builder()
                        .platformComponents(mPlatformComponents)
                        .requiredBrokerProtocolVersion(REQUIRED_PROTOCOL_VERSION_FIFTEEN)
                        .sdkType(SdkType.MSAL_CPP)
                        .applicationName(mActivity.getPackageName())
                        .applicationVersion(CLIENT_TEST_SDK_VERSION)
                        .sdkVersion(CLIENT_TEST_SDK_VERSION)
                        .authority(Authority.getAuthorityFromAuthorityUrl(mAuthorityUrl))
                        .account(accountRecord)
                        .forceRefresh(true)
                        .scopes(Collections.singleton(GRAPH_SCOPE))
                        .redirectUri(HUB_APP_REDIRECT_URI)
                        .clientId(HUB_APP_CLIENT_ID)
                        .childRedirectUri(NESTED_APP_REDIRECT_URI)
                        .childClientId(NESTED_APP_CLIENT_ID)
                        .authenticationScheme(new BearerAuthenticationSchemeInternal())
                        .correlationId(UUID.randomUUID().toString())
                        .claimsRequestJson(claimsJsonString)
                        .build();

        Assert.assertEquals(mLabAccount.getUsername(), accountRecord.getUsername());

        final AcquireTokenResult acquireTokenSilentResult = mController.acquireTokenSilent(mSilentTokenCommandParameters);
        Assert.assertNotNull(acquireTokenSilentResult);
        Assert.assertTrue(acquireTokenSilentResult.getSucceeded());
        // NAA requests must not be serviced from cache
        Assert.assertFalse(acquireTokenSilentResult.getLocalAuthenticationResult().isServicedFromCache());
        // cannot parse jwt for MSA
        if (mLabAccount.getUserType() != UserType.MSA) {
            final String appId = (String) IDToken.parseJWT(acquireTokenSilentResult.getLocalAuthenticationResult().getAccessToken()).get(APP_ID);
            Assert.assertEquals(NESTED_APP_CLIENT_ID, appId);
        }
    }

    protected void performATSilentForNestedAppInUSGovCloud(AccountRecord accountRecord) throws BaseException {
        final SilentTokenCommandParameters mSilentTokenCommandParameters =
                SilentTokenCommandParameters
                        .builder()
                        .platformComponents(mPlatformComponents)
                        .requiredBrokerProtocolVersion(REQUIRED_PROTOCOL_VERSION_FIFTEEN)
                        .sdkType(SdkType.MSAL_CPP)
                        .applicationName(mActivity.getPackageName())
                        .applicationVersion("1.0.0")
                        .sdkVersion("1.0.0")
                        .authority(Authority.getAuthorityFromAuthorityUrl(US_GOV_AUTHORITY))
                        .account(accountRecord)
                        .forceRefresh(true)
                        .scopes(Collections.singleton(GRAPH_SCOPE))
                        .redirectUri(HUB_APP_US_GOV_REDIRECT_URI)
                        .clientId(HUB_APP_CLIENT_ID)
                        .childRedirectUri(NESTED_APP_REDIRECT_URI)
                        .childClientId(NESTED_APP_US_GOV_CLIENT_ID)
                        .authenticationScheme(new BearerAuthenticationSchemeInternal())
                        .correlationId(UUID.randomUUID().toString())
                        .build();

        Assert.assertEquals(mLabAccount.getUsername(), accountRecord.getUsername());

        final AcquireTokenResult acquireTokenSilentResult = mController.acquireTokenSilent(mSilentTokenCommandParameters);
        Assert.assertNotNull(acquireTokenSilentResult);
        Assert.assertTrue(acquireTokenSilentResult.getSucceeded());
        // NAA requests must not be serviced from cache
        Assert.assertFalse(acquireTokenSilentResult.getLocalAuthenticationResult().isServicedFromCache());
        // cannot parse jwt for MSA
        if (mLabAccount.getUserType() != UserType.MSA) {
            final String appId = (String) IDToken.parseJWT(acquireTokenSilentResult.getLocalAuthenticationResult().getAccessToken()).get(APP_ID);
            Assert.assertEquals(NESTED_APP_US_GOV_CLIENT_ID, appId);
        }
    }

    protected void performInteractiveATForNestedApp(boolean shouldAddDeviceIdClaim) {
        String claimsJsonString = "";
        if (shouldAddDeviceIdClaim) {
            claimsJsonString = DEVICE_ID_CLAIM;
        }
        mInteractiveParameters = AndroidActivityInteractiveTokenCommandParameters
                .builder()
                .platformComponents(mPlatformComponents)
                .sdkType(SdkType.MSAL_CPP)
                .callerPackageName(mActivity.getPackageName())
                .callerSignature(mActivity.getApplicationContext().getPackageName())
                .applicationName(mActivity.getApplicationContext().getPackageName())
                .applicationVersion("1.0.0")
                .sdkVersion()
                .authority(Authority.getAuthorityFromAuthorityUrl(mAuthorityUrl))
                .scopes(Collections.singleton(GRAPH_SCOPE))
                .redirectUri(HUB_APP_REDIRECT_URI)
                .clientId(HUB_APP_CLIENT_ID)
                .requiredBrokerProtocolVersion(REQUIRED_PROTOCOL_VERSION_FIFTEEN)
                .loginHint(mLabAccount.getUsername())
                .authenticationScheme(new BearerAuthenticationSchemeInternal())
                .correlationId(UUID.randomUUID().toString())
                .childRedirectUri(NESTED_APP_REDIRECT_URI)
                .childClientId(NESTED_APP_CLIENT_ID)
                .prompt(OpenIdConnectPromptParameter.UNSET)
                .claimsRequestJson(claimsJsonString)
                .authorizationAgent(AuthorizationAgent.WEBVIEW)
                .build();

        final CompletableFuture<AcquireTokenResult> acquireTokenFuture = CompletableFuture.supplyAsync(this::acquireTokenAsync);


        CompletableFuture<Void> handlePromptFuture = CompletableFuture.runAsync(() -> {
            handlePromptAsync(this::handlePrompt);
        });

        try {
            if (shouldAddDeviceIdClaim) {
                CompletableFuture<Void>  handleRegisterPromptFuture = CompletableFuture.runAsync(() -> {
                    handlePromptAsync(this::handleRegistration);
                });
                CompletableFuture.allOf(handlePromptFuture, handleRegisterPromptFuture, acquireTokenFuture).get(TIME_OUT_IN_SECONDS, TimeUnit.SECONDS);
            } else {
                CompletableFuture.allOf(handlePromptFuture, acquireTokenFuture).get(TIME_OUT_IN_SECONDS, TimeUnit.SECONDS);
            }
            final AcquireTokenResult tokenResult = acquireTokenFuture.get();
            Assert.assertNotNull(tokenResult);
            Assert.assertTrue(tokenResult.getSucceeded());
            final String appId =
                    (String) IDToken.parseJWT(tokenResult.getLocalAuthenticationResult().getAccessToken()).get(APP_ID);
            Assert.assertEquals(NESTED_APP_CLIENT_ID, appId);

        } catch (InterruptedException | ExecutionException | TimeoutException |
                 ServiceException e) {
            throw new AssertionError(e);
        }
    }

    protected AccountRecord getAccountRecordAfterHubAppAT() {
        try {
            Assert.assertEquals(1, mController.getAccounts(mInteractiveParameters).size());
            final AccountRecord accountRecord = mController.getAccounts(mInteractiveParameters).get(0).getAccount();
            Assert.assertEquals(mLabAccount.getUsername(), accountRecord.getUsername());
            return accountRecord;
        } catch (BaseException e) {
            throw new RuntimeException(e);
        }
    }

    protected void getAccountRecordAfterNestedAppAT() {
        try {
            // nested app interactive requests do not save the account in the cache
            Assert.assertEquals(0, mController.getAccounts(mInteractiveParameters).size());
        } catch (BaseException e) {
            throw new RuntimeException(e);
        }
    }

    private void handlePrompt() {
        // perform UI action
        final AadLoginComponentHandler aadLoginComponentHandler = new AadLoginComponentHandler(CommonUtils.FIND_UI_ELEMENT_TIMEOUT_LONG);
        aadLoginComponentHandler.handlePasswordField(mLabAccount.getPassword());
    }

    private void handlePromptAsync(@NonNull Runnable promptHandler) {
        CompletableFuture.supplyAsync(() -> {
            promptHandler.run();
            return null;
        });
    }

    private AcquireTokenResult acquireTokenAsync() {
        try {
            return mController.acquireToken(mInteractiveParameters);
        } catch (BaseException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRegistration() {
        final UiObject registerBtn = UiAutomatorUtils.obtainUiObjectWithText("Register", CommonUtils.FIND_UI_ELEMENT_TIMEOUT_LONG);
        Assert.assertTrue("Register page appears.", registerBtn.exists());
        UiAutomatorUtils.handleButtonClick("idSIButton9", CommonUtils.FIND_UI_ELEMENT_TIMEOUT_LONG);
    }
}
