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
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;

import java.util.ArrayList;
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
                                                   @NonNull final PublicClientApplicationConfiguration developerConfig) {
        super(context, developerConfig);
        initializeSharedPreferenceFileManager(context);
    }

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final String clientId) {
        super(context, clientId);
        initializeSharedPreferenceFileManager(context);
    }

    protected SingleAccountPublicClientApplication(@NonNull final Context context,
                                                   @NonNull final String clientId,
                                                   @NonNull final String authority) {
        super(context, clientId, authority);
        initializeSharedPreferenceFileManager(context);
    }

    private void initializeSharedPreferenceFileManager(@NonNull final Context context) {
        sharedPreferencesFileManager = new SharedPreferencesFileManager(
                context,
                SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                new StorageHelper(context));
    }

    @Override
    public void getCurrentAccount(@NonNull final CurrentAccountCallback callback) {
        final PublicClientApplicationConfiguration configuration = getConfiguration();

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

    @Override
    public void removeCurrentAccount(@NonNull final TaskCompletedCallbackWithError<Boolean, Exception> callback) {
        final PublicClientApplicationConfiguration configuration = getConfiguration();

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
    @Nullable
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
}
