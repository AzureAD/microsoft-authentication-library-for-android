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
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class SignUpEmailOTPAttributesTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    override val defaultConfigType = ConfigType.SIGN_UP_OTP_ATTRIBUTES

    /**
     * Signup user with custom attributes with verify OTP as last step.
     * (hero scenario 2, use case 2.1.2, Test case 2)
     */
    @Test
    fun testSuccessAttributesFirst() {
        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val attributes = UserAttributes.Builder().country("Ireland").city("Dublin").build()
                val signUpResult = application.signUp(user, attributes = attributes)
                assertResult<SignUpResult.CodeRequired>(signUpResult)

                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)

                Assert.assertTrue(submitCodeResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Verify email OTP first and then collect custom attributes.
     * (hero scenario 3, use case 2.1.3, Test case 3)
     */
    @Test
    fun testSuccessAttributesLastSameScreen() {
        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val signUpResult = application.signUp(user)
                assertResult<SignUpResult.CodeRequired>(signUpResult)

                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                assertResult<SignUpResult.AttributesRequired>(submitCodeResult)

                val requiredAttributes = (submitCodeResult as SignUpResult.AttributesRequired).requiredAttributes
                val attributes = UserAttributes.Builder()
                for (attr in requiredAttributes) {
                    Assert.assertNotNull(attr.attributeName)
                    attributes.customAttribute(attr.attributeName!!, "somevalue")
                }
                val submitAttributesResult = submitCodeResult.nextState.submitAttributes(attributes.build())
                Assert.assertTrue(submitAttributesResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Verify email OTP first and then collect custom attributes in multiple steps (mimicking a multi-screen UX).
     * (hero scenario 4, use case 2.1.4, Test case 4)
     */
    @Test
    fun testSuccessAttributesLastMultipleScreens() {
        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val signUpResult = application.signUp(user)
                assertResult<SignUpResult.CodeRequired>(signUpResult)

                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                assertResult<SignUpResult.AttributesRequired>(submitCodeResult)

                val requiredAttributes = (submitCodeResult as SignUpResult.AttributesRequired).requiredAttributes
                val attributes = UserAttributes.Builder()
                for (attr in requiredAttributes) { // Loop through all the required attributes and send them to the API one by one, mimicking a multi-screen UX.
                    Assert.assertNotNull(attr.attributeName)
                    attributes.customAttribute(attr.attributeName!!, "somevalue")
                    val submitAttributesResult = submitCodeResult.nextState.submitAttributes(attributes.build())
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