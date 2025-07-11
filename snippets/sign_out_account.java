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
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

public class AccountSignOut {

    /**
     * Signs out and removes the account from MSAL cache in multiple account mode
     */
    public void signOutMultipleAccount(
            IMultipleAccountPublicClientApplication pca,
            IAccount account,
            final SignOutCallback callback) {
        pca.removeAccount(account, new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
            @Override
            public void onRemoved() {
                // Account successfully removed
                callback.onComplete(null);
            }

            @Override
            public void onError(MsalException exception) {
                // Failed to remove account
                callback.onComplete(exception);
            }
        });
    }

    /**
     * Signs out the current account in single account mode
     */
    public void signOutSingleAccount(
            ISingleAccountPublicClientApplication pca,
            final SignOutCallback callback) {
        pca.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                // Sign out successful
                callback.onComplete(null);
            }

            @Override
            public void onError(MsalException exception) {
                // Failed to sign out
                callback.onComplete(exception);
            }
        });
    }

    /**
     * Example usage for multiple account mode
     */
    public void exampleMultipleAccountUsage(IMultipleAccountPublicClientApplication pca, IAccount account) {
        signOutMultipleAccount(pca, account, new SignOutCallback() {
            @Override
            public void onComplete(MsalException exception) {
                if (exception == null) {
                    System.out.println("Account successfully signed out");
                    // Update UI, clear account-specific data
                } else {
                    System.out.println("Failed to sign out: " + exception.getMessage());
                    // Handle the error
                }
            }
        });
    }

    /**
     * Example usage for single account mode
     */
    public void exampleSingleAccountUsage(ISingleAccountPublicClientApplication pca) {
        signOutSingleAccount(pca, new SignOutCallback() {
            @Override
            public void onComplete(MsalException exception) {
                if (exception == null) {
                    System.out.println("Successfully signed out");
                    // Update UI to signed-out state
                } else {
                    System.out.println("Failed to sign out: " + exception.getMessage());
                    // Handle the error
                }
            }
        });
    }

    /**
     * Callback interface for sign out operations
     */
    public interface SignOutCallback {
        void onComplete(MsalException exception);
    }
}
