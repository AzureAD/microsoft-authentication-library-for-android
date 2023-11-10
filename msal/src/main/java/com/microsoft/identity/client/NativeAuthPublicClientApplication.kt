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

package com.microsoft.identity.client

import android.content.Context
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.client.statemachine.BrowserRequiredError
import com.microsoft.identity.client.statemachine.GeneralError
import com.microsoft.identity.client.statemachine.InvalidAttributesError
import com.microsoft.identity.client.statemachine.InvalidEmailError
import com.microsoft.identity.client.statemachine.InvalidPasswordError
import com.microsoft.identity.client.statemachine.PasswordIncorrectError
import com.microsoft.identity.client.statemachine.UserAlreadyExistsError
import com.microsoft.identity.client.statemachine.UserNotFoundError
import com.microsoft.identity.client.statemachine.results.SignInResult
import com.microsoft.identity.client.statemachine.results.SignInUsingPasswordResult
import com.microsoft.identity.client.statemachine.results.SignUpResult
import com.microsoft.identity.client.statemachine.results.SignUpUsingPasswordResult
import com.microsoft.identity.client.statemachine.states.AccountResult
import com.microsoft.identity.client.statemachine.states.Callback
import com.microsoft.identity.client.statemachine.states.SignInAfterSignUpState
import com.microsoft.identity.client.statemachine.states.SignInCodeRequiredState
import com.microsoft.identity.client.statemachine.states.SignInPasswordRequiredState
import com.microsoft.identity.client.statemachine.states.SignUpAttributesRequiredState
import com.microsoft.identity.client.statemachine.states.SignUpCodeRequiredState
import com.microsoft.identity.client.statemachine.states.SignUpPasswordRequiredState
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager
import com.microsoft.identity.common.internal.commands.GetCurrentAccountCommand
import com.microsoft.identity.common.internal.commands.SignInStartCommand
import com.microsoft.identity.common.internal.commands.SignUpStartCommand
import com.microsoft.identity.common.internal.controllers.LocalMSALController
import com.microsoft.identity.common.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.common.internal.net.cache.HttpCache
import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.CommandCallback
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInStartCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpStartCommandResult
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.util.checkAndWrapCommandResultType
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

    interface GetCurrentAccountCallback : Callback<AccountResult?>

    /**
     * Retrieve the current signed in account from cache; callback variant.
     *
     * @param callback [com.microsoft.identity.client.NativeAuthPublicClientApplication.GetCurrentAccountCallback] to receive the result.
     * @return [com.microsoft.identity.client.statemachine.states.AccountResult] if there is a signed in account, null otherwise.
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
     * @return [com.microsoft.identity.client.statemachine.states.AccountResult] if there is a signed in account, null otherwise.
     */
    override suspend fun getCurrentAccount(): AccountResult? {
        return withContext(Dispatchers.IO) {
            val account = getCurrentAccountInternal(nativeAuthConfig)
            return@withContext if (account != null) {
                AccountResult.createFromAccountResult(
                    account = account,
                    config = nativeAuthConfig
                )
            } else {
                null
            }
        }
    }

    interface SignInCallback : Callback<SignInResult>

    /**
     * Sign in a user with a given username; callback variant.
     *
     * @param username username of the account to sign in.
     * @param scopes (Optional) scopes to request during the sign in.
     * @param callback [com.microsoft.identity.client.NativeAuthPublicClientApplication.SignInCallback] to receive the result.
     * @return [com.microsoft.identity.client.statemachine.results.SignInResult] see detailed possible return state under the object.
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
     * @return [com.microsoft.identity.client.statemachine.results.SignInResult] see detailed possible return state under the object.
     * @throws [MsalException] if an account is already signed in.
     */
    override suspend fun signIn(
        username: String,
        scopes: List<String>?
    ): SignInResult {
        return withContext(Dispatchers.IO) {
            LogSession.logMethodCall(TAG, "${TAG}.signIn")

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
                            flowToken = result.credentialToken,
                            scopes = scopes,
                            config = nativeAuthConfig
                        ),
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel
                    )
                }
                is INativeAuthCommandResult.UnknownError -> {
                    SignInResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            details = result.details,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    )
                }
                is SignInCommandResult.UserNotFound -> {
                    SignInResult.UserNotFound(
                        error = UserNotFoundError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes
                        )
                    )
                }
                is SignInCommandResult.PasswordRequired -> {
                    SignInResult.PasswordRequired(
                        nextState = SignInPasswordRequiredState(
                            flowToken = result.credentialToken,
                            scopes = scopes,
                            config = nativeAuthConfig
                        )
                    )
                }
                is SignInCommandResult.InvalidCredentials -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result $result"
                    )
                    SignInResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = "Unexpected state",
                            error = "unexpected_state",
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes
                        )
                    )
                }
                is SignInCommandResult.Complete -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result $result"
                    )
                    SignInResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = "Unexpected state",
                            error = "unexpected_state",
                            correlationId = "UNSET"
                        )
                    )
                }
                is INativeAuthCommandResult.Redirect -> {
                    SignInResult.BrowserRequired(
                        error = BrowserRequiredError(
                            correlationId = result.correlationId
                        )
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
     * @param callback [com.microsoft.identity.client.NativeAuthPublicClientApplication.SignInUsingPasswordCallback] to receive the result.
     * @return [com.microsoft.identity.client.statemachine.results.SignInUsingPasswordResult] see detailed possible return state under the object.
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
     * @return [com.microsoft.identity.client.statemachine.results.SignInUsingPasswordResult] see detailed possible return state under the object.
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
                            resultValue = AccountResult.createFromAuthenticationResult(
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
                                flowToken = result.credentialToken,
                                scopes = scopes,
                                config = nativeAuthConfig
                            ),
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel
                        )
                    }
                    is SignInCommandResult.UserNotFound -> {
                        SignInResult.UserNotFound(
                            error = UserNotFoundError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId,
                                errorCodes = result.errorCodes
                            )
                        )
                    }

                    is SignInCommandResult.InvalidCredentials -> {
                        SignInResult.InvalidCredentials(
                            error = PasswordIncorrectError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId,
                                errorCodes = result.errorCodes
                            )
                        )
                    }

                    is INativeAuthCommandResult.Redirect -> {
                        SignInResult.BrowserRequired(
                            error = BrowserRequiredError(
                                correlationId = result.correlationId
                            )
                        )
                    }

                    is INativeAuthCommandResult.UnknownError -> {
                        SignInResult.UnexpectedError(
                            error = GeneralError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId,
                                details = result.details,
                                errorCodes = result.errorCodes,
                                exception = result.exception
                            )
                        )
                    }
                    is SignInCommandResult.PasswordRequired -> {
                        Logger.warn(
                            TAG,
                            "Unexpected result $result"
                        )
                        SignInResult.UnexpectedError(
                            error = GeneralError(
                                errorMessage = "Unexpected state",
                                error = "unexpected_state",
                                correlationId = "UNSET"
                            )
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
     * @param callback [com.microsoft.identity.client.NativeAuthPublicClientApplication.SignUpUsingPasswordCallback] to receive the result.
     * @return [com.microsoft.identity.client.statemachine.results.SignUpUsingPasswordResult] see detailed possible return state under the object.
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
     * @return [com.microsoft.identity.client.statemachine.results.SignUpUsingPasswordResult] see detailed possible return state under the object.
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
                    is SignUpCommandResult.AuthNotSupported -> {
                        SignUpUsingPasswordResult.AuthNotSupported(
                            error = GeneralError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId
                            )
                        )
                    }

                    is SignUpCommandResult.InvalidPassword -> {
                        SignUpResult.InvalidPassword(
                            InvalidPasswordError(
                                error = result.error,
                                errorMessage = result.errorDescription,
                                correlationId = result.correlationId
                            )
                        )
                    }

                    is SignUpCommandResult.AttributesRequired -> {
                        SignUpResult.AttributesRequired(
                            nextState = SignUpAttributesRequiredState(
                                flowToken = result.signupToken,
                                username = username,
                                config = nativeAuthConfig
                            ),
                            requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                        )
                    }

                    is SignUpCommandResult.CodeRequired -> {
                        SignUpResult.CodeRequired(
                            nextState = SignUpCodeRequiredState(
                                flowToken = result.signupToken,
                                username = username,
                                config = nativeAuthConfig
                            ),
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel,
                        )
                    }

                    is SignUpCommandResult.UsernameAlreadyExists -> {
                        SignUpResult.UserAlreadyExists(
                            error = UserAlreadyExistsError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId
                            )
                        )
                    }

                    is SignUpCommandResult.InvalidEmail -> {
                        SignUpResult.InvalidEmail(
                            error = InvalidEmailError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId
                            )
                        )
                    }

                    is SignUpCommandResult.InvalidAttributes -> {
                        SignUpResult.InvalidAttributes(
                            error = InvalidAttributesError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId
                            ),
                            invalidAttributes = result.invalidAttributes
                        )
                    }

                    is SignUpCommandResult.Complete -> {
                        SignUpResult.Complete(
                            nextState = SignInAfterSignUpState(
                                signInVerificationCode = result.signInSLT,
                                username = username,
                                config = nativeAuthConfig
                            )
                        )
                    }

                    is INativeAuthCommandResult.Redirect -> {
                        SignUpResult.BrowserRequired(
                            error = BrowserRequiredError(
                                correlationId = result.correlationId
                            )
                        )
                    }

                    is INativeAuthCommandResult.UnknownError -> {
                        SignUpResult.UnexpectedError(
                            error = GeneralError(
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId,
                                details = result.details,
                                exception = result.exception
                            )
                        )
                    }

                    is SignUpCommandResult.PasswordRequired -> {
                        Logger.warn(
                            TAG,
                            "Unexpected result $result"
                        )
                        SignUpResult.UnexpectedError(
                            error = GeneralError(
                                errorMessage = "Unexpected state",
                                error = "unexpected_state",
                                correlationId = "UNSET"
                            )
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
     * @param callback [com.microsoft.identity.client.NativeAuthPublicClientApplication.SignUpCallback] to receive the result.
     * @return [com.microsoft.identity.client.statemachine.results.SignUpResult] see detailed possible return state under the object.
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
     * @return [com.microsoft.identity.client.statemachine.results.SignUpResult] see detailed possible return state under the object.
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
                is SignUpCommandResult.AttributesRequired -> {
                    SignUpResult.AttributesRequired(
                        nextState = SignUpAttributesRequiredState(
                            flowToken = result.signupToken,
                            username = username,
                            config = nativeAuthConfig
                        ),
                        requiredAttributes = result.requiredAttributes.toListOfRequiredUserAttribute()
                    )
                }

                is SignUpCommandResult.CodeRequired -> {
                    SignUpResult.CodeRequired(
                        nextState = SignUpCodeRequiredState(
                            flowToken = result.signupToken,
                            username = username,
                            config = nativeAuthConfig
                        ),
                        codeLength = result.codeLength,
                        sentTo = result.challengeTargetLabel,
                        channel = result.challengeChannel,
                    )
                }

                is SignUpCommandResult.UsernameAlreadyExists -> {
                    SignUpResult.UserAlreadyExists(
                        error = UserAlreadyExistsError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId
                        )
                    )
                }

                is SignUpCommandResult.InvalidEmail -> {
                    SignUpResult.InvalidEmail(
                        error = InvalidEmailError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId
                        )
                    )
                }

                is SignUpCommandResult.InvalidAttributes -> {
                    SignUpResult.InvalidAttributes(
                        error = InvalidAttributesError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId
                        ),
                        invalidAttributes = result.invalidAttributes
                    )
                }

                is SignUpCommandResult.PasswordRequired -> {
                    SignUpResult.PasswordRequired(
                        nextState = SignUpPasswordRequiredState(
                            flowToken = result.signupToken,
                            username = username,
                            config = nativeAuthConfig
                        )
                    )
                }

                is SignUpCommandResult.Complete -> {
                    SignUpResult.Complete(
                        nextState = SignInAfterSignUpState(
                            signInVerificationCode = result.signInSLT,
                            username = username,
                            config = nativeAuthConfig
                        )
                    )
                }

                is INativeAuthCommandResult.Redirect -> {
                    SignUpResult.BrowserRequired(
                        error = BrowserRequiredError(
                            correlationId = result.correlationId
                        )
                    )
                }

                is INativeAuthCommandResult.UnknownError -> {
                    SignUpResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            details = result.details,
                            exception = result.exception
                        )
                    )
                }

                is SignUpCommandResult.InvalidPassword -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result $result"
                    )
                    SignUpResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = "Unexpected state",
                            error = "unexpected_state",
                            correlationId = result.correlationId
                        )
                    )
                }

                is SignUpCommandResult.AuthNotSupported -> {
                    Logger.warn(
                        TAG,
                        "Unexpected result $result"
                    )
                    SignUpResult.UnexpectedError(
                        error = GeneralError(
                            errorMessage = "Unexpected state",
                            error = "unexpected_state",
                            correlationId = result.correlationId
                        )
                    )
                }
            }
        }
    }



    private fun verifyUserIsNotSignedIn() {
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
            override fun onResult(result: AccountResult?) {
                future.setResult(result != null)
            }

            override fun onError(exception: BaseException) {
                Logger.error(TAG, "Exception thrown in checkForPersistedAccount", exception)
                future.setException(exception)
            }
        })

        return future
    }
}
