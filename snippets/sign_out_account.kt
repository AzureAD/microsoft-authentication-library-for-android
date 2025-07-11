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

class AccountSignOut {
    /**
     * Signs out and removes the account from MSAL cache in multiple account mode
     */
    fun signOutMultipleAccount(
        pca: IMultipleAccountPublicClientApplication,
        account: IAccount,
        callback: (MsalException?) -> Unit
    ) {
        pca.removeAccount(
            account,
            object : IMultipleAccountPublicClientApplication.RemoveAccountCallback {
                override fun onRemoved() {
                    // Account successfully removed
                    callback(null)
                }

                override fun onError(exception: MsalException) {
                    // Failed to remove account
                    callback(exception)
                }
            }
        )
    }

    /**
     * Signs out the current account in single account mode
     */
    fun signOutSingleAccount(
        pca: ISingleAccountPublicClientApplication,
        callback: (MsalException?) -> Unit
    ) {
        pca.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                // Sign out successful
                callback(null)
            }

            override fun onError(exception: MsalException) {
                // Failed to sign out
                callback(exception)
            }
        })
    }

    /**
     * Example usage for multiple account mode
     */
    fun exampleMultipleAccountUsage(pca: IMultipleAccountPublicClientApplication, account: IAccount) {
        signOutMultipleAccount(pca, account) { exception ->
            if (exception == null) {
                println("Account successfully signed out")
                // Update UI, clear account-specific data
            } else {
                println("Failed to sign out: ${exception.message}")
                // Handle the error
            }
        }
    }

    /**
     * Example usage for single account mode
     */
    fun exampleSingleAccountUsage(pca: ISingleAccountPublicClientApplication) {
        signOutSingleAccount(pca) { exception ->
            if (exception == null) {
                println("Successfully signed out")
                // Update UI to signed-out state
            } else {
                println("Failed to sign out: ${exception.message}")
                // Handle the error
            }
        }
    }
}
