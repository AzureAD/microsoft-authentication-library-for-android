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
import com.microsoft.identity.client.exception.MsalException;

/**
 * Snippet showing how to remove an account from the msal cache in multiple account mode.
 * 
 * Use removeAccount for sign-out in MULTIPLE ACCOUNT MODE.
 * Do NOT use this in single account mode. For single account mode, use signOut.
 */
public class AccountRemove {

    IMultipleAccountPublicClientApplication mPCA;

    /**
     * Removes the account from MSAL cache in multiple account mode.
     */
    public void removeAccount(
            IAccount account,
            final RemoveAccountCallback callback) {
        mPCA.removeAccount(account, new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
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
     * Example usage for multiple account mode
     */
    public void exampleMultipleAccountUsage(IAccount account) {
        mPCA = /* Initialize your IMultipleAccountPublicClientApplication instance here */;
        removeAccount(account, new RemoveAccountCallback() {
            @Override
            public void onComplete(MsalException exception) {
                if (exception == null) {
                    System.out.println("Account successfully removed");
                    // Update UI, clear account-specific data
                } else {
                    System.out.println("Failed to remove account: " + exception.getMessage());
                    // Handle the error
                }
            }
        });
    }

    /**
     * Callback interface for remove account operations
     */
    public interface RemoveAccountCallback {
        void onComplete(MsalException exception);
    }
}
