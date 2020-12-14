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
package com.microsoft.identity.client.msal.automationapp.testpass.b2c;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.b2c.B2CPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.b2c.B2CProvider;
import com.microsoft.identity.client.ui.automation.interaction.b2c.IdLabB2cSisoPolicyPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

public class B2CIdLabSisoPolicyTest extends AbstractB2CTest {

    final static B2CProvider[] b2CProviders = new B2CProvider[]{
            B2CProvider.Local,
            B2CProvider.MSA,
            B2CProvider.Google,
            B2CProvider.Facebook,
    };

    @Parameterized.Parameters(name = "{0}")
    public static B2CProvider[] data() {
        return b2CProviders;
    }

    private final B2CProvider mB2cProvider;

    public B2CIdLabSisoPolicyTest(@NonNull final B2CProvider b2CProvider) {
        mB2cProvider = b2CProvider;
    }

    @Override
    public B2CProvider getB2cProvider() {
        return mB2cProvider;
    }

    public void testCanLoginWithLocalAndSocialAccounts() {
        final TokenRequestLatch latch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();


        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        ((IApp) mBrowser).handleFirstRun();

                        final String username = mLoginHint;
                        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

                        final B2CPromptHandlerParameters promptHandlerParameters = B2CPromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(mLoginHint)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(null)
                                .expectingBrokerAccountChooserActivity(false)
                                .b2cProvider(getB2cProvider())
                                .build();

                        new IdLabB2cSisoPolicyPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await(TokenRequestTimeout.MEDIUM);

        // ------ do silent request ------

        IAccount account = getAccount();

        final TokenRequestLatch silentLatch = new TokenRequestLatch(1);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(getAuthority())
                .forceRefresh(false)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulSilentCallback(silentLatch))
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        silentLatch.await(TokenRequestTimeout.SILENT);

        // ------ do force refresh silent request ------

        account = getAccount();

        final TokenRequestLatch silentForceLatch = new TokenRequestLatch(1);

        final AcquireTokenSilentParameters silentForceParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(getAuthority())
                .forceRefresh(true)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulSilentCallback(silentForceLatch))
                .build();

        mApplication.acquireTokenSilentAsync(silentForceParameters);
        silentForceLatch.await(TokenRequestTimeout.SILENT);
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_b2c_siso;
    }
}
