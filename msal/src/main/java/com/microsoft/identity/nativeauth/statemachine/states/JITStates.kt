package com.microsoft.identity.nativeauth.statemachine.states

import android.os.Parcel
import android.os.Parcelable
import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.JITChallengeAuthMethodCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.JITCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.nativeauth.internal.commands.JITChallengeAuthMethodCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.parameters.NativeAuthChallengeAuthMethodParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthRegisterStrongAuthVerificationRequiredResultParameter
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.RegisterStrongAuthChallengeError
import com.microsoft.identity.nativeauth.statemachine.errors.RegisterStrongAuthSubmitChallengeError
import com.microsoft.identity.nativeauth.statemachine.results.RegisterStrongAuthChallengeResult
import com.microsoft.identity.nativeauth.statemachine.results.RegisterStrongAuthSubmitChallengeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.utils.NativeAuthCommandParametersAdapter
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterStrongAuthState(
    override val continuationToken: String,
    override val correlationId: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {
    private val TAG: String = RegisterStrongAuthState::class.java.simpleName

    /**
     * ChallengeAuthMethodCallback receives the result for challengeAuthMethod() in strong authentication method registration flows in native authentication.
     */
    interface ChallengeAuthMethodCallback : Callback<RegisterStrongAuthChallengeResult>

    /**
     * Requests the server to send the challenge to the default authentication method
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param parameters [com.microsoft.identity.nativeauth.parameters.NativeAuthChallengeAuthMethodParameters] Parameters used to challenge an authentication method.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthState.ChallengeAuthMethodCallback] to receive the result on.
     */
    fun challengeAuthMethod(parameters: NativeAuthChallengeAuthMethodParameters, callback: ChallengeAuthMethodCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.challengeAuthMethod(callback: ChallengeAuthMethodCallback)"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = challengeAuthMethod(parameters)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in challengeAuthMethod", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Requests the server to send the challenge to the default authentication method; Kotlin coroutines variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param parameters [com.microsoft.identity.nativeauth.parameters.NativeAuthChallengeAuthMethodParameters] Parameters used to challenge an authentication method.
     * @return The result of the challenge authentication method action.
     */
    suspend fun challengeAuthMethod(parameters: NativeAuthChallengeAuthMethodParameters): RegisterStrongAuthChallengeResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.challengeAuthMethod(parameters: NativeAuthChallengeAuthMethodParameters)"
        )

        Logger.warn(
            TAG,
            "Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications."
        )

        val verificationContact = parameters.verificationContact ?: parameters.authMethod.loginHint
        val params =
            NativeAuthCommandParametersAdapter.createChallengeAuthMethodCommandParameters(
                configuration = config,
                tokenCache = config.oAuth2TokenCache,
                verificationContact = verificationContact,
                challengeType = parameters.authMethod.challengeType,
                continuationToken = continuationToken,
                correlationId = correlationId
            )
        val command = JITChallengeAuthMethodCommand(
                    parameters = params,
                    controller = NativeAuthMsalController(),
                    publicApiId = PublicApiId.NATIVE_AUTH_JIT_CHALLENGE_AUTH_METHOD
                )
        val rawCommandResult =
                    CommandDispatcher.submitSilentReturningFuture(command)
                        .get()
        return withContext(Dispatchers.IO) {
            return@withContext when (val result =
                rawCommandResult.checkAndWrapCommandResultType<JITChallengeAuthMethodCommandResult>()) {
                is INativeAuthCommandResult.APIError -> {
                    Logger.warnWithObject(
                        TAG,
                        result.correlationId,
                        "Challenge auth method received unexpected result: ",
                        result
                    )
                    RegisterStrongAuthChallengeError(
                        errorMessage = (result as INativeAuthCommandResult.Error).errorDescription,
                        error = (result as INativeAuthCommandResult.Error).error,
                        correlationId = (result as INativeAuthCommandResult.Error).correlationId,
                        errorCodes = (result as INativeAuthCommandResult.Error).errorCodes,
                        exception = (result as INativeAuthCommandResult.APIError).exception
                    )

                }
                is SignInCommandResult.Complete -> {
                    val authenticationResult =
                        AuthenticationResultAdapter.adapt(result.authenticationResult)
                    SignInResult.Complete(
                        resultValue = AccountState.createFromAuthenticationResult(
                            authenticationResult = authenticationResult,
                            correlationId = result.correlationId,
                            config = config
                        )
                    )
                }
                is JITCommandResult.IncorrectVerificationContact -> {
                    RegisterStrongAuthChallengeError(
                        errorType = ErrorTypes.INVALID_INPUT,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId,
                        errorCodes = result.errorCodes
                    )
                }
                is JITCommandResult.VerificationRequired -> {
                    RegisterStrongAuthChallengeResult.VerificationRequired(
                        result = NativeAuthRegisterStrongAuthVerificationRequiredResultParameter(
                            nextState = RegisterStrongAuthVerificationRequiredState(
                                continuationToken = result.continuationToken,
                                correlationId = result.correlationId,
                                config = config
                            ),
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel
                        )
                    )
                }
            }

        }
//        return withContext(Dispatchers.IO) {
//            try {
//                val params =
//                    NativeAuthCommandParametersAdapter.createChallengeAuthMethodCommandParameters(
//                        config,
//                        config.oAuth2TokenCache,
//                        continuationToken,
//                        correlationId,
//                        scopes
//                    )
//                val command = MFAChallengeCommand(
//                    parameters = params,
//                    controller = NativeAuthMsalController(),
//                    publicApiId = PublicApiId.NATIVE_AUTH_MFA_DEFAULT_CHALLENGE
//                )
//
//                val rawCommandResult =
//                    CommandDispatcher.submitSilentReturningFuture(command)
//                        .get()
//
//                return@withContext when (val result =
//                    rawCommandResult.checkAndWrapCommandResultType<MFAChallengeCommandResult>()) {
//                    is MFACommandResult.VerificationRequired -> {
//                        MFARequiredResult.VerificationRequired(
//                            nextState = MFARequiredState(
//                                continuationToken = result.continuationToken,
//                                correlationId = result.correlationId,
//                                scopes = scopes,
//                                config = config
//                            ),
//                            codeLength = result.codeLength,
//                            sentTo = result.challengeTargetLabel,
//                            channel = result.challengeChannel
//                        )
//                    }
//
//                    is MFACommandResult.SelectionRequired -> {
//                        MFARequiredResult.SelectionRequired(
//                            nextState = MFARequiredState(
//                                continuationToken = result.continuationToken,
//                                correlationId = result.correlationId,
//                                scopes = scopes,
//                                config = config
//                            ),
//                            authMethods = result.authMethods.toListOfAuthMethods()
//                        )
//                    }
//
//                    is INativeAuthCommandResult.APIError -> {
//                        Logger.warnWithObject(
//                            TAG,
//                            result.correlationId,
//                            "requestChallenge() received unexpected result: ",
//                            result
//                        )
//                        MFARequestChallengeError(
//                            errorMessage = result.errorDescription,
//                            error = result.error,
//                            correlationId = result.correlationId,
//                            errorCodes = result.errorCodes,
//                            exception = result.exception
//                        )
//                    }
//
//                    is INativeAuthCommandResult.Redirect -> {
//                        MFARequestChallengeError(
//                            errorType = ErrorTypes.BROWSER_REQUIRED,
//                            error = result.error,
//                            errorMessage = result.errorDescription,
//                            correlationId = result.correlationId
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                MFARequestChallengeError(
//                    errorType = ErrorTypes.CLIENT_EXCEPTION,
//                    errorMessage = "MSAL client exception occurred in requestChallenge().",
//                    exception = e,
//                    correlationId = correlationId
//                )
//            }
//        }
    }

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString() ?: "",
        correlationId = parcel.readString() ?: "UNSET",
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RegisterStrongAuthState> {
        override fun createFromParcel(parcel: Parcel): RegisterStrongAuthState {
            return RegisterStrongAuthState(parcel)
        }

        override fun newArray(size: Int): Array<RegisterStrongAuthState?> {
            return arrayOfNulls(size)
        }
    }
}

class RegisterStrongAuthVerificationRequiredState(
    override val continuationToken: String,
    override val correlationId: String,
    private val config: NativeAuthPublicClientApplicationConfiguration
) : BaseState(continuationToken = continuationToken, correlationId = correlationId), State, Parcelable {

    private val TAG: String = RegisterStrongAuthVerificationRequiredState::class.java.simpleName

    /**
     * SubmitChallengeCallback receives the result for submitChallenge() in strong authentication method registration flows in native authentication.
     */
    interface SubmitChallengeCallback : Callback<RegisterStrongAuthSubmitChallengeResult>

    /**
     * Submits the challenge value to the server; callback variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param challenge The challenge value to be submitted.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthState.SubmitChallengeCallback] to receive the result on.
     */
    fun submitChallenge(challenge: String, callback: SubmitChallengeCallback) {
        NativeAuthPublicClientApplication.pcaScope.launch {
            val result = submitChallenge(challenge)
            callback.onResult(result)
        }
    }

    /**
     * Submits the challenge value to the server; Kotlin coroutines variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param challenge The challenge value to be submitted.
     * @return The results of the submit challenge action.
     */
    suspend fun submitChallenge(challenge: String): RegisterStrongAuthSubmitChallengeResult {
        return RegisterStrongAuthSubmitChallengeError(
            errorType = "errorType",
            error = "error",
            errorMessage = "errorMessage",
            correlationId = "correlationId",
            errorCodes = listOf(500123),
            exception = null
        )
    }

    /**
     * ChallengeAuthMethodCallback receives the result for challengeAuthMethod() in strong authentication method registration flows in native authentication.
     */
    interface ChallengeAuthMethodCallback : Callback<RegisterStrongAuthChallengeResult>

    /**
     * Requests the server to send the challenge to the default authentication method; callback variant
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param parameters [com.microsoft.identity.nativeauth.parameters.NativeAuthChallengeAuthMethodParameters] Parameters used to challenge an authentication method.
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthState.ChallengeAuthMethodCallback] to receive the result on.
     */
    fun challengeAuthMethod(parameters: NativeAuthChallengeAuthMethodParameters, callback: ChallengeAuthMethodCallback) {
        NativeAuthPublicClientApplication.pcaScope.launch {
            val result = challengeAuthMethod(parameters)
            callback.onResult(result)
        }
    }

    /**
     * Requests the server to send the challenge to the default authentication method; Kotlin coroutines variant.
     *
     * <strong><u>Warning: this API is experimental. It may be changed in the future without notice. Do not use in production applications.</u></strong>
     * @param parameters [com.microsoft.identity.nativeauth.parameters.NativeAuthChallengeAuthMethodParameters] Parameters used to challenge an authentication method.
     * @return The result of the challenge authentication method action.
     */
    suspend fun challengeAuthMethod(parameters: NativeAuthChallengeAuthMethodParameters): RegisterStrongAuthChallengeResult {
        val nextState = RegisterStrongAuthVerificationRequiredState(
            continuationToken = "continuationToken",
            correlationId = "correlationId",
            config = config
        )
        val params = NativeAuthRegisterStrongAuthVerificationRequiredResultParameter(
            nextState = nextState,
            codeLength = 8,
            sentTo = "sentTo",
            channel = "email"
        )
        return RegisterStrongAuthChallengeResult.VerificationRequired(result = params)
    }

    constructor(parcel: Parcel) : this(
        continuationToken = parcel.readString() ?: "",
        correlationId = parcel.readString() ?: "UNSET",
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RegisterStrongAuthVerificationRequiredState> {
        override fun createFromParcel(parcel: Parcel): RegisterStrongAuthVerificationRequiredState {
            return RegisterStrongAuthVerificationRequiredState(parcel)
        }

        override fun newArray(size: Int): Array<RegisterStrongAuthVerificationRequiredState?> {
            return arrayOfNulls(size)
        }
    }
}