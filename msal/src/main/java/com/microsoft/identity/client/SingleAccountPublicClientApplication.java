package com.microsoft.identity.client;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.client.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.CacheRecord;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;

import java.util.ArrayList;
import java.util.List;

public class SingleAccountPublicClientApplication extends PublicClientApplication
        implements ISingleAccountPublicClientApplication {
    private static final String TAG = SingleAccountPublicClientApplication.class.getSimpleName();

    /**
     * Name of the shared preference cache for storing current account.
     * */
    public static final String SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES =
            "com.microsoft.identity.client.single_account_credential_cache";

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final PublicClientApplicationConfiguration developerConfig) {
        super(context, developerConfig);
    }

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final String clientId) {
        super(context, clientId);
    }

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final String clientId,
                                                   @NonNull final String authority) {
        super(context, clientId, authority);
    }

    @Override
    public void getCurrentAccount(@NonNull final CurrentAccountCallback callback) {
        final PublicClientApplicationConfiguration configuration = getConfiguration();

        new BrokerMsalController().getCurrentAccount(
                configuration,
                new TaskCompletedCallbackWithError<List<ICacheRecord>, Exception>() {
                    @Override
                    public void onTaskCompleted(List<ICacheRecord> cacheRecords) {
                        MultiTenantAccount currentAccount = getPersistedCurrentAccount(configuration.getAppContext());
                        MultiTenantAccount newAccount = getAccountFromICacheRecordList(cacheRecords);

                        String currentAccountHomeAccountId = currentAccount == null ? "" : currentAccount.getHomeAccountId();
                        String newAccountHomeAccountId = newAccount == null ? "" :  newAccount.getHomeAccountId();

                        boolean isCurrentAccountChanged = !currentAccountHomeAccountId.equalsIgnoreCase(newAccountHomeAccountId);
                        if (isCurrentAccountChanged){
                            persistCurrentAccount(configuration.getAppContext(), cacheRecords);
                            callback.onAccountChanged(currentAccount, newAccount);
                        }

                        callback.onAccountLoaded(newAccount);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                });
    }

    @Override
    public void removeCurrentAccount(@NonNull final TaskCompletedCallbackWithError<Boolean, Exception> callback) {
        final PublicClientApplicationConfiguration configuration = getConfiguration();

        new BrokerMsalController().removeAccountFromSharedDevice(
                configuration,
                new TaskCompletedCallbackWithError<Boolean, Exception>() {
                    @Override
                    public void onTaskCompleted(Boolean success) {
                        if (success) {
                            persistCurrentAccount(configuration.getAppContext(), null);
                        }

                        callback.onTaskCompleted(success);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                });
    }

    private MsalOAuth2TokenCache getTokenCache(@NonNull final Context appContext,
                                               @NonNull final IAccountCredentialCache accountCredentialCache) {
        return new MsalOAuth2TokenCache(
                appContext,
                accountCredentialCache,
                new MicrosoftStsAccountCredentialAdapter());
    }

    private IAccountCredentialCache getCurrentAccountCredentialCache(final Context appContext){
        return new SharedPreferencesAccountCredentialCache(
                new CacheKeyValueDelegate(),
                new SharedPreferencesFileManager(
                        appContext,
                        SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                        new StorageHelper(appContext)
                )
        );
    }

    /**
     * Get current account that is persisted in shared preference.
     *
     * @param appContext
     * @return a persisted MultiTenantAccount. This could be null.
     * */
    @Nullable
    private MultiTenantAccount getPersistedCurrentAccount(@NonNull final Context appContext) {
        final String methodName = ":getPersistedCurrentAccount";
        final IAccountCredentialCache accountCredentialCache = getCurrentAccountCredentialCache(appContext);
        final MsalOAuth2TokenCache msalOAuth2TokenCache = getTokenCache(appContext, accountCredentialCache);

        List<AccountRecord> accountRecordList = accountCredentialCache.getAccounts();
        if (accountRecordList.size() == 0) {
            return null;
        }

        List<ICacheRecord> cacheRecordList = new ArrayList<>();
        for (final AccountRecord accountRecord : accountRecordList) {
            List<IdTokenRecord> idTokenRecordList = msalOAuth2TokenCache.getIdTokensForAccountRecord(null, accountRecord);

            if (idTokenRecordList.size() != 1) {
                com.microsoft.identity.common.internal.logging.Logger.verbose(
                        TAG + methodName,
                        "Find " + idTokenRecordList.size() + " associated IdTokenRecord. Skip this accountRecord.");
                continue;
            }

            CacheRecord cacheRecord = new CacheRecord();
            cacheRecord.setAccount(accountRecord);
            cacheRecord.setIdToken(idTokenRecordList.get(0));
            cacheRecordList.add(cacheRecord);
        }

        return getAccountFromICacheRecordList(cacheRecordList);
    }

    /**
     * Persists current account to shared preference.
     *
     * @param appContext
     * @param cacheRecords list of cache record that belongs to an account.
     *                     Please note that this layer will not verify if the list belongs to a single account or not.
     * */
    private void persistCurrentAccount(@NonNull final Context appContext,
                                       @Nullable List<ICacheRecord> cacheRecords) {
        final IAccountCredentialCache accountCredentialCache = getCurrentAccountCredentialCache(appContext);
        final MsalOAuth2TokenCache msalOAuth2TokenCache = getTokenCache(appContext, accountCredentialCache);

        // clear the current entry.
        accountCredentialCache.clearAll();

        if (cacheRecords == null || cacheRecords.size() == 0) {
            // Do nothing.
            return;
        }

        for(ICacheRecord record : cacheRecords){
            msalOAuth2TokenCache.save(record.getAccount(), record.getIdToken());
        }
    }

    /**
     * Get a MultiTenantAccount from a list of ICacheRecord.
     *
     * @param cacheRecords list of cache record that belongs to an account.
     *                     If the list can be converted to multiple accounts, only the first one will be returned.
     * */
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
}
