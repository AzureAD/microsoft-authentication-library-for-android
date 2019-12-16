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

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.AsyncResult;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.IShareSingleSignOnState;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.controllers.LoadAccountCommand;
import com.microsoft.identity.common.internal.controllers.RemoveAccountCommand;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.migration.AdalMigrationAdapter;
import com.microsoft.identity.common.internal.migration.TokenMigrationCallback;
import com.microsoft.identity.common.internal.migration.TokenMigrationUtility;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.ResultFuture;
import com.microsoft.identity.common.internal.eststelemetry.PublicApiId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.client.internal.MsalUtils.throwOnMainThread;

public class MultipleAccountPublicClientApplication extends PublicClientApplication
        implements IMultipleAccountPublicClientApplication {
    private static final String TAG = MultipleAccountPublicClientApplication.class.getSimpleName();

    protected MultipleAccountPublicClientApplication(@NonNull PublicClientApplicationConfiguration config,
                                                     @Nullable final String clientId,
                                                     @Nullable final String authority) {
        super(config, clientId, authority);
    }

    @Override
    public IAuthenticationResult acquireTokenSilent(@NonNull String[] scopes, @NonNull IAccount account, @NonNull String authority) throws MsalException, InterruptedException {
        return acquireTokenSilentSyncInternal(scopes, authority, account, false, PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_SCOPES_ACCOUNT_AUTHORITY);
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @NonNull final IAccount account,
                                        @NonNull final String authority,
                                        @NonNull final SilentAuthenticationCallback callback) {
        final AcquireTokenSilentParameters acquireTokenSilentParameters = buildAcquireTokenSilentParameters(
                scopes,
                account,
                authority,
                false,
                null, // claimsRequest
                callback
        );

        acquireTokenSilentAsyncInternal(acquireTokenSilentParameters, PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_SCOPES_ACCOUNT_AUTHORITY_CALLBACK);
    }

    /**
     * Asynchronously returns a List of {@link IAccount} objects for which this application has RefreshTokens.
     *
     * @param callback The callback to notify once this action has finished.
     */
    @Override
    public void getAccounts(@NonNull final LoadAccountsCallback callback) {
        getAccountsInternal(callback, PublicApiId.MULTIPLE_ACCOUNT_PCA_GET_ACCOUNTS_WITH_CALLBACK);
    }


    /**
     * Asynchronously returns a List of {@link IAccount} objects for which this application has RefreshTokens.
     *
     * @param callback The callback to notify once this action has finished.
     */
    private void getAccountsInternal(@NonNull final LoadAccountsCallback callback,
                                    @NonNull final String publicApiId) {
        final String methodName = ":getAccounts";
        final List<ICacheRecord> accounts =
                mPublicClientConfiguration
                        .getOAuth2TokenCache()
                        .getAccountsWithAggregatedAccountData(
                                null, // * wildcard
                                mPublicClientConfiguration.getClientId()
                        );

        final Handler handler;

        if (null != Looper.myLooper() && Looper.getMainLooper() != Looper.myLooper()) {
            handler = new Handler(Looper.myLooper());
        } else {
            handler = new Handler(Looper.getMainLooper());
        }

        if (accounts.isEmpty()) {
            // Create the SharedPreferencesFileManager for the legacy accounts/credentials
            final IStorageHelper storageHelper = new StorageHelper(mPublicClientConfiguration.getAppContext());
            final ISharedPreferencesFileManager sharedPreferencesFileManager =
                    new SharedPreferencesFileManager(
                            mPublicClientConfiguration.getAppContext(),
                            "com.microsoft.aad.adal.cache",
                            storageHelper
                    );

            // Load the old TokenCacheItems as key/value JSON
            final Map<String, String> credentials = sharedPreferencesFileManager.getAll();

            final Map<String, String> redirects = new HashMap<>();
            redirects.put(
                    mPublicClientConfiguration.mClientId, // Our client id
                    mPublicClientConfiguration.mRedirectUri // Our redirect uri
            );

            new TokenMigrationUtility<MicrosoftAccount, MicrosoftRefreshToken>()._import(
                    new AdalMigrationAdapter(
                            mPublicClientConfiguration.getAppContext(),
                            redirects,
                            false
                    ),
                    credentials,
                    (IShareSingleSignOnState<MicrosoftAccount, MicrosoftRefreshToken>) mPublicClientConfiguration.getOAuth2TokenCache(),
                    new TokenMigrationCallback() {
                        @Override
                        public void onMigrationFinished(int numberOfAccountsMigrated) {
                            final String extendedMethodName = ":onMigrationFinished";
                            com.microsoft.identity.common.internal.logging.Logger.info(
                                    TAG + methodName + extendedMethodName,
                                    "Migrated [" + numberOfAccountsMigrated + "] accounts"
                            );
                        }
                    }
            );
        } else {
            // The cache contains items - mark migration as complete
            new AdalMigrationAdapter(
                    mPublicClientConfiguration.getAppContext(),
                    null, // unused for this path
                    false
            ).setMigrationStatus(true);
        }

        try {
            final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());
            final LoadAccountCommand loadAccountCommand = new LoadAccountCommand(
                    params,
                    MSALControllerFactory.getAllControllers(
                            mPublicClientConfiguration.getAppContext(),
                            params.getAuthority(),
                            mPublicClientConfiguration
                    ),
                    getLoadAccountsCallback(callback)
            );

            loadAccountCommand.setPublicApiId(publicApiId);
            CommandDispatcher.submitSilent(loadAccountCommand);
        } catch (final MsalClientException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(e);
                }
            });
        }
    }

    @Override
    public List<IAccount> getAccounts() throws InterruptedException, MsalException {

        throwOnMainThread("getAccounts");

        final ResultFuture<AsyncResult<List<IAccount>>> future = new ResultFuture<>();

        getAccountsInternal(new LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(List<IAccount> result) {
                future.setResult(new AsyncResult<List<IAccount>>(result, null));
            }

            @Override
            public void onError(MsalException exception) {
                future.setResult(new AsyncResult<List<IAccount>>(null, exception));
            }
        }, PublicApiId.MULTIPLE_ACCOUNT_PCA_GET_ACCOUNTS);

        final AsyncResult<List<IAccount>> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
        }
    }

    /**
     * Retrieve the IAccount object matching the identifier.
     * The identifier could be homeAccountIdentifier, localAccountIdentifier or username.
     *
     * @param identifier String of the identifier
     * @param callback   The callback to notify once this action has finished.
     */
    @Override
    public void getAccount(@NonNull final String identifier,
                           @NonNull final GetAccountCallback callback) {
        getAccountInternal(identifier, callback, PublicApiId.MULTIPLE_ACCOUNT_PCA_GET_ACCOUNT_WITH_IDENTIFIER_CALLBACK);
    }

    /**
     * Retrieve the IAccount object matching the identifier.
     * The identifier could be homeAccountIdentifier, localAccountIdentifier or username.
     *
     * @param identifier String of the identifier
     * @param callback   The callback to notify once this action has finished.
     */
    private void getAccountInternal(@NonNull final String identifier,
                                    @NonNull final GetAccountCallback callback,
                                    @NonNull final String publicApiId) {
        final String methodName = ":getAccount";

        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Get account with the identifier."
        );

        try {
            final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());
            final LoadAccountCommand loadAccountCommand = new LoadAccountCommand(
                    params,
                    MSALControllerFactory.getAllControllers(
                            mPublicClientConfiguration.getAppContext(),
                            params.getAuthority(),
                            mPublicClientConfiguration
                    ),
                    new CommandCallback<List<ICacheRecord>, BaseException>() {
                        @Override
                        public void onTaskCompleted(final List<ICacheRecord> result) {
                            if (null == result || result.size() == 0) {
                                com.microsoft.identity.common.internal.logging.Logger.verbose(
                                        TAG + methodName,
                                        "No account found.");
                                callback.onTaskCompleted(null);
                            } else {
                                // First, transform the result into IAccount + TenantProfile form
                                final List<IAccount>
                                        accounts = AccountAdapter.adapt(result);

                                final String trimmedIdentifier = identifier.trim();

                                // Evaluation precedence...
                                //     1. home_account_id
                                //     2. local_account_id
                                //     3. username
                                //     4. Give up.

                                final AccountMatcher accountMatcher = new AccountMatcher(
                                        homeAccountMatcher,
                                        localAccountMatcher,
                                        usernameMatcher
                                );

                                for (final IAccount account : accounts) {
                                    if (accountMatcher.matches(trimmedIdentifier, account)) {
                                        callback.onTaskCompleted(account);
                                        return;
                                    }
                                }

                                callback.onTaskCompleted(null);
                            }
                        }

                        @Override
                        public void onError(final BaseException exception) {
                            com.microsoft.identity.common.internal.logging.Logger.error(
                                    TAG + methodName,
                                    exception.getMessage(),
                                    exception
                            );
                            callback.onError(MsalExceptionAdapter.msalExceptionFromBaseException(exception));
                        }

                        @Override
                        public void onCancel(){

                        }
                    }
            );

            loadAccountCommand.setPublicApiId(publicApiId);
            CommandDispatcher.submitSilent(loadAccountCommand);
        } catch (final MsalClientException e) {
            com.microsoft.identity.common.internal.logging.Logger.error(
                    TAG + methodName,
                    e.getMessage(),
                    e
            );
            callback.onError(e);
        }
    }

    @Override
    public IAccount getAccount(@NonNull String identifier) throws InterruptedException, MsalException {

        throwOnMainThread("getAccount");

        final ResultFuture<AsyncResult<IAccount>> future = new ResultFuture<>();

        getAccountInternal(identifier, new GetAccountCallback() {
            @Override
            public void onTaskCompleted(IAccount result) {
                future.setResult(new AsyncResult<IAccount>(result, null));
            }

            @Override
            public void onError(MsalException exception) {
                future.setResult(new AsyncResult<IAccount>(null, exception));
            }
        }, PublicApiId.MULTIPLE_ACCOUNT_PCA_GET_ACCOUNT_WITH_IDENTIFIER);

        AsyncResult<IAccount> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
        }

    }

    @Override
    public void removeAccount(@Nullable final IAccount account,
                              @NonNull final RemoveAccountCallback callback) {
        removeAccountInternal(account, callback, PublicApiId.MULTIPLE_ACCOUNT_PCA_REMOVE_ACCOUNT_WITH_ACCOUNT_CALLBACK);
    }

    private void removeAccountInternal(@Nullable final IAccount account,
                                       @NonNull final RemoveAccountCallback callback,
                                       @NonNull final String publicApiId) {
        // First, cast the input IAccount to a MultiTenantAccount
        final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) account;

        //create the parameter
        if (null == multiTenantAccount) {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                    TAG,
                    "Requisite IAccount or IAccount fields were null. Insufficient criteria to remove IAccount."
            );

            callback.onError(new MsalClientException(MsalClientException.INVALID_PARAMETER));
            return;
        }

        final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());

        // TODO Clean this up, only the cache should make these records...
        // The broker strips these properties out of this object to hit the cache
        // Refactor this out...
        final AccountRecord requestAccountRecord = new AccountRecord();
        requestAccountRecord.setEnvironment(multiTenantAccount.getEnvironment());
        requestAccountRecord.setHomeAccountId(multiTenantAccount.getHomeAccountId());
        params.setAccount(requestAccountRecord);

        try {
            final RemoveAccountCommand removeAccountCommand = new RemoveAccountCommand(
                    params,
                    MSALControllerFactory.getAllControllers(
                            mPublicClientConfiguration.getAppContext(),
                            params.getAuthority(),
                            mPublicClientConfiguration
                    ),
                    new CommandCallback<Boolean, BaseException>() {
                        @Override
                        public void onError(BaseException error) {
                            callback.onError(MsalExceptionAdapter.msalExceptionFromBaseException(error));
                        }

                        @Override
                        public void onTaskCompleted(Boolean success) {
                            callback.onRemoved();
                        }

                        @Override
                        public void onCancel(){
                            //Do nothing
                        }
                    }
            );

            removeAccountCommand.setPublicApiId(publicApiId);
            CommandDispatcher.submitSilent(removeAccountCommand);

        } catch (final MsalClientException e) {
            callback.onError(e);
        }
    }

    @Override
    public boolean removeAccount(@Nullable IAccount account) throws MsalException, InterruptedException {

        final ResultFuture<AsyncResult<Boolean>> future = new ResultFuture();
        removeAccountInternal(account,
                new RemoveAccountCallback() {
                    @Override
                    public void onRemoved() {
                        future.setResult(new AsyncResult<Boolean>(true, null));
                    }

                    @Override
                    public void onError(@NonNull MsalException exception) {
                        future.setResult(new AsyncResult<Boolean>(false, exception));
                    }
                }, PublicApiId.MULTIPLE_ACCOUNT_PCA_REMOVE_ACCOUNT_WITH_ACCOUNT);

        AsyncResult<Boolean> result = future.get();

        if (result.getSuccess()) {
            return result.getResult().booleanValue();
        } else {
            throw result.getException();
        }

    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final String loginHint,
                             @NonNull final AuthenticationCallback callback) {
        final AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
                activity,
                scopes,
                null, // account
                null, // uiBehavior
                null, // extraQueryParams
                null, // extraScopes
                null, // authority
                callback,
                loginHint,
                null // claimsRequest
        );

        acquireTokenInternal(acquireTokenParameters, PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_LOGINHINT_CALLBACK);
    }
}
