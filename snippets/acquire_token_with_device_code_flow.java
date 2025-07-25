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

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import androidx.annotation.NonNull;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Snippet showing how to use device code flow to acquire tokens.
 * 
 * IMPORTANT: Device Code Flow is not recommended due to security concerns in the industry.
 * Only use this method in niche scenarios where devices lack input methods necessary for interactive authentication.
 * For standard authentication scenarios, use acquireToken (for multiple account mode) or signIn (for single account mode).
 */
public class DeviceCodeFlowTokenAcquisition {
    private IPublicClientApplication mPCA; // Use ISingleAccountPublicClientApplication or IMultipleAccountPublicClientApplication as needed

    /**
     * Acquires token using Device Code Flow. This should only be used in specific scenarios
     * where the device cannot handle interactive authentication.
     */
    public void acquireTokenWithDeviceCode(List<String> scopes, final DeviceCodeCallback callback) {
        mPCA.acquireTokenWithDeviceCode(
            scopes,
            new IPublicClientApplication.DeviceCodeFlowCallback() {
                @Override
                public void onUserCodeReceived(@NonNull String deviceCode,
                                             @NonNull String verificationUri,
                                             @NonNull String message,
                                             @NonNull Date expiresOn) { // Display the device code and instructions to the user
                    callback.onDeviceCodeReceived(message); // Show the message to the user through app ui
                }

                @Override
                public void onTokenReceived(@NonNull IAuthenticationResult result) { // Token acquisition successful, handle result
                    callback.onComplete(result, null);
                }

                @Override
                public void onError(@NonNull MsalException exception) { // Token acquisition failed, handle error
                    callback.onComplete(null, exception);
                }
            }
        );
    }

    /**
     * Example usage with Microsoft Graph scopes
     */
    public void exampleUsage() {
        mPCA = /* Initialize your IPublicClientApplication instance here */;
        List<String> graphScopes = Collections.singletonList("User.Read");
        
        acquireTokenWithDeviceCode(graphScopes, new DeviceCodeCallback() {
            @Override
            public void onDeviceCodeReceived(String message) {
                // Display message to user. This includes the device code and instructions
                System.out.println(message);
            }

            @Override
            public void onComplete(IAuthenticationResult result, MsalException exception) {
                if (result != null) {
                    // Use the access token
                    String accessToken = result.getAccessToken();
                    // Make API calls with the token
                } else if (exception != null) {
                    // Handle the exception
                    System.out.println("Error acquiring token: " + exception.getMessage());
                }
            }
        });
    }

    /**
     * Callback interface for Device Code Flow operations
     */
    public interface DeviceCodeCallback {
        void onDeviceCodeReceived(String message);
        void onComplete(IAuthenticationResult result, MsalException exception);
    }
}
