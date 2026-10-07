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
package com.microsoft.identity.client.e2e.utils

import com.microsoft.identity.nativeauth.statemachine.errors.Error

/**
 * An [AssertionError] that additionally carries the structured Native Auth [Error] that caused the
 * assertion to fail, so callers (e.g. the throttle-aware retry loop in
 * `NativeAuthPublicClientApplicationAbstractTest`) can classify the failure from typed data such as
 * [Error.errorCodes] instead of re-parsing the formatted assertion message.
 *
 * It remains an [AssertionError] subclass so JUnit continues to report it as a test failure exactly
 * as `Assert.fail` did.
 *
 * Note: [Error] here is [com.microsoft.identity.nativeauth.statemachine.errors.Error], not
 * [kotlin.Error].
 *
 * @param message the assertion message, unchanged from the previous `Assert.fail` message format.
 * @param nativeAuthError the Native Auth error that failed the assertion, or null if the actual
 * value was not a Native Auth [Error].
 */
class NativeAuthAssertionError(message: String, val nativeAuthError: Error?) : AssertionError(message)

inline fun <reified ExpectedType> assertResult(actual: Any) {
    val condition = actual is ExpectedType
    if (!condition) {
        val nativeAuthError: Error? = actual as? Error
        val assertMessage: String = if (nativeAuthError != null) {
            "Type comparison failed. Expected: ${ExpectedType::class.java}, actual: ${actual.javaClass}. Error: ${nativeAuthError.error} - ${nativeAuthError.errorMessage}"
        } else {
            "Type comparison failed. Expected: ${ExpectedType::class.java}, actual: ${actual.javaClass}"
        }
        throw NativeAuthAssertionError(assertMessage, nativeAuthError)
    }
}