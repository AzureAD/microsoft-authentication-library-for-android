//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.

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
