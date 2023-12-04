package com.microsoft.identity.client.statemachine

import com.microsoft.identity.client.statemachine.results.SignInResendCodeResult
import com.microsoft.identity.client.statemachine.results.SignInResult
import com.microsoft.identity.client.statemachine.results.SignInSubmitCodeResult
import com.microsoft.identity.client.statemachine.results.SignInSubmitPasswordResult
import com.microsoft.identity.client.statemachine.results.SignInUsingPasswordResult

class SignInErrorTypes () {
    companion object {
        const val invalid_credentials = "invalid_credentials"
    }
}

open class SignInError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): SignInResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {
    fun isUserNotFound(): Boolean = this.errorType == ErrorTypes.user_not_found
}
class SignInUsingPasswordError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override  val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): SignInUsingPasswordResult, SignInError(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {

    fun isInvalidCredentials(): Boolean = this.errorType == SignInErrorTypes.invalid_credentials
}

class SignInSubmitPasswordError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String,
    override  val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): SignInSubmitPasswordResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {
    fun isInvalidCredentials(): Boolean = this.errorType == SignInErrorTypes.invalid_credentials
}
