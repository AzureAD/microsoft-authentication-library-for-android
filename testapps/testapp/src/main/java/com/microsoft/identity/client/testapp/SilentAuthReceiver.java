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
package com.microsoft.identity.client.testapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

import java.util.Arrays;
import java.util.List;

/**
 * BroadcastReceiver that triggers acquireTokenSilent from a background context.
 *
 * This runs at PROCESS_STATE_RECEIVER priority, which does NOT elevate the
 * Broker process enough to get a dozable-allow firewall rule. This simulates
 * the production scenario where Outlook handles an FCM push in the background
 * and calls the Broker via OneAuth.
 *
 * Usage:
 *   adb shell am broadcast \
 *     -a com.microsoft.identity.client.testapp.SILENT_AUTH \
 *     -n com.msft.identity.client.sample.local/com.microsoft.identity.client.testapp.SilentAuthReceiver \
 *     --es scopes "https://graph.microsoft.com/.default"
 */
public class SilentAuthReceiver extends BroadcastReceiver {

    private static final String TAG = "SilentAuthReceiver";
    public static final String ACTION_SILENT_AUTH =
            "com.microsoft.identity.client.testapp.SILENT_AUTH";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.w(TAG, "=== SilentAuthReceiver triggered (PROCESS_STATE_RECEIVER) ===");

        final String scopes = intent.getStringExtra("scopes");
        final String scopeString = (scopes != null) ? scopes : "https://graph.microsoft.com/.default";

        Log.w(TAG, "Scopes: " + scopeString);

        // Use goAsync() to extend the receiver's lifecycle beyond the 10s limit
        final PendingResult pendingResult = goAsync();

        // Create PCA with default config (uses broker)
        final int configResourceId = Constants.getResourceIdFromConfigFile(Constants.ConfigFile.DEFAULT);

        PublicClientApplication.create(context.getApplicationContext(),
                configResourceId,
                new PublicClientApplication.ApplicationCreatedListener() {
                    @Override
                    public void onCreated(IPublicClientApplication application) {
                        // Force-disable powerOptCheck so the Broker attempts the real
                        // network call instead of proactively blocking with a Doze check.
                        // This matches production Authenticator behavior (which doesn't
                        // have powerOptCheckEnabled set by OneAuth).
                        application.getConfiguration().setPowerOptCheckEnabled(false);

                        Log.w(TAG, "PCA created, mode: " +
                                (application instanceof ISingleAccountPublicClientApplication
                                        ? "SingleAccount" : "MultipleAccount"));
                        Log.w(TAG, "powerOptCheckEnabled forced to: " +
                                application.getConfiguration().isPowerOptCheckForEnabled());
                        loadAccountsAndAcquire(application, scopeString, pendingResult);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e(TAG, "Failed to create PCA: " + exception.getMessage(), exception);
                        pendingResult.finish();
                    }
                });
    }

    private void loadAccountsAndAcquire(
            final IPublicClientApplication app,
            final String scopeString,
            final PendingResult pendingResult) {

        if (app instanceof ISingleAccountPublicClientApplication) {
            final ISingleAccountPublicClientApplication singleApp =
                    (ISingleAccountPublicClientApplication) app;
            try {
                final IAccount account = singleApp.getCurrentAccount().getCurrentAccount();
                if (account == null) {
                    Log.e(TAG, "No signed-in account found in single-account mode.");
                    pendingResult.finish();
                    return;
                }
                doSilentAuth(app, account, scopeString, pendingResult);
            } catch (Exception e) {
                Log.e(TAG, "Error loading account: " + e.getMessage(), e);
                pendingResult.finish();
            }
        } else if (app instanceof IMultipleAccountPublicClientApplication) {
            final IMultipleAccountPublicClientApplication multiApp =
                    (IMultipleAccountPublicClientApplication) app;
            multiApp.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
                @Override
                public void onTaskCompleted(List<IAccount> result) {
                    if (result == null || result.isEmpty()) {
                        Log.e(TAG, "No accounts found in multiple-account mode.");
                        pendingResult.finish();
                        return;
                    }
                    Log.w(TAG, "Found " + result.size() + " account(s). Using first.");
                    doSilentAuth(app, result.get(0), scopeString, pendingResult);
                }

                @Override
                public void onError(MsalException exception) {
                    Log.e(TAG, "Error loading accounts: " + exception.getMessage(), exception);
                    pendingResult.finish();
                }
            });
        }
    }

    private void doSilentAuth(
            final IPublicClientApplication app,
            final IAccount account,
            final String scopeString,
            final PendingResult pendingResult) {

        Log.w(TAG, "Calling acquireTokenSilent for account: " + account.getUsername());
        Log.w(TAG, "Authority: " + account.getAuthority());

        final AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .withScopes(Arrays.asList(scopeString.toLowerCase().split(" ")))
                .forceRefresh(true)  // Force network call to eSTS (no cache)
                .withCallback(new AuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        Log.w(TAG, "=== SUCCESS === Token acquired silently!");
                        Log.w(TAG, "Access token (first 20 chars): " +
                                authenticationResult.getAccessToken().substring(0, 20) + "...");
                        pendingResult.finish();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e(TAG, "=== FAILED === " + exception.getClass().getSimpleName());
                        Log.e(TAG, "Error code: " + exception.getErrorCode());
                        Log.e(TAG, "Message: " + exception.getMessage());
                        if (exception.getCause() != null) {
                            Log.e(TAG, "Cause: " + exception.getCause().getClass().getSimpleName()
                                    + " - " + exception.getCause().getMessage());
                        }
                        pendingResult.finish();
                    }

                    @Override
                    public void onCancel() {
                        Log.w(TAG, "=== CANCELLED ===");
                        pendingResult.finish();
                    }
                })
                .build();

        app.acquireTokenSilentAsync(parameters);
    }
}
