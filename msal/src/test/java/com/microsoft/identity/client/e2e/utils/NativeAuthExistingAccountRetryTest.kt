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
// furnished to do so, subject to the following conditions:
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
package com.microsoft.identity.client.e2e.utils

import com.microsoft.identity.client.e2e.tests.network.nativeauth.SignUpV2EmailAbstractTest
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParametersV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NativeAuthExistingAccountRetryTest {
    private val application = mockk<INativeAuthPublicClientApplication>()
    private val harness = Harness()
    private val email = "test@example.com"

    @Test
    fun retriesSameAccountWithBoundedBackoffAndStopsOnSignIn() = runTest(timeout = 60.seconds) {
        val signUp = result(NativeAuthFlowScenarioV2.SIGN_UP)
        val signIn = result(NativeAuthFlowScenarioV2.SIGN_IN)
        val attempts = mutableListOf<Long>()
        coEvery { application.signUpV2(any<NativeAuthSignUpParametersV2>()) } coAnswers {
            assertEquals(email, firstArg<NativeAuthSignUpParametersV2>().username)
            attempts += testScheduler.currentTime
            if (attempts.size < 3) signUp else signIn
        }

        assertSame(signIn, harness.start(application, email))
        assertEquals(listOf(0L, 2_000L, 6_000L), attempts)
        coVerify(exactly = 3) { application.signUpV2(any<NativeAuthSignUpParametersV2>()) }
    }

    @Test
    fun failsAfterFourSignUpResponses() = runTest(timeout = 60.seconds) {
        coEvery { application.signUpV2(any<NativeAuthSignUpParametersV2>()) } returns
            result(NativeAuthFlowScenarioV2.SIGN_UP)

        try {
            harness.start(application, email)
            fail("Expected the existing-account check to fail")
        } catch (error: AssertionError) {
            assertEquals("Account was not recognized after 4 sign-up attempts", error.message)
        }
        assertEquals(14_000L, testScheduler.currentTime)
        coVerify(exactly = 4) { application.signUpV2(any<NativeAuthSignUpParametersV2>()) }
    }

    @Test
    fun doesNotRetryErrorsEvenWhenScenarioIsSignUp() = runTest(timeout = 60.seconds) {
        val error = NativeAuthErrorV2(
            errorMessage = "Service failure",
            correlationId = "test",
            scenario = NativeAuthFlowScenarioV2.SIGN_UP
        )
        coEvery { application.signUpV2(any<NativeAuthSignUpParametersV2>()) } returns error

        assertSame(error, harness.start(application, email))
        assertEquals(0L, testScheduler.currentTime)
        coVerify(exactly = 1) { application.signUpV2(any<NativeAuthSignUpParametersV2>()) }
    }

    @Test
    fun propagatesCancellationWithoutRetrying() = runTest(timeout = 60.seconds) {
        val cancellation = CancellationException("Cancelled")
        coEvery { application.signUpV2(any<NativeAuthSignUpParametersV2>()) } throws cancellation

        try {
            harness.start(application, email)
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        coVerify(exactly = 1) { application.signUpV2(any<NativeAuthSignUpParametersV2>()) }
    }

    private fun result(scenario: NativeAuthFlowScenarioV2): NativeAuthResultV2 =
        mockk { every { this@mockk.scenario } returns scenario }

    private class Harness : SignUpV2EmailAbstractTest() {
        suspend fun start(application: INativeAuthPublicClientApplication, email: String) =
            startExistingAccountSignIn(application, email)
    }
}
