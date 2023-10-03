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
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager
import com.microsoft.identity.common.internal.controllers.LocalMSALController
import com.microsoft.identity.common.internal.controllers.NativeAuthMsalController
import com.microsoft.identity.common.internal.net.cache.HttpCache
import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.CommandCallback
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.results.ICommandResult
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger.LogLevel
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.java.util.ResultFuture
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
}
