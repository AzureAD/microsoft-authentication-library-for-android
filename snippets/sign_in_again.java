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
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.Prompt;
import java.util.Arrays;

/**
 * Demonstrates how to sign in again (reauthenticate existing user) using MSAL's Parameters-based API in SINGLE ACCOUNT MODE.
 *
 * This is useful when you want to force a fresh sign-in, ignoring any existing sessions.
 * Use signInAgain for single account mode. For multiple account mode, use acquireToken with .withPrompt(Prompt.LOGIN) to force authentication.
 */
public class SignInAgainHelper {

    private ISingleAccountPublicClientApplication mPCA;

    /**
     * Signs in the user again interactively in single account mode.
     * This will prompt for credentials even if there's an existing session.
     */
    public void signInAgain(Activity activity, List<String> scopes, final SignInAgainCallback callback) {
        SignInParameters parameters = SignInParameters.builder()
            .withScopes(scopes)
            .withActivity(activity)
            .withCallback(new AuthenticationCallback() {
                @Override
                public void onCancel() {
                    // Handle cancellation
                }
                @Override
                public void onSuccess(IAuthenticationResult authenticationResult) {
                    callback.onComplete(authenticationResult, null);
                }
                @Override
                public void onError(MsalException exception) {
                    callback.onComplete(null, exception);
                }
            })
            .build();

        mPCA.signInAgain(parameters);
    }

    /**
     * Example usage for single account mode
     */
    public void exampleSingleAccountUsage(Activity activity) {
        mPCA = /* Initialize your ISingleAccountPublicClientApplication instance here */;
        List<String> graphScopes = Collections.singletonList("User.Read");
        signInAgain(activity, graphScopes, new SignInAgainCallback() {
            @Override
            public void onComplete(IAuthenticationResult result, MsalException exception) {
                if (result != null) {
                    String accessToken = result.getAccessToken();
                    // Use the access token
                } else if (exception != null) {
                    System.out.println("Failed to sign in again: " + exception.getMessage());
                    // Handle the error
                }
            }
        });
    }

    /**
     * Callback interface for sign in again operations
     */
    public interface SignInAgainCallback {
        void onComplete(IAuthenticationResult result, MsalException exception);
    }
}
