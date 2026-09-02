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
package com.microsoft.identity.client.msal.automationapp.testpass.perf.broker.nonjoined;

import android.util.Log;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Broker Auth for non-joined account - select_account
// https://identitydivision.visualstudio.com/DefaultCollection/DevEx/_workitems/edit/497069
//@RetryOnFailure(retryCount = 2)
public class TestBrokerPerfNonJoined extends AbstractMsalBrokerTest {

    final ExecutorService executorService = Executors.newFixedThreadPool(25);

    int f = 2;

    @Test
    public void testBrokerPerfNonJoined() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final MsalSdk msalSdk = new MsalSdk();

        // Interactive call
        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(getScopes()[0]))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(username)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .broker(mBroker)
                        .expectingBrokerAccountChooserActivity(false)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult.assertSuccess();

        final IAccount account = msalSdk.getAccount(mActivity,getConfigFileResourceId(),username);

        final int numberOfOccurrenceOfTest = getScopes().length * f;

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Silent call
        for(int i = 0; i < numberOfOccurrenceOfTest; i++) {
            int finalI = i;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    final MsalAuthTestParams silentParams = MsalAuthTestParams.builder()
                            .activity(mActivity)
                            .loginHint(username)
                            .authority(account.getAuthority())
                            .forceRefresh(true)
                            .scopes(Arrays.asList(getScopes()[finalI % f]))
                            .msalConfigResourceId(getConfigFileResourceId())
                            .build();


                    final long startTime = System.currentTimeMillis();

                    final MsalAuthResult silentAuthResult;
                    try {
                        silentAuthResult = msalSdk.acquireTokenSilent(silentParams, TokenRequestTimeout.SILENT);
                    } catch (Throwable e) {
                        future.completeExceptionally(e);
                        throw new RuntimeException(e);
                    }

                    final long endTime = System.currentTimeMillis();
                    final long elapsedTime = endTime - startTime;
                    Log.d("Perf", "Elapsed time for #" + finalI + " = " + elapsedTime);

                    silentAuthResult.assertSuccess();

                    if (finalI == (numberOfOccurrenceOfTest - 1)) {
                        future.complete(true);
                    }
                }
            });
        }

        future.get();
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{
                "user.read",
                "https://graph.microsoft.com/user.read",
                "https://graph.windows.net/user.read",
                "https://outlook.office.com/user.read",
                "https://graph.microsoft.com/.default"
        };
    }

    @Override
    public String getAuthority() {
        return "https://login.microsoftonline.com/common";
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

}
