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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.joined;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

// Broker Delete account via Account Manager
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/833558
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class})
@RetryOnFailure(retryCount = 2)
public class TestCase833558 extends AbstractMsalBrokerTest {
    final String TAG = TestCase833558.class.getSimpleName();
    private IAccount mTempAccount = null;
    @Test
    public void test_msal_833558() throws Throwable {
        Logger.i(TAG, "Get user account from lab");
        final String deviceRegistrationOwnerUsername = mLabAccount.getUsername();
        final String deviceRegistrationOwnerPassword = mLabAccount.getPassword();

        Logger.i(TAG, "Perform device registration with the user account from lab");
        mBroker.performDeviceRegistration(deviceRegistrationOwnerUsername, deviceRegistrationOwnerPassword);

        mBroker.forceStop();

        String tempAccountUserName = createTempAccountAndAcquireToken(false);

        Logger.i(TAG, "Remove temp account from settings page");
        getSettingsScreen().removeAccount(tempAccountUserName);

        Logger.i(TAG, "Perform silent token request for temp account, expects no_account_found error");
        final TokenRequestLatch silentTokenLatch = new TokenRequestLatch(1);
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(getAuthority())
                .forAccount(mTempAccount)
                .withCallback(failedSilentCallback(silentTokenLatch, "no_account_found"))
                .build();
        mApplication.acquireTokenSilentAsync(silentParameters);
        silentTokenLatch.await(TokenRequestTimeout.SILENT);

        Logger.i(TAG, "Remove device registration owner account from settings page");
        getSettingsScreen().removeAccount(deviceRegistrationOwnerUsername);

        createTempAccountAndAcquireToken(true);
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
        return "https://login.microsoftonline.com/common";
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
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

    private SilentAuthenticationCallback failedSilentCallback(final CountDownLatch latch, final String errorCode) {
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Assert.fail("Unexpected success");
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                Assert.assertEquals(errorCode, exception.getErrorCode());
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
}
