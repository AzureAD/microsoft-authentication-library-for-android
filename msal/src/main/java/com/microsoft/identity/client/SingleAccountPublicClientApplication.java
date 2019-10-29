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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalDeclinedScopeException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.AsyncResult;
import com.microsoft.identity.client.internal.controllers.BrokerMsalController;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.ApiDispatcher;
import com.microsoft.identity.common.internal.controllers.LoadAccountCommand;
import com.microsoft.identity.common.internal.controllers.RemoveAccountCommand;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.request.ILocalAuthenticationCallback;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.internal.result.ResultFuture;

import java.util.Arrays;
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


    void logToLogcat(String message){
        Log.w(TAG, "[thread-id:" + Thread.currentThread().getId() + "] - " + message);
    }

    protected SingleAccountPublicClientApplication(@NonNull PublicClientApplicationConfiguration config,
                                                   @Nullable final String clientId,
                                                   @Nullable final String authority,
                                                   @NonNull final Boolean isSharedDevice) {
        super(config, clientId, authority);

        logToLogcat("SingleAccountPublicClientApplication initialized. isSharedDevice: " + mIsSharedDevice);

        initializeSharedPreferenceFileManager(config.getAppContext());
        mIsSharedDevice = isSharedDevice;
    }

    private void initializeSharedPreferenceFileManager(@NonNull final Context context) {
        sharedPreferencesFileManager = new SharedPreferencesFileManager(
                context,
                SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                new StorageHelper(context));
    }

    @Override
    public void getCurrentAccountAsync(@NonNull final CurrentAccountCallback callback) {
        final String methodName = ":getCurrentAccount";
        final PublicClientApplicationConfiguration configuration = getConfiguration();

        logToLogcat(":getCurrentAccountAsync");


        try {
            if (mIsSharedDevice) {
                getCurrentAccountFromSharedDevice(callback, configuration);
                return;
            }

            logToLogcat("Get current account async in non-shared mode");

            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Getting the current account"
            );

            final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());
            final LoadAccountCommand command = new LoadAccountCommand(
                    params,
                    MSALControllerFactory.getAcquireTokenController(
                            mPublicClientConfiguration.getAppContext(),
                            params.getAuthority(),
                            mPublicClientConfiguration
                    ),
                    new TaskCompletedCallbackWithError<List<ICacheRecord>, BaseException>() {
                        @Override
                        public void onTaskCompleted(final List<ICacheRecord> result) {
                            // To simplify the logic, if more than one account is returned, the first account will be picked.
                            // We do not support switching from MULTIPLE to SINGLE.
                            // See getAccountFromICacheRecordList() for more details.
                            checkCurrentAccountNotifyCallback(callback, result);
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
                    }


            );

            ApiDispatcher.getAccounts(command);

        } catch (MsalClientException clientException) {
            callback.onError(clientException);
        }
    }

    @Override
    public ICurrentAccountResult getCurrentAccount() throws InterruptedException, MsalException {
        logToLogcat(":getCurrentAccount");

        throwOnMainThread("getCurrentAccount");

        final ResultFuture<AsyncResult<CurrentAccountResult>> future = new ResultFuture<>();

        getCurrentAccountAsync(new CurrentAccountCallback() {
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
        });

        AsyncResult<CurrentAccountResult> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
        }
    }

    private void getCurrentAccountFromSharedDevice(@NonNull final CurrentAccountCallback callback,
                                                   @NonNull final PublicClientApplicationConfiguration configuration) {

        logToLogcat(":getCurrentAccountFromSharedDevice");

        //TODO: migrate to Command.
        new BrokerMsalController().getCurrentAccount(
                configuration,
                configuration.getOAuth2TokenCache(),
                new TaskCompletedCallbackWithError<List<ICacheRecord>, MsalException>() {
                    @Override
                    public void onTaskCompleted(List<ICacheRecord> cacheRecords) {
                        String currentAccountJsonString = cacheRecords == null ? "null" : MsalBrokerResultAdapter.getJsonStringFromICacheRecordList(cacheRecords);
                        logToLogcat("Obtained cacheRecords from Broker: " + currentAccountJsonString);

                        checkCurrentAccountNotifyCallback(callback, cacheRecords);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        logToLogcat("Failed to get current account async in non-shared mode. Exception: " + exception.getMessage());
                        callback.onError(exception);
                    }
                });
    }

    private String printMultiTenantAccount(MultiTenantAccount account){
        if (account == null){
            return null;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("\tUsername: " + account.getUsername() + "\n");
        builder.append("\tId: " + account.getId() + "\n");
        builder.append("\tTenantId: " + account.getTenantId() + "\n");
        builder.append("\tHomeAccountId: " + account.getHomeAccountId() + "\n");
        builder.append("\tEnvironment: " + account.getEnvironment() + "\n");
        builder.append("\tTenantProfiles: " + account.getTenantProfiles()+ "\n");
        builder.append("\tClaims: " + account.getClaims() + "\n");

        return builder.toString();
    }


    private void checkCurrentAccountNotifyCallback(@NonNull final CurrentAccountCallback callback,
                                                   @Nullable final List<ICacheRecord> newAccountRecords) {
        logToLogcat(":checkCurrentAccountNotifyCallback");

        MultiTenantAccount localAccount = getPersistedCurrentAccount();
        MultiTenantAccount newAccount = newAccountRecords == null ? null : getAccountFromICacheRecordList(newAccountRecords);

        logToLogcat("Successfully obtained the current account." + "\n" +
                "saved account:\n" + printMultiTenantAccount(localAccount) + "\n" +
                "account obtained from broker:\n" + printMultiTenantAccount(newAccount) + "\n"
        );

        if (didCurrentAccountChange(newAccount)) {
            logToLogcat("HomeAccountId are different. Trigger OnAccountChanged.");
            callback.onAccountChanged(localAccount, newAccount);
        }

        persistCurrentAccount(newAccountRecords, ":checkCurrentAccountNotifyCallback");
        callback.onAccountLoaded(newAccount);
    }

    @Override
    public void signIn(@NonNull final Activity activity,
                       @NonNull final String loginHint,
                       @NonNull final String[] scopes,
                       @NonNull final AuthenticationCallback callback) {
        logToLogcat(":signIn");

        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount != null) {
            logToLogcat("Persisted account is not null. Can't invoke signIn.");
            callback.onError(new MsalClientException(MsalClientException.INVALID_PARAMETER));
            return;
        }

        acquireToken(
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
    }

    @Override
    protected ILocalAuthenticationCallback getLocalAuthenticationCallback(
            @NonNull final SilentAuthenticationCallback authenticationCallback,
            @NonNull final TokenParameters tokenParameters) {
        return new ILocalAuthenticationCallback() {

            @Override
            public void onSuccess(@NonNull final ILocalAuthenticationResult localAuthenticationResult) {
                logToLogcat(":getLocalAuthenticationCallback:onSuccess");


                //Get Local Authentication Result then check if the current account is set or not
                MultiTenantAccount localAccount = getPersistedCurrentAccount();
                MultiTenantAccount newAccount = getAccountFromICacheRecordList(localAuthenticationResult.getCacheRecordWithTenantProfileData());

                logToLogcat("Successfully acquired token." + "\n" +
                        "saved account:\n" + printMultiTenantAccount(localAccount) + "\n" +
                        "account obtained from broker:\n" + printMultiTenantAccount(newAccount) + "\n"
                );


                if (didCurrentAccountChange(newAccount)) {
                    logToLogcat("persisted account and the newly obtained account is different.");
                    if (getPersistedCurrentAccount() != null) {
                        logToLogcat("persisted account is not null. This is unexpected.");
                        authenticationCallback.onError(new MsalClientException(MsalClientException.CURRENT_ACCOUNT_MISMATCH));
                        return;
                    } else {
                        persistCurrentAccount(localAuthenticationResult.getCacheRecordWithTenantProfileData(), ":getLocalAuthenticationCallback.onSuccess()");
                    }
                } else {
                    persistCurrentAccount(localAuthenticationResult.getCacheRecordWithTenantProfileData(), ":getLocalAuthenticationCallback.onSuccess()");
                }

                postAuthResult(localAuthenticationResult, tokenParameters, authenticationCallback);

            }

            @Override
            public void onError(BaseException exception) {
                logToLogcat("Failed to acquire token. Exception: " + exception.getMessage());

                MsalException msalException = MsalExceptionAdapter.msalExceptionFromBaseException(exception);
                authenticationCallback.onError(msalException);
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
        logToLogcat(":signOut");
        final PublicClientApplicationConfiguration configuration = getConfiguration();

        final MultiTenantAccount persistedCurrentAccount = getPersistedCurrentAccount();
        if (persistedCurrentAccount == null) {
            logToLogcat("Persisted account is null. Can't invoke signOut.");
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
            return;
        }

        if (mIsSharedDevice) {
            removeAccountFromSharedDevice(callback, configuration);
            return;
        }

        try {
            final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration, mPublicClientConfiguration.getOAuth2TokenCache());
            final AccountRecord requestAccountRecord = new AccountRecord();
            requestAccountRecord.setEnvironment(persistedCurrentAccount.getEnvironment());
            requestAccountRecord.setHomeAccountId(persistedCurrentAccount.getHomeAccountId());
            params.setAccount(requestAccountRecord);

            final RemoveAccountCommand command = new RemoveAccountCommand(
                    params,
                    MSALControllerFactory.getAcquireTokenController(
                            mPublicClientConfiguration.getAppContext(),
                            params.getAuthority(),
                            mPublicClientConfiguration
                    ),
                    new TaskCompletedCallbackWithError<Boolean, BaseException>() {
                        @Override
                        public void onError(BaseException error) {
                            callback.onError(MsalExceptionAdapter.msalExceptionFromBaseException(error));
                        }

                        @Override
                        public void onTaskCompleted(Boolean result) {
                            persistCurrentAccount(null, ":signOut");
                            callback.onSignOut();
                        }
                    }
            );

            ApiDispatcher.removeAccount(command);
        } catch (final MsalClientException clientException) {
            callback.onError(clientException);
        }
    }

    @Override
    public boolean signOut() throws MsalException, InterruptedException {
        logToLogcat(":signOut");

        throwOnMainThread("signOut");

        final ResultFuture<AsyncResult<Boolean>> future = new ResultFuture<>();

        signOut(new SignOutCallback() {
            @Override
            public void onSignOut() {
                future.setResult(new AsyncResult<Boolean>(true, null));
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                future.setResult(new AsyncResult<Boolean>(false, exception));
            }
        });

        final AsyncResult<Boolean> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
        }
    }

    private void removeAccountFromSharedDevice(@NonNull final SignOutCallback callback,
                                               @NonNull final PublicClientApplicationConfiguration configuration) {
        logToLogcat(":removeAccountFromSharedDevice");

        //TODO: migrate to Command.
        new BrokerMsalController().removeAccountFromSharedDevice(
                configuration,
                new TaskCompletedCallbackWithError<Void, MsalException>() {
                    @Override
                    public void onTaskCompleted(Void aVoid) {
                        persistCurrentAccount(null, ":removeAccountFromSharedDevice");
                        callback.onSignOut();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        callback.onError(exception);
                    }
                });
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
    private void persistCurrentAccount(@Nullable final List<ICacheRecord> cacheRecords, String caller) {
        logToLogcat(":persistCurrentAccount. Called by " + caller);

        sharedPreferencesFileManager.clear();
        logToLogcat("sharedPref is cleared.");

        if (cacheRecords == null || cacheRecords.size() == 0) {
            // Do nothing.
            logToLogcat("No account to save. Return.");
            return;
        }

        String currentAccountJsonString = MsalBrokerResultAdapter.getJsonStringFromICacheRecordList(cacheRecords);
        sharedPreferencesFileManager.putString(CURRENT_ACCOUNT_SHARED_PREFERENCE_KEY, currentAccountJsonString);
        logToLogcat("current account persisted: " + currentAccountJsonString);
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
        logToLogcat(":acquireToken(Activity,Scopes,Callback)");

        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            logToLogcat("persistedAccount is null. Cannot invoke acquireToken");
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
            return;
        }

        acquireToken(
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
    }

    @Override
    public void acquireToken(@NonNull final AcquireTokenParameters acquireTokenParameters) {
        logToLogcat(":acquireToken(AcquireTokenParameters)");
        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount != null) {
            // If the account exists, overwrite Account and ignore loginHint.
            acquireTokenParameters.setAccount(persistedAccount);
            acquireTokenParameters.setLoginHint("");
        }

        super.acquireToken(acquireTokenParameters);
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @NonNull final String authority,
                                        @NonNull final SilentAuthenticationCallback callback) {
        logToLogcat(":acquireTokenSilentAsync(SCOPES,AUTHORITY,CALLBACK)");

        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            logToLogcat("persistedAccount is null. Cannot invoke acquireTokenSilentAsync(SCOPES,AUTHORITY,CALLBACK)");
            callback.onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
            return;
        }

        acquireTokenSilent(
                scopes,
                persistedAccount,
                authority,
                false,
                null, // claimsRequest
                callback
        );
    }

    @WorkerThread
    public IAuthenticationResult acquireTokenSilent(@NonNull final String[] scopes,
                                                    @NonNull final String authority) throws MsalException, InterruptedException {
        logToLogcat(":acquireTokenSilent(SCOPES,AUTHORITY)");

        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            logToLogcat("persistedAccount is null. Cannot invoke acquireTokenSilent(SCOPES,AUTHORITY)");
            throw new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT);
        }

        return acquireTokenSilentSync(scopes, authority, persistedAccount, false);
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) {
        logToLogcat(":acquireTokenSilentAsync(AcquireTokenSilentParameters)");

        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            logToLogcat("persistedAccount is null. Cannot invoke acquireTokenSilentAsync(AcquireTokenSilentParameters)");
            acquireTokenSilentParameters.getCallback().onError(new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT));
            return;
        }

        // In SingleAccount mode, always overwrite 'Account' with current account.
        acquireTokenSilentParameters.setAccount(persistedAccount);

        super.acquireTokenSilentAsync(acquireTokenSilentParameters);
    }

    @Override
    public IAuthenticationResult acquireTokenSilent(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) throws InterruptedException, MsalException {
        logToLogcat(":acquireTokenSilent(AcquireTokenSilentParameters)");

        final IAccount persistedAccount = getPersistedCurrentAccount();
        if (persistedAccount == null) {
            logToLogcat("persistedAccount is null. Cannot invoke acquireTokenSilent(AcquireTokenSilentParameters)");
            throw new MsalClientException(MsalClientException.NO_CURRENT_ACCOUNT);
        }

        // In SingleAccount mode, always overwrite 'Account' with current account.
        acquireTokenSilentParameters.setAccount(persistedAccount);

        return super.acquireTokenSilent(acquireTokenSilentParameters);
    }
}
