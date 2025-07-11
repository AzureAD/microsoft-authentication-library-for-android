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
import android.app.Activity;
import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import java.util.Arrays;
import java.util.List;

public class TokenAcquisition {
    private IPublicClientApplication mPCA;

    /**
     * Acquires token interactively, launching browser for user authentication if necessary.
     * This is the recommended method for initial sign-in in multiple account mode.
     */
    public void acquireTokenInteractively(
            Activity activity,
            List<String> scopesList,
            final TokenCallback callback) {
        // Build parameters using the modern Parameters-based API
        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
            .withScopes(scopesList)
            .startAuthorizationFromActivity(activity)
            .withCallback(new AuthenticationCallback() {
                @Override
                public void onSuccess(IAuthenticationResult authenticationResult) {
                    callback.onComplete(authenticationResult, null);
                }

                @Override
                public void onError(MsalException exception) {
                    callback.onComplete(null, exception);
                }

                @Override
                public void onCancel() {
                    // Handle the cancellation
                    System.out.println("User cancelled the authentication");
                }
            })
            .build();

        // Acquire token using the parameters
        mPCA.acquireToken(parameters);
    }

    /**
     * Example usage with Microsoft Graph scopes
     */
    public void exampleUsage(Activity activity) {
        String[] graphScopes = new String[]{"User.Read"};
        
        acquireTokenInteractively(activity, graphScopes, new TokenCallback() {
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
     * Callback interface for token operations
     */
    public interface TokenCallback {
        void onComplete(IAuthenticationResult result, MsalException exception);
    }
}
