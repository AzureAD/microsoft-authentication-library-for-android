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

import android.app.Activity
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import java.util.Collections

/**
 * Demonstrates how to sign in using MSAL's Parameters-based API in SINGLE ACCOUNT MODE.
 * 
 * Use signIn for single account mode. For multiple account mode, use acquireToken.
 */
class SignInHelper {

    var mPCA: ISingleAccountPublicClientApplication? = null

    /**
     * Signs in the user interactively in single account mode.
     */
    fun signIn(activity: Activity, scopes: List<String>, callback: SignInCallback) {
        val parameters = SignInParameters.builder()
            // .withLoginHint(mUsername) // Can pass user's login hint if available
            .withScopes(scopes)
            .withActivity(activity)
            .withCallback(object : AuthenticationCallback {
                override fun onCancel() {
                    // Handle cancellation
                }

                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    callback.onComplete(authenticationResult, null)
                }

                override fun onError(exception: MsalException) {
                    callback.onComplete(null, exception)
                }
            })
            .build()

        mPCA?.signIn(parameters)
    }

    /**
     * Example usage for single account mode
     */
    fun exampleSingleAccountUsage(activity: Activity) {
        mPCA = /* Initialize your ISingleAccountPublicClientApplication instance here */
        val graphScopes = Collections.singletonList("User.Read")

        signIn(activity, graphScopes, object : SignInCallback {
            override fun onComplete(result: IAuthenticationResult?, exception: MsalException?) {
                if (result != null) {
                    val accessToken = result.accessToken
                    // Use the access token
                } else if (exception != null) {
                    println("Failed to sign in: ${exception.message}")
                    // Handle the error
                }
            }
        })
    }

    /**
     * Callback interface for sign in operations
     */
    interface SignInCallback {
        fun onComplete(result: IAuthenticationResult?, exception: MsalException?)
    }
}
