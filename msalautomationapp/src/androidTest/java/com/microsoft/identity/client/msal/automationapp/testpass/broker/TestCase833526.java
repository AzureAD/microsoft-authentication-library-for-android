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
package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.IMdmAgent;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.UiResponse;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

// Broker Auth for MDM account
@RetryOnFailure(retryCount = 2)
public class TestCase833526 extends AbstractMsalBrokerTest {

    @Test
    public void test_833526() throws InterruptedException {
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        final CountDownLatch latch = new CountDownLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(cancelInteractiveCallback(latch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();

        // start interactive token request in MSAL
        // we expect to see enroll page as device is not enrolled
        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(username)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(getBroker())
                                .expectingBrokerAccountChooserActivity(false)
                                .enrollPageExpected(true)
                                // cancel enroll here to short circuit as enroll will be started manually from CP anyway
                                .enrollPageResponse(UiResponse.DECLINE)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await();

        // enroll device with CP

        final IMdmAgent mdmAgent = (IMdmAgent) mBroker;
        mdmAgent.enrollDevice(username, password);

        // SECOND REQUEST WITH LOGIN HINT

        final CountDownLatch latchTryAcquireAgain = new CountDownLatch(1);

        // try another interactive token request in MSAL
        // we should not see enroll page and request should succeed as device is already enrolled
        final AcquireTokenParameters parametersTryAcquireAgain = new AcquireTokenParameters.Builder()
                .withLoginHint(username)
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latchTryAcquireAgain))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();


        final InteractiveRequest interactiveRequestTryAgain = new InteractiveRequest(
                mApplication,
                parametersTryAcquireAgain,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(username)
                                .sessionExpected(true)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(getBroker())
                                .expectingBrokerAccountChooserActivity(true)
                                .enrollPageExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequestTryAgain.execute();
        latchTryAcquireAgain.await();
    }


    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        query.protectionPolicy = LabConstants.ProtectionPolicy.MDM_CA;
        return query;
    }

    @Override
    public String getTempUserType() {
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
    public ITestBroker getBroker() {
        return new BrokerCompanyPortal();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}
