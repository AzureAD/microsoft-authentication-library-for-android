package com.microsoft.identity.nativeauth.statemachine.errors

import com.microsoft.identity.nativeauth.statemachine.results.MFAGetAuthMethodsResult
import com.microsoft.identity.nativeauth.statemachine.results.MFARequiredResult
import com.microsoft.identity.nativeauth.statemachine.results.MFASubmitChallengeResult

open class MFAError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): MFARequiredResult, BrowserRequiredError, MFAGetAuthMethodsResult, MFASubmitChallengeResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception)

class SubmitChallengeError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    val subError: String? = null,
    override var exception: Exception? = null
): MFASubmitChallengeResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception)
{
    fun isInvalidChallenge(): Boolean = this.errorType == ErrorTypes.INVALID_CODE
}