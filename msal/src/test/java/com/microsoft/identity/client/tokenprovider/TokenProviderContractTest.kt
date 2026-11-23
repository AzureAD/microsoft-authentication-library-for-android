// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.client.tokenprovider

import java.util.Date
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenProviderContractTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `provider without cancellation handle completes asynchronously`() = runTest {
        lateinit var completion: CallbackTokenProvider.Callback
        val provider = adaptTokenProvider(CallbackTokenProvider { _, callback ->
            completion = callback
            null
        }, StandardTestDispatcher(testScheduler))
        val waiting = async { provider.getAccessToken(parameters()) }
        runCurrent()
        assertFalse(waiting.isCompleted)
        val result = tokenResult()
        completion.onSuccess(result)
        completion.onError(Exception("Late error"))
        completion.onSuccess(tokenResult())
        assertSame(result, waiting.await())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `provider without cancellation handle ignores callbacks after cancellation during or after startup`() = runTest {
        for (cancelDuringStartup in listOf(true, false)) {
            lateinit var completion: CallbackTokenProvider.Callback
            lateinit var waiting: Deferred<TokenProviderResult>
            var resumed = false
            val provider = adaptTokenProvider(CallbackTokenProvider { _, callback ->
                completion = callback
                if (cancelDuringStartup) waiting.cancel()
                null
            }, StandardTestDispatcher(testScheduler))
            waiting = async {
                provider.getAccessToken(parameters()).also { resumed = true }
            }
            runCurrent()
            waiting.cancel()
            runCurrent()
            completion.onSuccess(tokenResult())
            completion.onError(Exception("Late error"))
            runCurrent()
            assertTrue(waiting.isCancelled)
            assertFalse(resumed)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancellation reaches a published provider handle and late callbacks cannot resume SDK work`() = runTest {
        var cancellations = 0
        var resumed = false
        lateinit var completion: CallbackTokenProvider.Callback
        val provider = adaptTokenProvider(CallbackTokenProvider { _, callback ->
            completion = callback
            CallbackTokenProvider.Cancellation { cancellations++ }
        }, StandardTestDispatcher(testScheduler))
        val waiting = async { provider.getAccessToken(parameters()); resumed = true }
        runCurrent()
        waiting.cancel()
        waiting.cancel()
        runCurrent()
        completion.onSuccess(tokenResult())
        completion.onError(Exception("Late error"))
        runCurrent()
        assertEquals(1, cancellations)
        assertFalse(resumed)
        assertTrue(waiting.isCancelled)
    }

    @Test
    fun `callback provider runs off caller thread and accepts only its first completion`() = runBlocking {
        val caller = Thread.currentThread()
        val providerThread = AtomicReference<Thread>()
        val result = tokenResult()
        val provider = TokenProvider.from { parameters, callback ->
            providerThread.set(Thread.currentThread())
            assertEquals(listOf("scope"), parameters.scopes)
            callback.onSuccess(result)
            callback.onError(Exception("Late error"))
            callback.onSuccess(tokenResult())
            CallbackTokenProvider.Cancellation { }
        }

        assertSame(result, provider.getAccessToken(parameters()))
        assertFalse(caller === providerThread.get())
    }

    @Test
    fun `callback and synchronous provider failures preserve identity including sign-in cancellation`() = runBlocking {
        for (error in listOf(Exception("Host failure"), CancellationException("Sign-in dismissed"))) {
            for (throwsDirectly in listOf(false, true)) {
                val provider = TokenProvider.from { _, callback ->
                    if (throwsDirectly) throw error
                    callback.onError(error)
                    null
                }
                assertSame(error, runCatching { provider.getAccessToken(parameters()) }.exceptionOrNull())
            }
        }
    }

    @Test
    fun `cancel during provider startup cancels eventual handle once and ignores late completion`() = runBlocking {
        val entered = CountDownLatch(1)
        val returnHandle = CountDownLatch(1)
        val cancellations = AtomicInteger()
        val callback = AtomicReference<CallbackTokenProvider.Callback>()
        val provider = TokenProvider.from { _, completion ->
            callback.set(completion)
            entered.countDown()
            check(returnHandle.await(5, TimeUnit.SECONDS))
            CallbackTokenProvider.Cancellation { cancellations.incrementAndGet() }
        }
        val result = async(start = CoroutineStart.UNDISPATCHED) { provider.getAccessToken(parameters()) }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            result.cancel()
            result.cancel()
        } finally {
            returnHandle.countDown()
        }
        result.join()
        callback.get().onSuccess(tokenResult())
        callback.get().onError(Exception("Late error"))
        assertEquals(1, cancellations.get())
        assertTrue(result.isCancelled)
    }

    @Test
    fun `request scopes cannot change after construction or through a returned list`() {
        val scopes = mutableListOf("requested-scope")
        val parameters = TokenProviderParameters("https://example.invalid/tenant", scopes)
        scopes.clear()

        assertEquals(listOf("requested-scope"), parameters.scopes)
        assertThrows(UnsupportedOperationException::class.java) {
            (parameters.scopes as MutableList<String>).clear()
        }
    }

    @Test
    fun `result snapshots scopes and expiry instead of exposing mutable authentication metadata`() {
        val scopes = mutableListOf("granted-scope")
        val expiresOn = Date(1000L)
        val result = TokenProviderResult(
            accessToken = "test-token",
            expiresOn = expiresOn,
            scopes = scopes,
            tenantId = "tenant-id",
            objectId = "object-id",
            authorityUrl = "https://example.invalid/tenant",
            authenticationScheme = "Bearer"
        )
        scopes.clear()
        expiresOn.time = 2000L

        assertEquals(listOf("granted-scope"), result.scopes)
        assertThrows(UnsupportedOperationException::class.java) {
            (result.scopes as MutableList<String>).clear()
        }
        assertEquals(Date(1000L), result.expiresOn)
        result.expiresOn?.time = 3000L
        assertEquals(Date(1000L), result.expiresOn)
    }

    @Test
    fun `request and result strings do not disclose tokens claims or account metadata`() {
        val sensitive = "sensitive-test-value"
        val parameters = TokenProviderParameters(
            authorityUrl = sensitive,
            scopes = listOf(sensitive),
            claimsRequestJson = sensitive,
            tenantId = sensitive,
            objectId = sensitive,
            loginHint = sensitive,
            allowsInteraction = true,
            correlationId = UUID.randomUUID()
        )
        val result = TokenProviderResult(
            accessToken = sensitive,
            expiresOn = null,
            scopes = listOf(sensitive),
            tenantId = sensitive,
            objectId = sensitive,
            authorityUrl = sensitive,
            authenticationScheme = sensitive
        )

        assertFalse(parameters.toString().contains(sensitive))
        assertFalse(result.toString().contains(sensitive))
    }

    private fun parameters() = TokenProviderParameters("https://example.invalid/tenant", listOf("scope"))

    private fun tokenResult() = TokenProviderResult(
        "test-token", null, listOf("scope"), "tenant", "object",
        "https://example.invalid/tenant", "Bearer"
    )
}
