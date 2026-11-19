// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MfaOperationTest {
    private val dispatcher = QueuedDispatcher()
    private val outcomes = mutableListOf<Any?>()
    private val callback = object : MfaCallback<String> {
        override fun onSuccess(result: String) { outcomes.add(result) }
        override fun onError(exception: Exception) { outcomes.add(exception) }
    }

    @Test
    fun `committed success is delivered asynchronously even if cancelled before delivery`() {
        val operation = launchMfaOperation(callback, dispatcher) { "success" }
        assertTrue(outcomes.isEmpty())
        dispatcher.runNext()
        assertTrue(outcomes.isEmpty())
        operation.cancel()
        dispatcher.runAll()
        assertEquals(listOf("success"), outcomes)
    }

    @Test
    fun `cancel before starting skips work and reports exactly one cancellation`() {
        var started = false
        val operation = launchMfaOperation(callback, dispatcher) { started = true; "success" }
        operation.cancel()
        operation.cancel()
        dispatcher.runAll()
        assertFalse(started)
        assertEquals(1, outcomes.size)
        assertTrue(outcomes.single() is CancellationException)
    }

    @Test
    fun `cancellation waits for required cleanup without cancelling callback delivery`() {
        val cleanup = CompletableDeferred<Unit>()
        var cleaningUp = false
        val operation = launchMfaOperation(callback, dispatcher) {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    cleaningUp = true
                    cleanup.await()
                }
            }
        }
        dispatcher.runAll()
        operation.cancel()
        dispatcher.runAll()
        assertTrue(cleaningUp)
        assertTrue(outcomes.isEmpty())
        cleanup.complete(Unit)
        dispatcher.runAll()
        assertEquals(1, outcomes.size)
        assertTrue(outcomes.single() is CancellationException)
    }

    @Test
    fun `SDK provider and provider cancellation failures retain identity`() {
        val failures = listOf(
            MfaSdkException.create(MfaErrorType.GENERAL_ERROR, "SDK failure", "correlation"),
            Exception("Host failure"),
            CancellationException("Provider sign-in dismissed")
        )
        for (failure in failures) {
            outcomes.clear()
            val operation = launchMfaOperation(callback, dispatcher) { throw failure }
            dispatcher.runNext()
            operation.cancel()
            dispatcher.runAll()
            assertSame(failure, outcomes.single())
        }
    }

    @Test
    fun `callback exceptions and fatal operation errors are not sent to onError`() {
        val uncaught = mutableListOf<Throwable>()
        val context = dispatcher + CoroutineExceptionHandler { _, error -> uncaught.add(error) }
        val callbackFailure = IllegalStateException("Application callback failed")
        launchMfaOperation(object : MfaCallback<String> {
            override fun onSuccess(result: String) { throw callbackFailure }
            override fun onError(exception: Exception) { outcomes.add(exception) }
        }, context) { "success" }
        dispatcher.runAll()
        assertSame(callbackFailure, uncaught.single())
        assertTrue(outcomes.isEmpty())

        val fatal = AssertionError("Fatal operation failure")
        launchMfaOperation(callback, context) { throw fatal }
        dispatcher.runAll()
        assertEquals(2, uncaught.size)
        assertTrue(uncaught.last() is AssertionError)
        assertTrue(outcomes.isEmpty())
    }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val queue = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { queue.add(block) }
        fun runNext() { queue.removeFirst().run() }
        fun runAll() { while (queue.isNotEmpty()) runNext() }
    }
}
