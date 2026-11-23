// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MfaPublicApiTest {
    @Test
    fun `manager API does not expose kotlinx coroutine types`() {
        for (method in MfaSdkManager::class.java.methods) {
            val signatureTypes = method.parameterTypes.toList() + method.returnType
            assertFalse("$method exposes a coroutine implementation type",
                signatureTypes.any { it.name.startsWith("kotlinx.coroutines.") })
        }
    }

    @Test
    fun `public value models do not expose generated copy or destructuring methods`() {
        for (type in listOf(
            MfaRegistrationParameters.QrCode::class.java,
            MfaRegistrationParameters.Activation::class.java,
            MfaRegistrationResult::class.java,
            MfaAction.Approve::class.java
        )) {
            val generatedMethods = type.declaredMethods.map { it.name }.filter {
                it == "copy" || it == "copy\$default" || it.matches(Regex("component\\d+"))
            }
            assertTrue("${type.simpleName} exposes $generatedMethods", generatedMethods.isEmpty())
        }
    }

    @Test
    fun `public value models compare every field and retain value hashing`() {
        val cases = listOf(
            listOf(
                MfaRegistrationParameters.QrCode("text"),
                MfaRegistrationParameters.QrCode("text"),
                MfaRegistrationParameters.QrCode("other")
            ),
            listOf(
                MfaRegistrationParameters.Activation("code", "url"),
                MfaRegistrationParameters.Activation("code", "url"),
                MfaRegistrationParameters.Activation("other", "url"),
                MfaRegistrationParameters.Activation("code", "other")
            ),
            listOf(
                MfaRegistrationResult("registration", "Account", "user"),
                MfaRegistrationResult("registration", "Account", "user"),
                MfaRegistrationResult("other", "Account", "user"),
                MfaRegistrationResult("registration", "Other", "user"),
                MfaRegistrationResult("registration", "Account", "other")
            ),
            listOf(MfaAction.Approve(), MfaAction.Approve(null), MfaAction.Approve("42")),
            listOf(MfaAction.Approve("42"), MfaAction.Approve("42"), MfaAction.Approve("24"))
        )
        for (values in cases) {
            val original = values[0]
            val equivalent = values[1]
            assertEquals(original, original)
            assertEquals(original, equivalent)
            assertEquals(equivalent, original)
            assertEquals(original.hashCode(), equivalent.hashCode())
            assertTrue(hashSetOf(original).contains(equivalent))
            assertNotEquals(original, null)
            assertNotEquals(original, Any())
            values.drop(2).forEach { assertNotEquals(original, it) }
        }
    }

    @Test
    fun `invalid operations return sanitized correlated SDK errors`() = runBlocking {
        val manager = MfaTestFixture().manager()
        val correlationIds = mutableSetOf<String>()

        for ((name, operation) in operations(manager).filterNot {
            it.first in setOf("updatePushRegistration", "registeredAuthMethods",
                "handlePushNotification", "pullPendingNotifications")
        }) {
            val failure = runCatching { operation() }.exceptionOrNull()
            assertTrue("$name must fail with an MFA error", failure is MfaSdkException)
            val error = failure as MfaSdkException
            assertEquals(error.errorMessage, error.message)
            assertNull(error.cause)
            assertEquals(error.correlationId, UUID.fromString(error.correlationId).toString())
            assertTrue("$name must get a new operation ID", correlationIds.add(error.correlationId))
        }

    }

    @Test
    fun `each SDK classification selects exactly one caller action`() {
        val cases = listOf(
            MfaErrorType.PUSH_TOKEN_MISSING to MfaSdkException::isPushTokenMissing,
            MfaErrorType.REGISTRATION_NOT_FOUND to MfaSdkException::isRegistrationNotFound,
            MfaErrorType.INVALID_CHALLENGE to MfaSdkException::isInvalidChallenge,
            MfaErrorType.INVALID_CODE to MfaSdkException::isInvalidCode,
            MfaErrorType.GENERAL_ERROR to MfaSdkException::isGeneralError
        )

        for ((type, classifier) in cases) {
            val error = MfaSdkException.create(type, "Safe SDK diagnostic.", UUID.randomUUID().toString())
            assertTrue("$type must select its caller action", classifier(error))
            assertEquals("$type must not select another action", 1, cases.count { it.second(error) })
        }
    }

    @Test
    fun `cancelled operations preserve cancellation instead of reporting an SDK error`() {
        val manager = MfaTestFixture().manager()
        for ((name, operation) in operations(manager)) {
            val cancellation = CancellationException("Host cancellation")
            val job = Job().apply { cancel(cancellation) }
            var failure: Throwable? = null

            // Start the body directly; a cancelled dispatcher must not short-circuit the SDK call.
            operation.startCoroutine(object : Continuation<Any?> {
                override val context: CoroutineContext = job

                override fun resumeWith(result: Result<Any?>) {
                    failure = result.exceptionOrNull()
                }
            })

            assertSame("$name must preserve the original cancellation", cancellation, failure)
        }
    }

    @Test
    fun `model string representations omit activation numbers and account identifiers`() {
        val sensitive = "sensitive-test-value"
        val models = listOf(
            MfaRegistrationParameters.QrCode(sensitive),
            MfaRegistrationParameters.Activation(sensitive, sensitive),
            MfaRegistrationResult(sensitive, sensitive, sensitive),
            MfaAuthSession.create(sensitive, sensitive, sensitive, sensitive, true),
            MfaAction.Approve(sensitive)
        )

        for (model in models) {
            assertFalse("${model.javaClass.simpleName} must redact its values", model.toString().contains(sensitive))
        }
    }

    private fun operations(
        manager: MfaSdkManager
    ): List<Pair<String, suspend () -> Any?>> {
        val session = MfaAuthSession.create(
            guid = "session-id",
            registrationId = "registration-id",
            accountName = "Account display",
            username = "user@example.invalid",
            requiresNumberMatching = true
        )
        return listOf(
            "updatePushRegistration" to { manager.updatePushRegistration("opaque-fcm-token") },
            "registerAuthMethod QR" to {
                manager.registerAuthMethod(MfaRegistrationParameters.QrCode("activation-input"))
            },
            "registerAuthMethod manual" to {
                manager.registerAuthMethod(
                    MfaRegistrationParameters.Activation("activation-code", "https://example.invalid")
                )
            },
            "registeredAuthMethods" to { manager.registeredAuthMethods() },
            "handlePushNotification" to { manager.handlePushNotification(mapOf("key" to "payload")) },
            "pullPendingNotifications" to { manager.pullPendingNotifications() },
            "respond number matching" to { manager.respond(session, MfaAction.Approve("42")) },
            "respond ordinary approval" to { manager.respond(session, MfaAction.Approve(null)) },
            "respond denial" to { manager.respond(session, MfaAction.Deny) }
        )
    }
}
