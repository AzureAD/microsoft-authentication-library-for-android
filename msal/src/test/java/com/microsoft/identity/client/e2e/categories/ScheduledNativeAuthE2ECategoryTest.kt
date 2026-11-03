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

package com.microsoft.identity.client.e2e.categories

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Guards the scheduled suite boundary without instantiating network test fixtures.
 */
class ScheduledNativeAuthE2ECategoryTest {
    @Test
    fun scheduledSuitesHaveCategory() {
        listOf(
            "SSPRTest",
            "SignInEmailOTPTest",
            "SignInEmailPasswordTest",
            "SignUpEmailOTPTest",
            "SignUpEmailOTPAttributesTest",
            "SignUpEmailPasswordTest",
            "SignUpEmailPasswordAttributesTest"
        ).forEach { name ->
            assertTrue("$name must be scheduled-only", isScheduled(name))
        }
    }

    @Test
    fun otherSuitesAndBaseFixtureRemainUncategorized() {
        listOf(
            "GetTokenTests",
            "SignInJITTest",
            "SignInMFATest",
            "SignInV2EmailPasswordTest",
            "NativeAuthPublicClientApplicationAbstractTest"
        ).forEach { name ->
            assertFalse("$name must not inherit the scheduled category", isScheduled(name))
        }
    }

    private fun isScheduled(name: String): Boolean {
        // Loading without initialization avoids the base fixture's lab client initializer.
        val suite = Class.forName(
            "com.microsoft.identity.client.e2e.tests.network.nativeauth.$name",
            false,
            javaClass.classLoader
        )
        return suite.getAnnotation(Category::class.java)?.value
            ?.contains(ScheduledNativeAuthE2E::class) == true
    }
}
