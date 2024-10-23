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

package com.microsoft.identity.client.e2e.tests.network.nativeauth

import com.microsoft.identity.client.e2e.utils.assertResult
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class SignUpEmailPasswordAttributesTest : _NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    override val defaultConfigType = ConfigType.SIGN_UP_PASSWORD_ATTRIBUTES

    /**
     * Sign up with password and attributes on start, then verify OTP as last step.
     * Mimic a 2-step UX:
     * 1. Capture email address, password and attributes
     * 2. Validate OTP.
     * (hero scenario 10, use case 1.1.3, Test case 15)
     */
    @Ignore("Fetching OTP code is unstable")
    @Test
    fun testEmailPasswordAttributesOnSameScreen() {
        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val password = getSafePassword().toCharArray()
                val attributes = UserAttributes.Builder().country("Ireland").city("Dublin").build()
                val signUpResult = application.signUp(
                    username = user,
                    password = password,
                    attributes = attributes
                )
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                assertResult<SignUpResult.Complete>(submitCodeResult)
            }
        }
    }

    /**
     * Sign up with verify email OTP as first step, then set password & custom attributes at end.
     * Mimic a 3-step UX:
     * 1. Capture email address & validate
     * 2. Set password
     * 3. Set custom attributes.
     * (hero scenario 12, use case 1.1.6) - Test case 28
     */
    @Ignore("Fetching OTP code is unstable")
    @Test
    fun testSeparateEmailPasswordAndAttributesOnSameScreen() {
        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val signUpResult = application.signUp(user)
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                assertResult<SignUpResult.PasswordRequired>(submitCodeResult)
                val submitPasswordResult = (submitCodeResult as SignUpResult.PasswordRequired).nextState.submitPassword(getSafePassword().toCharArray())
                assertResult<SignUpResult.AttributesRequired>(submitPasswordResult)
                val requiredAttributes = (submitPasswordResult as SignUpResult.AttributesRequired).requiredAttributes
                val attributes = UserAttributes.Builder()
                for (attr in requiredAttributes) {
                    Assert.assertNotNull(attr.attributeName)
                    attributes.customAttribute(attr.attributeName!!, "somevalue")
                }
                val submitAttributesResult = submitPasswordResult.nextState.submitAttributes(attributes.build())
                Assert.assertTrue(submitAttributesResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Sign up with verify email OTP as first step, then set password & custom attributes at end across multiple screens.
     * Mimic a 3+ step UX:
     * 1. Capture email address & validate
     * 2. Set password
     * 3. Set first attribute.
     * 4. Set second attribute.
     * 5. etc.
     * ((hero scenario 13) - Test case 29
     */
    @Ignore("Fetching OTP code is unstable")
    @Test
    fun testSeparateEmailPasswordAndAttributesOnMultipleScreens() {
        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val signUpResult = application.signUp(user)
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                assertResult<SignUpResult.PasswordRequired>(submitCodeResult)
                val submitPasswordResult = (submitCodeResult as SignUpResult.PasswordRequired).nextState.submitPassword(getSafePassword().toCharArray())
                assertResult<SignUpResult.AttributesRequired>(submitPasswordResult)
                val requiredAttributes = (submitPasswordResult as SignUpResult.AttributesRequired).requiredAttributes
                val attributes = UserAttributes.Builder()
                for (attr in requiredAttributes) { // Loop through all the required attributes and send them to the API one by one, mimicking a multi-screen UX.
                    Assert.assertNotNull(attr.attributeName)
                    attributes.customAttribute(attr.attributeName!!, "somevalue")
                    val submitAttributesResult = submitPasswordResult.nextState.submitAttributes(attributes.build())
                    if (submitAttributesResult is SignUpResult.AttributesRequired) {
                        continue
                    } else if (submitAttributesResult is SignUpResult.Complete) {
                        break // All attributes submitted, user account is now created.
                    } else {
                        Assert.fail("Unexpected state $submitAttributesResult")
                    }
                }
            }
        }
   }
}