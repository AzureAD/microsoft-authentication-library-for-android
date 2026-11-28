// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

import android.content.Context
import android.os.Build
import com.microsoft.identity.nativeauth.mfa.internal.EncryptedMfaStore
import com.microsoft.identity.nativeauth.mfa.internal.MfaEngine
import com.microsoft.identity.nativeauth.mfa.internal.MfaServiceClient
import com.microsoft.identity.nativeauth.mfa.internal.OkHttpMfaTransport
import com.microsoft.identity.nativeauth.mfa.internal.mfaOperation

/**
 * Registers MFA accounts and responds to sign-in requests using the hosting app's push delivery.
 *
 * Uses encrypted, application-private storage and trusted commercial-cloud service endpoints.
 * The host owns FCM registration, notification delivery and user interaction. A validation-only
 * service response is not a successful workflow. No Graph deletion or token acquisition is provided.
 * A failed storage commit disables further stateful work on this manager for the process lifetime:
 * the on-disk state may have advanced, and continuing from a cached snapshot is unsafe.
 *
 * SDK-originated errors use the same five classifiers across operations. Cancellation remains
 * separate; neither failure nor cancellation implies rollback or permission to replay a mutation.
 * Retain one manager for enrollment, push callbacks and session operations.
 */
class MfaSdkManager private constructor(engineProvider: () -> MfaEngine) {
    private val engine by lazy(engineProvider)
    /**
     * Supplies the current raw FCM token without requesting notification permission or enrolling.
     *
     * Persists the token, then updates existing registrations sequentially using
     * push validation without reenrollment. Accounts already using the token are skipped; with no
     * updates needed, no service call is made. Forward validation notifications to
     * [handlePushNotification] while this operation is pending.
     *
     * Success requires validation and local persistence for every affected account. Failure stops
     * remaining work without rolling back completed accounts; the latest token may already be stored.
     * Uncertain submitted updates require service-defined recovery, not automatic replay or a promise
     * of resumability after process restart. Supplying the old token does not resolve an uncertain
     * update. A validation handler already in flight can persist a confirmed update after its
     * initiating caller has stopped waiting.
     * @param deviceToken Opaque raw FCM token. Never log it.
     * @throws MfaSdkException If validation, rotation or persistence fails.
     */
    suspend fun updatePushRegistration(deviceToken: String): Unit =
        mfaOperation { engine.updatePushRegistration(deviceToken) }

    /**
     * Registers an account from decoded QR text or manual activation details.
     *
     * First await [updatePushRegistration]. Validates and normalizes the activation
     * input; begins activation and processes validation pushes concurrently through
     * [handlePushNotification]. Keep pending enrollment in memory, validate the expected account,
     * then confirm with the service and persist locally before returning display metadata.
     *
     * Failure or cancellation returns no registration but does not prove remote rollback. Lost
     * confirmation responses and local-save failures may leave an activated remote method.
     * No automatic retry, reconciliation, compensating deletion or durable enrollment resumption
     * is promised. A new enrollment attempt is not recovery of a previous uncertain attempt.
     * @param registrationParameters Activation material. Never log it.
     * @throws MfaSdkException If input, enrollment, confirmation or persistence fails.
     */
    suspend fun registerAuthMethod(
        registrationParameters: MfaRegistrationParameters
    ): MfaRegistrationResult = mfaOperation { engine.register(registrationParameters) }

    /**
     * Lists confirmed, locally stored registrations without querying the service.
     *
     * Returns an empty list when none exist and excludes pending enrollment.
     * Listing does not detect server-side removal or prove current validity.
     * @throws MfaSdkException If local storage cannot be read.
     */
    suspend fun registeredAuthMethods(): List<MfaRegistrationResult> =
        mfaOperation { engine.registrations() }

    /**
     * Handles an FCM data message without presenting UI or submitting a user decision.
     *
     * Validates recognized MFA fields and their account/pending-operation context.
     * Authentication notifications retrieve sessions; enrollment/token-update validation completes
     * the corresponding pending operation and returns no sessions. Forward validation pushes even
     * while registration or token update is pending. Unrelated notifications return an empty list
     * without a service call; malformed or mismatched MFA input throws rather than appearing empty.
     *
     * Returned sessions support both approval and denial; unsupported policy fails retrieval.
     * The host owns notification/background lifecycle and explicit user interaction. Delivery or a
     * notification tap alone is not approval.
     * @param payload FCM data map. Never log its contents.
     * @throws MfaSdkException If recognized input, association, policy or service validation fails.
     */
    suspend fun handlePushNotification(payload: Map<String, String>): List<MfaAuthSession> =
        mfaOperation { engine.handlePush(payload) }

    /**
     * Retrieves pending sessions, deduplicated by GUID, for locally registered accounts.
     *
     * No accounts means no service calls. An account check/fetch failure or
     * unsupported policy throws instead of returning misleading empty or partial results.
     * Every returned session supports approval and denial, subject to current service validation.
     * Retain the original session for [respond]; the host owns presentation.
     * @throws MfaSdkException If any account check, session fetch or policy validation fails.
     */
    suspend fun pullPendingNotifications(): List<MfaAuthSession> = mfaOperation { engine.pull() }

    /**
     * Submits an explicit approval or denial and returns only its service-acknowledged decision.
     *
     * Requires the original session issued by this manager and rejects submitted or
     * completed sessions. Validate a required matching number before submission; missing input never
     * downgrades approval. Ordinary approval requires authoritative policy and backend support.
     * Ordinary approval is currently unsupported and fails session retrieval. Denial requires no
     * number. Unsupported policies fail retrieval rather than yielding deny-only
     * sessions, and revalidation must fail before submission if support is no longer established.
     *
     * Persist replay protection before submission. After an uncertain failure or cancellation, keep
     * the conservative replay block: do not automatically retry, switch actions or change modes.
     * Wrong-number continuation requires a confirmed service contract, not an assumed retry allowance.
     * Failed approval throws rather than returning [MfaDecision.DENIED]. An acknowledgment does not
     * mean sign-in completed or a token was issued.
     * @param session Original object returned by this manager, not a reconstructed identifier.
     * @param action Explicit user choice; never log a supplied matching number.
     * @throws MfaSdkException If the session, number, policy or service acknowledgment is invalid.
     */
    suspend fun respond(session: MfaAuthSession, action: MfaAction): MfaDecision =
        mfaOperation { engine.respond(session, action) }

    /** Java variant of [updatePushRegistration]; completion follows [MfaCallback]'s background contract. */
    fun updatePushRegistration(deviceToken: String, callback: MfaCallback<Void?>): MfaOperation =
        launchMfaOperation(callback) { updatePushRegistration(deviceToken); null }

    /** Java variant of [registerAuthMethod]; delegates to the same suspend operation. */
    fun registerAuthMethod(
        registrationParameters: MfaRegistrationParameters,
        callback: MfaCallback<MfaRegistrationResult>
    ): MfaOperation = launchMfaOperation(callback) { registerAuthMethod(registrationParameters) }

    /** Java variant of [registeredAuthMethods]; delegates to the same suspend operation. */
    fun registeredAuthMethods(callback: MfaCallback<List<MfaRegistrationResult>>): MfaOperation =
        launchMfaOperation(callback) { registeredAuthMethods() }

    /** Java variant of [handlePushNotification]; delegates to the same suspend operation. */
    fun handlePushNotification(
        payload: Map<String, String>,
        callback: MfaCallback<List<MfaAuthSession>>
    ): MfaOperation = launchMfaOperation(callback) { handlePushNotification(payload) }

    /** Java variant of [pullPendingNotifications]; delegates to the same suspend operation. */
    fun pullPendingNotifications(callback: MfaCallback<List<MfaAuthSession>>): MfaOperation =
        launchMfaOperation(callback) { pullPendingNotifications() }

    /** Java variant of [respond]; delegates to the same suspend operation. */
    fun respond(
        session: MfaAuthSession,
        action: MfaAction,
        callback: MfaCallback<MfaDecision>
    ): MfaOperation = launchMfaOperation(callback) { respond(session, action) }

    /** Factory for the hosting application's manager. */
    companion object {
        private val instances = HashMap<String, MfaSdkManager>()

        /**
         * Returns the process-wide manager for the hosting application. Only the application context
         * is retained; storage and service clients initialize lazily on the first operation.
         * @param context Hosting Android context.
         */
        @JvmStatic
        fun create(context: Context): MfaSdkManager {
            val app = context.applicationContext
            return synchronized(instances) {
                instances.getOrPut(app.packageName) {
                    MfaSdkManager {
                        @Suppress("DEPRECATION")
                        val info = app.packageManager.getPackageInfo(app.packageName, 0)
                        @Suppress("DEPRECATION")
                        val version = info.versionName ?: info.versionCode.toString()
                        MfaEngine(EncryptedMfaStore(app, "msal-mfa-v1.bin"),
                            MfaServiceClient(OkHttpMfaTransport(), app.packageName, version,
                                Build.VERSION.RELEASE, Build.MODEL))
                    }
                }
            }
        }

        @JvmSynthetic
        internal fun create(engine: MfaEngine): MfaSdkManager = MfaSdkManager { engine }
    }
}
