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
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import java.util.Date

/**
 * IMPORTANT: Device Code Flow is not recommended due to security concerns in the industry.
 * Only use this method in niche scenarios where devices lack input methods necessary for interactive authentication.
 * For standard authentication scenarios, use acquireToken (for multiple account mode) or signIn (for single account mode).
 */
class DeviceCodeFlowTokenAcquisition {
    private lateinit var mPCA: IPublicClientApplication

    /**
     * Acquires token using Device Code Flow. This should only be used in specific scenarios
     * where the device cannot handle interactive authentication.
     */
    fun acquireTokenWithDeviceCode(
        scopes: Array<String>,
        onDeviceCode: (String) -> Unit,
        onComplete: (IAuthenticationResult?, MsalException?) -> Unit
    ) {
        mPCA.acquireTokenWithDeviceCode(
            scopes.toList(),
            object : IPublicClientApplication.DeviceCodeFlowCallback {
                override fun onUserCodeReceived(
                    deviceCode: String,
                    verificationUri: String,
                    message: String,
                    expiresOn: Date
                ) {
                    onDeviceCode(message)
                }

                override fun onTokenReceived(result: IAuthenticationResult) {
                    onComplete(result, null)
                }

                override fun onError(exception: MsalException) {
                    onComplete(null, exception)
                }
            }
        )
    }

    /**
     * Example usage with Microsoft Graph scopes
     */
    fun exampleUsage() {
        val graphScopes = arrayOf("User.Read")
        
        acquireTokenWithDeviceCode(
            scopes = graphScopes,
            onDeviceCode = { message ->
                // Display message to user. This includes the device code and instructions
                println(message)
            },
            onComplete = { result, exception ->
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
        )
    }
}
