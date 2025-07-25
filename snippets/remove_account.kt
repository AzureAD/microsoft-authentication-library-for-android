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
 * Snippet showing how to remove an account from the msal cache in MULTIPLE ACCOUNT MODE.
 *
 * Use removeAccount for sign-out in MULTIPLE ACCOUNT MODE.
 * Do NOT use this in single account mode. For single account mode, use signOut.
 */
class AccountRemoveHelper {

    var mPCA: IMultipleAccountPublicClientApplication? = null

    /**
     * Removes the account from MSAL cache in multiple account mode.
     */
    fun removeAccount(
        account: IAccount,
        callback: RemoveAccountCallback
    ) {
        mPCA?.removeAccount(account, object : IMultipleAccountPublicClientApplication.RemoveAccountCallback {
            override fun onRemoved() {
                // Account successfully removed
                callback.onComplete(null)
            }

            override fun onError(exception: MsalException) {
                // Failed to remove account
                callback.onComplete(exception)
            }
        })
    }

    /**
     * Example usage for multiple account mode
     */
    fun exampleMultipleAccountUsage(account: IAccount) {
        mPCA = /* Initialize your IMultipleAccountPublicClientApplication instance here */
        removeAccount(account, object : RemoveAccountCallback {
            override fun onComplete(exception: MsalException?) {
                if (exception == null) {
                    println("Account successfully removed")
                    // Update UI, clear account-specific data
                } else {
                    println("Failed to remove account: ${exception.message}")
                    // Handle the error
                }
            }
        })
    }

    /**
     * Callback interface for remove account operations
     */
    interface RemoveAccountCallback {
        fun onComplete(exception: MsalException?)
    }
}
