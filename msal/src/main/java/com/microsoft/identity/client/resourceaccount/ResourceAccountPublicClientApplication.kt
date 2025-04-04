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
package com.microsoft.identity.client.resourceaccount

import android.content.Context
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication.LoadAccountsCallback
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.PublicClientApplicationConfiguration
import com.microsoft.identity.client.PublicClientApplicationConfigurationFactory
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.java.util.TaskCompletedCallbackWithError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResourceAccountPublicClientApplication(configuration: PublicClientApplicationConfiguration) : IResourceAccountPublicClientApplication, PublicClientApplication(configuration) {

    companion object {
        private const val TAG = "ResourceAccountPublicClientApplication"

        @JvmStatic
        fun create(configuration: PublicClientApplicationConfiguration): ResourceAccountPublicClientApplication {
            if (!configuration.useBroker) {
                throw MsalClientException("ResourceAccountPublicClientApplication must be created with broker enabled.")
            }
            // more validations can be added here if needed
            // e.g. Is MTR device?
            return ResourceAccountPublicClientApplication(configuration)
        }

        @JvmStatic
        fun create(context: Context, configId: Int): ResourceAccountPublicClientApplication {
            val configuration = PublicClientApplicationConfigurationFactory.initializeConfiguration(context, configId)
            return ResourceAccountPublicClientApplication(configuration)
        }
    }

    override fun getResourceAccountsAsync(callback: LoadAccountsCallback) {
    }

    @Throws(InterruptedException::class, MsalException::class)
    override fun getResourceAccounts(): List<IAccount> {
        return emptyList()
    }

    override fun acquireTokenForResourceAccountAsync(tokenParameters: ResoureAccountTokenParameters) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = try {
                acquireTokenForResourceAccount(tokenParameters)
            } catch (e: Exception) {
                null // Handle exceptions gracefully
            }

            withContext(Dispatchers.Main) {
                tokenParameters.callback?.let { callback ->
                    if (result != null) {
                        callback.onSuccess(result)
                    } else {
                        callback.onError(MsalClientException("Error acquiring token for resource account"))
                    }
                }
            }
        }

    }

    @Throws(InterruptedException::class, MsalException::class)
    override fun acquireTokenForResourceAccount(tokenParameters: ResoureAccountTokenParameters): IAuthenticationResult? {
        return null
    }

    override fun getAadDeviceId(homeTenantId: String): String {
        TODO("Not yet implemented")
    }

    override fun getAadDeviceIdAsync(
        homeTenantId: String,
        callback: TaskCompletedCallbackWithError<String, MsalException>
    ) {
        TODO("Not yet implemented")
    }
}
