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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.controllers.BrokerMsalController;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;

import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;

import com.microsoft.identity.common.exception.BaseException;

import com.microsoft.identity.common.internal.controllers.ApiDispatcher;
import com.microsoft.identity.common.internal.controllers.LoadAccountCommand;
import com.microsoft.identity.common.internal.controllers.RemoveAccountCommand;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.request.ILocalAuthenticationCallback;
import com.microsoft.identity.common.internal.request.OperationParameters;

import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;

import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;


import java.util.List;

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


    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final PublicClientApplicationConfiguration developerConfig,
                                                   @NonNull final Boolean isSharedDevice) {
        super(context, developerConfig);
        initializeSharedPreferenceFileManager(context);
        mIsSharedDevice = isSharedDevice;
    }

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final String clientId,
                                                   @NonNull final Boolean isSharedDevice) {
        super(context, clientId);
        initializeSharedPreferenceFileManager(context);
        mIsSharedDevice = isSharedDevice;
    }

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final String clientId,
                                                   @NonNull final String authority,
                                                   @NonNull final Boolean isSharedDevice) {
        super(context, clientId, authority);
        initializeSharedPreferenceFileManager(context);
        mIsSharedDevice = isSharedDevice;
    }

    private void initializeSharedPreferenceFileManager(@NonNull final Context context) {
        sharedPreferencesFileManager = new SharedPreferencesFileManager(
                context,
                SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                new StorageHelper(context));
    }


    @Override
    public void getCurrentAccount(final CurrentAccountCallback callback) {
        final String methodName = ":getCurrentAccount";
        final PublicClientApplicationConfiguration configuration = getConfiguration();


        try {
            if (MSALControllerFactory.brokerEligible(
                    configuration.getAppContext(),
                    configuration.getDefaultAuthority(),
                    configuration) && mIsSharedDevice) {
                getCurrentAccountFromSharedDevice(callback, configuration);
                return;
            }

            ApiDispatcher.initializeDiagnosticContext();

            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Getting the current account"
            );


            final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration);
            final LoadAccountCommand command = new LoadAccountCommand(
                    params,
                    MSALControllerFactory.getAcquireTokenController(
                            mPublicClientConfiguration.getAppContext(),
                            params.getAuthority(),
                            mPublicClientConfiguration
                    ),
                    new TaskCompletedCallbackWithError<List<ICacheRecord>, Exception>() {
                        @Override
                        public void onTaskCompleted(final List<ICacheRecord> result) {
                            if (null == result || result.size() == 0) {
                                com.microsoft.identity.common.internal.logging.Logger.verbose(
                                        TAG + methodName,
                                        "No account found.");
                                checkCurrentAccountNotifyCallback(callback, null);
                            } else {
                                // First, transform the result into IAccount + TenantProfile form
                                final List<IAccount>
                                        accounts = AccountAdapter.adapt(result);

                                final String trimmedIdentifier = getPersistedCurrentAccount().getHomeAccountId();

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
                                        //callback.onTaskCompleted(account);
                                        checkCurrentAccountNotifyCallback(callback, result);
                                        return;
                                    }
                                }

                                checkCurrentAccountNotifyCallback(callback, null);
                            }
                        }

                        @Override
                        public void onError(final Exception exception) {
                            com.microsoft.identity.common.internal.logging.Logger.error(
                                    TAG + methodName,
                                    exception.getMessage(),
                                    exception
                            );
                            callback.onError(exception);
                        }
                    }


            );

            ApiDispatcher.getAccounts(command);

        }catch(MsalClientException clientException){
            callback.onError(clientException);
        }
    }


    private void getCurrentAccountFromSharedDevice(final CurrentAccountCallback callback, final PublicClientApplicationConfiguration configuration){
        new BrokerMsalController().getCurrentAccount(
                configuration,
                new TaskCompletedCallbackWithError<List<ICacheRecord>, Exception>() {
                    @Override
                    public void onTaskCompleted(List<ICacheRecord> cacheRecords) {
                        MultiTenantAccount currentAccount = getPersistedCurrentAccount();
                        MultiTenantAccount newAccount = getAccountFromICacheRecordList(cacheRecords);

                        String currentAccountHomeAccountId = currentAccount == null ? "" : currentAccount.getHomeAccountId();
                        String newAccountHomeAccountId = newAccount == null ? "" : newAccount.getHomeAccountId();

                        boolean isCurrentAccountChanged = !currentAccountHomeAccountId.equalsIgnoreCase(newAccountHomeAccountId);
                        if (isCurrentAccountChanged) {
                            callback.onAccountChanged(currentAccount, newAccount);
                        }

                        persistCurrentAccount(cacheRecords);
                        callback.onAccountLoaded(newAccount);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);

                    }
                });
    }

    private void checkCurrentAccountNotifyCallback(final CurrentAccountCallback callback, List<ICacheRecord> newAccountRecords){
        IAccount localAccount = getPersistedCurrentAccount();
        IAccount newAccount = newAccountRecords == null ? null : getAccountFromICacheRecordList(newAccountRecords);

        if (localAccount == null) {
            if (newAccount != null) {
                callback.onAccountChanged(null, newAccount);
            }
        } else if (localAccount.getId() != newAccount.getId()) {
            callback.onAccountChanged(localAccount, newAccount);
        }

        persistCurrentAccount(newAccountRecords);
        callback.onAccountLoaded(newAccount);


    }


    @Override
    public void signIn(@NonNull Activity activity,

                @NonNull String[] scopes,
                @NonNull AuthenticationCallback callback) {
        acquireToken(
                activity,
                new String[]{"user.read"},
                null, // account
                null, // uiBehavior
                null, // extraQueryParams
                scopes, // extraScopes
                null, // authority
                callback,
                null, // loginHint
                null // claimsRequest
        );
    }

    @Override
    protected ILocalAuthenticationCallback getLocalAuthenticationCallback(final AuthenticationCallback authenticationCallback) {

        return new ILocalAuthenticationCallback() {

            @Override
            public void onSuccess(ILocalAuthenticationResult localAuthenticationResult) {

                //Get Local Authentication Result then check if the current account is set or not

                MultiTenantAccount newAccount =  getAccountFromICacheRecordList(localAuthenticationResult.getCacheRecordWithTenantProfileData());

                if(didExistingCurrentAccountChange(newAccount)){
                    //Throw on Error with UserMismatchException
                    authenticationCallback.onError(new MsalClientException(MsalClientException.CURRENT_ACCOUNT_MISMATCH));
                    return;
                }else{
                    persistCurrentAccount(localAuthenticationResult.getCacheRecordWithTenantProfileData());
                }

                IAuthenticationResult authenticationResult = AuthenticationResultAdapter.adapt(localAuthenticationResult);
                authenticationCallback.onSuccess(authenticationResult);
            }

            @Override
            public void onError(BaseException exception) {
                MsalException msalException = MsalExceptionAdapter.msalExceptionFromBaseException(exception);
                authenticationCallback.onError(msalException);
            }

            @Override
            public void onCancel() {
                authenticationCallback.onCancel();
            }
        };
    }

    private boolean didExistingCurrentAccountChange(MultiTenantAccount newAccount){
        MultiTenantAccount persistedAccount = getPersistedCurrentAccount();
        return (persistedAccount != null || newAccount.getId() != persistedAccount.getId()) ? true : false;

    }

    @Override
    public void signOut(@NonNull final TaskCompletedCallbackWithError<Boolean, Exception> callback){
        final String methodName = ":signOut";
        final PublicClientApplicationConfiguration configuration = getConfiguration();


        try{
            if (MSALControllerFactory.brokerEligible(
                    configuration.getAppContext(),
                    configuration.getDefaultAuthority(),
                    configuration) && mIsSharedDevice) {
                removeAccountFromSharedDevice(callback, configuration);
                return;
            }


            ApiDispatcher.initializeDiagnosticContext();

            MultiTenantAccount persistedCurrentAccount = getPersistedCurrentAccount();

            if (persistedCurrentAccount != null) {
                final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration);
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
                        callback
                );

                ApiDispatcher.removeAccount(command);
            } else {
                callback.onError(new MsalClientException(MsalClientException.NO_ACCOUNT_TO_SIGN_OUT, "No account is currently signed in to your Single Account Public Client Application"));
            }

        }catch(MsalClientException clientException){
            callback.onError(clientException);
        }


    }

    private void removeAccountFromSharedDevice(@NonNull final TaskCompletedCallbackWithError<Boolean, Exception> callback,
                                               PublicClientApplicationConfiguration configuration){

            new BrokerMsalController().removeAccountFromSharedDevice(
                    configuration,
                    new TaskCompletedCallbackWithError<Boolean, Exception>() {
                        @Override
                        public void onTaskCompleted(Boolean success) {
                            if (success) {
                                persistCurrentAccount(null);
                            }

                            callback.onTaskCompleted(success);
                        }

                        @Override
                        public void onError(Exception exception) {
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
        String currentAccountJsonString = sharedPreferencesFileManager.getString(CURRENT_ACCOUNT_SHARED_PREFERENCE_KEY);
        if (currentAccountJsonString == null) {
            return null;
        }

        List<ICacheRecord> cacheRecordList = MsalBrokerResultAdapter.getICacheRecordListFromJsonString(currentAccountJsonString);
        return getAccountFromICacheRecordList(cacheRecordList);
    }

    /**
     * Persists current account to shared preference.
     *
     * @param cacheRecords list of cache record that belongs to an account.
     *                     Please note that this layer will not verify if the list ubelongs to a single account or not.
     */
    private void persistCurrentAccount(@Nullable List<ICacheRecord> cacheRecords) {

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
    private MultiTenantAccount getAccountFromICacheRecordList(@NonNull List<ICacheRecord> cacheRecords) {
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
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @NonNull final AuthenticationCallback callback) {
        acquireTokenSilent(
                scopes,
                getPersistedCurrentAccount(),
                null, // authority
                false, // forceRefresh
                null, // claimsRequest
                callback
        );
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @Nullable final String authority,
                                        final boolean forceRefresh,
                                        @NonNull final AuthenticationCallback callback) {
        acquireTokenSilent(
                scopes,
                getPersistedCurrentAccount(),
                authority,
                forceRefresh,
                null, // claimsRequest
                callback
        );
    }


}
