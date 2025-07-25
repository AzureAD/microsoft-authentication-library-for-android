//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.

import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException

/**
 * Snippet showing how to sign out in SINGLE ACCOUNT MODE.
 *
 * Use signOut for sign-out in SINGLE ACCOUNT MODE.
 * Do NOT use this in multiple account mode. For multiple account mode, use removeAccount.
 */
class AccountSignOut {

    var mPCA: ISingleAccountPublicClientApplication? = null

    /**
     * Signs out the current account in single account mode.
     */
    fun signOut(callback: SignOutCallback) {
        mPCA?.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                // Sign out successful
                callback.onComplete(null)
            }

            override fun onError(exception: MsalException) {
                // Failed to sign out
                callback.onComplete(exception)
            }
        })
    }

    /**
     * Example usage for single account mode
     */
    fun exampleSingleAccountUsage() {
        mPCA = /* Initialize your ISingleAccountPublicClientApplication instance here */
        signOut(object : SignOutCallback {
            override fun onComplete(exception: MsalException?) {
                if (exception == null) {
                    println("Successfully signed out")
                    // Update UI to signed-out state
                } else {
                    println("Failed to sign out: ${exception.message}")
                    // Handle the error
                }
            }
        })
    }

    /**
     * Callback interface for sign out operations
     */
    interface SignOutCallback {
        fun onComplete(exception: MsalException?)
    }
}
