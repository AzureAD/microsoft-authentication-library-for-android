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
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException

/**
 * Demonstrates how to acquire tokens silently (no interactive flow).
 * 
 * Includes both synchronous (background thread) and asynchronous (main thread) patterns.
 * Use this snippet in both single and multiple account modes.
 */
class SilentTokenAcquisition {
    private lateinit var mPCA: IPublicClientApplication // Use ISingleAccountPublicClientApplication or IMultipleAccountPublicClientApplication as needed

    /**
     * IMPORTANT: This method must be called from a background thread.
     * For main thread operations, use acquireTokenSilentAsync instead.
     * 
     * Attempts to acquire a token silently from the cache.
     */
    @WorkerThread
    @Throws(MsalException::class, InterruptedException::class)
    fun acquireTokenSilent(
        account: IAccount,
        scopes: List<String>
    ): IAuthenticationResult {
        // Build parameters using the modern Parameters-based API
        val parameters = AcquireTokenSilentParameters.Builder()
            .withScopes(scopes)
            .forAccount(account)
            .fromAuthority(account.getAuthority())
            .build()

        // Synchronously acquire token - MUST be called from background thread
        return mPCA.acquireTokenSilent(parameters)
    }

    /**
     * Safe to call from main thread. Uses asynchronous token acquisition.
     */
    fun acquireTokenSilentAsync(
        account: IAccount,
        scopes: List<String>,
        callback: (IAuthenticationResult?, MsalException?) -> Unit
    ) {
        // Build parameters using the modern Parameters-based API
        val parameters = AcquireTokenSilentParameters.Builder()
            .withScopes(scopes)
            .forAccount(account)
            .fromAuthority(account.getAuthority())
            .withCallback(object : SilentAuthenticationCallback {
                override fun onSuccess(result: IAuthenticationResult) { // token acquisition successful, handle result
                    callback(result, null)
                }

                override fun onError(exception: MsalException) { // token acquisition failed, handle error
                    callback(null, exception)
                }
            })
            .build()

        // Asynchronously acquire token - safe to call from any thread
        mPCA.acquireTokenSilentAsync(parameters)
    }

    /**
     * Example usage showing both synchronous and asynchronous patterns
     */
    fun exampleUsage(account: IAccount) {
        mPCA = /* Initialize your IPublicClientApplication instance here, either multiple or single account. */;
        val graphScopes = listOf("User.Read")

        // Example 1: Background thread usage (synchronous)
        Thread {
            try {
                // Must be called from background thread
                val result = acquireTokenSilent(account, graphScopes)
                val accessToken = result.accessToken
                // Use the access token
            } catch (e: Exception) {
                // Handle exceptions
                println("Failed to acquire token silently: ${e.message}")
                // Fall back to interactive authentication
            }
        }.start()

        // Example 2: Main thread usage (asynchronous)
        acquireTokenSilentAsync(account, graphScopes) { result, exception ->
            when {
                result != null -> {
                    val accessToken = result.accessToken
                    // Use the access token
                }
                exception != null -> {
                    println("Failed to acquire token silently: ${exception.message}")
                    // Fall back to interactive authentication
                }
            }
        }
    }
}
