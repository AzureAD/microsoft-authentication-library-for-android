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
package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

// SovCloud: Interactive Auth w/o cache w/o MFA w/ Prompt Auto w/ Broker
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/796049
public class TestCase796049 extends AbstractMsalBrokerTest {

    @Test
    public void test_796049() {
        final TokenRequestLatch latch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withResource(mScopes[0])
                .withCallback(successfulInteractiveCallback(latch))
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
                                .loginHint(username)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(getBroker())
                                .expectingBrokerAccountChooserActivity(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await(TokenRequestTimeout.MEDIUM);

        // SECOND REQUEST WITHOUT LOGIN HINT

        final TokenRequestLatch latchNoLoginHint = new TokenRequestLatch(1);

        final AcquireTokenParameters parametersNoLoginHint = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withResource(mScopes[0])
                .withCallback(successfulInteractiveCallback(latchNoLoginHint))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();


        final InteractiveRequest interactiveRequestNoLoginHint = new InteractiveRequest(
                mApplication,
                parametersNoLoginHint,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final String username = mLoginHint;
                        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(null)
                                .sessionExpected(true)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(getBroker())
                                .expectingBrokerAccountChooserActivity(true)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequestNoLoginHint.execute();
        latchNoLoginHint.await(TokenRequestTimeout.MEDIUM);
    }


    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_GERMANY_CLOUD;
        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"00000002-0000-0000-c000-000000000000"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public ITestBroker getBroker() {
        return new BrokerMicrosoftAuthenticator();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_instance_aware_common;
    }
}
