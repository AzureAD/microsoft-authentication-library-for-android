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
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException

/**
 * Snippet showing how to acquire a token interactively.
 * 
 * This method can be used for initial sign-in in multiple account mode. It can be used to re-authenticate a user in either
 * single account mode or multiple account mode. In single account mode, to sign in a user for the first time, use signIn.
 */
class TokenAcquisition {
    private lateinit var mPCA: IMultipleAccountPublicClientApplication

    /**
     * Acquires token interactively, launching browser for user authentication if necessary.
     * This is the recommended method for initial sign-in in multiple account mode.
     */
    fun acquireTokenInteractively(
        activity: Activity,
        scopes: List<String>,
        callback: (IAuthenticationResult?, MsalException?) -> Unit
    ) {
        // Build parameters using the modern Parameters-based API
        val parameters = AcquireTokenParameters.Builder()
            .withScopes(scopes)
            .startAuthorizationFromActivity(activity)
            // .withPrompt(Prompt.LOGIN) // Use Prompt.LOGIN to force interactive re-authentication
            .withCallback(object : AuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    callback(authenticationResult, null)
                }

                override fun onError(exception: MsalException) {
                    callback(null, exception)
                }

                override fun onCancel() {
                    // Handle the cancellation
                    println("User cancelled the authentication")
                }
            })
            .build()

        // Acquire token using the parameters
        mPCA.acquireToken(parameters)
    }

    /**
     * Example usage with Microsoft Graph scopes
     */
    fun exampleUsage(activity: Activity) {
        mPCA = /* Initialize your IMultipleAccountPublicClientApplication instance here */;
        val graphScopes = listOf("User.Read")
        
        acquireTokenInteractively(activity, graphScopes) { result, exception ->
            when {
                result != null -> {
                    // Use the access token
                    val accessToken = result.accessToken
                    // Make API calls with the token
                }
                exception != null -> {
                    // Handle the exception
                    println("Error acquiring token: ${exception.message}")
                }
            }
        }
    }
}
