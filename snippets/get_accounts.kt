//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.

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
        mPCA?.getAccounts(object : IMultipleAccountPublicClientApplication.LoadAccountsCallback {
            override fun onTaskCompleted(result: List<IAccount>?) {
                callback.onComplete(result, null)
            }

            override fun onError(exception: MsalException) {
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
                            println("Account: ${account.username}")
                        }
                    }
                } else if (exception != null) {
                    println("Failed to get accounts: ${exception.message}")
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
