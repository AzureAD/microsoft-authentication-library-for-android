// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.mfa.MfaDeletionError
import com.microsoft.identity.mfa.MfaDeletionException
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DeletionCallbackBridgeTest {
    @Test fun cancelledAcquisitionIgnoresLateTokenAndBlocksOverlapUntilCallback() = runTest {
        val bridge = DeletionCallbackBridge()
        lateinit var completion: (Result<String>) -> Unit
        var consumed = false
        val job = launch { bridge.await<String> { completion = it }; consumed = true }
        yield()
        job.cancelAndJoin()
        try {
            bridge.await<String> { error("Must not start second request") }
            fail("Expected busy")
        } catch (e: MfaDeletionException) { assertEquals(MfaDeletionError.OPERATION_IN_PROGRESS, e.code) }
        completion(Result.success("late token"))
        assertFalse(consumed)
        assertEquals("next", bridge.await<String> { it(Result.success("next")) })
    }

    @Test fun duplicateCallbackCannotCompleteOrReleaseAnotherRequest() = runTest {
        val bridge = DeletionCallbackBridge()
        lateinit var first: (Result<String>) -> Unit
        assertEquals("one", bridge.await<String> { first = it; it(Result.success("one")) })
        lateinit var second: (Result<String>) -> Unit
        val request = async { bridge.await<String> { second = it } }
        yield()
        first(Result.success("duplicate"))
        assertFalse(request.isCompleted)
        second(Result.success("two"))
        assertEquals("two", request.await())
    }
}
