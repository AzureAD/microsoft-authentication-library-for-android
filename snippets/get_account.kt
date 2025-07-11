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
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException

class AccountManagement {
    /**
     * Gets all accounts for multiple account mode applications
     */
    fun getAccounts(
        pca: IMultipleAccountPublicClientApplication,
        callback: (List<IAccount>?, MsalException?) -> Unit
    ) {
        pca.getAccounts(object : IMultipleAccountPublicClientApplication.LoadAccountsCallback {
            override fun onTaskCompleted(result: List<IAccount>) {
                callback(result, null)
            }

            override fun onError(exception: MsalException) {
                callback(null, exception)
            }
        })
    }

    /**
     * Gets the current account for single account mode applications
     */
    fun getCurrentAccount(
        pca: ISingleAccountPublicClientApplication,
        callback: (IAccount?, MsalException?) -> Unit
    ) {
        pca.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(account: IAccount?) {
                callback(account, null)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                // Handle account changes
                callback(currentAccount, null)
            }

            override fun onError(exception: MsalException) {
                callback(null, exception)
            }
        })
    }

    /**
     * Example usage for multiple account mode
     */
    fun exampleMultipleAccountUsage(pca: IMultipleAccountPublicClientApplication) {
        getAccounts(pca) { accounts, exception ->
            when {
                accounts != null -> {
                    if (accounts.isEmpty()) {
                        println("No accounts found")
                        // Handle no accounts scenario
                    } else {
                        // Process the accounts
                        accounts.forEach { account ->
                            println("Account: ${account.username}")
                        }
                    }
                }
                exception != null -> {
                    println("Failed to get accounts: ${exception.message}")
                    // Handle the error
                }
            }
        }
    }

    /**
     * Example usage for single account mode
     */
    fun exampleSingleAccountUsage(pca: ISingleAccountPublicClientApplication) {
        getCurrentAccount(pca) { account, exception ->
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
    }
}
