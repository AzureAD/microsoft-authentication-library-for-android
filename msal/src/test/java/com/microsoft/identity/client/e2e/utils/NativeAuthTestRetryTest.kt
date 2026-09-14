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

import com.microsoft.identity.client.e2e.utils.NativeAuthTestRetry.retryOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Exercises the retry policy without initializing Robolectric or shared account-cache handles.
 */
class NativeAuthTestRetryTest {

    @Test
    fun retryOperationDoesNotRetryNonThrottleExceptions() {
        val expected = IllegalStateException("Non-throttle failure")
        var attempts = 0

        val actual = assertThrows(IllegalStateException::class.java) {
            retryOperation(maxRetries = 1) {
                attempts++
                throw expected
            }
        }

        assertSame(expected, actual)
        assertEquals(1, attempts)
    }

    @Test
    fun retryOperationDoesNotRetryNonThrottleAssertions() {
        val expected = AssertionError("Unexpected reset password result")
        var attempts = 0

        val actual = assertThrows(AssertionError::class.java) {
            retryOperation(maxRetries = 1) {
                attempts++
                throw expected
            }
        }

        assertSame(expected, actual)
        assertEquals(1, attempts)
    }

    @Test
    fun retryOperationPreservesThrottleFailureWhenRetriesAreExhausted() {
        val expected = AssertionError("AADSTS701014: Cannot generate more one time passcodes")
        var attempts = 0

        val actual = assertThrows(AssertionError::class.java) {
            retryOperation(maxRetries = 0) {
                attempts++
                throw expected
            }
        }

        assertSame(expected, actual.cause)
        assertEquals(1, attempts)
    }
}
