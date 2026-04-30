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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.concurrent;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.LongUIAutomationTest;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Concurrent / stress test for AcquireTokenSilent via broker.
 *
 * <p>Thread count ({@value #CONCURRENT_THREADS}) and iteration count
 * ({@value #ITERATIONS_PER_THREAD}) mirror the default values shown in the
 * msaltest app's Concurrent AcquireTokenSilent UI section
 * ({@code concurrent_count = 13}, {@code concurrent_iterations = 1000}).
 *
 * <p>The concurrency machinery (barrier synchronization, per-request latches,
 * scope rotation, error collection) is shared via
 * {@link ConcurrentAcquireTokenSilentHelper}, which parallels the design of
 * the msaltestapp's {@code ConcurrentAcquireTokenExecutor}.
 *
 * <p>Test flow:</p>
 * <ol>
 *   <li>Broker app is installed automatically by the rule chain inherited from
 *       {@link AbstractMsalBrokerTest}.</li>
 *   <li>Interactive sign-in with a basic lab account.</li>
 *   <li>{@value #CONCURRENT_THREADS} threads are launched; on each of
 *       {@value #ITERATIONS_PER_THREAD} iteration waves every thread waits at a
 *       {@link java.util.concurrent.CyclicBarrier} so all threads fire
 *       {@code acquireTokenSilentAsync} with {@code forceRefresh=true} at the
 *       exact same moment, maximising contention in the MSAL command dispatcher
 *       and the broker IPC layer.</li>
 *   <li>The test asserts that every single request completes (i.e. the operation
 *       does not get stuck) and that no request returns an error.</li>
 * </ol>
 */
@RetryOnFailure(retryCount = 2)
@LongUIAutomationTest
public class TestCaseConcurrentAcquireTokenSilent extends AbstractMsalBrokerTest {

    /**
     * Number of threads fired simultaneously – matches the default value of the
     * {@code concurrent_count} field in the msaltest app UI.
     */
    private static final int CONCURRENT_THREADS = 13;

    /**
     * Number of iteration waves each thread executes – matches the default value
     * of the {@code concurrent_iterations} field in the msaltest app UI.
     */
    private static final int ITERATIONS_PER_THREAD = 100;

    /**
     * Maximum time (seconds) for all {@value #CONCURRENT_THREADS} callbacks in
     * a single wave to complete before the run is aborted.
     */
    private static final long PER_WAVE_TIMEOUT_SECONDS = 10;

    @Test
    public void test_concurrentAcquireTokenSilent_withBroker() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final MsalSdk msalSdk = new MsalSdk();

        // -----------------------------------------------------------------------
        // Step 1 – Acquire token interactively with a basic lab account.
        // -----------------------------------------------------------------------
        final MsalAuthTestParams interactiveParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult interactiveResult = msalSdk.acquireTokenInteractive(
                interactiveParams,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final PromptHandlerParameters promptHandlerParameters =
                                PromptHandlerParameters.builder()
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
                },
                TokenRequestTimeout.MEDIUM);

        interactiveResult.assertSuccess();

        // -----------------------------------------------------------------------
        // Step 2 – Retrieve the signed-in account for silent calls.
        // -----------------------------------------------------------------------
        final IAccount account = msalSdk.getAccount(
                mActivity, getConfigFileResourceId(), username);
        Assert.assertNotNull("Account must not be null after a successful interactive sign-in",
                account);

        // -----------------------------------------------------------------------
        // Step 3 – Stress: CONCURRENT_THREADS threads × ITERATIONS_PER_THREAD
        //           waves, all synchronized via ConcurrentAcquireTokenSilentHelper
        //           (mirrors the msaltestapp's ConcurrentAcquireTokenExecutor design).
        // -----------------------------------------------------------------------
        final ConcurrentAcquireTokenSilentHelper.StressResult result =
                ConcurrentAcquireTokenSilentHelper.run(
                        CONCURRENT_THREADS,
                        ITERATIONS_PER_THREAD,
                        PER_WAVE_TIMEOUT_SECONDS,
                        (threadIndex, iteration, done, errors) -> {
                            final AcquireTokenSilentParameters silentParameters =
                                    new AcquireTokenSilentParameters.Builder()
                                            .forAccount(account)
                                            .fromAuthority(account.getAuthority())
                                            .withScopes(ConcurrentAcquireTokenSilentHelper
                                                    .scopesForThread(threadIndex))
                                            .forceRefresh(true)
                                            .withCallback(new SilentAuthenticationCallback() {
                                                @Override
                                                public void onSuccess(
                                                        final IAuthenticationResult r) {
                                                    done.countDown();
                                                }

                                                @Override
                                                public void onError(
                                                        final MsalException exception) {
                                                    errors.add("Thread " + threadIndex
                                                            + " iter " + iteration + ": "
                                                            + exception.getMessage());
                                                    done.countDown();
                                                }
                                            })
                                            .build();

                            mApplication.acquireTokenSilentAsync(silentParameters);
                        });

        // -----------------------------------------------------------------------
        // Step 4 – Assert that no request gets stuck and no errors occurred.
        // -----------------------------------------------------------------------
        Assert.assertTrue(
                "Concurrent AcquireTokenSilent stress test got stuck – not all "
                        + CONCURRENT_THREADS + " threads completed "
                        + ITERATIONS_PER_THREAD + " iterations"
                        + " (per-wave timeout: " + PER_WAVE_TIMEOUT_SECONDS + "s)",
                result.allCompleted);

        Assert.assertTrue(
                "Some concurrent AcquireTokenSilent calls failed: " + result.errors,
                result.errors.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Test configuration
    // -------------------------------------------------------------------------

    @Override
    public LabQuery getLabQuery() {
        return null;
    }

    @Override
    public TempUserType getTempUserType() {
        return TempUserType.BASIC;
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
}
