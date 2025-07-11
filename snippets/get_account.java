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
import java.util.List;

public class AccountManagement {

    /**
     * Gets all accounts for multiple account mode applications
     */
    public void getAccounts(
            IMultipleAccountPublicClientApplication pca,
            final AccountsCallback callback) {
        pca.getAccounts(new IMultipleAccountPublicClientApplication.LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(List<IAccount> result) {
                callback.onComplete(result, null);
            }

            @Override
            public void onError(MsalException exception) {
                callback.onComplete(null, exception);
            }
        });
    }

    /**
     * Gets the current account for single account mode applications
     */
    public void getCurrentAccount(
            ISingleAccountPublicClientApplication pca,
            final CurrentAccountCallback callback) {
        pca.getCurrentAccount(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(IAccount account) {
                callback.onComplete(account, null);
            }

            @Override
            public void onAccountChanged(IAccount priorAccount, IAccount currentAccount) {
                // Handle account changes
                callback.onComplete(currentAccount, null);
            }

            @Override
            public void onError(MsalException exception) {
                callback.onComplete(null, exception);
            }
        });
    }

    /**
     * Example usage for multiple account mode
     */
    public void exampleMultipleAccountUsage(IMultipleAccountPublicClientApplication pca) {
        getAccounts(pca, new AccountsCallback() {
            @Override
            public void onComplete(List<IAccount> accounts, MsalException exception) {
                if (accounts != null) {
                    if (accounts.isEmpty()) {
                        System.out.println("No accounts found");
                        // Handle no accounts scenario
                    } else {
                        // Process the accounts
                        for (IAccount account : accounts) {
                            System.out.println("Account: " + account.getUsername());
                        }
                    }
                } else if (exception != null) {
                    System.out.println("Failed to get accounts: " + exception.getMessage());
                    // Handle the error
                }
            }
        });
    }

    /**
     * Example usage for single account mode
     */
    public void exampleSingleAccountUsage(ISingleAccountPublicClientApplication pca) {
        getCurrentAccount(pca, new CurrentAccountCallback() {
            @Override
            public void onComplete(IAccount account, MsalException exception) {
                if (account != null) {
                    System.out.println("Current account: " + account.getUsername());
                    // Use the account
                } else if (exception != null) {
                    System.out.println("Failed to get current account: " + exception.getMessage());
                    // Handle the error
                } else {
                    System.out.println("No account signed in");
                    // Handle no account scenario
                }
            }
        });
    }

    /**
     * Callback interface for retrieving multiple accounts
     */
    public interface AccountsCallback {
        void onComplete(List<IAccount> accounts, MsalException exception);
    }

    /**
     * Callback interface for retrieving current account
     */
    public interface CurrentAccountCallback {
        void onComplete(IAccount account, MsalException exception);
    }
}
