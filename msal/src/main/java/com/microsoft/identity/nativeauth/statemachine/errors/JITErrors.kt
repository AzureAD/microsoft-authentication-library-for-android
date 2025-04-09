package com.microsoft.identity.nativeauth.statemachine.errors

import com.microsoft.identity.nativeauth.statemachine.results.RegisterStrongAuthChallengeResult
import com.microsoft.identity.nativeauth.statemachine.results.RegisterStrongAuthSubmitChallengeResult

class RegisterStrongAuthChallengeError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): RegisterStrongAuthChallengeResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception)
{
    fun isInvalidInput(): Boolean = this.errorType == ErrorTypes.INVALID_INPUT
}

class RegisterStrongAuthSubmitChallengeError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): RegisterStrongAuthSubmitChallengeResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception)
{
    fun isInvalidChallenge(): Boolean = this.errorType == ErrorTypes.INVALID_CHALLENGE
}