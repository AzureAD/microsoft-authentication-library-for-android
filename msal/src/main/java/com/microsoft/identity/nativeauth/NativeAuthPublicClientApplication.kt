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
import com.microsoft.identity.client.claims.ClaimsRequest
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager
import com.microsoft.identity.common.internal.commands.GetCurrentAccountCommand
import com.microsoft.identity.common.internal.controllers.LocalMSALController
import com.microsoft.identity.common.internal.net.cache.HttpCache
import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.CommandCallback
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpStartCommandResult
import com.microsoft.identity.common.java.nativeauth.util.checkAndWrapCommandResultType
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2ResetPasswordStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.NativeAuthV2SignInStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.ResetPasswordStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignInStartCommand
import com.microsoft.identity.common.nativeauth.internal.commands.SignUpStartCommand
import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.common.nativeauth.internal.controllers.v2.NativeAuthV2FlowController
import com.microsoft.identity.nativeauth.parameters.NativeAuthResetPasswordParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.GetAccountError
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordError
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.errors.SignInErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.SignInErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpError
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpErrorTypes
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.states.AccountState
import com.microsoft.identity.nativeauth.statemachine.states.AwaitingMFAState
import com.microsoft.identity.nativeauth.statemachine.states.Callback
import com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthState
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignInContinuationState
import com.microsoft.identity.nativeauth.statemachine.states.SignInPasswordRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.SignUpPasswordRequiredState
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.nativeauth.utils.getCancellable
import com.microsoft.identity.nativeauth.utils.launchOwningPasswordSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
            LogSession.logMethodCall(
                tag = TAG,
                correlationId = null,
                methodName = "${TAG}.getCurrentAccountInternal(config: NativeAuthPublicClientApplicationConfiguration)"
            )

            val params = CommandParametersAdapter.createCommandParameters(
                config,
                config.oAuth2TokenCache
            )

            val command = GetCurrentAccountCommand(
                params,
                LocalMSALController().asControllerFactory(),
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
            LogSession.logMethodCall(
                tag = TAG,
                correlationId = null,
                methodName = "${TAG}.getAccountFromICacheRecordsList(cacheRecords: List<ICacheRecord?>?)"
            )
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
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.initializeApplication"
        )
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
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getCurrentAccount(callback: GetCurrentAccountCallback)"
        )
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
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.getCurrentAccount"
        )
        return withContext(Dispatchers.IO) {
            try {
                val account = getCurrentAccountInternal(nativeAuthConfig)
                return@withContext if (account != null) {
                    GetAccountResult.AccountFound(
                        resultValue = AccountState.createFromAccountResult(
                            account = account,
                            correlationId = DiagnosticContext.INSTANCE.threadCorrelationId,
                            config = nativeAuthConfig
                        )
                    )
                } else {
                    GetAccountResult.NoAccountFound
                }
            } catch (e: Exception) {
                GetAccountError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in getCurrentAccount.",
                    exception = e,
                    correlationId = DiagnosticContext.INSTANCE.threadCorrelationId
                )
            }
        }
    }

    interface SignInCallback : Callback<SignInResult>

    /**
     * Sign in the account using username and password; callback variant.
     *
     * @param username username of the account to sign in.
     * @param password (Optional) password of the account to sign in.
     * @param scopes (Optional) list of scopes to request.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignInCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignInResult] see detailed possible return state under the object.
     */
    @Deprecated("This method is now deprecated. Use the method 'signIn(parameters:, callback:)' instead.")
    override fun signIn(
        username: String,
        password: CharArray?,
        scopes: List<String>?,
        callback: SignInCallback
    ) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signIn(username: String, password: CharArray?, scopes: List<String>?, callback: SignInCallback)"
        )
        pcaScope.launch {
            try {
                val result = internalSignIn(username, password, scopes, claimsRequest = null)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signIn", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Sign in a user for a provided parameters; callback variant.
     *
     * @param parameters parameters used for signIn operation.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignInCallback] to receive the result.
     * @throws [MsalException] if an account is already signed in.
     */
    override fun signIn(parameters: NativeAuthSignInParameters, callback: SignInCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signIn(parameters: NativeAuthSignInParameters, callback: SignInCallback)"
        )
        pcaScope.launch {
            try {
                val result = internalSignIn(parameters.username, parameters.password, parameters.scopes, parameters.claimsRequest)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signIn", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Sign in the account using username and password; Kotlin coroutines variant.
     *
     * @param username username of the account to sign in.
     * @param password (Optional) password of the account to sign in.
     * @param scopes (Optional) list of scopes to request.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignInResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    @Deprecated("This method is now deprecated. Use the method 'signIn(parameters:)' instead.")
    override suspend fun signIn(
        username: String,
        password: CharArray?,
        scopes: List<String>?
    ): SignInResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signIn(username: String, password: CharArray?, scopes: List<String>?)"
        )
        return internalSignIn(username, password, scopes, claimsRequest = null)
    }

    /**
     * Sign in a user for a provided parameters; Kotlin coroutines variant.
     *
     * @param parameters parameters used for signIn operation.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignInResult] see detailed possible return state under the object.
     * @throws [MsalException] if an account is already signed in.
     */
    override suspend fun signIn(parameters: NativeAuthSignInParameters): SignInResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signIn(parameters: NativeAuthSignInParameters)"
        )
        return internalSignIn(parameters.username, parameters.password, parameters.scopes, parameters.claimsRequest)
    }


    interface SignUpCallback : Callback<SignUpResult>

    /**
     * Sign up the account using username and password; callback variant.
     *
     * @param username username of the account to sign up.
     * @param password (Optional) password of the account to sign up.
     * @param attributes (Optional) user attributes to be used during account creation
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignUpCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignUpResult] see detailed possible return state under the object.
     */
    @Deprecated("This method is now deprecated. Use the method 'signUp(parameters:, callback:)' instead.")
    override fun signUp(
        username: String,
        password: CharArray?,
        attributes: UserAttributes?,
        callback: SignUpCallback
    ) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signUp(username: String, password: CharArray?, attributes: UserAttributes?, callback: SignUpCallback)"
        )
        pcaScope.launch {
            try {
                val result = internalSignUp(password, username, attributes)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signUp", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Sign up the account for a provided parameters; callback variant.
     *
     * @param parameters parameters used for signUp operation.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.SignUpCallback] to receive the result.
     * @throws MsalClientException if an account is already signed in.
     */
    override fun signUp(parameters: NativeAuthSignUpParameters, callback: SignUpCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signUp(parameters: NativeAuthSignUpParameters, callback: SignUpCallback)"
        )
        pcaScope.launch {
            try {
                val result = internalSignUp(parameters.password, parameters.username, parameters.attributes)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signUp", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Sign up the account using username and password. Kotlin coroutines variant.
     *
     * @param username username of the account to sign up.
     * @param password (Optional) password of the account to sign up.
     * @param attributes (Optional) user attributes to be used during account creation
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignUpResult] see detailed possible return state under the object.
     */
    @Deprecated("This method is now deprecated. Use the method 'signUp(parameters:)' instead.")
    override suspend fun signUp(
        username: String,
        password: CharArray?,
        attributes: UserAttributes?
    ): SignUpResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signUp(username: String, password: CharArray?, attributes: UserAttributes?)"
        )
        return internalSignUp(password, username, attributes)
    }



    /**
     * Sign up the account for a provided parameters; Kotlin coroutines variant.
     *
     * @param parameters parameters used for signUp operation.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.SignUpResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override suspend fun signUp(parameters: NativeAuthSignUpParameters): SignUpResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signUp(parameters: NativeAuthSignUpParameters)"
        )
        return internalSignUp(parameters.password, parameters.username, parameters.attributes)
    }

    interface ResetPasswordCallback : Callback<ResetPasswordStartResult>

    /**
     * Reset password for the account starting from a username; callback variant.
     *
     * @param username username of the account to reset password.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.ResetPasswordCallback] to receive the result.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult] see detailed possible return state under the object.
     */
    @Deprecated("This method is now deprecated. Use the method 'resetPassword(parameters:, callback:)' instead.")
    override fun resetPassword(username: String, callback: ResetPasswordCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.resetPassword(username: String, callback: ResetPasswordCallback)"
        )
        pcaScope.launch {
            try {
                val result = internalResetPassword(username)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in resetPassword", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Reset password for the account for a provided parameters; callback variant.
     *
     * @param parameters parameters used for resetPassword operation.
     * @param callback [com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication.ResetPasswordCallback] to receive the result.
     * @throws MsalClientException if an account is already signed in.
     */
    override fun resetPassword(
        parameters: NativeAuthResetPasswordParameters,
        callback: ResetPasswordCallback
    ) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.resetPassword(parameters: NativeAuthResetPasswordParameters, callback: ResetPasswordCallback)"
        )
        pcaScope.launch {
            try {
                val result = internalResetPassword(parameters.username)
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
     */
    @Deprecated("This method is now deprecated. Use the method 'resetPassword(parameters:)' instead.")
    override suspend fun resetPassword(username: String): ResetPasswordStartResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.resetPassword(username: String)"
        )
        return internalResetPassword(username)
    }

    /**
     * Reset password for the account for a provided parameters; Kotlin coroutines variant.
     *
     * @param parameters parameters used for resetPassword operation.
     * @return [com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult] see detailed possible return state under the object.
     * @throws MsalClientException if an account is already signed in.
     */
    override suspend fun resetPassword(parameters: NativeAuthResetPasswordParameters): ResetPasswordStartResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.resetPassword(parameters: NativeAuthResetPasswordParameters)"
        )
        return internalResetPassword(parameters.username)
    }

    interface NativeAuthV2Callback : Callback<NativeAuthResultV2>

    private suspend fun notImplementedV2(scenario: NativeAuthFlowScenarioV2): NativeAuthResultV2 = withContext(Dispatchers.IO) {
        verifyNoUserIsSignedIn()
        NativeAuthErrorV2(
            errorType = ErrorTypes.NOT_IMPLEMENTED,
            errorMessage = "This is not implemented yet",
            correlationId = UUID.randomUUID().toString(),
            scenario = scenario
        )
    }

    override suspend fun signInV2(parameters: NativeAuthSignInParameters): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signInV2(parameters: NativeAuthSignInParameters)"
        )
        return withContext(Dispatchers.IO) {
            var correlationId = DiagnosticContext.INSTANCE.threadCorrelationId ?: "UNSET"
            // Copied so the caller keeps ownership of its own buffer, while this copy is cleared
            // below on every exit path, including cancellation. A password is never stored in a
            // Parcelable state or in the opaque continuation state.
            val passwordCopy = parameters.password?.copyOf()
            try {
                // Kept inside the try so an already-signed-in account is surfaced through the V2
                // result contract rather than escaping as a thrown MsalClientException, matching
                // resetPasswordV2. Android V1 behaviour is otherwise preserved: V2 rejects sign-in
                // while an account is signed in, and does not support repeated same-account or
                // switch-account sign-in.
                verifyNoUserIsSignedIn()

                if (parameters.username.isBlank()) {
                    return@withContext SignInErrorV2(
                        errorType = ErrorTypes.INVALID_USERNAME,
                        errorMessage = "Empty or blank username",
                        correlationId = correlationId,
                        scenario = NativeAuthFlowScenarioV2.SIGN_IN
                    )
                }

                val cmdParams = CommandParametersAdapter.createSignInV2StartCommandParameters(
                    nativeAuthConfig,
                    nativeAuthConfig.oAuth2TokenCache,
                    parameters.username,
                    passwordCopy,
                    parameters.scopes,
                    parameters.claimsRequest
                )
                correlationId = cmdParams.correlationId ?: correlationId

                val command = NativeAuthV2SignInStartCommand(
                    cmdParams,
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_V2_SIGN_IN_START
                )

                ensureActive()
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
                ensureActive()

                when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2SignInStartCommandResult>()) {
                    is NativeAuthV2CommandResult.Complete -> {
                        val localAuthResult = result.authenticationResult
                        if (localAuthResult != null) {
                            NativeAuthResultV2.Complete(
                                resultValue = AccountState.createFromAuthenticationResult(
                                    authenticationResult = AuthenticationResultAdapter.adapt(localAuthResult),
                                    correlationId = result.correlationId,
                                    config = nativeAuthConfig
                                ),
                                scenario = NativeAuthFlowScenarioV2.SIGN_IN
                            )
                        } else {
                            Logger.warn(TAG, result.correlationId, "V2 signIn Complete with no authentication result.")
                            SignInErrorV2(
                                errorMessage = "Sign in completed but no authentication result was returned.",
                                correlationId = result.correlationId,
                                scenario = NativeAuthFlowScenarioV2.SIGN_IN
                            )
                        }
                    }
                    is NativeAuthV2CommandResult.PasswordRequired -> {
                        NativeAuthResultV2.PasswordRequired(
                            nextState = PasswordRequiredStateV2(
                                continuationState = result.continuationState,
                                scenario = NativeAuthFlowScenarioV2.SIGN_IN,
                                config = nativeAuthConfig
                            ),
                            scenario = NativeAuthFlowScenarioV2.SIGN_IN
                        )
                    }
                    is NativeAuthV2CommandResult.MFARequired -> {
                        val authMethods = result.authMethods.toListOfV2AuthMethods()
                        NativeAuthResultV2.MFARequired(
                            nextState = MFARequiredStateV2(
                                continuationState = result.continuationState,
                                authMethods = authMethods,
                                scenario = NativeAuthFlowScenarioV2.SIGN_IN,
                                config = nativeAuthConfig
                            ),
                            scenario = NativeAuthFlowScenarioV2.SIGN_IN,
                            authMethods = authMethods
                        )
                    }
                    is NativeAuthV2CommandResult.InvalidCredentials -> {
                        SignInErrorV2(
                            errorType = SignInErrorTypes.INVALID_CREDENTIALS,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.SIGN_IN,
                            errorCodes = result.errorCodes
                        )
                    }
                    is NativeAuthV2CommandResult.UserNotFound -> {
                        SignInErrorV2(
                            errorType = ErrorTypes.USER_NOT_FOUND,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.SIGN_IN,
                            errorCodes = result.errorCodes
                        )
                    }
                    is INativeAuthCommandResult.InvalidUsername -> {
                        SignInErrorV2(
                            errorType = ErrorTypes.INVALID_USERNAME,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.SIGN_IN,
                            errorCodes = result.errorCodes
                        )
                    }
                    is NativeAuthV2CommandResult.NotImplemented -> {
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.NOT_IMPLEMENTED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.SIGN_IN
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        SignInErrorV2(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.SIGN_IN
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        SignInErrorV2(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.SIGN_IN,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, correlationId, "Exception thrown in signInV2", e)
                SignInErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in signInV2.",
                    correlationId = correlationId,
                    scenario = NativeAuthFlowScenarioV2.SIGN_IN,
                    exception = e
                )
            } finally {
                StringUtil.overwriteWithNull(passwordCopy)
            }
        }
    }

    override fun signInV2(parameters: NativeAuthSignInParameters, callback: NativeAuthV2Callback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signInV2(parameters: NativeAuthSignInParameters, callback: NativeAuthV2Callback)"
        )
        val parametersSnapshot = NativeAuthSignInParameters(parameters.username).also {
            it.password = parameters.password?.copyOf()
            it.scopes = parameters.scopes?.toList()
            it.claimsRequest = parameters.claimsRequest
        }
        pcaScope.launchOwningPasswordSnapshot(parametersSnapshot.password) {
            try {
                callback.onResult(signInV2(parametersSnapshot))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signInV2", e)
                callback.onError(e)
            }
        }
    }

    override suspend fun signUpV2(parameters: NativeAuthSignUpParameters): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signUpV2(parameters: NativeAuthSignUpParameters)"
        )
        return notImplementedV2(NativeAuthFlowScenarioV2.SIGN_UP)
    }

    override fun signUpV2(parameters: NativeAuthSignUpParameters, callback: NativeAuthV2Callback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.signUpV2(parameters: NativeAuthSignUpParameters, callback: NativeAuthV2Callback)"
        )
        pcaScope.launch {
            try {
                callback.onResult(signUpV2(parameters))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signUpV2", e)
                callback.onError(e)
            }
        }
    }

    override suspend fun resetPasswordV2(parameters: NativeAuthResetPasswordParameters): NativeAuthResultV2 {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.resetPasswordV2(parameters: NativeAuthResetPasswordParameters)"
        )
        return withContext(Dispatchers.IO) {
            var correlationId = DiagnosticContext.INSTANCE.threadCorrelationId ?: "UNSET"
            try {
                // Kept inside the try so an already-signed-in account is surfaced through the V2
                // result contract as a ResetPasswordErrorV2, rather than escaping as a thrown
                // MsalClientException. This matches the V1 flows, which catch the same exception.
                verifyNoUserIsSignedIn()

                if (parameters.username.isBlank()) {
                    return@withContext ResetPasswordErrorV2(
                        errorType = ErrorTypes.INVALID_USERNAME,
                        errorMessage = "Empty or blank username",
                        correlationId = correlationId,
                        scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD
                    )
                }

                val cmdParams = CommandParametersAdapter.createResetPasswordV2StartCommandParameters(
                    nativeAuthConfig,
                    nativeAuthConfig.oAuth2TokenCache,
                    parameters.username
                )
                correlationId = cmdParams.correlationId ?: correlationId

                val command = NativeAuthV2ResetPasswordStartCommand(
                    cmdParams,
                    NativeAuthV2FlowController(),
                    PublicApiId.NATIVE_AUTH_V2_RESET_PASSWORD_START
                )

                ensureActive()
                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).getCancellable()
                ensureActive()

                when (val result = rawCommandResult.checkAndWrapCommandResultType<NativeAuthV2ResetPasswordStartCommandResult>()) {
                    is NativeAuthV2CommandResult.CodeRequired -> {
                        NativeAuthResultV2.CodeRequired(
                            nextState = CodeRequiredStateV2(
                                continuationState = result.continuationState,
                                scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD,
                                config = nativeAuthConfig
                            ),
                            scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD,
                            codeLength = result.codeLength,
                            sentTo = result.challengeTargetLabel,
                            channel = result.challengeChannel
                        )
                    }
                    is NativeAuthV2CommandResult.Complete -> {
                        val localAuthResult = result.authenticationResult
                        if (localAuthResult != null) {
                            val authenticationResult = AuthenticationResultAdapter.adapt(localAuthResult)
                            NativeAuthResultV2.Complete(
                                resultValue = AccountState.createFromAuthenticationResult(
                                    authenticationResult = authenticationResult,
                                    correlationId = result.correlationId,
                                    config = nativeAuthConfig
                                ),
                                scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD
                            )
                        } else {
                            Logger.warn(TAG, result.correlationId, "V2 resetPassword start Complete with no inline sign-in result.")
                            ResetPasswordErrorV2(
                                errorMessage = "Password reset completed but no sign-in result was returned.",
                                correlationId = result.correlationId,
                                scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD
                            )
                        }
                    }
                    is NativeAuthV2CommandResult.UserNotFound -> {
                        ResetPasswordErrorV2(
                            errorType = ErrorTypes.USER_NOT_FOUND,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD,
                            errorCodes = result.errorCodes
                        )
                    }
                    is INativeAuthCommandResult.InvalidUsername -> {
                        ResetPasswordErrorV2(
                            errorType = ErrorTypes.INVALID_USERNAME,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD,
                            errorCodes = result.errorCodes
                        )
                    }
                    is NativeAuthV2CommandResult.NotImplemented -> {
                        NativeAuthErrorV2(
                            errorType = ErrorTypes.NOT_IMPLEMENTED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD
                        )
                    }
                    is INativeAuthCommandResult.Redirect -> {
                        ResetPasswordErrorV2(
                            errorType = ErrorTypes.BROWSER_REQUIRED,
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD
                        )
                    }
                    is INativeAuthCommandResult.APIError -> {
                        ResetPasswordErrorV2(
                            error = result.error,
                            errorMessage = result.errorDescription,
                            correlationId = result.correlationId,
                            scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD,
                            errorCodes = result.errorCodes,
                            exception = result.exception
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.error(TAG, correlationId, "Exception thrown in resetPasswordV2", e)
                ResetPasswordErrorV2(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in resetPasswordV2.",
                    correlationId = correlationId,
                    scenario = NativeAuthFlowScenarioV2.RESET_PASSWORD,
                    exception = e
                )
            }
        }
    }

    override fun resetPasswordV2(parameters: NativeAuthResetPasswordParameters, callback: NativeAuthV2Callback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.resetPasswordV2(parameters: NativeAuthResetPasswordParameters, callback: NativeAuthV2Callback)"
        )
        pcaScope.launch {
            try {
                callback.onResult(resetPasswordV2(parameters))
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in resetPasswordV2", e)
                callback.onError(e)
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
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.checkForPersistedAccount"
        )
        val future = ResultFuture<Boolean>()
        getCurrentAccount(object : NativeAuthPublicClientApplication.GetCurrentAccountCallback {
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

    private suspend fun internalSignIn(
        username: String,
        password: CharArray?,
        scopes: List<String>?,
        claimsRequest: ClaimsRequest?
    ): SignInResult {
        return withContext(Dispatchers.IO) {
            try {
                verifyNoUserIsSignedIn()

                if (username.isBlank()) {
                    return@withContext SignInError(
                        errorType = ErrorTypes.INVALID_USERNAME,
                        errorMessage = "Empty or blank username",
                        correlationId = "UNSET"
                    )
                }

                val hasPassword = password?.isNotEmpty() == true

                val params =
                    CommandParametersAdapter.createSignInStartCommandParameters(
                        nativeAuthConfig,
                        nativeAuthConfig.oAuth2TokenCache,
                        username,
                        password,
                        scopes,
                        claimsRequest
                    )

                val command = SignInStartCommand(
                    params,
                    NativeAuthMsalController(),
                    if (hasPassword) PublicApiId.NATIVE_AUTH_SIGN_IN_WITH_EMAIL_PASSWORD else PublicApiId.NATIVE_AUTH_SIGN_IN_WITH_EMAIL
                )

                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

                try {
                    return@withContext when (val result =
                        rawCommandResult.checkAndWrapCommandResultType<SignInStartCommandResult>()) {
                        is SignInCommandResult.Complete -> {
                            if (hasPassword) {
                                val authenticationResult =
                                    AuthenticationResultAdapter.adapt(result.authenticationResult)

                                SignInResult.Complete(
                                    resultValue = AccountState.createFromAuthenticationResult(
                                        authenticationResult = authenticationResult,
                                        correlationId = result.correlationId,
                                        config = nativeAuthConfig
                                    )
                                )
                            } else {
                                Logger.warnWithObject(
                                    TAG,
                                    result.correlationId,
                                    "Sign in received unexpected result: ",
                                    result
                                )
                                SignInError(
                                    errorMessage = "unexpected state",
                                    error = ErrorTypes.INVALID_STATE,
                                    correlationId = result.correlationId
                                )
                            }
                        }

                        is SignInCommandResult.CodeRequired -> {
                            Logger.warn(
                                TAG,
                                result.correlationId,
                                "Server requires a code"
                            )
                            SignInResult.CodeRequired(
                                nextState = SignInCodeRequiredState(
                                    continuationToken = result.continuationToken,
                                    correlationId = result.correlationId,
                                    username = username,
                                    scopes = scopes,
                                    config = nativeAuthConfig,
                                    claimsRequestJson = params.claimsRequestJson
                                ),
                                codeLength = result.codeLength,
                                sentTo = result.challengeTargetLabel,
                                channel = result.challengeChannel
                            )
                        }

                        is INativeAuthCommandResult.InvalidUsername -> {
                            SignInError(
                                errorType = ErrorTypes.INVALID_USERNAME,
                                errorMessage = result.errorDescription,
                                error = result.error,
                                correlationId = result.correlationId,
                                errorCodes = result.errorCodes
                            )
                        }

                        is SignInCommandResult.PasswordRequired -> {
                            if (hasPassword) {
                                Logger.warnWithObject(
                                    TAG,
                                    "Sign in using password received unexpected result: ",
                                    result
                                )
                                SignInError(
                                    errorMessage = "unexpected state",
                                    error = ErrorTypes.INVALID_STATE,
                                    correlationId = result.correlationId
                                )
                            } else {
                                SignInResult.PasswordRequired(
                                    nextState = SignInPasswordRequiredState(
                                        continuationToken = result.continuationToken,
                                        correlationId = result.correlationId,
                                        username = username,
                                        scopes = scopes,
                                        config = nativeAuthConfig,
                                        claimsRequestJson = params.claimsRequestJson
                                    )
                                )
                            }
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
                            if (hasPassword) {
                                SignInError(
                                    errorType = SignInErrorTypes.INVALID_CREDENTIALS,
                                    errorMessage = result.errorDescription,
                                    error = result.error,
                                    correlationId = result.correlationId,
                                    errorCodes = result.errorCodes
                                )
                            } else {
                                Logger.warnWithObject(
                                    TAG,
                                    "Sign in received Unexpected result: ",
                                    result
                                )
                                SignInError(
                                    errorMessage = "unexpected state",
                                    error = ErrorTypes.INVALID_STATE,
                                    correlationId = result.correlationId,
                                    errorCodes = result.errorCodes
                                )
                            }
                        }

                        is SignInCommandResult.MFARequired -> {
                            SignInResult.MFARequired(
                                nextState = AwaitingMFAState(
                                    continuationToken = result.continuationToken,
                                    correlationId = result.correlationId,
                                    scopes = scopes,
                                    config = nativeAuthConfig
                                ),
                                authMethods = result.authMethods.toListOfAuthMethods()
                            )
                        }

                        is SignInCommandResult.StrongAuthMethodRegistrationRequired -> {
                            SignInResult.StrongAuthMethodRegistrationRequired(
                                nextState = RegisterStrongAuthState(
                                    continuationToken = result.continuationToken,
                                    correlationId = result.correlationId,
                                    config = nativeAuthConfig
                                ),
                                authMethods = result.authMethods.toListOfAuthMethods()
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

                        is INativeAuthCommandResult.APIError -> {
                            SignInError(
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
            } catch (e: Exception) {
                SignInError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in signIn.",
                    exception = e,
                    correlationId = "UNSET"
                )
            }
        }
    }

    private suspend fun internalSignUp(
        password: CharArray?,
        username: String,
        attributes: UserAttributes?
    ): SignUpResult {
        val hasPassword = password?.isNotEmpty() == true

        return withContext(Dispatchers.IO) {
            try {
                verifyNoUserIsSignedIn()

                if (username.isBlank()) {
                    return@withContext SignUpError(
                        errorType = ErrorTypes.INVALID_USERNAME,
                        errorMessage = "Empty or blank username",
                        correlationId = "UNSET"
                    )
                }

                val parameters =
                    CommandParametersAdapter.createSignUpStartCommandParameters(
                        nativeAuthConfig,
                        nativeAuthConfig.oAuth2TokenCache,
                        username,
                        password,
                        attributes?.toMap()
                    )

                val command = SignUpStartCommand(
                    parameters,
                    NativeAuthMsalController(),
                    PublicApiId.NATIVE_AUTH_SIGN_UP_START
                )

                val rawCommandResult = CommandDispatcher.submitSilentReturningFuture(command).get()

                try {
                    return@withContext when (val result =
                        rawCommandResult.checkAndWrapCommandResultType<SignUpStartCommandResult>()) {
                        is SignUpCommandResult.Complete -> {
                            SignUpResult.Complete(
                                nextState = SignInContinuationState(
                                    continuationToken = result.continuationToken,
                                    correlationId = result.correlationId,
                                    username = username,
                                    config = nativeAuthConfig
                                )
                            )
                        }

                        is SignUpCommandResult.AttributesRequired -> {
                            SignUpResult.AttributesRequired(
                                nextState = SignUpAttributesRequiredState(
                                    continuationToken = result.continuationToken,
                                    correlationId = result.correlationId,
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
                                    correlationId = result.correlationId,
                                    username = username,
                                    config = nativeAuthConfig
                                ),
                                codeLength = result.codeLength,
                                sentTo = result.challengeTargetLabel,
                                channel = result.challengeChannel,
                            )
                        }

                        is SignUpCommandResult.PasswordRequired -> {
                            if (hasPassword) {
                                Logger.warnWithObject(
                                    TAG,
                                    result.correlationId,
                                    "Sign up using password received unexpected result: ",
                                    result
                                )
                                SignUpError(
                                    errorMessage = "Unexpected state",
                                    error = ErrorTypes.INVALID_STATE,
                                    correlationId = result.correlationId
                                )
                            } else {
                                SignUpResult.PasswordRequired(
                                    nextState = SignUpPasswordRequiredState(
                                        continuationToken = result.continuationToken,
                                        correlationId = result.correlationId,
                                        username = username,
                                        config = nativeAuthConfig
                                    )
                                )
                            }
                        }

                        is SignUpCommandResult.AuthNotSupported -> {
                            SignUpError(
                                errorType = SignUpErrorTypes.AUTH_NOT_SUPPORTED,
                                error = result.error,
                                errorMessage = result.errorDescription,
                                correlationId = result.correlationId
                            )
                        }

                        is SignUpCommandResult.InvalidPassword -> {
                            if (hasPassword) {
                                SignUpError(
                                    errorType = ErrorTypes.INVALID_PASSWORD,
                                    error = result.error,
                                    errorMessage = result.errorDescription,
                                    correlationId = result.correlationId
                                )
                            } else {
                                Logger.warnWithObject(
                                    TAG,
                                    result.correlationId,
                                    "Sign up received unexpected result: ",
                                    result
                                )
                                SignUpError(
                                    error = ErrorTypes.INVALID_STATE,
                                    errorMessage = "Unexpected state",
                                    correlationId = result.correlationId,
                                )
                            }
                        }

                        is SignUpCommandResult.UsernameAlreadyExists -> {
                            SignUpError(
                                errorType = SignUpErrorTypes.USER_ALREADY_EXISTS,
                                error = result.error,
                                errorMessage = result.errorDescription,
                                correlationId = result.correlationId
                            )
                        }

                        is INativeAuthCommandResult.InvalidUsername -> {
                            SignUpError(
                                errorType = ErrorTypes.INVALID_USERNAME,
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

                        is INativeAuthCommandResult.APIError -> {
                            SignUpError(
                                errorMessage = "Unexpected state",
                                error = ErrorTypes.INVALID_STATE,
                                correlationId = result.correlationId
                            )
                        }
                    }
                } finally {
                    StringUtil.overwriteWithNull(parameters.password)
                }
            } catch (e: Exception) {
                SignUpError(
                    errorType = ErrorTypes.CLIENT_EXCEPTION,
                    errorMessage = "MSAL client exception occurred in signIn.",
                    exception = e,
                    correlationId = "UNSET"
                )
            }
        }
    }

    private suspend fun internalResetPassword(username: String): ResetPasswordStartResult {
        try {
            return withContext(Dispatchers.IO) {
                verifyNoUserIsSignedIn()

                if (username.isBlank()) {
                    return@withContext ResetPasswordError(
                        errorType = ErrorTypes.INVALID_USERNAME,
                        errorMessage = "Empty or blank username",
                        correlationId = "UNSET"
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

                return@withContext when (val result =
                    rawCommandResult.checkAndWrapCommandResultType<ResetPasswordStartCommandResult>()) {
                    is ResetPasswordCommandResult.CodeRequired -> {
                        ResetPasswordStartResult.CodeRequired(
                            nextState = ResetPasswordCodeRequiredState(
                                continuationToken = result.continuationToken,
                                username = username,
                                correlationId = result.correlationId,
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

                    is INativeAuthCommandResult.InvalidUsername -> {
                        ResetPasswordError(
                            errorType = ErrorTypes.INVALID_USERNAME,
                            errorMessage = result.errorDescription,
                            error = result.error,
                            correlationId = result.correlationId,
                            errorCodes = result.errorCodes
                        )
                    }

                    is INativeAuthCommandResult.APIError -> {
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
                        Logger.warnWithObject(
                            TAG,
                            result.correlationId,
                            "Reset password received unexpected result: ",
                            result
                        )
                        ResetPasswordError(
                            error = ErrorTypes.INVALID_STATE,
                            errorMessage = "Unexpected state",
                            correlationId = result.correlationId,
                        )
                    }

                    is ResetPasswordCommandResult.EmailNotVerified -> {
                        Logger.warnWithObject(
                            TAG,
                            result.correlationId,
                            "Reset password received unexpected result: ",
                            result
                        )
                        ResetPasswordError(
                            error = ErrorTypes.INVALID_STATE,
                            errorMessage = "Unexpected state",
                            correlationId = result.correlationId,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            return ResetPasswordError(
                errorType = ErrorTypes.CLIENT_EXCEPTION,
                errorMessage = "MSAL client exception occurred in resetPassword.",
                exception = e,
                correlationId = "UNSET"
            )
        }
    }
}
