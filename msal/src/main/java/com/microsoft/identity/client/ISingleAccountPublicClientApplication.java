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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;

/**
 * An interface that contains list of operations that are available when MSAL is in 'single account' mode.
 * - In this mode, the user can 'sign-in' an account to the device.
 * - Once an account is 'signed-in', every app on the device will be able to retrieve this account, and use them to silently perform API calls.
 * - If the user wants to acquire a token for another account, the previous account must be removed from the device first through globalSignOut().
 * Otherwise, the operation will fail.
 * <p>
 * Currently, this mode is only set when the device is registered as 'shared'.
 */
public interface ISingleAccountPublicClientApplication extends IPublicClientApplication {
    /**
     * Gets the current account and notify if the current account changes.
     * This method must be called whenever the application is resumed or prior to running a scheduled background operation.
     *
     * @param listener a callback to be invoked when the operation finishes.
     */
    void getCurrentAccount(@NonNull final CurrentAccountCallback listener);

    /**
     * Removes the Account and Credentials (tokens) of the account that is currently signed into the device.
     *
     * @param callback a callback to be invoked when the operation finishes.
     */
    void removeCurrentAccount(@NonNull final TaskCompletedCallbackWithError<Boolean, Exception> callback);

    /**
     * Callback for asynchronous loading of the msal IAccount account.
     */
    interface CurrentAccountCallback {
        /**
         * Invoked when the account is loaded.
         *
         * @param activeAccount the signed-in account. This could be nil.
         */
        void onAccountLoaded(@Nullable final IAccount activeAccount);

        /**
         * Invoked when signed-in account is changed after the application resumes, or prior to running a scheduled background operation.
         * The calling app is responsible for keeping track of this account and cleaning its states if the account changes.
         *
         * @param priorAccount   the previous signed-in account. This could be nil.
         * @param currentAccount the current signed-in account. This could be nil.
         */
        void onAccountChanged(@Nullable final IAccount priorAccount, @Nullable final IAccount currentAccount);

        /**
         * Invoked when the account failed to load.
         *
         * @param exception the exception object.
         */
        void onError(@NonNull final Exception exception);
    }
}
