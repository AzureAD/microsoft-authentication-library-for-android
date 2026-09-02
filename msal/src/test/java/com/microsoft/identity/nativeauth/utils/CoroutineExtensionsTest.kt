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
package com.microsoft.identity.nativeauth.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [launchOwningPasswordSnapshot], covering both the normal-completion cleanup path
 * and the safety-net path where the launched job is cancelled before its body ever runs.
 */
class CoroutineExtensionsTest {

    @Test
    fun launchOwningPasswordSnapshotClearsTheSnapshotAfterNormalCompletion() = runTest {
        val snapshot = "Password123!".toCharArray()
        var bodyRan = false

        val job = launchOwningPasswordSnapshot(snapshot) {
            // Intentionally does not clear the array itself, so this test proves the extension's
            // own completion handler -- not the body -- is what performs the clearing.
            bodyRan = true
        }
        job.join()

        assertTrue(bodyRan)
        assertArrayEquals(CharArray("Password123!".length), snapshot)
    }

    @Test
    fun launchOwningPasswordSnapshotClearsTheSnapshotWhenTheJobIsCancelledBeforeItsBodyEverRuns() {
        val snapshot = "Password123!".toCharArray()
        val parentJob = Job()
        // Cancelling the parent job up-front means any coroutine subsequently launched as its
        // child is cancelled before it ever gets a chance to run.
        parentJob.cancel()
        val scope = CoroutineScope(parentJob)
        var bodyRan = false

        val job = scope.launchOwningPasswordSnapshot(snapshot) {
            bodyRan = true
        }
        // The job is cancelled up-front, but completion (and therefore the invokeOnCompletion
        // handler that clears the snapshot) can still finish asynchronously; wait for it.
        runBlocking { job.join() }

        assertTrue(job.isCancelled)
        assertFalse("The job body must not have run", bodyRan)
        assertArrayEquals(CharArray("Password123!".length), snapshot)
    }
}
