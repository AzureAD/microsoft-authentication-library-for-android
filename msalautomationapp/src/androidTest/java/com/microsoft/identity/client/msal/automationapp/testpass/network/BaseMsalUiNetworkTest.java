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
package com.microsoft.identity.client.msal.automationapp.testpass.network;

import android.util.Log;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.rules.NetworkTestRule;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Rule;

import java.util.Arrays;

/**
 * Sets up a base class for running MSAL network tests.
 */
public abstract class BaseMsalUiNetworkTest extends AbstractMsalUiTest {

    @Rule(order = 20)
    public NetworkTestRule networkTestRule = new NetworkTestRule();


    @Override
    public void setup() {
        super.setup();
        Logger.getInstance()
                .setEnableLogcatLog(true);
    }

    @Override
    public LabUserQuery getLabUserQuery() {
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


    public IAuthenticationResult runAcquireTokenInteractive() throws Throwable {
        final ResultFuture<IAuthenticationResult, Exception> resultFuture = new ResultFuture<>();
        final TokenRequestTimeout timeout = TokenRequestTimeout.LONG;

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(new AuthenticationCallback() {
                    @Override
                    public void onCancel() {
                        Log.e("Network", "Request cancelled");
                        resultFuture.setException(new Exception("Interactive flow cancelled by user."));
                    }

                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        Log.e("Network", "Authentication result: " + authenticationResult.getAccessToken());
                        resultFuture.setResult(authenticationResult);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e("Network", "Request failed", exception);
                        resultFuture.setException(exception);
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

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();

        return resultFuture.get(timeout.getTime(), timeout.getTimeUnit());
    }


    public IAuthenticationResult runAcquireTokenSilent(final boolean forceRefresh) throws Throwable {
        final IAuthenticationResult interactiveResult = runAcquireTokenInteractive();

        final IAccount account = interactiveResult.getAccount();
        final ResultFuture<IAuthenticationResult, Exception> resultFuture = new ResultFuture<>();
        final TokenRequestTimeout timeout = TokenRequestTimeout.SILENT;

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .forceRefresh(forceRefresh)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(new SilentAuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        resultFuture.setResult(authenticationResult);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        resultFuture.setException(exception);
                    }
                })
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);

        return resultFuture.get(timeout.getTime(), timeout.getTimeUnit());
    }
}
