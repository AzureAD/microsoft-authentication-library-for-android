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
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException

class MSALInitialization {
    companion object {
        private const val CONFIG_FILE_NAME = "auth_config.json"
    }

    private lateinit var mPCA: IPublicClientApplication

    /**
     * Initializes MSAL PublicClientApplication with configuration from auth_config.json
     */
    fun initializeMSAL(context: Context, callback: (IPublicClientApplication?, MsalException?) -> Unit) {
        // Create PCA from config file
        PublicClientApplication.createMultipleAccountPublicClientApplication(
            context,
            CONFIG_FILE_NAME,
            object : IPublicClientApplication.ApplicationCreatedListener {
                override fun onCreated(application: IPublicClientApplication) {
                    mPCA = application
                    callback(mPCA, null)
                }

                override fun onError(exception: MsalException) {
                    callback(null, exception)
                }
            }
        )
    }

    // For single account mode, use this initialization instead:
    fun initializeSingleAccountMSAL(context: Context, callback: (IPublicClientApplication?, MsalException?) -> Unit) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            CONFIG_FILE_NAME,
            object : IPublicClientApplication.ApplicationCreatedListener {
                override fun onCreated(application: IPublicClientApplication) {
                    mPCA = application
                    callback(mPCA, null)
                }

                override fun onError(exception: MsalException) {
                    callback(null, exception)
                }
            }
        )
    }
}
