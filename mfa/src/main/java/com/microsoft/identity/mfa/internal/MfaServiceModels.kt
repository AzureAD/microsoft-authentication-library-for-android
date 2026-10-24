// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa.internal

import com.microsoft.identity.mfa.MfaAuthSession
import com.microsoft.identity.mfa.MfaDecision
import com.microsoft.identity.mfa.MfaDisplayMetadata
import com.microsoft.identity.mfa.MfaRegistrationParameters
import com.microsoft.identity.mfa.MfaSdkStatus
import java.util.Collections

/** Prevents nested request/result rendering from disclosing identifiers or credential material. */
internal sealed class MfaServiceModel {
    final override fun toString(): String = "${javaClass.simpleName}(<redacted>)"
}

/** Equality includes every scope; username is intentionally not an identity component. */
internal data class MfaAccountIdentity(
    val environment: String,
    val tenantId: String,
    val objectId: String,
    val registrationId: String
) : MfaServiceModel()

/**
 * Identifies one logical operation and its attempt within an installation/registration generation.
 * These are correlation identities, not a claim that any future service supports idempotency.
 */
internal data class MfaOperationContext(
    val operationId: String,
    val attemptId: String,
    val installationId: String,
    val installationGeneration: Long,
    val registrationGeneration: Long
) : MfaServiceModel()

/** Reference to an internally managed credential; no provisioning or persistence is implemented. */
internal class MfaCredentialHandle(val value: String) : MfaServiceModel()

/** Opaque material, defensively copied. Callers own and must clear any exported byte snapshot. */
internal class MfaCredentialMaterial(bytes: ByteArray) : MfaServiceModel() {
    private val bytes = bytes.copyOf()
    fun copyBytes(): ByteArray = bytes.copyOf()
}

internal class MfaRegistrationContext(
    val operation: MfaOperationContext,
    val account: MfaAccountIdentity,
    val credential: MfaCredentialHandle
) : MfaServiceModel()

internal class MfaActivationRequest(
    val operation: MfaOperationContext,
    val parameters: MfaRegistrationParameters
) : MfaServiceModel()

/** Activation acknowledgement alone is not confirmed enrollment. */
internal class MfaEnrollmentMaterial(
    val context: MfaRegistrationContext,
    val material: MfaCredentialMaterial,
    val displayMetadata: MfaDisplayMetadata
) : MfaServiceModel()

/** Validation may arrive independently, before the activation call returns. */
internal class MfaValidationRequest(
    val context: MfaRegistrationContext,
    val challengeHandle: String
) : MfaServiceModel()

internal class MfaValidationReceipt(
    val context: MfaRegistrationContext,
    val receiptHandle: String
) : MfaServiceModel()

internal class MfaConfirmationRequest(
    val context: MfaRegistrationContext,
    val validationReceipt: MfaValidationReceipt
) : MfaServiceModel()

internal class MfaChallengeRequest(
    val context: MfaRegistrationContext,
    val challengeHandle: String
) : MfaServiceModel()

internal class MfaChallengeEnvelope(
    val context: MfaRegistrationContext,
    val session: MfaAuthSession
) : MfaServiceModel()

internal class MfaChallengeBatch(challenges: List<MfaChallengeEnvelope>) : MfaServiceModel() {
    val challenges: List<MfaChallengeEnvelope> =
        Collections.unmodifiableList(ArrayList(challenges))
}

/** User intent is separate from the public acknowledged MfaDecision. */
internal enum class MfaDecisionIntent { APPROVE, DENY }

internal class MfaDecisionRequest(
    val context: MfaRegistrationContext,
    val sessionHandle: String,
    val intent: MfaDecisionIntent
) : MfaServiceModel()

/** A supplied number, never the expected answer. Must not fall through to ordinary approval. */
internal class MfaNumberRequest(
    val context: MfaRegistrationContext,
    val sessionHandle: String,
    val number: String
) : MfaServiceModel()

/** Host push token; not an MSAL access/refresh token or a provider-specific type. */
internal class MfaPushToken(val value: String) : MfaServiceModel()

internal class MfaPushTokenRequest(
    val context: MfaRegistrationContext,
    val token: MfaPushToken
) : MfaServiceModel()

internal class MfaReconciliationRequest(
    val context: MfaRegistrationContext,
    val operationToReconcile: String
) : MfaServiceModel()

/** Null decision means no acknowledged decision was established. */
internal class MfaReconciliationResult(
    val status: MfaSdkStatus,
    val decision: MfaDecision?
) : MfaServiceModel()

/** No lifecycle or rich-push support may be assumed merely because an operation is declared. */
internal data class MfaServiceCapabilities(
    val pushTokenUpdates: Boolean = false,
    val unregistration: Boolean = false,
    val reconciliation: Boolean = false,
    val richPush: Boolean = false,
    val challengePull: Boolean = false
)

/** Definite rejection reasons, unlike transport failure or an unknown operation outcome. */
internal enum class MfaServiceRejection {
    INVALID_INPUT,
    INVALID_SESSION,
    UNKNOWN_ACCOUNT,
    UNSUPPORTED_POLICY,
    EXPIRED,
    INVALID_NUMBER,
    SERVICE_REJECTION,
    RECOVERY_REQUIRED
}

/**
 * Acknowledgement means a service response, never local intent. Unknown may already have taken
 * effect and must not be blindly retried. No branch carries raw service bodies or exceptions.
 */
internal sealed class MfaServiceOutcome<out T> : MfaServiceModel() {
    class Acknowledged<T>(val value: T) : MfaServiceOutcome<T>()
    class Rejected(val reason: MfaServiceRejection) : MfaServiceOutcome<Nothing>()
    object Unknown : MfaServiceOutcome<Nothing>()
    object NotSupported : MfaServiceOutcome<Nothing>()
    object Unavailable : MfaServiceOutcome<Nothing>()
}
