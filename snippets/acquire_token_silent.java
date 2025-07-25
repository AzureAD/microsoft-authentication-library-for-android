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
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates how to acquire tokens silently (no interactive flow).
 * 
 * Includes both synchronous (background thread) and asynchronous (main thread) patterns.
 * Use this snippet in both single and multiple account modes.
 */
public class SilentTokenAcquisition {
    private IPublicClientApplication mPCA; // Use ISingleAccountPublicClientApplication or IMultipleAccountPublicClientApplication as needed

    /**
     * IMPORTANT: This method must be called from a background thread.
     * For main thread operations, use acquireTokenSilentAsync instead.
     * 
     * Attempts to acquire a token silently from the cache.
     */
    public IAuthenticationResult acquireTokenSilent(IAccount account, List<String> scopesList)
            throws MsalException, InterruptedException {
        // Build parameters using the modern Parameters-based API
        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
            .withScopes(scopesList)
            .forAccount(account)
            .build();

        // Synchronously acquire token - MUST be called from background thread
        return mPCA.acquireTokenSilent(parameters);
    }

    /**
     * Safe to call from main thread. Uses asynchronous token acquisition.
     */
    public void acquireTokenSilentAsync(
            IAccount account,
            List<String> scopesList,
            final TokenCallback callback) {
        // Build parameters using the modern Parameters-based API
        // Notice that it incudes a callback for asynchronous operations
        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
            .withScopes(scopesList)
            .forAccount(account)
            .withCallback(new SilentAuthenticationCallback() {
                @Override
                public void onSuccess(IAuthenticationResult result) { // Token acquisition successful, handle result
                    callback.onComplete(result, null);
                }

                @Override
                public void onError(MsalException exception) { // Token acquisition failed, handle the error
                    callback.onComplete(null, exception);
                }
            })
            .build();

        // Asynchronously acquire token - safe to call from any thread
        mPCA.acquireTokenSilentAsync(parameters);
    }

    /**
     * Example usage showing both synchronous and asynchronous patterns
     */
    public void exampleUsage(IAccount account) {
        mPCA = /* Initialize your IPublicClientApplication instance here, can be multiple or single account mode.*/;
        List<String> graphScopes = Collections.singletonList("User.Read");

        // Example 1: Background thread usage (synchronous)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Must be called from background thread
                    IAuthenticationResult result = acquireTokenSilent(account, graphScopes);
                    String accessToken = result.getAccessToken();
                    // Use the access token
                } catch (Exception e) {
                    // Handle exceptions
                    System.out.println("Failed to acquire token silently: " + e.getMessage());
                    // Fall back to interactive authentication
                }
            }
        }).start();

        // Example 2: Main thread usage (asynchronous)
        acquireTokenSilentAsync(account, graphScopes, new TokenCallback() {
            @Override
            public void onComplete(IAuthenticationResult result, MsalException exception) {
                if (result != null) {
                    String accessToken = result.getAccessToken();
                    // Use the access token
                } else if (exception != null) {
                    System.out.println("Failed to acquire token silently: " + exception.getMessage());
                    // Fall back to interactive authentication
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
