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
import com.microsoft.identity.client.exception.MsalException

/**
 * Demonstrates how to get all accounts in MULTIPLE ACCOUNT MODE.
 *
 * Use getAccounts for multiple account mode.
 * Do NOT use this in single account mode. For single account mode, use getCurrentAccount.
 */
class GetAccountsHelper {

    var mPCA: IMultipleAccountPublicClientApplication? = null

    /**
     * Gets all accounts for multiple account mode applications.
     */
    fun getAccounts(callback: AccountsCallback) {
        mPCA?.getAccounts(object : LoadAccountsCallback {
            override fun onTaskCompleted(result: List<IAccount>) { // Successfully retrieved accounts, handle result
                callback.onComplete(result, null)
            }

            override fun onError(exception: MsalException) { // Failed to retrieve accounts, handle error
                callback.onComplete(null, exception)
            }
        })
    }

    /**
     * Example usage for multiple account mode
     */
    fun exampleMultipleAccountUsage() {
        mPCA = /* Initialize your IMultipleAccountPublicClientApplication instance here */
        getAccounts(object : AccountsCallback {
            override fun onComplete(accounts: List<IAccount>?, exception: MsalException?) {
                if (accounts != null) {
                    if (accounts.isEmpty()) {
                        println("No accounts found")
                        // Handle no accounts scenario
                    } else {
                        // Process the accounts
                        for (account in accounts) {
                            println("Account: " + account.username)
                        }
                    }
                } else if (exception != null) {
                    println("Failed to get accounts: " + exception.message)
                    // Handle the error
                }
            }
        })
    }

    /**
     * Callback interface for retrieving multiple accounts
     */
    interface AccountsCallback {
        fun onComplete(accounts: List<IAccount>?, exception: MsalException?)
    }
}
