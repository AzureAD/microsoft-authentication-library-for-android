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
package com.microsoft.identity.client.msal.automationapp.testpass.perf;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.appender.FileAppender;
import com.microsoft.identity.client.ui.automation.logging.formatter.SimpleTextFormatter;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.common.java.marker.PerfConstants;
import com.microsoft.identity.common.java.marker.CodeMarkerManager;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

// Silent Auth with force_refresh
// https://identitydivision.visualstudio.com/DefaultCollection/IDDP/_workitems/edit/99563
public class TestCasePerf extends AbstractMsalUiTest {

    @Test
    public void test_acquireTokenSilentlyWithoutBroker() {
        CodeMarkerManager codeMarkerManager = CodeMarkerManager.getInstance();
        final TokenRequestLatch latch = new TokenRequestLatch(1);
        final int numberOfOccurrenceOfTest = 10;
        final String outputFilenamePrefix = "PerfDataTarget";

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
                        final String username = mLoginHint;
                        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(mLoginHint)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await(TokenRequestTimeout.SHORT);

        final IAccount account = getAccount();
        codeMarkerManager.setEnableCodeMarker(true);
        //Setting up scenario code. 100 -> Non Brokered, 200 -> Brokered
        codeMarkerManager.setPrefixScenarioCode(PerfConstants.ScenarioConstants.SCENARIO_NON_BROKERED_ACQUIRE_TOKEN_SILENTLY);

        for(int i = 0; i < numberOfOccurrenceOfTest; i++) {
            codeMarkerManager.clearMarkers();
            final TokenRequestLatch silentLatch = new TokenRequestLatch(1);

            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .forAccount(account)
                    .fromAuthority(account.getAuthority())
                    .forceRefresh(true)
                    .withScopes(Arrays.asList(mScopes))
                    .withCallback(successfulSilentCallback(silentLatch))
                    .build();

            mApplication.acquireTokenSilentAsync(silentParameters);
            silentLatch.await(TokenRequestTimeout.SILENT);
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
            if(i < numberOfOccurrenceOfTest - 1) {
                // CommandDispatcherHelper.clear();
                try {
                    // Sleep for 30 seconds so that the cache access token cache is cleared.
                    Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                } catch (InterruptedException e) {
                    throw new AssertionError("Interrupted while sleeping for 30 seconds so that old access token could have been out of chache");
                }
            }
        }
        codeMarkerManager.clearMarkers();
        codeMarkerManager.setEnableCodeMarker(false);
    }


    @Override
    public LabUserQuery getLabQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
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
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_webview;
    }
}
