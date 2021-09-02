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
package com.microsoft.identity.client.msal.automationapp.testpass.perf;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.appender.FileAppender;
import com.microsoft.identity.client.ui.automation.logging.formatter.SimpleTextFormatter;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.common.java.marker.CodeMarkerManager;
import com.microsoft.identity.common.java.marker.PerfConstants;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

//Perf test case with Joined AcquireToken test with MSAL and Broker
//This test case is build over the test case number 832430
public class TestCasePerfBrokered extends AbstractMsalBrokerTest {

    @Test
    public void test_acquireTokenSilentlyWithBroker() throws Throwable {
        CodeMarkerManager codeMarkerManager = CodeMarkerManager.getInstance();
        final int numberOfOccurrenceOfTest = 10;
        final String outputFilenamePrefix = "PerfDataTargetBrokerHostWR"; // With Resource
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        final MsalSdk msalSdk = new MsalSdk();

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .resource(mScopes[0])
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
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
                        .registerPageExpected(true)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult.assertSuccess();

        codeMarkerManager.setEnableCodeMarker(true);
        //Setting up scenario code. 100 -> Non Brokered, 200 -> Brokered
        codeMarkerManager.setPrefixScenarioCode(PerfConstants.ScenarioConstants.SCENARIO_BROKERED_ACQUIRE_TOKEN_SILENTLY);
        //acquiring token silently

        final IAccount account = msalSdk.getAccount(mActivity,getConfigFileResourceId(),username);
        for(int i = 0; i < numberOfOccurrenceOfTest; i++) {
            codeMarkerManager.clearMarkers();

            //acquiring token silently
            final MsalAuthTestParams silentParams = MsalAuthTestParams.builder()
                    .activity(mActivity)
                    .loginHint(username)
                    .authority(account.getAuthority())
                    .resource(mScopes[0])
                    .msalConfigResourceId(getConfigFileResourceId())
                    .build();

            final MsalAuthResult silentAuthResult = msalSdk.acquireTokenSilent(silentParams, TokenRequestTimeout.MEDIUM);
            silentAuthResult.assertSuccess();

            try {
                FileAppender fileAppender = new FileAppender(outputFilenamePrefix + i + ".txt", new SimpleTextFormatter());
                fileAppender.append(codeMarkerManager.getFileContent());
                CommonUtils.copyFileToFolderInSdCard(
                        fileAppender.getLogFile(),
                        "automation"
                );
            } catch (IOException e) {
                throw new AssertionError("IOException while writing Perf data file");
            }
            // If this is not the last iteration, then we need either to clear cache of access token manually or wait for 30 seconds.
            if (i < numberOfOccurrenceOfTest - 1) {
                // CommandDispatcherHelper.clear();
                try {
                    // Sleep for 30 seconds so that the cache access token cache is cleared.
                    Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                } catch (InterruptedException e) {
                    throw new AssertionError("Interrupted while sleeping for 30 seconds so that old access token could have been out of cache");
                }
            }
        }

        codeMarkerManager.clearMarkers();
        codeMarkerManager.setEnableCodeMarker(false);
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        query.protectionPolicy = LabConstants.ProtectionPolicy.MAM_CA;
        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"00000003-0000-0ff1-ce00-000000000000"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

}
