package com.microsoft.identity.nativeauth.statemachine.states

import android.os.Parcel
import android.os.Parcelable
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.parameters.NativeAuthChallengeAuthMethodParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthRegisterStrongAuthVerificationRequiredResultParameter
import com.microsoft.identity.nativeauth.statemachine.errors.RegisterStrongAuthSubmitChallengeError
import com.microsoft.identity.nativeauth.statemachine.results.RegisterStrongAuthChallengeResult
import com.microsoft.identity.nativeauth.statemachine.results.RegisterStrongAuthSubmitChallengeResult
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.launch

class RegisterStrongAuthState(
    override val continuationToken: String,
    override val correlationId: String,
    private val scopes: List<String>?,
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
            scopes = emptyList(),
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
        scopes = parcel.createStringArrayList(),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeStringList(scopes)
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
    private val scopes: List<String>?,
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
            scopes = emptyList(),
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
        scopes = parcel.createStringArrayList(),
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(continuationToken)
        parcel.writeString(correlationId)
        parcel.writeStringList(scopes)
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