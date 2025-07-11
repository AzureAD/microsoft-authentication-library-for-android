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
import android.content.Context;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

public class MSALInitialization {
    private static final String CONFIG_FILE = "auth_config.json";
    private IPublicClientApplication mPCA;

    /**
     * Initializes MSAL PublicClientApplication with configuration from auth_config.json
     */
    public void initializeMSAL(Context context, final InitializationCallback callback) {
        // Create PCA from config file
        PublicClientApplication.createMultipleAccountPublicClientApplication(
            context,
                CONFIG_FILE,
            new IPublicClientApplication.ApplicationCreatedListener() {
                @Override
                public void onCreated(IPublicClientApplication application) {
                    mPCA = application;
                    callback.onComplete(mPCA, null);
                }

                @Override
                public void onError(MsalException exception) {
                    callback.onComplete(null, exception);
                }
            }
        );
    }

    /**
     * For single account mode, use this initialization instead
     */
    public void initializeSingleAccountMSAL(Context context, final InitializationCallback callback) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context,
            CONFIG_FILE,
            new IPublicClientApplication.ApplicationCreatedListener() {
                @Override
                public void onCreated(IPublicClientApplication application) {
                    mPCA = application;
                    callback.onComplete(mPCA, null);
                }

                @Override
                public void onError(MsalException exception) {
                    callback.onComplete(null, exception);
                }
            }
        );
    }

    /**
     * Callback interface for MSAL initialization
     */
    public interface InitializationCallback {
        void onComplete(IPublicClientApplication application, MsalException exception);
    }
}
