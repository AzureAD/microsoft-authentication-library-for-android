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
package com.microsoft.identity.client.msal.automationapp.testpass.stress;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.powerbi.PowerBi;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TestCaseStress extends AbstractMsalUiStressTest<IAccount, IAuthenticationResult> {

    private String accessToken = null;
    private Date expiresAt = null;

    @Test
    public void testDStuff() {
        super.setup();

        final TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(new AuthenticationCallback() {
                    @Override
                    public void onCancel() {
                        tokenRequestLatch.countDown();
                    }

                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        tokenRequestLatch.countDown();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        tokenRequestLatch.countDown();
                    }
                })
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();

        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final String username = mLoginHint;
                        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(mLoginHint)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters).handlePrompt(username, password);
                    }
                }
        );

        tokenRequestLatch.await(TokenRequestTimeout.SHORT);
    }

    @Test
    public void test_acquireTokenSilentlyWithCachedTokens() throws Exception {
        run();
    }

    @Override
    public boolean isTestPassed(IAuthenticationResult result) {
        if (result == null) {
            return false;
        }

        if (accessToken == null || expiresAt.before(new Date())) {
            accessToken = result.getAccessToken();
            expiresAt = result.getExpiresOn();
        }

        return accessToken.equals(result.getAccessToken());
    }

    @Override
    public IAccount prepare() {
        final TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(tokenRequestLatch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();

        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final String username = mLoginHint;
                        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(mLoginHint)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters).handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        tokenRequestLatch.await(TokenRequestTimeout.SHORT);

        return getAccount();
    }

    @Override
    public IAuthenticationResult execute(final IAccount account) throws Exception {
        final AcquireTokenSilentParameters acquireTokenParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .forceRefresh(false)
                .withScopes(Arrays.asList(mScopes))
                .build();

        return mApplication.acquireTokenSilent(acquireTokenParameters);
    }

    @Override
    public int getNumberOfThreads() {
        return 10;
    }

    @Override
    public long getTimeLimit() {
        return TimeUnit.HOURS.toMinutes(4);
    }

    @Override
    public String getOutputFileName() {
        return "StressTestsAcquireTokenSilent.txt";
    }
}
