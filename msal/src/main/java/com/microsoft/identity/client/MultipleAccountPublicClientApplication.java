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

import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.AsyncResult;
import com.microsoft.identity.client.internal.CommandParametersAdapter;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.internal.commands.LoadAccountCommand;
import com.microsoft.identity.common.internal.commands.RemoveAccountCommand;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.java.controllers.CommandDispatcher;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.eststelemetry.PublicApiId;
import com.microsoft.identity.common.internal.migration.TokenMigrationCallback;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.identity.client.exception.MsalClientException.UNKNOWN_ERROR;
import static com.microsoft.identity.client.internal.MsalUtils.throwOnMainThread;
import static com.microsoft.identity.client.internal.MsalUtils.validateNonNullArg;

public class MultipleAccountPublicClientApplication extends PublicClientApplication
        implements IMultipleAccountPublicClientApplication {
    private static final String TAG = MultipleAccountPublicClientApplication.class.getSimpleName();

    protected MultipleAccountPublicClientApplication(@NonNull PublicClientApplicationConfiguration config) throws MsalClientException {
        super(config);
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
        TokenMigrationCallback migrationCallback = new TokenMigrationCallback() {
            @Override
            public void onMigrationFinished(int numberOfAccountsMigrated) {
                final Handler handler;

                if (null != Looper.myLooper() && Looper.getMainLooper() != Looper.myLooper()) {
                    handler = new Handler(Looper.myLooper());
                } else {
                    handler = new Handler(Looper.getMainLooper());
                }

                try {
                    final CommandParameters params = CommandParametersAdapter.createCommandParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());
                    final LoadAccountCommand loadAccountCommand = new LoadAccountCommand(
                            params,
                            MSALControllerFactory.getAllControllers(
                                    mPublicClientConfiguration.getAppContext(),
                                    mPublicClientConfiguration.getDefaultAuthority(),
                                    mPublicClientConfiguration
                            ),
                            getLoadAccountsCallback(callback),
                            publicApiId
                    );

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
        };

        performMigration(migrationCallback);
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

        try {
            final AsyncResult<List<IAccount>> result = future.get();

            if (result.getSuccess()) {
                return result.getResult();
            } else {
                throw result.getException();
            }
        } catch (final ExecutionException e) {
            // Shouldn't be thrown.
            throw new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unexpected error while loading accounts.",
                    e
            );
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
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null or empty");
        }
        try {
            validateNonNullArg(identifier, "identifier");
        } catch (MsalArgumentException e) {
            callback.onError(e);
        }

        TokenMigrationCallback migrationCallback = new TokenMigrationCallback() {
            @Override
            public void onMigrationFinished(int numberOfAccountsMigrated) {
                final String methodName = ":getAccount";

                Logger.verbose(TAG + methodName, "Get account with the identifier.");

                try {
                    final CommandParameters params = CommandParametersAdapter.createCommandParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());
                    final LoadAccountCommand loadAccountCommand = new LoadAccountCommand(
                            params,
                            MSALControllerFactory.getAllControllers(
                                    mPublicClientConfiguration.getAppContext(),
                                    mPublicClientConfiguration.getDefaultAuthority(),
                                    mPublicClientConfiguration
                            ),
                            new CommandCallback<List<ICacheRecord>, BaseException>() {
                                @Override
                                public void onTaskCompleted(final List<ICacheRecord> result) {
                                    if (null == result || result.size() == 0) {
                                        Logger.verbose(TAG + methodName, "No account found.");
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
                                    Logger.error(TAG + methodName, exception.getMessage(), exception);
                                    callback.onError(MsalExceptionAdapter.msalExceptionFromBaseException(exception));
                                }

                                @Override
                                public void onCancel() {

                                }
                            },
                            publicApiId
                    );

                    CommandDispatcher.submitSilent(loadAccountCommand);
                } catch (final MsalClientException e) {
                    Logger.error(TAG + methodName, e.getMessage(), e);
                    callback.onError(e);
                }
            }
        };

        performMigration(migrationCallback);
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

        try {
            AsyncResult<IAccount> result = future.get();

            if (result.getSuccess()) {
                return result.getResult();
            } else {
                throw result.getException();
            }
        } catch (final ExecutionException e) {
            // Shouldn't be thrown.
            throw new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unexpected error while loading account",
                    e
            );
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
            Logger.warn(TAG,
                    "Requisite IAccount or IAccount fields were null. " +
                            "Insufficient criteria to remove IAccount."
            );

            callback.onError(new MsalClientException(MsalClientException.INVALID_PARAMETER));
            return;
        }

        // TODO Clean this up, only the cache should make these records...
        // The broker strips these properties out of this object to hit the cache
        // Refactor this out...
        final AccountRecord requestAccountRecord = new AccountRecord();
        requestAccountRecord.setEnvironment(multiTenantAccount.getEnvironment());
        requestAccountRecord.setHomeAccountId(multiTenantAccount.getHomeAccountId());

        final RemoveAccountCommandParameters params = CommandParametersAdapter
                .createRemoveAccountCommandParameters(
                        mPublicClientConfiguration,
                        mPublicClientConfiguration.getOAuth2TokenCache(),
                        requestAccountRecord
                );

        try {
            final RemoveAccountCommand removeAccountCommand = new RemoveAccountCommand(
                    params,
                    MSALControllerFactory.getAllControllers(
                            mPublicClientConfiguration.getAppContext(),
                            mPublicClientConfiguration.getDefaultAuthority(),
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
                        public void onCancel() {
                            //Do nothing
                        }
                    },
                    publicApiId
            );

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

        try {
            final AsyncResult<Boolean> result = future.get();

            if (result.getSuccess()) {
                return result.getResult().booleanValue();
            } else {
                throw result.getException();
            }
        } catch (final ExecutionException e) {
            // Shouldn't be thrown.
            throw new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unexpected error while removing account.",
                    e
            );
        }
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final List<String> scopes,
                             @Nullable final String loginHint,
                             @NonNull final AuthenticationCallback callback) {
        final String[] scopesAsArray = (String[]) scopes.toArray();
        acquireToken(activity, scopesAsArray, loginHint, callback);
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final String loginHint,
                             @NonNull final AuthenticationCallback callback) {
        final AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
                activity,
                null,
                Arrays.asList(scopes),
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
