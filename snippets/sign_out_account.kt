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
