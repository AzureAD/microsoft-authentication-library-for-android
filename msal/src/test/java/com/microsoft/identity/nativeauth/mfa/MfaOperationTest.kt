// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

@file:OptIn(ExperimentalMfaApi::class)

package com.microsoft.identity.nativeauth.mfa

import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
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
        var request: Throwable? = null
        val operation = launchMfaOperation(callback, dispatcher) {
            currentCoroutineContext().job.invokeOnCompletion { request = it }
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
        assertSame(request, outcomes.single())
    }

    @Test
    fun `success including null survives cancellation during required cleanup`() {
        for (value in listOf("success", null)) {
            outcomes.clear()
            val cleanup = CompletableDeferred<Unit>()
            var cleaningUp = false
            val operation = launchMfaOperation(object : MfaCallback<String?> {
                override fun onSuccess(result: String?) { outcomes.add(result) }
                override fun onError(exception: Exception) { outcomes.add(exception) }
            }, dispatcher) {
                try {
                    value
                } finally {
                    withContext(NonCancellable) {
                        cleaningUp = true
                        cleanup.await()
                    }
                }
            }
            dispatcher.runAll()
            assertTrue(cleaningUp)
            operation.cancel()
            dispatcher.runAll()
            assertTrue(outcomes.isEmpty())
            cleanup.complete(Unit)
            dispatcher.runAll()
            assertEquals(listOf(value), outcomes)
        }
    }

    @Test
    fun `SDK and unrelated cancellation failures survive cancellation during cleanup`() {
        val failures = listOf(
            MfaSdkException.create(MfaErrorType.GENERAL_ERROR, "SDK failure", "correlation"),
            CancellationException("Unrelated cancellation")
        )
        for (failure in failures) {
            outcomes.clear()
            val cleanup = CompletableDeferred<Unit>()
            var cleaningUp = false
            val operation = launchMfaOperation(callback, dispatcher) {
                try {
                    throw failure
                } finally {
                    withContext(NonCancellable) {
                        cleaningUp = true
                        cleanup.await()
                    }
                }
            }
            dispatcher.runAll()
            assertTrue(cleaningUp)
            operation.cancel()
            dispatcher.runAll()
            assertTrue(outcomes.isEmpty())
            cleanup.complete(Unit)
            dispatcher.runAll()
            assertSame(failure, outcomes.single())
        }
    }

    @Test
    fun `nested cancellation after suspension delivers the original request`() {
        var request: Throwable? = null
        val operation = launchMfaOperation(callback, dispatcher) {
            currentCoroutineContext().job.invokeOnCompletion { request = it }
            try {
                awaitCancellation()
            } catch (exception: CancellationException) {
                // Two wrappers exercise the recovered-exception case even when recovery is disabled.
                val inner = CancellationException("Inner cancellation").also { it.initCause(exception) }
                throw CancellationException("Outer cancellation").also { it.initCause(inner) }
            }
        }
        dispatcher.runAll()
        operation.cancel()
        dispatcher.runAll()
        assertTrue(request is CancellationException)
        assertSame(request, outcomes.single())
    }

    @Test
    fun `only cancellation exceptions wrapping the request are normalized`() {
        for (wrapAsCancellation in listOf(false, true)) {
            outcomes.clear()
            val cleanup = CompletableDeferred<Unit>()
            var request: CancellationException? = null
            var failure: Exception? = null
            val operation = launchMfaOperation(callback, dispatcher) {
                val workContext = currentCoroutineContext()
                withContext(NonCancellable) { cleanup.await() }
                try {
                    // Synchronous check obtains the request without stack-trace recovery copies.
                    workContext.ensureActive()
                    error("Expected cancellation")
                } catch (exception: CancellationException) {
                    request = exception
                    val wrapped = if (wrapAsCancellation) {
                        CancellationException("Propagated cancellation").also {
                            it.initCause(exception)
                        }
                    } else {
                        IllegalStateException("Cleanup failed", exception)
                    }
                    failure = wrapped
                    throw wrapped
                }
            }
            dispatcher.runAll()
            operation.cancel()
            dispatcher.runAll()
            assertTrue(outcomes.isEmpty())
            cleanup.complete(Unit)
            dispatcher.runAll()
            assertTrue(request is CancellationException)
            assertSame(if (wrapAsCancellation) request else failure, outcomes.single())
        }
    }

    @Test
    fun `cancellation at work context completion cannot replace its outcome`() {
        val failure = MfaSdkException.create(MfaErrorType.GENERAL_ERROR, "SDK failure", "correlation")
        for (result in listOf(Result.success("success"), Result.failure<String>(failure))) {
            outcomes.clear()
            var completed = false
            lateinit var operation: MfaOperation
            operation = launchMfaOperation(callback, dispatcher) {
                // Runs at withContext completion, before the old outer result commit.
                currentCoroutineContext().job.invokeOnCompletion {
                    completed = true
                    operation.cancel()
                }
                result.getOrThrow()
            }
            dispatcher.runNext()
            assertTrue(completed)
            assertTrue(outcomes.isEmpty())
            dispatcher.runAll()
            assertSame(result.fold({ it }, { it }), outcomes.single())
        }
    }

    @Test
    fun `SDK and coroutine cancellation failures retain identity`() {
        val failures = listOf(
            MfaSdkException.create(MfaErrorType.GENERAL_ERROR, "SDK failure", "correlation"),
            CancellationException("Operation cancelled")
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
