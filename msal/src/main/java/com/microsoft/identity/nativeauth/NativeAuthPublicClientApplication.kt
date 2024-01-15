// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.identity.nativeauth

import android.content.Context
import com.microsoft.identity.client.AccountAdapter
import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInUsingPasswordResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpUsingPasswordResult
import com.microsoft.identity.nativeauth.statemachine.states.Callback
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignInAfterSignUpState
import com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignInPasswordRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpPasswordRequiredState
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager
import com.microsoft.identity.common.internal.commands.GetCurrentAccountCommand
import com.microsoft.identity.common.internal.controllers.LocalMSALController
import com.microsoft.identity.common.nativeauth.internal.commands.ResetPasswordStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignUpStartCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.common.internal.net.cache.HttpCache
import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.CommandCallback
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpStartCommandResult
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordError
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.errors.SignInErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.SignInUsingPasswordError
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpError
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpUsingPasswordError
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult
import com.microsoft.identity.nativeauth.statemachine.states.AccountState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * NativeAuthPublicClientApplication provides implementation for the top level interface
 * [INativeAuthPublicClientApplication] used by third party developers.
 */
class NativeAuthPublicClientApplication(
    private val nativeAuthConfig: NativeAuthPublicClientApplicationConfiguration
) : INativeAuthPublicClientApplication, PublicClientApplication(nativeAuthConfig) {

    private lateinit var sharedPreferencesFileManager: SharedPreferencesFileManager

    init {
        initializeApplication()
        initializeSharedPreferenceFileManager(nativeAuthConfig.appContext)
    }

    companion object {
        /**
         * Name of the shared preference cache for storing NativeAuthPublicClientApplication data.
         */
        private const val NATIVE_AUTH_CREDENTIAL_SHARED_PREFERENCES =
            "com.microsoft.identity.client.native_auth_credential_cache"

        internal val TAG = NativeAuthPublicClientApplication::class.java.toString()

        //  The Native Auth client code works on the basis of callbacks and coroutines.
        //  To avoid duplicating the code, callback methods are routed through their
        //  coroutine-equivalent through this CoroutineScope.
        val pcaScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getCurrentAccountInternal(config: NativeAuthPublicClientApplicationConfiguration): IAccount? {
            LogSession.logMethodCall(TAG, "${TAG}.getCurrentAccountInternal")

            val params = CommandParametersAdapter.createCommandParameters(
                config,
                config.oAuth2TokenCache
            )

            val command = GetCurrentAccountCommand(
                params,
                LocalMSALController(),
                object : CommandCallback<List<ICacheRecord?>?, BaseException?> {
                    override fun onTaskCompleted(result: List<ICacheRecord?>?) {
                        // Do nothing, handled by CommandDispatcher.submitSilentReturningFuture()
                    }

                    override fun onError(error: BaseException?) {
                        // Do nothing, handled by CommandDispatcher.submitSilentReturningFuture()
                    }

                    override fun onCancel() {
                        // Not required
                    }
                },
                PublicApiId.NATIVE_AUTH_GET_ACCOUNT
            )
            val result = CommandDispatcher.submitSilentReturningFuture(command)
                .get().result as List<ICacheRecord?>?

            // To simplify the logic, if more than one account is returned, the first account will be picked.
            // We do not support switching from MULTIPLE to SINGLE.
            return getAccountFromICacheRecordsList(result)
        }

        /**
         * Get an IAccount from a list of ICacheRecord.
         *
         * @param cacheRecords list of cache record that belongs to an account.
         * If the list can be converted to multiple accounts, only the first one will be returned.
         */
        private fun getAccountFromICacheRecordsList(cacheRecords: List<ICacheRecord?>?): IAccount? {
            LogSession.logMethodCall(TAG, "${TAG}.getAccountFromICacheRecordsList")
            if (cacheRecords.isNullOrEmpty()) {
                return null
            }
            val accountList = AccountAdapter.adapt(cacheRecords)
            if (accountList.isNullOrEmpty()) {
                Logger.error(
                    TAG,
                    "Returned cacheRecords were adapted into empty or null IAccount list. " +
                        "This is unexpected in native auth mode." +
                        "Returning null.",
                    null
                )
                return null
            }
            if (accountList.size != 1) {
                Logger.warn(
                    TAG,
                    "Returned cacheRecords were adapted into multiple IAccount. " +
                        "This is unexpected in native auth mode." +
                        "Returning the first adapted account."
                )
            }
            return accountList[0]
        }
    }

    @Throws(MsalClientException::class)
    private fun initializeApplication() {
        val context = nativeAuthConfig.appContext

        AzureActiveDirectory.setEnvironment(nativeAuthConfig.environment)
        Authority.addKnownAuthorities(nativeAuthConfig.authorities)
        initializeLoggerSettings(nativeAuthConfig.loggerConfiguration)

        // Since network request is sent from the sdk, if calling app doesn't declare the internet
        // permission in the manifest, we cannot make the network call.
        checkInternetPermission(nativeAuthConfig)

        // Init HTTP cache
        HttpCache.initialize(context.cacheDir)
        LogSession.logMethodCall(TAG, "${TAG}.initializeApplication")
    }

    private fun initializeSharedPreferenceFileManager(context: Context) {
        sharedPreferencesFileManager = SharedPreferencesFileManager(
            context,
            NATIVE_AUTH_CREDENTIAL_SHARED_PREFERENCES,
            AndroidAuthSdkStorageEncryptionManager(context)
        )
    }

    interface GetCurrentAccountCallback : Callback<GetAccountResult>

    /**
     * Retrieve the current signed in account from cache; callback variant.
     *
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.GetCurrentAccountCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.states.AccountState] if there is a signed in account, null otherwise.
     */
    override fun getCurrentAccount(callback: GetCurrentAccountCallback) {
        pcaScope.launch {
            try {
                val result = getCurrentAccount()
                callback.onResult(result)
            } catch (e: MsalException) {
                callback.onError(e)
            }
        }
    }

    /**
     * Retrieve the current signed in account from cache; Kotlin coroutines variant.
     *
     * @return [com.microsoft.identity.nativeauth.statemachine.states.AccountState] if there is a signed in account, null otherwise.
     */
    override suspend fun getCurrentAccount(): GetAccountResult {
        return withContext(Dispatchers.IO) {
            val account = getCurrentAccountInternal(nativeAuthConfig)
            return@withContext if (account != null) {
                GetAccountResult.AccountFound(
                    resultValue = AccountState.createFromAccountResult(
                        account = account,
                        config = nativeAuthConfig
                    )
                )
            } else {
                GetAccountResult.NoAccountFound
            }
        }
    }

    interface SignInCallback : Callback<SignInResult>

    /**
     * Sign in a user with a given username; callback variant.
     *
     * @param username username of the account to sign in.
     * @param scopes (Optional) scopes to request during the sign in.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignInCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignInResult] see detailed possible return state under the object.
     * @throws [MsalException] if an account is already signed in.
     */
    override fun signIn(username: String, scopes: List<String>?, callback: SignInCallback) {
        LogSession.logMethodCall(TAG, "${TAG}.signIn(username: String, scopes: List<String>?, callback: SignInCallback)")
        pcaScope.launch {
            try {
                val result = signIn(username, scopes)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signIn", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Sign in a user with a given username; Kotlin coroutines variant.
     *
     * @param username username of the account to sign in.
     * @param scopes (Optional) scopes to request during the sign in.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignInResult] see detailed possible return state under the object.
     * @throws [MsalException] if an account is already signed in.
     */
    override suspend fun signIn(
        username: String,
        scopes: List<String>?
    ): SignInResult {
        return withContext(Dispatchers.IO) {
            LogSession.logMethodCall(TAG, "${TAG}.signIn")

            verifyNoUserIsSignedIn()

            val params = CommandParametersAdapter.createSignInStartCommandParameters(
                nativeAuthConfig,
                nativeAuthConfig.oAuth2TokenCache,
                username
            )

            val command = SignInStartCommand(
                params,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_IN_WITH_EMAIL
            )

            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignInStartCommandResult>()) {
                is SignInCommandResult.CodeRequired -> {
                    SignInResult.CodeRequired(
                        nextState = SignInCodeRequiredState(
                            continuationToken = result.continuationToken,
                            scopes = scopes,
                            config = nativeAuthConfig
                        ),
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel
                    )
                }
                is SignInCommandResult.PasswordRequired -> {
                    SignInResult.PasswordRequired(
                        nextState = SignInPasswordRequiredState(
                            continuationToken = result.continuationToken,
                            scopes = scopes,
                            config = nativeAuthConfig
                        )
                    )
                }
                is SignInCommandResult.Complete -> {
                    Logger.warn(
                        TAG,
                        "Sign in received unexpected result $result"
                    )
                    SignInError(
                        errorMessage = "unexpected state",
                        error = "unexpected_state",
                        correlationId = "UNSET"
                    )
                }
                is SignInCommandResult.UserNotFound -> {
                    SignInError(
                        errorType = ErrorTypes.USER_NOT_FOUND,
                        errorMessage = result.errorDescription,
                        error = result.error,
                        correlationId = result.correlationId,
                        errorCodes = result.errorCodes
                    )
                }
                is SignInCommandResult.InvalidCredentials -> {
                    Logger.warn(
                        TAG,
                        "Sign in received Unexpected result $result"
                    )
                    SignInError(
                        errorMessage = "unexpected state",
                        error = "unexpected_state",
                        correlationId = result.correlationId,
                        errorCodes = result.errorCodes
                    )
                }
                is INativeAuthCommandResult.Redirect -> {
                    SignInError(
                        errorType = ErrorTypes.BROWSER_REQUIRED,
                        errorMessage = result.errorDescription,
                        error = result.error,
                        correlationId = result.correlationId
                    )
                }
                is INativeAuthCommandResult.UnknownError -> {
                    SignInError(
                        errorMessage = result.errorDescription,
                        error = result.error,
                        correlationId = result.correlationId,
                        errorCodes = result.errorCodes,
                        exception = result.exception
                    )
                }
            }
        }
    }

    interface SignInUsingPasswordCallback : Callback<SignInUsingPasswordResult>

    /**
     * Sign in the account using username and password; callback variant.
     *
     * @param username username of the account to sign in.
     * @param password password of the account to sign in.
     * @param scopes (Optional) list of scopes to request.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignInUsingPasswordCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignInUsingPasswordResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override fun signInUsingPassword(
        username: String,
        password: CharArray,
        scopes: List<String>?,
        callback: SignInUsingPasswordCallback
    ) {
        LogSession.logMethodCall(TAG, "${TAG}.signIn(username: String, password: String, scopes: List<String>?, callback: SignInUsingPasswordCallback)")
        pcaScope.launch {
            try {
                val result = signInUsingPassword(username, password, scopes)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signInUsingPassword", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Sign in the account using username and password; Kotlin coroutines variant.
     *
     * @param username username of the account to sign in.
     * @param password password of the account to sign in.
     * @param scopes (Optional) list of scopes to request.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignInUsingPasswordResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override suspend fun signInUsingPassword(
        username: String,
        password: CharArray,
        scopes: List<String>?
    ): SignInUsingPasswordResult {
        LogSession.logMethodCall(TAG, "${TAG}.signInUsingPassword")
        return withContext(Dispatchers.IO) {
            LogSession.logMethodCall(TAG, "${TAG}.signInUsingPassword.withContext")

            verifyNoUserIsSignedIn()

            val params =
                CommandParametersAdapter.createSignInStartUsingPasswordCommandParameters(
                    nativeAuthConfig,
                    nativeAuthConfig.oAuth2TokenCache,
                    username,
                    password,
                    scopes
                )

            try {
                val command = SignInStartCommand(
                    params,
                    NativeAuthMsalController(),
                    PublicApiId.NATIVE_AUTH_SIGN_IN_WITH_EMAIL_PASSWORD
                )

                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

                return@withContext when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<SignInStartCommandResult>()) {
                    is SignInCommandResult.Complete -> {
                        val authenticationResult =
                            AuthenticationResultAdapter.adapt(result.authenticationResult)

                        SignInResult.Complete(
                            resultValue = AccountState.createFromAuthenticationResult(
                                authenticationResult,
                                nativeAuthConfig
                            )
                        )
                    }
                    is SignInCommandResult.CodeRequired -> {
                        Logger.warn(
                            TAG,
                            "Sign in with password flow was started, but server requires" +
                                    "a code. Password was not sent to the API; switching to code " +
                                    "authentication."
                        )
                        SignInResult.CodeRequired(
                            nextState = SignInCodeRequiredState(
                                continuationToken = result.continuationToken,
                                scopes = scopes,
                                config = nativeAuthConfig
                            ),
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel
                        )
                    }
                    is SignInCommandResult.PasswordRequired -> {
                        Logger.warn(
                            TAG,
                            "Sign in using password received unexpected result $result"
                        )
                        SignInUsingPasswordError(
                            errorMessage = "unexpected state",
                            error = "unexpected_state",
                            correlationId = "UNSET"
                        )
                    }
                    is SignInCommandResult.UserNotFound -> {
                        SignInUsingPasswordError(
                            errorType = ErrorTypes.USER_NOT_FOUND,
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes
                        )
                    }
                    is SignInCommandResult.InvalidCredentials -> {
                        SignInUsingPasswordError(
                            errorType = SignInErrorTypes.INVALID_CREDENTIALS,
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        SignInUsingPasswordError(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId
                        )
                    }
                    is INativeAuthCommandResult.UnknownError -> {
                        SignInUsingPasswordError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    }
                }
            } finally {
                StringUtil.overwriteWithNull(params.password)
            }
        }
    }

    interface SignUpUsingPasswordCallback : Callback<SignUpUsingPasswordResult>

    /**
     * Sign up the account using username and password; callback variant.
     *
     * @param username username of the account to sign up.
     * @param password password of the account to sign up.
     * @param attributes (Optional) user attributes to be used during account creation
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignUpUsingPasswordCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignUpUsingPasswordResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override fun signUpUsingPassword(
        username: String,
        password: CharArray,
        attributes: UserAttributes?,
        callback: SignUpUsingPasswordCallback
    ) {
        LogSession.logMethodCall(TAG, "${TAG}.signUpUsingPassword")
        pcaScope.launch {
            try {
                val result = signUpUsingPassword(username, password, attributes)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signUpUsingPassword", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Sign up the account using username and password. Kotlin coroutines variant.
     *
     * @param username username of the account to sign up.
     * @param password password of the account to sign up.
     * @param attributes (Optional) user attributes to be used during account creation
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignUpUsingPasswordResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override suspend fun signUpUsingPassword(
        username: String,
        password: CharArray,
        attributes: UserAttributes?
    ): SignUpUsingPasswordResult {
        LogSession.logMethodCall(TAG, "${TAG}.signUpUsingPassword(username: String, password: String, attributes: UserAttributes?)")

        return withContext(Dispatchers.IO) {
            val doesAccountExist = checkForPersistedAccount().get()
            if (doesAccountExist) {
                throw MsalClientException(
                    MsalClientException.INVALID_PARAMETER,
                    "An account is already signed in."
                )
            }

            val parameters =
                CommandParametersAdapter.createSignUpStartUsingPasswordCommandParameters(
                    nativeAuthConfig,
                    nativeAuthConfig.oAuth2TokenCache,
                    username,
                    password,
                    attributes?.toMap()
                )

            val command = SignUpStartCommand(
                parameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_UP_START_WITH_PASSWORD
            )

            try {
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

                return@withContext when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<SignUpStartCommandResult>()) {
                    is SignUpCommandResult.Complete -> {
                        SignUpResult.Complete(
                            nextState = SignInAfterSignUpState(
                                continuationToken = result.continuationToken,
                                username = username,
                                config = nativeAuthConfig
                            )
                        )
                    }

                    is SignUpCommandResult.AttributesRequired -> {
                        SignUpResult.AttributesRequired(
                            nextState = SignUpAttributesRequiredState(
                                continuationToken = result.continuationToken,
                                username = username,
                                config = nativeAuthConfig
                            ),
                            requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                        )
                    }

                    is SignUpCommandResult.CodeRequired -> {
                        SignUpResult.CodeRequired(
                            nextState = SignUpCodeRequiredState(
                                continuationToken = result.continuationToken,
                                username = username,
                                config = nativeAuthConfig
                            ),
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel,
                        )
                    }

                    is SignUpCommandResult.AuthNotSupported -> {
                        SignUpUsingPasswordError(
                            errorType = SignUpErrorTypes.AUTH_NOT_SUPPORTED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }

                    is SignUpCommandResult.InvalidPassword -> {
                        SignUpUsingPasswordError(
                            errorType = ErrorTypes.INVALID_PASSWORD,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }

                    is SignUpCommandResult.UsernameAlreadyExists -> {
                        SignUpUsingPasswordError(
                            errorType = SignUpErrorTypes.USER_ALREADY_EXISTS,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }

                    is SignUpCommandResult.InvalidEmail -> {
                        SignUpUsingPasswordError(
                            errorType = SignUpErrorTypes.INVALID_USERNAME,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }

                    is SignUpCommandResult.InvalidAttributes -> {
                        SignUpUsingPasswordError(
                            errorType = SignUpErrorTypes.INVALID_ATTRIBUTES,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }

                    is INativeAuthCommandResult.Redirect -> {
                        SignUpUsingPasswordError(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId
                        )
                    }

                    is INativeAuthCommandResult.UnknownError -> {
                        SignUpUsingPasswordError(
                            errorMessage = "Unexpected state",
                            error = "unexpected_state",
                            correlationId = "UNSET"
                        )
                    }

                    is SignUpCommandResult.PasswordRequired -> {
                        Logger.warn(
                            TAG,
                            "Sign up using password received unexpected result $result"
                        )
                        SignUpUsingPasswordError(
                            errorMessage = "Unexpected state",
                            error = "unexpected_state",
                            correlationId = "UNSET"
                        )
                    }
                }
            } finally {
                StringUtil.overwriteWithNull(parameters.password)
            }
        }
    }

    interface SignUpCallback : Callback<SignUpResult>

    /**
     * Sign up the account starting from a username; callback variant.
     *
     * @param username username of the account to sign up.
     * @param attributes (Optional) user attributes to be used during account creation.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignUpCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignUpResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override fun signUp(
        username: String,
        attributes: UserAttributes?,
        callback: SignUpCallback
    ) {
        LogSession.logMethodCall(TAG, "${TAG}.signUp")

        pcaScope.launch {
            try {
                val result = signUp(username, attributes)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signUp", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Sign up the account starting from a username; Kotlin coroutines variant.
     *
     * @param username username of the account to sign up.
     * @param attributes (Optional) user attributes to be used during account creation.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignUpResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override suspend fun signUp(
        username: String,
        attributes: UserAttributes?
    ): SignUpResult {
        LogSession.logMethodCall(TAG, "${TAG}.signUp(username: String, attributes: UserAttributes?)")

        return withContext(Dispatchers.IO) {
            val doesAccountExist = checkForPersistedAccount().get()
            if (doesAccountExist) {
                throw MsalClientException(
                    MsalClientException.INVALID_PARAMETER,
                    "An account is already signed in."
                )
            }

            val parameters =
                CommandParametersAdapter.createSignUpStartCommandParameters(
                    nativeAuthConfig,
                    nativeAuthConfig.oAuth2TokenCache,
                    username,
                    attributes?.userAttributes
                )

            val command = SignUpStartCommand(
                parameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_SIGN_UP_START
            )
            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<SignUpStartCommandResult>()) {
                is SignUpCommandResult.Complete -> {
                    SignUpResult.Complete(
                        nextState = SignInAfterSignUpState(
                            continuationToken = result.continuationToken,
                            username = username,
                            config = nativeAuthConfig
                        )
                    )
                }

                is SignUpCommandResult.AttributesRequired -> {
                    SignUpResult.AttributesRequired(
                        nextState = SignUpAttributesRequiredState(
                            continuationToken = result.continuationToken,
                            username = username,
                            config = nativeAuthConfig
                        ),
                        requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                    )
                }

                is SignUpCommandResult.CodeRequired -> {
                    SignUpResult.CodeRequired(
                        nextState = SignUpCodeRequiredState(
                            continuationToken = result.continuationToken,
                            username = username,
                            config = nativeAuthConfig
                        ),
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel,
                    )
                }

                is SignUpCommandResult.PasswordRequired -> {
                    SignUpResult.PasswordRequired(
                        nextState = SignUpPasswordRequiredState(
                            continuationToken = result.continuationToken,
                            username = username,
                            config = nativeAuthConfig
                        )
                    )
                }

                is SignUpCommandResult.UsernameAlreadyExists -> {
                    SignUpError(
                        errorType = SignUpErrorTypes.USER_ALREADY_EXISTS,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }

                is SignUpCommandResult.InvalidEmail -> {
                    SignUpError(
                        errorType = SignUpErrorTypes.INVALID_USERNAME,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }

                is SignUpCommandResult.InvalidAttributes -> {
                    SignUpError(
                        errorType = SignUpErrorTypes.INVALID_ATTRIBUTES,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }

                is INativeAuthCommandResult.Redirect -> {
                    SignUpError(
                        errorType = ErrorTypes.BROWSER_REQUIRED,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }

                is INativeAuthCommandResult.UnknownError -> {
                    SignUpError(
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId,
                        exception = result.exception
                    )
                }

                is SignUpCommandResult.InvalidPassword -> {
                    Logger.warn(
                        TAG,
                        "Sign up received unexpected result $result"
                    )
                    SignUpError(
                        error = "unexpected_state",
                        errorMessage = "Unexpected state",
                        correlationId = result.correlationId,
                    )
                }

                is SignUpCommandResult.AuthNotSupported -> {
                    Logger.warn(
                        TAG,
                        "Sign up received unexpected result $result"
                    )
                    SignUpError(
                        error = "unexpected_state",
                        errorMessage = "Unexpected state",
                        correlationId = result.correlationId,
                    )
                }
            }
        }
    }

    interface ResetPasswordCallback : Callback<ResetPasswordStartResult>

    /**
     * Reset password for the account starting from a username; callback variant.
     *
     * @param username username of the account to reset password.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.ResetPasswordCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override fun resetPassword(username: String, callback: ResetPasswordCallback) {
        LogSession.logMethodCall(TAG, "${TAG}.resetPassword")
        pcaScope.launch {
            try {
                val result = resetPassword(username = username)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in resetPassword", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Reset password for the account starting from a username; Kotlin coroutines variant.
     *
     * @param username username of the account to reset password.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override suspend fun resetPassword(username: String): ResetPasswordStartResult {
        LogSession.logMethodCall(TAG, "${TAG}.resetPassword(username: String)")

        return withContext(Dispatchers.IO) {
            val doesAccountExist = checkForPersistedAccount().get()
            if (doesAccountExist) {
                throw MsalClientException(
                    MsalClientException.INVALID_PARAMETER,
                    "An account is already signed in."
                )
            }

            val parameters = CommandParametersAdapter.createResetPasswordStartCommandParameters(
                nativeAuthConfig,
                nativeAuthConfig.oAuth2TokenCache,
                username
            )

            val command = ResetPasswordStartCommand(
                parameters,
                NativeAuthMsalController(),
                PublicApiId.NATIVE_AUTH_RESET_PASSWORD_START
            )

            val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

            return@withContext when (val result = rawCommandResult.checkAndWrapCommandResultType<ResetPasswordStartCommandResult>()) {
                is ResetPasswordCommandResult.CodeRequired -> {
                    ResetPasswordStartResult.CodeRequired(
                        nextState = ResetPasswordCodeRequiredState(
                            continuationToken = result.continuationToken,
                            username = username,
                            config = nativeAuthConfig
                        ),
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel
                    )
                }

                is ResetPasswordCommandResult.UserNotFound -> {
                    ResetPasswordError(
                        errorType = ErrorTypes.USER_NOT_FOUND,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }

                is INativeAuthCommandResult.UnknownError -> {
                    ResetPasswordError(
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId,
                        exception = result.exception
                    )
                }

                is INativeAuthCommandResult.Redirect -> {
                    ResetPasswordError(
                        errorType = ErrorTypes.BROWSER_REQUIRED,
                        error = result.error,
                        errorMessage = result.errorDescription,
                        correlationId = result.correlationId
                    )
                }

                is ResetPasswordCommandResult.PasswordNotSet -> {
                    Logger.warn(
                        TAG,
                        "Reset password received unexpected result $result",
                    )
                    ResetPasswordError(
                        error = "unexpected_state",
                        errorMessage = "Unexpected state",
                        correlationId = result.correlationId,
                    )
                }

                is ResetPasswordCommandResult.EmailNotVerified -> {
                    Logger.warn(
                        TAG,
                        "Reset password received unexpected result $result"
                    )
                    ResetPasswordError(
                        error = "unexpected_state",
                        errorMessage = "Unexpected state",
                        correlationId = result.correlationId,
                    )
                }
            }
        }
    }
    
    private fun verifyNoUserIsSignedIn() {
        val doesAccountExist = checkForPersistedAccount().get()
        if (doesAccountExist) {
            Logger.error(
                TAG,
                "An account is already signed in.",
                null
            )
            throw MsalClientException(
                MsalClientException.INVALID_PARAMETER,
                "An account is already signed in."
            )
        }
    }

    private fun checkForPersistedAccount(): ResultFuture<Boolean> {
        LogSession.logMethodCall(TAG, "${TAG}.checkForPersistedAccount")
        val future = ResultFuture<Boolean>()
        getCurrentAccount(object : GetCurrentAccountCallback {
            override fun onResult(result: GetAccountResult) {
                future.setResult(result is GetAccountResult.AccountFound)
            }

            override fun onError(exception: BaseException) {
                Logger.error(TAG, "Exception thrown in checkForPersistedAccount", exception)
                future.setException(exception)
            }
        })

        return future
    }
}
