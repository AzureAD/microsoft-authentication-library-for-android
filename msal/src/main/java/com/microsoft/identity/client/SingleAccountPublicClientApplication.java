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
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.AsyncResult;
import com.microsoft.identity.client.internal.CommandParametersAdapter;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.common.adal.internal.util.JsonExtensions;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.commands.CommandCallback;
import com.microsoft.identity.common.internal.commands.GetCurrentAccountCommand;
import com.microsoft.identity.common.internal.commands.RemoveCurrentAccountCommand;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.internal.migration.TokenMigrationCallback;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.logging.Logger;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.identity.client.exception.MsalClientException.UNKNOWN_ERROR;
import static com.microsoft.identity.client.internal.MsalUtils.throwOnMainThread;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_SCOPES_AUTHORITY_CALLBACK;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_SCOPES_AUTHORITY;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_CALLBACK;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_EXISTING_SIGN_IN_WITH_PROMPT;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT_ASYNC;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_SIGN_IN;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_SIGN_IN_WITH_PROMPT;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_SIGN_OUT;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.SINGLE_ACCOUNT_PCA_SIGN_OUT_WITH_CALLBACK;

public class SingleAccountPublicClientApplication
        extends PublicClientApplication
        implements ISingleAccountPublicClientApplication {
    private static final String TAG = SingleAccountPublicClientApplication.class.getSimpleName();

    /**
     * Name of the shared preference cache for storing SingleAccountPublicClientApplication data.
     */
    public static final String SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES =
            "com.microsoft.identity.client.single_account_credential_cache";

    /**
     * SharedPreference key for storing current account.
     */
    public static final String CURRENT_ACCOUNT_SHARED_PREFERENCE_KEY =
            "com.microsoft.identity.client.single_account_credential_cache.current_account";

    private SharedPreferencesFileManager sharedPreferencesFileManager;

    protected SingleAccountPublicClientApplication(@NonNull final PublicClientApplicationConfiguration config) throws MsalClientException {
        super(config);
        initializeSharedPreferenceFileManager(config.getAppContext());
    }

    private void initializeSharedPreferenceFileManager(@NonNull final Context context) {
        sharedPreferencesFileManager = new SharedPreferencesFileManager(
                context,
                SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                new AndroidAuthSdkStorageEncryptionManager(context, null)
        );
    }

    @Override
    public void getCurrentAccountAsync(@NonNull final CurrentAccountCallback callback) {
        getCurrentAccountAsyncInternal(callback, SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT_ASYNC);
    }

    private void getCurrentAccountAsyncInternal(@NonNull final CurrentAccountCallback callback,
                                                @NonNull final String publicApiId) {
        TokenMigrationCallback migrationCallback = new TokenMigrationCallback() {
            @Override
            public void onMigrationFinished(int numberOfAccountsMigrated) {
                final CommandParameters params = CommandParametersAdapter.createCommandParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());
                final List<BaseController> controllers;
                try {
                    controllers = MSALControllerFactory.getAllControllers(
                            mPublicClientConfiguration.getAppContext(),
                            mPublicClientConfiguration.getDefaultAuthority(),
                            mPublicClientConfiguration);
                } catch (MsalClientException e) {
                    callback.onError(e);
                    return;
                }

                final GetCurrentAccountCommand command = new GetCurrentAccountCommand(
                        params,
                        controllers,
                        new CommandCallback<List<ICacheRecord>, BaseException>() {
                            @Override
                            public void onTaskCompleted(final List<ICacheRecord> result) {
                                // To simplify the logic, if more than one account is returned, the first account will be picked.
                                // We do not support switching from MULTIPLE to SINGLE.
                                // See getAccountFromICacheRecordList() for more details.
                                checkCurrentAccountNotifyCallback(callback, result);
                                persistCurrentAccount(result);
                            }

                            @Override
                            public void onError(final BaseException exception) {
                                callback.onError(MsalExceptionAdapter.msalExceptionFromBaseException(exception));
                            }

                            @Override
                            public void onCancel() {
                                //Do nothing
                            }
                        },
                        publicApiId
                );

                CommandDispatcher.submitSilent(command);
            }
        };

        performMigration(migrationCallback);
    }

    @Override
    public ICurrentAccountResult getCurrentAccount() throws InterruptedException, MsalException {
        throwOnMainThread("getCurrentAccount");

        final ResultFuture<AsyncResult<CurrentAccountResult>> future = new ResultFuture<>();

        getCurrentAccountAsyncInternal(
                new CurrentAccountCallback() {
                    @Override
                    public void onAccountLoaded(@Nullable final IAccount activeAccount) {
                        final CurrentAccountResult currentAccountResult = new CurrentAccountResult(
                                activeAccount,
                                null,
                                false
                        );

                        future.setResult(new AsyncResult<>(currentAccountResult, null));
                    }

                    @Override
                    public void onAccountChanged(@Nullable final IAccount priorAccount,
                                                 @Nullable final IAccount currentAccount) {
                        final CurrentAccountResult currentAccountResult = new CurrentAccountResult(
                                currentAccount,
                                priorAccount,
                                false
                        );

                        future.setResult(new AsyncResult<>(currentAccountResult, null));
                    }

                    @Override
                    public void onError(@NonNull final MsalException exception) {
                        future.setResult(new AsyncResult<CurrentAccountResult>(null, exception));
                    }
                },
                SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT
        );

        try {
            final AsyncResult<CurrentAccountResult> result = future.get();

            if (result.getSuccess()) {
                return result.getResult();
            } else {
                throw result.getException();
            }
        } catch (final ExecutionException e) {
            // Shouldn't be thrown.
            throw new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unknown exception while fetching current account.",
                    e
            );
        }
    }

    private void checkCurrentAccountNotifyCallback(@NonNull final CurrentAccountCallback callback,
                                                   @Nullable final List<ICacheRecord> newAccountRecords) {
        final MultiTenantAccount localAccount = getPersistedCurrentAccount();
        final MultiTenantAccount newAccount = newAccountRecords == null
                ? null
                : getAccountFromICacheRecordList(newAccountRecords);

        if (!isHomeAccountIdMatching(localAccount, newAccount)) {
            callback.onAccountChanged(localAccount, newAccount);
        }

        callback.onAccountLoaded(newAccount);
    }

    @Override
    public void signIn(@NonNull final Activity activity,
                       @Nullable final String loginHint,
                       @NonNull final String[] scopes,
                       @NonNull final AuthenticationCallback callback) {
        final IAccount persistedAccount = getPersistedCurrentAccount();

        if (persistedAccount != null) {
            callback.onError(
                    new MsalClientException(
                            MsalClientException.INVALID_PARAMETER,
                            "An account is already signed in."
                    )
            );
            return;
        }

        final AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
                activity,
                null,
                scopes,
                null, // account
                null, // uiBehavior
                null, // extraQueryParams
                null,
                null, // authority
                callback,
                loginHint, // loginHint
                null // claimsRequest
        );

        acquireTokenInternal(acquireTokenParameters, SINGLE_ACCOUNT_PCA_SIGN_IN);
    }

    @Override
    public void signIn(@NonNull final Activity activity,
                       @Nullable final String loginHint,
                       @NonNull final String[] scopes,
                       @Nullable final Prompt prompt,
                       @NonNull final AuthenticationCallback callback) {
        final IAccount persistedAccount = getPersistedCurrentAccount();

        if (persistedAccount != null) {
            callback.onError(
                    new MsalClientException(
                            MsalClientException.INVALID_PARAMETER,
                            "An account is already signed in."
                    )
            );
            return;
        }

        final AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
                activity,
                null,
                scopes,
                null, // account
                prompt, // prompt
                null, // extraQueryParams
                null,
                null, // authority
                callback,
                loginHint, // loginHint
                null // claimsRequest
        );

        acquireTokenInternal(acquireTokenParameters, SINGLE_ACCOUNT_PCA_SIGN_IN_WITH_PROMPT);
    }

    @Override
    public void signInAgain(@NonNull final Activity activity,
                            @NonNull final String[] scopes,
                            @Nullable final Prompt prompt,
                            @NonNull final AuthenticationCallback callback) {
        final MultiTenantAccount persistedCurrentAccount = getPersistedCurrentAccount();

        if (persistedCurrentAccount == null) {
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT,
                    MsalClientException.NO_CURRENT_ACCOUNT_ERROR_MESSAGE));
            return;
        }

        final AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
                activity,
                null,
                scopes,
                persistedCurrentAccount, // account
                prompt, // prompt
                null, // extraQueryParams
                null,
                null, // authority
                callback,
                null, // loginHint
                null // claimsRequest
        );

        acquireTokenInternal(acquireTokenParameters, SINGLE_ACCOUNT_PCA_EXISTING_SIGN_IN_WITH_PROMPT);
    }

    @Override
    protected CommandCallback<ILocalAuthenticationResult, BaseException> getCommandCallback(
            @NonNull final SilentAuthenticationCallback authenticationCallback,
            @NonNull final TokenParameters tokenParameters) {
        return new CommandCallback<ILocalAuthenticationResult, BaseException>() {

            @Override
            public void onTaskCompleted(@NonNull final ILocalAuthenticationResult localAuthenticationResult) {
                if (authenticationCallback == null) {
                    throw new IllegalStateException(NONNULL_CONSTANTS.CALLBACK + NONNULL_CONSTANTS.NULL_ERROR_SUFFIX);
                }

                persistCurrentAccount(localAuthenticationResult.getCacheRecordWithTenantProfileData());
                postAuthResult(localAuthenticationResult, tokenParameters, authenticationCallback);
            }

            @Override
            public void onError(final BaseException exception) {
                final MsalException msalException = MsalExceptionAdapter.msalExceptionFromBaseException(exception);
                if (authenticationCallback == null) {
                    throw new IllegalStateException(NONNULL_CONSTANTS.CALLBACK + NONNULL_CONSTANTS.NULL_ERROR_SUFFIX);
                } else {
                    authenticationCallback.onError(msalException);
                }
            }

            @Override
            public void onCancel() {
                if (authenticationCallback instanceof AuthenticationCallback) {
                    ((AuthenticationCallback) authenticationCallback).onCancel();
                } else {
                    throw new IllegalStateException("Silent requests cannot be cancelled.");
                }
            }
        };
    }

    /**
     * Returns true if the account ID of both account matches (or both accounts are null).
     * */
    private boolean isHomeAccountIdMatching(@Nullable final IAccount firstAccount, @Nullable final IAccount secondAccount) {
        final MultiTenantAccount firstMultiTenantAccount = firstAccount instanceof MultiTenantAccount ? (MultiTenantAccount) firstAccount : null;
        final MultiTenantAccount secondMultiTenantAccount = secondAccount instanceof MultiTenantAccount ? (MultiTenantAccount) secondAccount : null;

        final String firstMultiTenantAccountId = firstMultiTenantAccount == null ? "" : firstMultiTenantAccount.getHomeAccountId();
        final String secondMultiTenantAccountId = secondMultiTenantAccount == null ? "" : secondMultiTenantAccount.getHomeAccountId();

        return firstMultiTenantAccountId.equalsIgnoreCase(secondMultiTenantAccountId);
    }

    @Override
    public void signOut(@NonNull final SignOutCallback callback) {
        signOutInternal(callback, SINGLE_ACCOUNT_PCA_SIGN_OUT_WITH_CALLBACK);
    }

    void signOutInternal(@NonNull final SignOutCallback callback,
                         @NonNull final String publicApiId) {
        final MultiTenantAccount persistedCurrentAccount = getPersistedCurrentAccount();

        if (persistedCurrentAccount == null) {
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT,
                    MsalClientException.NO_CURRENT_ACCOUNT_ERROR_MESSAGE));
            return;
        }

        final AccountRecord requestAccountRecord = new AccountRecord();
        requestAccountRecord.setEnvironment(persistedCurrentAccount.getEnvironment());
        requestAccountRecord.setHomeAccountId(persistedCurrentAccount.getHomeAccountId());

        final RemoveAccountCommandParameters params =
                CommandParametersAdapter.createRemoveAccountCommandParameters(
                        mPublicClientConfiguration,
                        mPublicClientConfiguration.getOAuth2TokenCache(),
                        requestAccountRecord
                );

        final List<BaseController> controllers;
        try {
            controllers = MSALControllerFactory.getAllControllers(
                    mPublicClientConfiguration.getAppContext(),
                    mPublicClientConfiguration.getDefaultAuthority(),
                    mPublicClientConfiguration);
        } catch (MsalClientException e) {
            callback.onError(e);
            return;
        }

        final RemoveCurrentAccountCommand command = new RemoveCurrentAccountCommand(
                params,
                controllers,
                new CommandCallback<Boolean, BaseException>() {
                    @Override
                    public void onError(BaseException error) {
                        callback.onError(MsalExceptionAdapter.msalExceptionFromBaseException(error));
                    }

                    @Override
                    public void onTaskCompleted(Boolean result) {
                        persistCurrentAccount(null);
                        callback.onSignOut();
                    }

                    @Override
                    public void onCancel() {
                        //Do nothing
                    }
                },
                publicApiId
        );

        CommandDispatcher.submitSilent(command);
    }

    @Override
    public boolean signOut() throws MsalException, InterruptedException {

        throwOnMainThread("signOut");

        final ResultFuture<AsyncResult<Boolean>> future = new ResultFuture<>();

        signOutInternal(
                new SignOutCallback() {
                    @Override
                    public void onSignOut() {
                        future.setResult(new AsyncResult<>(true, null));
                    }

                    @Override
                    public void onError(@NonNull final MsalException exception) {
                        future.setResult(new AsyncResult<>(false, exception));
                    }
                },
                SINGLE_ACCOUNT_PCA_SIGN_OUT
        );

        try {
            final AsyncResult<Boolean> result = future.get();

            if (result.getSuccess()) {
                return result.getResult();
            } else {
                throw result.getException();
            }
        } catch (final ExecutionException e) {
            // Shouldn't be thrown.
            throw new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unexpected error during signOut.",
                    e
            );
        }
    }

    /**
     * Get current account that is persisted in shared preference.
     *
     * @return a persisted MultiTenantAccount. This could be null.
     */
    private MultiTenantAccount getPersistedCurrentAccount() {
        synchronized(SingleAccountPublicClientApplication.class) {
            final String currentAccountJsonString = sharedPreferencesFileManager.getString(CURRENT_ACCOUNT_SHARED_PREFERENCE_KEY);

            if (StringExtensions.isNullOrBlank(currentAccountJsonString)) {
                return null;
            }

            final List<ICacheRecord> cacheRecordList = JsonExtensions.getICacheRecordListFromJsonString(currentAccountJsonString);
            return getAccountFromICacheRecordList(cacheRecordList);
        }
    }

    /**
     * Persists current account to shared preference.
     *
     * @param cacheRecords list of cache record that belongs to an account.
     *                     Please note that this layer will not verify if the list belongs to a single account or not.
     */
    private void persistCurrentAccount(@Nullable final List<ICacheRecord> cacheRecords) {
        synchronized(SingleAccountPublicClientApplication.class) {
            if (cacheRecords == null || cacheRecords.size() == 0) {
                sharedPreferencesFileManager.clear();
                return;
            }

            final String currentAccountJsonString = JsonExtensions.getJsonStringFromICacheRecordList(cacheRecords);
            sharedPreferencesFileManager.putString(CURRENT_ACCOUNT_SHARED_PREFERENCE_KEY, currentAccountJsonString);
        }
    }

    /**
     * Get a MultiTenantAccount from a list of ICacheRecord.
     *
     * @param cacheRecords list of cache record that belongs to an account.
     *                     If the list can be converted to multiple accounts, only the first one will be returned.
     */
    @Nullable
    private MultiTenantAccount getAccountFromICacheRecordList(@NonNull final List<ICacheRecord> cacheRecords) {
        final String methodName = ":getAccountFromICacheRecords";

        if (cacheRecords == null || cacheRecords.size() == 0) {
            return null;
        }

        final List<IAccount> account = AccountAdapter.adapt(cacheRecords);

        if (account.size() != 1) {
            Logger.verbose(
                    TAG + methodName,
                    "Returned cacheRecords were adapted into multiple IAccount. " +
                            "This is unexpected in Single account mode." +
                            "Returning the first adapted account.");
        }

        return (MultiTenantAccount) account.get(0);
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @NonNull final AuthenticationCallback callback) {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT,
                    MsalClientException.NO_CURRENT_ACCOUNT_ERROR_MESSAGE));
            return;
        }

        final AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
                activity,
                null,
                scopes,
                getPersistedCurrentAccount(), // account, could be null.
                null, // uiBehavior
                null, // extraQueryParams
                null, // extraScopes
                null, // authority
                callback,
                null, // loginHint
                null // claimsRequest
        );

        acquireTokenInternal(
                acquireTokenParameters,
                SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_CALLBACK
        );
    }

    @Override
    public void acquireToken(@NonNull final AcquireTokenParameters acquireTokenParameters) {
        final IAccount persistedAccount = getPersistedCurrentAccount();

        // If persistedAccount exists, a matching account or login hint is expected.
        if (persistedAccount != null) {
            // Nothing is provided.
            if (acquireTokenParameters.getAccount() == null &&
                    StringExtensions.isNullOrBlank(acquireTokenParameters.getLoginHint())){
                acquireTokenParameters
                        .getCallback()
                        .onError(new MsalClientException(MsalClientException.CURRENT_ACCOUNT_MISMATCH,
                                MsalClientException.CURRENT_ACCOUNT_MISMATCH_ERROR_MESSAGE));
                return;
            }

            // If account is provided, check if the account's homeAccountId matches with the persisted account's.
            if (acquireTokenParameters.getAccount() != null &&
                    !isHomeAccountIdMatching(persistedAccount, acquireTokenParameters.getAccount())) {
                acquireTokenParameters
                        .getCallback()
                        .onError(new MsalClientException(MsalClientException.CURRENT_ACCOUNT_MISMATCH,
                                MsalClientException.CURRENT_ACCOUNT_MISMATCH_ERROR_MESSAGE));
                return;
            }

            // If login hint is provided, check if the login hint matches with the persisted account's.
            if (!StringExtensions.isNullOrBlank(acquireTokenParameters.getLoginHint()) &&
                    !persistedAccount.getUsername().equalsIgnoreCase(acquireTokenParameters.getLoginHint())){
                acquireTokenParameters
                        .getCallback()
                        .onError(new MsalClientException(MsalClientException.CURRENT_ACCOUNT_MISMATCH,
                                MsalClientException.CURRENT_ACCOUNT_MISMATCH_ERROR_MESSAGE));
                return;
            }
        }

        acquireTokenInternal(acquireTokenParameters, SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS);
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @NonNull final String authority,
                                        @NonNull final SilentAuthenticationCallback callback) {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT,
                    MsalClientException.NO_CURRENT_ACCOUNT_ERROR_MESSAGE));
            return;
        }

        final AcquireTokenSilentParameters acquireTokenSilentParameters = buildAcquireTokenSilentParameters(
                scopes,
                persistedAccount,
                authority,
                false,
                null, // claimsRequest
                callback
        );

        acquireTokenSilentAsyncInternal(
                acquireTokenSilentParameters,
                SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_SCOPES_AUTHORITY_CALLBACK
        );
    }

    @WorkerThread
    public IAuthenticationResult acquireTokenSilent(@NonNull final String[] scopes,
                                                    @NonNull final String authority) throws MsalException, InterruptedException {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            throw new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT,
                    MsalClientException.NO_CURRENT_ACCOUNT_ERROR_MESSAGE);
        }

        return acquireTokenSilentSyncInternal(
                scopes,
                authority,
                persistedAccount,
                false,
                SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_SCOPES_AUTHORITY
        );
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            acquireTokenSilentParameters
                    .getCallback()
                    .onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT,
                            MsalClientException.NO_CURRENT_ACCOUNT_ERROR_MESSAGE));
            return;
        }

        if (!isHomeAccountIdMatching(persistedAccount, acquireTokenSilentParameters.getAccount())) {
            acquireTokenSilentParameters
                    .getCallback()
                    .onError(new MsalClientException(MsalClientException.CURRENT_ACCOUNT_MISMATCH,
                            MsalClientException.CURRENT_ACCOUNT_MISMATCH_ERROR_MESSAGE));
            return;
        }

        acquireTokenSilentAsyncInternal(
                acquireTokenSilentParameters,
                SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS
        );
    }

    @Override
    public IAuthenticationResult acquireTokenSilent(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) throws InterruptedException, MsalException {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            throw new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT,
                    MsalClientException.NO_CURRENT_ACCOUNT_ERROR_MESSAGE);
        }

        if (!isHomeAccountIdMatching(persistedAccount, acquireTokenSilentParameters.getAccount())) {
            throw new MsalClientException(MsalClientException.CURRENT_ACCOUNT_MISMATCH,
                    MsalClientException.CURRENT_ACCOUNT_MISMATCH_ERROR_MESSAGE);
        }

        return acquireTokenSilentInternal(
                acquireTokenSilentParameters,
                SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS
        );
    }
}
