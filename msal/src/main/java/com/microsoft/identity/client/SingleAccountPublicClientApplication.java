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
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.eststelemetry.PublicApiId;
import com.microsoft.identity.common.internal.controllers.GetCurrentAccountCommand;
import com.microsoft.identity.common.internal.controllers.RemoveCurrentAccountCommand;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.internal.result.ResultFuture;

import java.util.List;

import static com.microsoft.identity.client.internal.MsalUtils.throwOnMainThread;

public class SingleAccountPublicClientApplication extends PublicClientApplication
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

    protected SingleAccountPublicClientApplication(@NonNull PublicClientApplicationConfiguration config,
                                                   @Nullable final String clientId,
                                                   @Nullable final String authority) {
        super(config, clientId, authority);
        initializeSharedPreferenceFileManager(config.getAppContext());
    }

    private void initializeSharedPreferenceFileManager(@NonNull final Context context) {
        sharedPreferencesFileManager = new SharedPreferencesFileManager(
                context,
                SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                new StorageHelper(context));
    }

    @Override
    public void getCurrentAccountAsync(@NonNull final CurrentAccountCallback callback) {
        getCurrentAccountAsyncInternal(callback, PublicApiId.SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT_ASYNC);
    }

    private void getCurrentAccountAsyncInternal(@NonNull final CurrentAccountCallback callback,
                                                @NonNull final String publicApiId) {
        final String methodName = ":getCurrentAccount";
        final PublicClientApplicationConfiguration configuration = getConfiguration();

        final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());
        final BaseController controller;
        try {
            controller = MSALControllerFactory.getDefaultController(
                    mPublicClientConfiguration.getAppContext(),
                    params.getAuthority(),
                    mPublicClientConfiguration);
        } catch (MsalClientException e) {
            callback.onError(e);
            return;
        }

        final GetCurrentAccountCommand command = new GetCurrentAccountCommand(
                params,
                controller,
                new CommandCallback<List<ICacheRecord>, BaseException>() {
                    @Override
                    public void onTaskCompleted(final List<ICacheRecord> result) {
                        // To simplify the logic, if more than one account is returned, the first account will be picked.
                        // We do not support switching from MULTIPLE to SINGLE.
                        // See getAccountFromICacheRecordList() for more details.
                        checkCurrentAccountNotifyCallback(callback, result);
                    }

                    @Override
                    public void onError(final BaseException exception) {
                        callback.onError(MsalExceptionAdapter.msalExceptionFromBaseException(exception));
                    }

                    @Override
                    public void onCancel() {
                        //Do nothing
                    }
                });

        command.setPublicApiId(publicApiId);
        CommandDispatcher.submitSilent(command);
    }

    @Override
    public ICurrentAccountResult getCurrentAccount() throws InterruptedException, MsalException {
        throwOnMainThread("getCurrentAccount");

        final ResultFuture<AsyncResult<CurrentAccountResult>> future = new ResultFuture<>();

        getCurrentAccountAsyncInternal(new CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                CurrentAccountResult currentAccountResult = new CurrentAccountResult(activeAccount, null, false);
                future.setResult(new AsyncResult<>(currentAccountResult, null));
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                CurrentAccountResult currentAccountResult = new CurrentAccountResult(currentAccount, priorAccount, false);
                future.setResult(new AsyncResult<>(currentAccountResult, null));
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                future.setResult(new AsyncResult<CurrentAccountResult>(null, exception));
            }
        }, PublicApiId.SINGLE_ACCOUNT_PCA_GET_CURRENT_ACCOUNT);

        AsyncResult<CurrentAccountResult> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
        }
    }

    private void checkCurrentAccountNotifyCallback(@NonNull final CurrentAccountCallback callback,
                                                   @Nullable final List<ICacheRecord> newAccountRecords) {
        MultiTenantAccount localAccount = getPersistedCurrentAccount();
        MultiTenantAccount newAccount = newAccountRecords == null ? null : getAccountFromICacheRecordList(newAccountRecords);

        if (didCurrentAccountChange(newAccount)) {
            callback.onAccountChanged(localAccount, newAccount);
        }

        persistCurrentAccount(newAccountRecords);
        callback.onAccountLoaded(newAccount);
    }

    @Override
    public void signIn(@NonNull final Activity activity,
                       @NonNull final String loginHint,
                       @NonNull final String[] scopes,
                       @NonNull final AuthenticationCallback callback) {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount != null) {
            callback.onError(new MsalClientException(MsalClientException.INVALID_PARAMETER));
            return;
        }

        final AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
                activity,
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

        acquireTokenInternal(acquireTokenParameters, PublicApiId.SINGLE_ACCOUNT_PCA_SIGN_IN);
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

                //Get Local Authentication Result then check if the current account is set or not
                MultiTenantAccount newAccount = getAccountFromICacheRecordList(localAuthenticationResult.getCacheRecordWithTenantProfileData());

                if (didCurrentAccountChange(newAccount)) {
                    if (getPersistedCurrentAccount() != null) {
                        authenticationCallback.onError(new MsalClientException(MsalClientException.CURRENT_ACCOUNT_MISMATCH));
                        return;
                    } else {
                        persistCurrentAccount(localAuthenticationResult.getCacheRecordWithTenantProfileData());
                    }
                } else {
                    persistCurrentAccount(localAuthenticationResult.getCacheRecordWithTenantProfileData());
                }

                postAuthResult(localAuthenticationResult, tokenParameters, authenticationCallback);

            }

            @Override
            public void onError(BaseException exception) {
                MsalException msalException = MsalExceptionAdapter.msalExceptionFromBaseException(exception);
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

    private boolean didCurrentAccountChange(@Nullable final MultiTenantAccount newAccount) {
        final MultiTenantAccount persistedAccount = getPersistedCurrentAccount();

        final String persistedAccountId = persistedAccount == null ? "" : persistedAccount.getHomeAccountId();
        final String newAccountId = newAccount == null ? "" : newAccount.getHomeAccountId();

        return !persistedAccountId.equalsIgnoreCase(newAccountId);
    }

    @Override
    public void signOut(@NonNull final SignOutCallback callback) {
        signOutInternal(callback, PublicApiId.SINGLE_ACCOUNT_PCA_SIGN_OUT_WITH_CALLBACK);
    }

    void signOutInternal(@NonNull final SignOutCallback callback,
                         @NonNull final String publicApiId) {
        final MultiTenantAccount persistedCurrentAccount = getPersistedCurrentAccount();
        if (persistedCurrentAccount == null) {
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
            return;
        }

        final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());
        final AccountRecord requestAccountRecord = new AccountRecord();
        requestAccountRecord.setEnvironment(persistedCurrentAccount.getEnvironment());
        requestAccountRecord.setHomeAccountId(persistedCurrentAccount.getHomeAccountId());
        params.setAccount(requestAccountRecord);

        final BaseController controller;
        try {
            controller = MSALControllerFactory.getDefaultController(
                    mPublicClientConfiguration.getAppContext(),
                    params.getAuthority(),
                    mPublicClientConfiguration);
        } catch (MsalClientException e) {
            callback.onError(e);
            return;
        }

        final RemoveCurrentAccountCommand command = new RemoveCurrentAccountCommand(
                params,
                controller,
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
                }
        );

        command.setPublicApiId(publicApiId);
        CommandDispatcher.submitSilent(command);
    }

    @Override
    public boolean signOut() throws MsalException, InterruptedException {

        throwOnMainThread("signOut");

        final ResultFuture<AsyncResult<Boolean>> future = new ResultFuture<>();

        signOutInternal(new SignOutCallback() {
            @Override
            public void onSignOut() {
                future.setResult(new AsyncResult<Boolean>(true, null));
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                future.setResult(new AsyncResult<Boolean>(false, exception));
            }
        }, PublicApiId.SINGLE_ACCOUNT_PCA_SIGN_OUT);

        final AsyncResult<Boolean> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
        }
    }

    /**
     * Get current account that is persisted in shared preference.
     *
     * @return a persisted MultiTenantAccount. This could be null.
     */
    private MultiTenantAccount getPersistedCurrentAccount() {
        final String currentAccountJsonString = sharedPreferencesFileManager.getString(CURRENT_ACCOUNT_SHARED_PREFERENCE_KEY);
        if (currentAccountJsonString == null) {
            return null;
        }

        final List<ICacheRecord> cacheRecordList = MsalBrokerResultAdapter.getICacheRecordListFromJsonString(currentAccountJsonString);
        return getAccountFromICacheRecordList(cacheRecordList);
    }

    /**
     * Persists current account to shared preference.
     *
     * @param cacheRecords list of cache record that belongs to an account.
     *                     Please note that this layer will not verify if the list ubelongs to a single account or not.
     */
    private void persistCurrentAccount(@Nullable final List<ICacheRecord> cacheRecords) {

        sharedPreferencesFileManager.clear();

        if (cacheRecords == null || cacheRecords.size() == 0) {
            // Do nothing.
            return;
        }

        String currentAccountJsonString = MsalBrokerResultAdapter.getJsonStringFromICacheRecordList(cacheRecords);
        sharedPreferencesFileManager.putString(CURRENT_ACCOUNT_SHARED_PREFERENCE_KEY, currentAccountJsonString);
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
            com.microsoft.identity.common.internal.logging.Logger.verbose(
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
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
            return;
        }

        final AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
                activity,
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

        acquireTokenInternal(acquireTokenParameters, PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_CALLBACK);
    }

    @Override
    public void acquireToken(@NonNull final AcquireTokenParameters acquireTokenParameters) {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount != null) {
            // If the account exists, overwrite Account and ignore loginHint.
            acquireTokenParameters.setAccount(persistedAccount);
            acquireTokenParameters.setLoginHint("");
        }

        acquireTokenInternal(acquireTokenParameters, PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS);
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @NonNull final String authority,
                                        @NonNull final SilentAuthenticationCallback callback) {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
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

        acquireTokenSilentAsyncInternal(acquireTokenSilentParameters, PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_SCOPES_AUTHORITY_CALLBACK);
    }

    @WorkerThread
    public IAuthenticationResult acquireTokenSilent(@NonNull final String[] scopes,
                                                    @NonNull final String authority) throws MsalException, InterruptedException {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            throw new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT);
        }

        return acquireTokenSilentSyncInternal(scopes, authority, persistedAccount, false, PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_SCOPES_AUTHORITY);
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            acquireTokenSilentParameters.getCallback().onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
            return;
        }

        // In SingleAccount mode, always overwrite 'Account' with current account.
        acquireTokenSilentParameters.setAccount(persistedAccount);

        acquireTokenSilentAsyncInternal(acquireTokenSilentParameters, PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS);
    }

    @Override
    public IAuthenticationResult acquireTokenSilent(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) throws InterruptedException, MsalException {
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            throw new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT);
        }

        // In SingleAccount mode, always overwrite 'Account' with current account.
        acquireTokenSilentParameters.setAccount(persistedAccount);

        return acquireTokenSilentInternal(acquireTokenSilentParameters, PublicApiId.SINGLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS);
    }
}
