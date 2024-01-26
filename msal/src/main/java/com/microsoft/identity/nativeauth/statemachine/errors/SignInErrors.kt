package com.microsoft.identity.nativeauth.statemachine.errors

import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInSubmitPasswordResult

/**
 * SignInErrorTypes class holds the specific error type values that can be returned
 * only by the signin flow.
 */
internal class SignInErrorTypes {
    companion object {
        /*
         * The INVALID_CREDENTIALS value indicates that credentials provided by the users are not acceptable to the server.
         * The flow should be restarted or the password should be re-submitted, as appropriate.
         */
        const val INVALID_CREDENTIALS = "invalid_credentials"
    }
}

/**
 * Sign in error. The user should use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication.signIn]
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
open class SignInError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String?,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): SignInResult, BrowserRequiredError, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {
    fun isUserNotFound(): Boolean = this.errorType == ErrorTypes.USER_NOT_FOUND

    fun isInvalidCredentials(): Boolean = this.errorType == SignInErrorTypes.INVALID_CREDENTIALS
}

/**
 * Sign in submit password result. The user should use the utility methods of this class
 * to identify and handle the error. This error is produced by
 * [com.microsoft.identity.nativeauth.statemachine.states.SignInPasswordRequiredState.submitPassword]
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
class SignInSubmitPasswordError(
    override val errorType: String? = null,
    override val error: String? = null,
    override val errorMessage: String?,
    override val correlationId: String?,
    override  val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): SignInSubmitPasswordResult, Error(errorType = errorType, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception) {
    fun isInvalidCredentials(): Boolean = this.errorType == SignInErrorTypes.INVALID_CREDENTIALS
}

/**
 * Sign in continuation error. The error has no utility methods
 * and must be treated as unexpected. This error is produced by
 * [com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication.signIn]
 * @param errorType the error type value of the error that occurred
 * @param error the error returned by the authentication server.
 * @param errorMessage the error message returned by the authentication server.
 * @param correlationId a unique identifier for the request that can help in diagnostics.
 * @param errorCodes a list of specific error codes returned by the authentication server.
 * @param exception an internal unexpected exception that happened.
 */
open class SignInContinuationError(
    override val error: String? = null,
    override val errorMessage: String?,
    // TODO: The parameter type of correlationId should be changed to String? after PBI https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2774018 is completed
    override val correlationId: String,
    override val errorCodes: List<Int>? = null,
    override var exception: Exception? = null
): SignInResult, Error(errorType = null, error = error, errorMessage= errorMessage, correlationId = correlationId, errorCodes = errorCodes, exception = exception)
