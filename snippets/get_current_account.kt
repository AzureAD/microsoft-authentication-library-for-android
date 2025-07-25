//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.

import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException

/**
 * Demonstrates how to get the current account in SINGLE ACCOUNT MODE.
 *
 * Use getCurrentAccount for single account mode.
 * Do NOT use this in multiple account mode. For multiple account mode, use getAccounts.
 */
class GetCurrentAccountHelper {

    var mPCA: ISingleAccountPublicClientApplication? = null

    /**
     * Gets the current account for single account mode applications.
     */
    fun getCurrentAccount(callback: CurrentAccountCallback) {
        mPCA?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(account: IAccount?) {
                callback.onComplete(account, null)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                // Handle account changes
                callback.onComplete(currentAccount, null)
            }

            override fun onError(exception: MsalException) {
                callback.onComplete(null, exception)
            }
        })
    }

    /**
     * Example usage for single account mode
     */
    fun exampleSingleAccountUsage() {
        mPCA = /* Initialize your ISingleAccountPublicClientApplication instance here */
        getCurrentAccount(object : CurrentAccountCallback {
            override fun onComplete(account: IAccount?, exception: MsalException?) {
                when {
                    account != null -> {
                        println("Current account: ${account.username}")
                        // Use the account
                    }
                    exception != null -> {
                        println("Failed to get current account: ${exception.message}")
                        // Handle the error
                    }
                    else -> {
                        println("No account signed in")
                        // Handle no account scenario
                    }
                }
            }
        })
    }

    /**
     * Callback interface for retrieving current account
     */
    interface CurrentAccountCallback {
        fun onComplete(account: IAccount?, exception: MsalException?)
    }
}
