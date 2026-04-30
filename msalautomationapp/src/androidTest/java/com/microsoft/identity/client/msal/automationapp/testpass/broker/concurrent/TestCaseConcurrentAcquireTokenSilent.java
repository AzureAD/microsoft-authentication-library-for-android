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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * Concurrent / stress test for AcquireTokenSilent via broker.
 *
 * <p>Thread count ({@value #CONCURRENT_THREADS}) and iteration count
 * ({@value #ITERATIONS_PER_THREAD}) mirror the default values shown in the
 * msaltest app's Concurrent AcquireTokenSilent UI section
 * ({@code concurrent_count = 13}, {@code concurrent_iterations = 1000}).
 *
 * <p>Test flow:</p>
 * <ol>
 *   <li>Broker app is installed automatically by the rule chain inherited from
 *       {@link AbstractMsalBrokerTest}.</li>
 *   <li>Interactive sign-in with a basic lab account.</li>
 *   <li>{@value #CONCURRENT_THREADS} threads are launched; on each of
 *       {@value #ITERATIONS_PER_THREAD} iteration waves every thread waits at a
 *       {@link CyclicBarrier} so all threads fire {@code acquireTokenSilentAsync}
 *       with {@code forceRefresh=true} at the exact same moment, maximising
 *       contention in the MSAL command dispatcher and the broker IPC layer.</li>
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
    private static final int ITERATIONS_PER_THREAD = 1000;

    /**
     * Maximum time (seconds) to wait for a single silent-token request callback
     * via the broker before declaring it stuck.
     */
    private static final long PER_REQUEST_TIMEOUT_SECONDS = 120;

    /**
     * Overall test timeout.  Each wave should complete in a few seconds under
     * normal conditions, so 4 hours gives ample headroom for
     * {@value #ITERATIONS_PER_THREAD} waves even on a slow device.
     */
    private static final long TOTAL_TIMEOUT_SECONDS = 14400;

    /**
     * Scopes rotated per-thread to prevent the CommandDispatcher from
     * collapsing concurrent requests that share identical parameters.
     * Covers all {@value #CONCURRENT_THREADS} threads (cycles when needed).
     */
    private static final String[][] THREAD_SCOPES = {
            {"User.read"},
            {"User.read", "profile"},
            {"User.read", "openid"},
            {"User.read", "email"},
            {"User.read", "offline_access"},
            {"User.read", "profile", "openid"},
            {"User.read", "profile", "email"},
            {"User.read", "openid", "email"},
            {"User.read", "openid", "offline_access"},
            {"User.read", "email", "offline_access"},
            {"User.read", "profile", "openid", "email"},
            {"User.read", "profile", "offline_access"},
            {"User.read", "openid", "email", "offline_access"}
    };

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
        // Step 3 – Stress: CONCURRENT_THREADS threads each run ITERATIONS_PER_THREAD
        //           iteration waves.  On every wave all threads synchronise at a
        //           CyclicBarrier so every acquireTokenSilentAsync(forceRefresh=true)
        //           call is dispatched at the exact same instant, maximising
        //           contention in both the MSAL command dispatcher and the broker
        //           IPC layer.
        //
        //           Each thread waits for its own per-request latch before moving
        //           to the next iteration, so the barrier ensures the START of each
        //           wave is simultaneous.
        // -----------------------------------------------------------------------
        final CyclicBarrier waveBarrier = new CyclicBarrier(CONCURRENT_THREADS);
        final CountDownLatch allThreadsDone = new CountDownLatch(CONCURRENT_THREADS);
        final List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            final int threadIndex = t;
            // Per-thread scope set to prevent CommandDispatcher request deduplication.
            final List<String> threadScopes = Arrays.asList(
                    THREAD_SCOPES[threadIndex % THREAD_SCOPES.length]);

            new Thread(() -> {
                try {
                    for (int iter = 0; iter < ITERATIONS_PER_THREAD; iter++) {
                        if (Thread.currentThread().isInterrupted()) {
                            break;
                        }

                        // Synchronise all threads so every wave fires together.
                        try {
                            waveBarrier.await();
                        } catch (final Exception barrierEx) {
                            errors.add("Thread " + threadIndex + " barrier failed at iter "
                                    + iter + ": " + barrierEx.getMessage());
                            break;
                        }

                        final CountDownLatch requestDone = new CountDownLatch(1);
                        final int currentIter = iter;

                        final AcquireTokenSilentParameters silentParameters =
                                new AcquireTokenSilentParameters.Builder()
                                        .forAccount(account)
                                        .fromAuthority(account.getAuthority())
                                        .withScopes(new ArrayList<>(threadScopes))
                                        .forceRefresh(true)
                                        .withCallback(new SilentAuthenticationCallback() {
                                            @Override
                                            public void onSuccess(
                                                    final IAuthenticationResult result) {
                                                requestDone.countDown();
                                            }

                                            @Override
                                            public void onError(
                                                    final MsalException exception) {
                                                errors.add("Thread " + threadIndex
                                                        + " iter " + currentIter + ": "
                                                        + exception.getMessage());
                                                requestDone.countDown();
                                            }
                                        })
                                        .build();

                        mApplication.acquireTokenSilentAsync(silentParameters);

                        // Wait for this request to complete before the next iteration
                        // so the barrier correctly aligns the next wave.
                        try {
                            if (!requestDone.await(PER_REQUEST_TIMEOUT_SECONDS,
                                    TimeUnit.SECONDS)) {
                                errors.add("Thread " + threadIndex + " iter " + currentIter
                                        + " timed out after " + PER_REQUEST_TIMEOUT_SECONDS + "s");
                                break;
                            }
                        } catch (final InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } finally {
                    allThreadsDone.countDown();
                }
            }, "ConcurrentATS-" + t).start();
        }

        // -----------------------------------------------------------------------
        // Step 4 – Assert that no request gets stuck.
        //           All CONCURRENT_THREADS threads must finish all their iterations
        //           within the total timeout window.
        // -----------------------------------------------------------------------
        final boolean allCompleted = allThreadsDone.await(TOTAL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Assert.assertTrue(
                "Concurrent AcquireTokenSilent stress test got stuck – not all "
                        + CONCURRENT_THREADS + " threads completed "
                        + ITERATIONS_PER_THREAD + " iterations within "
                        + TOTAL_TIMEOUT_SECONDS + "s",
                allCompleted);

        Assert.assertTrue(
                "Some concurrent AcquireTokenSilent calls failed: " + errors,
                errors.isEmpty());
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
