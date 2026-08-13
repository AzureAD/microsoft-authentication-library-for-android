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
package com.microsoft.identity.nativeauth.v2

import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.RequiredUserAttribute
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesInvalidStateV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFAVerificationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.NewPasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthRegistrationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthVerificationRequiredStateV2
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the Native Auth V2 result types, verifying they expose the values passed to them.
 */
@RunWith(RobolectricTestRunner::class)
class NativeAuthV2ResultsTest {

    private val continuationToken = "continuation-token"
    private val correlationId = "correlation-id"
    private val scenario = NativeAuthFlowScenarioV2.UNKNOWN
    private val config = NativeAuthPublicClientApplicationConfiguration()

    @Test
    fun testCodeRequiredExposesExpectedValues() {
        val result = NativeAuthResultV2.CodeRequired(
            nextState = CodeRequiredStateV2(continuationToken, correlationId, scenario, config),
            scenario = scenario,
            codeLength = 6,
            sentTo = "user@email.com",
            channel = "email"
        )
        assertEquals(6, result.codeLength)
        assertEquals("user@email.com", result.sentTo)
        assertEquals("email", result.channel)
        assertEquals(scenario, result.scenario)
    }

    @Test
    fun testPasswordRequiredExposesExpectedValues() {
        val result = NativeAuthResultV2.PasswordRequired(
            nextState = PasswordRequiredStateV2(continuationToken, correlationId, scenario, config),
            scenario = scenario
        )
        assertEquals(scenario, result.scenario)
    }

    @Test
    fun testNewPasswordRequiredExposesExpectedValues() {
        val result = NativeAuthResultV2.NewPasswordRequired(
            nextState = NewPasswordRequiredStateV2(continuationToken, correlationId, scenario, config),
            scenario = scenario
        )
        assertEquals(scenario, result.scenario)
    }

    @Test
    fun testAttributesRequiredExposesExpectedValues() {
        val requiredAttributes = listOf(RequiredUserAttribute("city", "string", true, null))
        val result = NativeAuthResultV2.AttributesRequired(
            nextState = AttributesRequiredStateV2(continuationToken, correlationId, scenario, config),
            scenario = scenario,
            requiredAttributes = requiredAttributes
        )
        assertEquals(requiredAttributes, result.requiredAttributes)
        assertEquals(scenario, result.scenario)
    }

    @Test
    fun testAttributesInvalidExposesExpectedValues() {
        val invalidAttributes = listOf("city")
        val result = NativeAuthResultV2.AttributesInvalid(
            nextState = AttributesInvalidStateV2(continuationToken, correlationId, scenario, config),
            scenario = scenario,
            invalidAttributes = invalidAttributes
        )
        assertEquals(invalidAttributes, result.invalidAttributes)
        assertEquals(scenario, result.scenario)
    }

    @Test
    fun testMFARequiredExposesExpectedValues() {
        val authMethods = listOf(AuthMethod("id", "oob", "user@email.com", "email"))
        val result = NativeAuthResultV2.MFARequired(
            nextState = MFARequiredStateV2(continuationToken, correlationId, scenario, config),
            scenario = scenario,
            authMethods = authMethods
        )
        assertEquals(authMethods, result.authMethods)
        assertEquals(scenario, result.scenario)
    }

    @Test
    fun testMFAVerificationRequiredExposesExpectedValues() {
        val result = NativeAuthResultV2.MFAVerificationRequired(
            nextState = MFAVerificationRequiredStateV2(continuationToken, correlationId, scenario, config),
            scenario = scenario,
            codeLength = 8,
            sentTo = "user@email.com",
            channel = "sms"
        )
        assertEquals(8, result.codeLength)
        assertEquals("user@email.com", result.sentTo)
        assertEquals("sms", result.channel)
        assertEquals(scenario, result.scenario)
    }

    @Test
    fun testStrongAuthRegistrationRequiredExposesExpectedValues() {
        val authMethods = listOf(AuthMethod("id", "oob", "user@email.com", "email"))
        val result = NativeAuthResultV2.StrongAuthRegistrationRequired(
            nextState = StrongAuthRegistrationRequiredStateV2(continuationToken, correlationId, scenario, config),
            scenario = scenario,
            authMethods = authMethods
        )
        assertEquals(authMethods, result.authMethods)
        assertEquals(scenario, result.scenario)
    }

    @Test
    fun testStrongAuthVerificationRequiredExposesExpectedValues() {
        val result = NativeAuthResultV2.StrongAuthVerificationRequired(
            nextState = StrongAuthVerificationRequiredStateV2(continuationToken, correlationId, scenario, config),
            scenario = scenario,
            codeLength = 4,
            sentTo = "user@email.com",
            channel = "email"
        )
        assertEquals(4, result.codeLength)
        assertEquals("user@email.com", result.sentTo)
        assertEquals("email", result.channel)
        assertEquals(scenario, result.scenario)
    }
}
