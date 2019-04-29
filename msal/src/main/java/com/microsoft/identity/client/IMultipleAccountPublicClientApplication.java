//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.common.internal.dto.AccountRecord;

import java.util.List;

/**
 * An interface that contains list of operations that are available when MSAL is in 'multiple account' mode.
 * - This mode allows an application to make API calls with more than one accounts.
 * - The application will only be able to retrieve/remove accounts that have been used to acquire token interactively in this application
 * - API calls' scope is limited to 'the calling app'. (i.e. removeAccount() will not remove credentials of the same account in other apps).
 *
 * This is MSAL's default mode.
 * */
public interface IMultipleAccountPublicClientApplication extends IPublicClientApplication{
    /**
     * Asynchronously returns a List of {@link IAccount} objects for which this application has RefreshTokens.
     *
     * @param callback The callback to notify once this action has finished.
     */
    void getAccounts(@NonNull final AccountsLoadedCallback callback);

    /**
     * Returns the IAccount object matching the supplied home_account_id.
     *
     * @param homeAccountIdentifier The home_account_id of the sought IAccount.
     * @param authority             The authority of the sought IAccount.
     * @return The IAccount stored in the cache or null, if no such matching entry exists.
     */
    IAccount getAccount(@NonNull final String homeAccountIdentifier, @Nullable final String authority);

    /**
     * Removes the Account and Credentials (tokens) for the supplied IAccount.
     *
     * @param account The IAccount whose entry and associated tokens should be removed.
     * @return True, if the account was removed. False otherwise.
     */
    void removeAccount(@Nullable final IAccount account, final AccountsRemovedCallback callback);

    /**
     * Listener callback for asynchronous loading of msal IAccount accounts.
     */
    interface AccountsLoadedCallback {
        /**
         * Called once Accounts have been loaded from the cache.
         *
         * @param accounts The accounts in the cache.
         */
        void onAccountsLoaded(List<IAccount> accounts);
    }

    /**
     * Listener callback for asynchronous loading of broker AccountRecord accounts.
     */
    interface BrokerAccountsLoadedCallback {
        /**
         * Called once Accounts have been loaded from the broker.
         * @param accountRecords The accountRecords in broker.
         */
        void onAccountsLoaded(List<AccountRecord> accountRecords);
    }

    /**
     * Listener callback for asynchronous loading of msal IAccount accounts.
     */
    interface AccountsRemovedCallback {
        /**
         * Called once Accounts have been removed from the cache.
         *
         * @param isSuccess true if the account is successfully removed.
         */
        void onAccountsRemoved(Boolean isSuccess);
    }
}
