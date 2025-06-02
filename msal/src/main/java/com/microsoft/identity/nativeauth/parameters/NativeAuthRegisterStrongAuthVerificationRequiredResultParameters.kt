package com.microsoft.identity.nativeauth.parameters

import com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthVerificationRequiredState

class NativeAuthRegisterStrongAuthVerificationRequiredResultParameter internal constructor(
    internal val nextState: RegisterStrongAuthVerificationRequiredState,
    internal val codeLength: Int,
    internal val sentTo: String,
    internal val channel: String
) {

    /**
     * The next state to use to continue the strong authentication method registration flow.
     */
    fun getNextState(): RegisterStrongAuthVerificationRequiredState {
        return nextState
    }

    /**
     * The length of the challenge required by the server.
     */
    fun getCodeLength(): Int {
        return codeLength
    }

    /**
     * The email/phone number the challenge was sent to.
     */
    fun getSentTo(): String {
        return sentTo
    }

    /**
     * the channel(email/phone) the challenge was sent through.
     */
    fun getChannel(): String {
        return channel
    }
}