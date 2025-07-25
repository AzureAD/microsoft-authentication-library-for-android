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
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException

/**
 * Snippets showing how to setup PublicClientApplication (PCA) objects.
 * These objects are used to call MSAL's various APIs in either single or multiple account mode.
 */
class MSALInitialization {
    companion object {
        private const val CONFIG_FILE = "auth_config.json"
    }

    private lateinit var mMultipleAccountPCA: IMultipleAccountPublicClientApplication
    private lateinit var mSingleAccountPCA: ISingleAccountPublicClientApplication

    /**
     * Initializes MSAL PublicClientApplication for multiple account mode with configuration from a config json file
     */
    fun initializeMultipleAccountMSAL(context: Context, callback: (IPublicClientApplication?, MsalException?) -> Unit) {
        // Create PCA from config file
        PublicClientApplication.createMultipleAccountPublicClientApplication(
            context,
            CONFIG_FILE,
            object : IPublicClientApplication.ApplicationCreatedListener {
                override fun onCreated(application: IPublicClientApplication) {
                    mMultipleAccountPCA = application
                    callback(mPCA, null)
                }

                override fun onError(exception: MsalException) {
                    callback(null, exception)
                }
            }
        )
    }

    /**
     * Initializes MSAL PublicClientApplication for single account mode with configuration from a config json file
     */
    fun initializeSingleAccountMSAL(context: Context, callback: (IPublicClientApplication?, MsalException?) -> Unit) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            CONFIG_FILE,
            object : IPublicClientApplication.ApplicationCreatedListener {
                override fun onCreated(application: IPublicClientApplication) {
                    mSingleAccountPCA = application
                    callback(mPCA, null)
                }

                override fun onError(exception: MsalException) {
                    callback(null, exception)
                }
            }
        )
    }
}
