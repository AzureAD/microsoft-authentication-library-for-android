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
package com.microsoft.identity.nativeauth.v2;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication;
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication;
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration;
import com.microsoft.identity.nativeauth.parameters.NativeAuthResetPasswordParameters;
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters;
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters;
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2;
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2;
import com.microsoft.identity.nativeauth.statemachine.errors.SignInErrorV2;
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpErrorV2;
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2;
import com.microsoft.identity.nativeauth.statemachine.states.MFAVerificationRequiredStateV2;
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterResetPasswordStateV2;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LEGACY)
@Config(shadows = {ShadowAndroidSdkStorageEncryptionManager.class})
public class NativeAuthV2InterfaceJavaTest extends PublicClientApplicationAbstractTest {

    private Context context;
    private INativeAuthPublicClientApplication application;
    private final String username = "user@email.com";

    @Override
    public String getConfigFilePath() {
        return "src/test/res/raw/native_auth_native_only_test_config.json";
    }

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        final File configFile = new File(getConfigFilePath());
        try {
            application = PublicClientApplication.createNativeAuthPublicClientApplication(context, configFile);
        } catch (InterruptedException | MsalException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void signInV2RejectsBlankUsername() throws ExecutionException, InterruptedException, TimeoutException {
        // A blank username is rejected before any command is dispatched, so this exercises the
        // Java callback surface of the now-implemented signInV2 without needing a service.
        final ResultFuture<NativeAuthResultV2> future = new ResultFuture<>();
        application.signInV2(new NativeAuthSignInParameters(" "), newCallback(future));
        final NativeAuthResultV2 result = future.get(30, TimeUnit.SECONDS);
        assertTrue(result instanceof SignInErrorV2);
        assertTrue(((SignInErrorV2) result).isInvalidUsername());
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, result.getScenario());
    }

    @Test
    public void signUpV2RejectsBlankUsername() throws ExecutionException, InterruptedException, TimeoutException {
        // A blank username is rejected before any command is dispatched, so this exercises the
        // Java callback surface of the now-implemented signUpV2 without needing a service.
        final ResultFuture<NativeAuthResultV2> future = new ResultFuture<>();
        application.signUpV2(new NativeAuthSignUpParameters(" "), newCallback(future));
        final NativeAuthResultV2 result = future.get(30, TimeUnit.SECONDS);
        assertTrue(result instanceof SignUpErrorV2);
        assertTrue(((SignUpErrorV2) result).isInvalidUsername());
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, result.getScenario());
    }

    @Test
    public void signInAfterResetPasswordReturnsNotImplemented() throws ExecutionException, InterruptedException, TimeoutException {
        final SignInAfterResetPasswordStateV2 state = new SignInAfterResetPasswordStateV2(
                "continuation-token",
                "correlation-id",
                NativeAuthFlowScenarioV2.RESET_PASSWORD,
                new NativeAuthPublicClientApplicationConfiguration(),
                null
        );
        final ResultFuture<NativeAuthResultV2> future = new ResultFuture<>();
        state.signIn(new SignInAfterResetPasswordStateV2.SignInCallback() {
            @Override
            public void onResult(NativeAuthResultV2 result) {
                future.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                future.setException(exception);
            }
        });

        assertNotImplemented(future.get(30, TimeUnit.SECONDS), NativeAuthFlowScenarioV2.RESET_PASSWORD);
    }

    @Test
    public void mfaVerificationRequiredResendChallengeReturnsInvalidState() throws ExecutionException, InterruptedException, TimeoutException {
        final MFAVerificationRequiredStateV2 state = new MFAVerificationRequiredStateV2(
                "continuation-token",
                "correlation-id",
                NativeAuthFlowScenarioV2.SIGN_IN,
                new NativeAuthPublicClientApplicationConfiguration(),
                null
        );
        final ResultFuture<NativeAuthResultV2> future = new ResultFuture<>();
        state.resendChallenge(new MFAVerificationRequiredStateV2.ResendChallengeCallback() {
            @Override
            public void onResult(final NativeAuthResultV2 result) {
                future.setResult(result);
            }

            @Override
            public void onError(@NonNull final BaseException exception) {
                future.setException(exception);
            }
        });

        assertInvalidState(future.get(30, TimeUnit.SECONDS), NativeAuthFlowScenarioV2.SIGN_IN);
    }

    @Test
    public void resultDefaultImplsDelegateToBaseResult() throws ExecutionException, InterruptedException, TimeoutException {
        final ResultFuture<NativeAuthResultV2> future = new ResultFuture<>();
        application.signUpV2(new NativeAuthSignUpParameters(username), newCallback(future));
        final NativeAuthResultV2 result = future.get(30, TimeUnit.SECONDS);
        assertFalse(NativeAuthResultV2.DefaultImpls.isSuccess(result));
        assertFalse(NativeAuthResultV2.DefaultImpls.isComplete(result));
        assertFalse(NativeAuthResultV2.DefaultImpls.isError(result));
    }

    private NativeAuthPublicClientApplication.NativeAuthV2Callback newCallback(final ResultFuture<NativeAuthResultV2> future) {
        return new NativeAuthPublicClientApplication.NativeAuthV2Callback() {
            @Override
            public void onResult(NativeAuthResultV2 result) {
                future.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                future.setException(exception);
            }
        };
    }

    private void assertNotImplemented(NativeAuthResultV2 result, NativeAuthFlowScenarioV2 scenario) {
        assertTrue(result instanceof NativeAuthErrorV2);
        NativeAuthErrorV2 error = (NativeAuthErrorV2) result;
        assertTrue(error.isNotImplemented());
        assertFalse(error.isBrowserRequired());
        assertEquals(scenario, error.getScenario());
    }

    private void assertInvalidState(final NativeAuthResultV2 result, final NativeAuthFlowScenarioV2 scenario) {
        assertTrue(result instanceof NativeAuthErrorV2);
        final NativeAuthErrorV2 error = (NativeAuthErrorV2) result;
        assertFalse(error.isNotImplemented());
        assertFalse(error.isBrowserRequired());
        assertEquals("The continuation state is unavailable. Restart the flow.", error.getErrorMessage());
        assertEquals(scenario, error.getScenario());
    }
}
