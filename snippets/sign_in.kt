//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.

import android.app.Activity
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalException

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
    fun signIn(activity: Activity, callback: SignInCallback) {
        val parameters = SignInParameters.Builder()
            .startActivity(activity)
            .withCallback(object : ISingleAccountPublicClientApplication.SignInCallback {
                override fun onSignIn(result: IAuthenticationResult) { // Sign-in successful, handle result
                    callback.onComplete(result, null)
                }

                override fun onError(exception: MsalException) { // Sign-in failed, handle error
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
        signIn(activity, object : SignInCallback {
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
