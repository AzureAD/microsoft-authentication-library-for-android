//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.nativeauth.statemachine.states

import android.os.Parcel
import android.os.Parcelable
import com.microsoft.identity.client.Account
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationResultAdapter
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.common.internal.commands.RemoveCurrentAccountCommand
import com.microsoft.identity.common.internal.controllers.LocalMSALController
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.commands.CommandCallback
import com.microsoft.identity.common.java.commands.SilentTokenCommand
import com.microsoft.identity.common.java.controllers.BaseController
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.ExceptionAdapter
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.exception.ServiceException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult
import com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.statemachine.errors.GetAccessTokenError
import com.microsoft.identity.nativeauth.statemachine.errors.GetAccessTokenErrorTypes
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.SignOutResult
import com.microsoft.identity.nativeauth.utils.serializable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 *  AccountState returned as part of a successful completion of sign in flow [com.microsoft.identity.nativeauth.statemachine.results.SignInResult.Complete].
 */
class AccountState private constructor(
    private val account: IAccount,
    private val config: NativeAuthPublicClientApplicationConfiguration,
    val correlationId: String
) : Parcelable {

    interface SignOutCallback : Callback<SignOutResult>

    constructor(parcel: Parcel) : this(
        account = parcel.serializable<IAccount>() as IAccount,
        correlationId = parcel.readString() ?: "UNSET",
        config = parcel.serializable<NativeAuthPublicClientApplicationConfiguration>() as NativeAuthPublicClientApplicationConfiguration
    )

    /**
     * Remove the current account from the cache; callback variant.
     *
     * @param callback [com.microsoft.identity.nativeauth.statemachine.states.AccountState.SignOutCallback] to receive the result on.
     */
    fun signOut(callback: SignOutCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "$TAG.signOut"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = signOut()
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in signOut", e)
                callback.onError(e)
            }
        }
    }

    private fun addDefaultScopes(scopes: List<String>?): List<String> {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.addDefaultScopes"
        )
        val requestScopes = scopes?.toMutableList() ?: mutableListOf()
        requestScopes.addAll(AuthenticationConstants.DEFAULT_SCOPES)
        // sanitize empty and null scopes
        requestScopes.removeAll(listOf("", null))
        return requestScopes.toList()
    }

    /**
     * Remove the current account from the cache; Kotlin coroutines variant.
     */
    suspend fun signOut(): SignOutResult {
        return withContext(Dispatchers.IO) {
            LogSession.logMethodCall(
                tag = TAG,
                correlationId = null,
                methodName = "$TAG.signOut.withContext"
            )

            val account: IAccount =
                NativeAuthPublicClientApplication.getCurrentAccountInternal(config)
                    ?: throw MsalClientException(
                        MsalClientException.NO_CURRENT_ACCOUNT,
                        MsalClientException.NO_CURRENT_ACCOUNT_ERROR_MESSAGE
                    )

            val requestAccountRecord = AccountRecord()
            requestAccountRecord.environment = (account as Account).environment
            requestAccountRecord.homeAccountId = account.homeAccountId

            val params = CommandParametersAdapter.createRemoveAccountCommandParameters(
                config,
                config.oAuth2TokenCache,
                requestAccountRecord
            )

            val removeCurrentAccountCommandParameters = RemoveCurrentAccountCommand(
                params,
                LocalMSALController().asControllerFactory(),
                object : CommandCallback<Boolean?, BaseException?> {
                    override fun onError(error: BaseException?) {
                        // Do nothing, handled by CommandDispatcher.submitSilentReturningFuture()
                    }

                    override fun onTaskCompleted(result: Boolean?) {
                        // Do nothing, handled by CommandDispatcher.submitSilentReturningFuture()
                    }

                    override fun onCancel() {
                        // Do nothing
                    }
                },
                PublicApiId.NATIVE_AUTH_ACCOUNT_SIGN_OUT
            )

            val result = CommandDispatcher.submitSilentReturningFuture(removeCurrentAccountCommandParameters)
                .get().result as Boolean

            return@withContext if (result) {
                SignOutResult.Complete
            } else {
                Logger.error(
                    TAG,
                    "Unexpected error during signOut.",
                    null
                )
                throw MsalClientException(
                    MsalClientException.UNKNOWN_ERROR,
                    "Unexpected error during signOut."
                )
            }
        }
    }

    /**
     * Gets the current account.
     *
     * @return account [com.microsoft.identity.client.IAccount].
     */
    fun getAccount(): IAccount {
        return account
    }

    /**
     * Gets the current account's ID token (if present).
     *
     * @return idToken [String].
     */
    fun getIdToken(): String? {
        return account.idToken
    }

    /**
     * Gets the claims associated with the current account.
     *
     * @return A Map of claims.
     */
    fun getClaims(): Map<String, *>? {
        return account.claims
    }

    interface GetAccessTokenCallback : Callback<GetAccessTokenResult>

    /**
     * Retrieves the access token for the currently signed in account from the cache. If multiple
     * access tokens are present in the cache then the one that matches the sign in scopes is returned.
     * If the access token is expired, it will be attempted to be refreshed using the refresh token that's stored in the cache;
     * callback variant.
     *
     * @return [com.microsoft.identity.client.IAuthenticationResult] If successful.
     * @throws [MsalClientException] If the the account doesn't exist in the cache.
     * @throws [ServiceException] If the refresh token doesn't exist in the cache/is expired, or the refreshing fails.
     */
    fun getAccessToken(forceRefresh: Boolean = false, callback: GetAccessTokenCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "$TAG.getAccessToken"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = getAccessToken(forceRefresh)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in getAccessToken", e)
                callback.onError(e)
            }
        }
    }

    /**
     * Retrieves the access token for the currently signed in account from the cache. If multiple
     * access tokens are present in the cache then the one that matches the sign in scopes is returned.
     * If the access token is expired, it will be attempted to be refreshed using the refresh token that's stored in the cache;
     * Kotlin coroutines variant.
     *
     * @return [com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult] The result of the getAccessToken action
     */
    suspend fun getAccessToken(forceRefresh: Boolean = false): GetAccessTokenResult {
        return getAccessTokenInternal(forceRefresh, null);
    }

    /**
     * Retrieves the access token for the currently signed in account from the cache.
     * If the access token is expired, it will be attempted to be refreshed using the refresh token that's stored in the cache;
     * If any of the cached access token matches the scope specified in scopes parameter then that
     * is returned otherwise a new access token with requested scopes is fetched.
     * Kotlin coroutines variant.
     *
     * @return [com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult] The result of the getAccessToken action
     */
    suspend fun getAccessToken(forceRefresh: Boolean = false, scopes: List<String>?): GetAccessTokenResult {
        return getAccessTokenInternal(forceRefresh, scopes)
    }

    /**
     * Retrieves the access token for the currently signed in account from the cache.
     * If the access token is expired, it will be attempted to be refreshed using the refresh token that's stored in the cache;
     * If any of the cached access token matches the scope specified in scopes parameter then that
     * is returned otherwise a new access token with requested scopes is fetched.
     * callback variant.
     *
     * @return [com.microsoft.identity.client.IAuthenticationResult] If successful.
     * @throws [MsalClientException] If the the account doesn't exist in the cache.
     * @throws [ServiceException] If the refresh token doesn't exist in the cache/is expired, or the refreshing fails.
     */
    fun getAccessToken(forceRefresh: Boolean = false, scopes: List<String>, callback: GetAccessTokenCallback) {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "$TAG.getAccessToken"
        )
        NativeAuthPublicClientApplication.pcaScope.launch {
            try {
                val result = getAccessToken(forceRefresh, scopes)
                callback.onResult(result)
            } catch (e: MsalException) {
                Logger.error(TAG, "Exception thrown in getAccessToken", e)
                callback.onError(e)
            }
        }
    }

    private suspend fun getAccessTokenInternal(forceRefresh: Boolean, scopes: List<String>?): GetAccessTokenResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "$TAG.getAccessToken(forceRefresh: Boolean)"
        )
        val mergedScopes = BaseController.addDefaultScopes(
            scopes?.toMutableSet() ?: mutableSetOf() ).toList()

        return withContext(Dispatchers.IO) {

            val accountResult = NativeAuthPublicClientApplication.getCurrentAccountInternal(config) as? Account
            val account = accountResult
                    ?: return@withContext GetAccessTokenError(
                        errorType = GetAccessTokenErrorTypes.NO_ACCOUNT_FOUND,
                        error = MsalClientException.NO_CURRENT_ACCOUNT,
                        errorMessage = MsalClientException.NO_CURRENT_ACCOUNT_ERROR_MESSAGE,
                        correlationId = "UNSET"
                    )

            val acquireTokenSilentParameters = AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.authority)
                .forceRefresh(forceRefresh)
                .withScopes(mergedScopes)
                .build()

            val accountRecord = PublicClientApplication.selectAccountRecordForTokenRequest(
                config,
                acquireTokenSilentParameters
            )
            acquireTokenSilentParameters.accountRecord = accountRecord

            val params = CommandParametersAdapter.createSilentTokenCommandParameters(
                config,
                config.oAuth2TokenCache,
                acquireTokenSilentParameters
            )

            val command = SilentTokenCommand(
                params,
                NativeAuthMsalController().asControllerFactory(),
                object : CommandCallback<Boolean?, BaseException?> {
                    override fun onError(error: BaseException?) {
                        // Do nothing, handled by CommandDispatcher.submitSilentReturningFuture()
                    }

                    override fun onTaskCompleted(result: Boolean?) {
                        // Do nothing, handled by CommandDispatcher.submitSilentReturningFuture()
                    }

                    override fun onCancel() {
                        // Do nothing
                    }
                },
                PublicApiId.NATIVE_AUTH_ACCOUNT_GET_ACCESS_TOKEN
            )

            val commandResult = CommandDispatcher.submitSilentReturningFuture(command)
                .get().result

            return@withContext when (commandResult) {
                is ServiceException -> {
                    GetAccessTokenError(
                        exception = ExceptionAdapter.convertToNativeAuthException(commandResult),
                        correlationId = "UNSET"
                    )
                }
                is Exception -> {
                    GetAccessTokenError(
                        exception = commandResult,
                        correlationId = "UNSET"
                    )
                }
                else -> {
                    GetAccessTokenResult.Complete(
                        resultValue =  AuthenticationResultAdapter.adapt(commandResult as ILocalAuthenticationResult)
                    )
                }
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(account)
        parcel.writeSerializable(correlationId)
        parcel.writeSerializable(config)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AccountState> {

        private val TAG = NativeAuthPublicClientApplication::class.java.simpleName
        override fun createFromParcel(parcel: Parcel): AccountState {
            return AccountState(parcel)
        }

        override fun newArray(size: Int): Array<AccountState?> {
            return arrayOfNulls(size)
        }

        fun createFromAuthenticationResult(
            authenticationResult: IAuthenticationResult,
            correlationId: String,
            config: NativeAuthPublicClientApplicationConfiguration
        ): AccountState {
            return AccountState(
                account = authenticationResult.account,
                correlationId = correlationId,
                config = config
            )
        }

        fun createFromAccountResult(
            account: IAccount,
            correlationId: String,
            config: NativeAuthPublicClientApplicationConfiguration
        ): AccountState {
            return AccountState(
                account = account,
                correlationId = correlationId,
                config = config
            )
        }
    }
}
