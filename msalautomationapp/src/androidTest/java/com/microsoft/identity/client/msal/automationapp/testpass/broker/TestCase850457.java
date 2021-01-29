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

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabResetHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.internal.testutils.labutils.LabResetHelper.resetPassword;

//[MSAL] Broker Auth with CA flow - password change
//https://identitydivision.visualstudio.com/DevEx/_workitems/edit/850457
public class TestCase850457 extends AbstractMsalBrokerTest {

    @Test
    public void test_850457() throws InterruptedException {
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        //acquiring token interactively
        final CountDownLatch interactiveLatch = new CountDownLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(interactiveLatch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();

        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(mLoginHint)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(getBroker())
                                .registerPageExpected(true)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        interactiveLatch.await();

        //changing password
        boolean resetPassword = resetPassword(username);
        Assert.assertTrue(resetPassword);

        Thread.sleep(TimeUnit.MINUTES.toMillis(10));

        //now expire AT.
        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();
        Thread.sleep(TimeUnit.SECONDS.toMillis(30));

        //acquiring token interactively after changing password.
        final CountDownLatch newInteractiveLatch = new CountDownLatch(1);

        final AcquireTokenParameters newparameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(null)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(newInteractiveLatch))
                .withPrompt(Prompt.WHEN_REQUIRED)
                .build();

        final InteractiveRequest newInteractiveRequest = new InteractiveRequest(
                mApplication,
                newparameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final PromptHandlerParameters newPromptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(null)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(getBroker())
                                .expectingLoginPageAccountPicker(true)
                                .expectingBrokerAccountChooserActivity(true)
                                .registerPageExpected(true)
                                .build();

                        new AadPromptHandler(newPromptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        newInteractiveRequest.execute();
        newInteractiveLatch.await();

    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        return null;
    }

    @Override
    public String getTempUserType() {
        return LabConstants.TempUserType.MAMCA;
    }
}
