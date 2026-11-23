// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

import android.content.Context
import com.microsoft.identity.client.tokenprovider.CallbackTokenProvider
import com.microsoft.identity.client.tokenprovider.TokenProviderParameters
import com.microsoft.identity.client.tokenprovider.TokenProviderResult
import com.microsoft.identity.client.tokenprovider.adaptTokenProvider
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock

class MfaJavaInteropTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Java consumer invokes all seven APIs without Continuation or Kotlin Unit`() {
        val source = temporaryFolder.newFile("JavaMfaConsumer.java")
        source.writeText("""
                import com.microsoft.identity.nativeauth.mfa.*;
                import com.microsoft.identity.client.tokenprovider.*;
                import java.util.*;
                import java.util.concurrent.*;
                import java.util.concurrent.atomic.*;

                public class JavaMfaConsumer {
                    public static CallbackTokenProvider providerWithoutCancellation(
                            AtomicReference<CallbackTokenProvider.Callback> completion) {
                        return (parameters, callback) -> {
                            completion.set(callback);
                            return null;
                        };
                    }

                    private static <T> MfaCallback<T> callback(Thread caller, CountDownLatch completed,
                            AtomicReference<Throwable> failure, AtomicInteger calls) {
                        return new MfaCallback<T>() {
                            public void onSuccess(T value) {
                                failure.set(new AssertionError("API-only stub unexpectedly succeeded"));
                                completed.countDown();
                            }
                            public void onError(Exception error) {
                                if (!(error instanceof MfaSdkException) || Thread.currentThread() == caller) {
                                    failure.set(new AssertionError("Wrong error or inline callback"));
                                }
                                calls.incrementAndGet();
                                completed.countDown();
                            }
                        };
                    }

                    public static void exercise(MfaSdkManager manager, MfaAuthSession session) throws Exception {
                        Thread caller = Thread.currentThread();
                        CountDownLatch completed = new CountDownLatch(7);
                        AtomicReference<Throwable> failure = new AtomicReference<>();
                        AtomicInteger calls = new AtomicInteger();
                        TokenProviderParameters parameters = new TokenProviderParameters(
                            "https://example.invalid/tenant", Collections.singletonList("scope"));
                        TokenProvider provider = TokenProvider.from((request, completion) -> {
                            throw new AssertionError("Stub must not acquire tokens");
                        });
                        MfaCallback<Void> noResult = callback(caller, completed, failure, calls);
                        MfaCallback<List<MfaAuthSession>> sessions = callback(caller, completed, failure, calls);
                        MfaCallback<List<MfaRegistrationResult>> registrations =
                            callback(caller, completed, failure, calls);
                        MfaOperation operation = manager.updatePushRegistration("opaque-token",
                            noResult);
                        manager.registerAuthMethod(new MfaRegistrationParameters.QrCode("qr"),
                            callback(caller, completed, failure, calls));
                        manager.registeredAuthMethods(registrations);
                        manager.handlePushNotification(Collections.emptyMap(), sessions);
                        manager.pullPendingNotifications(sessions);
                        manager.respond(session, new MfaAction.Approve(),
                            callback(caller, completed, failure, calls));
                        manager.deleteAuthMethod("registration", provider, noResult);
                        if (!completed.await(5, TimeUnit.SECONDS)) throw new AssertionError("Missing callback");
                        operation.cancel();
                        if (failure.get() != null) throw new AssertionError(failure.get());
                        if (calls.get() != 7) throw new AssertionError("Duplicate completion");
                    }
                }
            """.trimIndent())
        val classpath = listOf(MfaSdkManager::class.java, Unit::class.java, Context::class.java, StateFlow::class.java)
            .map { File(it.protectionDomain.codeSource.location.toURI()).path }
            .joinToString(File.pathSeparator)
        val compilerName = if (System.getProperty("os.name").startsWith("Windows")) "javac.exe" else "javac"
        val output = temporaryFolder.newFile("javac-output.txt")
        val compiler = ProcessBuilder(
            File(System.getProperty("java.home"), "bin${File.separator}$compilerName").path,
            "-classpath", classpath, "-proc:none", "-d", temporaryFolder.root.path, source.path
        ).redirectErrorStream(true).redirectOutput(output).start()
        try {
            assertTrue("Java compiler timed out", compiler.waitFor(30, TimeUnit.SECONDS))
            assertEquals(output.readText(), 0, compiler.exitValue())
        } finally {
            if (compiler.isAlive) compiler.destroyForcibly()
        }
        URLClassLoader(arrayOf(temporaryFolder.root.toURI().toURL()), javaClass.classLoader).use { loader ->
            loader.loadClass("JavaMfaConsumer")
                .getMethod("exercise", MfaSdkManager::class.java, MfaAuthSession::class.java)
                .invoke(null, MfaSdkManager.create(mock(Context::class.java)),
                    MfaAuthSession.create("session", "registration", "Account", "user", false))
            runTest {
                val completion = AtomicReference<CallbackTokenProvider.Callback>()
                val javaProvider = loader.loadClass("JavaMfaConsumer")
                    .getMethod("providerWithoutCancellation", AtomicReference::class.java)
                    .invoke(null, completion) as CallbackTokenProvider
                val provider = adaptTokenProvider(javaProvider, StandardTestDispatcher(testScheduler))
                val waiting = async {
                    provider.getAccessToken(TokenProviderParameters("https://example.invalid", listOf("scope")))
                }
                runCurrent()
                assertFalse(waiting.isCompleted)
                val result = TokenProviderResult(
                    "token", null, listOf("scope"), "tenant", "object",
                    "https://example.invalid", "Bearer"
                )
                completion.get().onSuccess(result)
                assertSame(result, waiting.await())
            }
        }
    }
}
