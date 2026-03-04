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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.joined;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;


import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

// [Joined][MSAL] In-line WPJ: Perform Device registration with deviceid claim
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1561125
@RetryOnFailure(retryCount = 2)
public class TestCase1561125 extends AbstractMsalBrokerTest {
    final String TAG = TestCase1561125.class.getSimpleName();
    private IAccount mTempAccount = null;

    @Test
    public void test_1561125_Joined_DeviceIdClaimWPJ() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final MsalSdk msalSdk = new MsalSdk();
        // create claims request object
        final ClaimsRequest claimsRequest = new ClaimsRequest();
        final RequestedClaimAdditionalInformation requestedClaimAdditionalInformation =
                new RequestedClaimAdditionalInformation();

        requestedClaimAdditionalInformation.setEssential(true);

        // request the deviceid claim in ID Token
        claimsRequest.requestClaimInIdToken("deviceid", requestedClaimAdditionalInformation);

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .claims(claimsRequest)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();


        // start interactive acquire token request in MSAL (should succeed)
        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.LOGIN)
                        .loginHint(username)
                        .sessionExpected(false)
                        .registerPageExpected(true)
                        .speedBumpExpected(false)
                        .broker(mBroker)
                        .expectingLoginPageAccountPicker(false)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult.assertSuccess();
        Map<String, ?> tokens = IDToken.parseJWT(authResult.getAccessToken());
        Assert.assertNotNull(tokens.get("deviceid"));

        // remove the device registration owner account from settings page. following AT should be interactive
        Logger.i(TAG, "Remove device registration owner account from settings page");
        getSettingsScreen().removeAccount(username);

        createTempAccountAndAcquireToken(true);
    }

    private String createTempAccountAndAcquireToken(boolean registerPageExpected) throws LabApiException {
        Logger.i(TAG, "Create temp account from lab");
        final ILabAccount labAccount = mLabClient.createTempAccount(TempUserType.BASIC);
        final String tempAccountUserName = labAccount.getUsername();
        final String tempAccountPassword = labAccount.getPassword();

        final TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(tempAccountUserName)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(interactiveRequestCallback(tokenRequestLatch))
                .withClaims(deviceIdClaimsRequest())
                .build();

        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                () -> {
                    final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                            .prompt(PromptParameter.SELECT_ACCOUNT)
                            .loginHint(tempAccountUserName)
                            .sessionExpected(false)
                            .consentPageExpected(false)
                            .registerPageExpected(registerPageExpected)
                            .speedBumpExpected(false)
                            .build();

                    new AadPromptHandler(promptHandlerParameters)
                            .handlePrompt(tempAccountUserName, tempAccountPassword);
                }
        );
        Logger.i(TAG, "Perform interactive token request for temp account, with deviceid claim.");
        interactiveRequest.execute();
        tokenRequestLatch.await(TokenRequestTimeout.SHORT);
        return tempAccountUserName;
    }

    private AuthenticationCallback interactiveRequestCallback(final CountDownLatch latch) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));
                mTempAccount = authenticationResult.getAccount();
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                Assert.fail(exception.getMessage());
                latch.countDown();
            }

            @Override
            public void onCancel() {
                Assert.fail("User cancelled flow");
                latch.countDown();
            }
        };
    }

    private ClaimsRequest deviceIdClaimsRequest() {
        RequestedClaimAdditionalInformation information = new RequestedClaimAdditionalInformation();
        information.setEssential(true);
        ClaimsRequest claimsRequest = new ClaimsRequest();
        claimsRequest.requestClaimInAccessToken("deviceid", information);
        return claimsRequest;
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}


