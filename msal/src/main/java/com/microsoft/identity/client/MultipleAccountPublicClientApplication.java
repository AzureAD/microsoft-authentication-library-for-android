package com.microsoft.identity.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.client.internal.controllers.BrokerMsalController;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.cache.IShareSingleSignOnState;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.ApiDispatcher;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.migration.AdalMigrationAdapter;
import com.microsoft.identity.common.internal.migration.TokenMigrationCallback;
import com.microsoft.identity.common.internal.migration.TokenMigrationUtility;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipleAccountPublicClientApplication extends PublicClientApplication
    implements IMultipleAccountPublicClientApplication {
    private static final String TAG = MultipleAccountPublicClientApplication.class.getSimpleName();

    protected MultipleAccountPublicClientApplication(@NonNull final Context context,
                                                     @NonNull final PublicClientApplicationConfiguration developerConfig) {
        super(context, developerConfig);
    }

    protected MultipleAccountPublicClientApplication(@NonNull final Context context,
                                                     @NonNull final String clientId) {
        super(context, clientId);
    }

    protected MultipleAccountPublicClientApplication(@NonNull final Context context,
                                                     @NonNull final String clientId,
                                                     @NonNull final String authority) {
        super(context, clientId, authority);
    }

    @Override
    public void getAccounts(@NonNull final AccountsLoadedCallback callback) {
        ApiDispatcher.initializeDiagnosticContext();
        final String methodName = ":getAccounts";
        final List<AccountRecord> accounts = getLocalAccounts();

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
                        // Merge migrated accounts with broker or local accounts.
                        if (MSALControllerFactory.brokerEligible(
                            mPublicClientConfiguration.getAppContext(),
                            mPublicClientConfiguration.getDefaultAuthority(),
                            mPublicClientConfiguration)) {
                            postBrokerAndLocalAccountsResult(handler, callback);
                        } else {
                            postLocalAccountsResult(handler, callback);
                        }
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

            if (MSALControllerFactory.brokerEligible(
                mPublicClientConfiguration.getAppContext(),
                mPublicClientConfiguration.getDefaultAuthority(),
                mPublicClientConfiguration)) {
                postBrokerAndLocalAccountsResult(handler, callback);
            } else {
                postLocalAccountsResult(handler, callback);
            }
        }
    }

    /**
     * Helper method which returns all the local accounts using {@link AccountsLoadedCallback}
     * @param handler : handler to post
     * @param callback: AccountsLoadedCallback
     */
    private void postLocalAccountsResult(final Handler handler, final AccountsLoadedCallback callback) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                List<IAccount> accountsToReturn = new ArrayList<>();
                for (AccountRecord accountRecord : getLocalAccounts()) {
                    accountsToReturn.add(AccountAdapter.adapt(accountRecord));
                }

                callback.onAccountsLoaded(accountsToReturn);
            }
        });
    }

    /**
     * Helper method which returns both broker and local accounts using {@link AccountsLoadedCallback}
     * @param handler : handler to post
     * @param callback: AccountsLoadedCallback
     */
    private void postBrokerAndLocalAccountsResult(final Handler handler, final AccountsLoadedCallback callback) {

        final String methodName = ":postBrokerAndLocalAccountsResult";

        new BrokerMsalController().getBrokerAccounts(
            mPublicClientConfiguration,
            new BrokerAccountsLoadedCallback() {
                @Override
                public void onAccountsLoaded(final List<AccountRecord> accountRecords) {
                    com.microsoft.identity.common.internal.logging.Logger.verbose(
                        TAG + methodName,
                        "Accounts loaded from broker "
                            + (accountRecords == null ? 0 : accountRecords.size())
                    );

                    // merge account
                    final List<IAccount> accountList = new ArrayList<>();
                    final List<AccountRecord> accountRecordList = new ArrayList<>();

                    if (accountRecords != null) {
                        //Add broker accounts
                        accountRecordList.addAll(accountRecords);
                    }

                    //Add local accounts
                    accountRecordList.addAll(getLocalAccounts());

                    if (accountRecordList.size() > 0) {
                        for (AccountRecord accountRecord : accountRecordList) {
                            accountList.add(AccountAdapter.adapt(accountRecord));
                        }
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onAccountsLoaded(accountList);
                        }
                    });
                }
            });
    }

    /**
     * Returns a List of {@link IAccount} objects for which this application has RefreshTokens.
     *
     * @return An immutable List of IAccount objects - empty if no IAccounts exist.
     */
    private List<AccountRecord> getLocalAccounts() {
        // Grab the Accounts from the common cache
        final List<AccountRecord> accountsInCache =
            mPublicClientConfiguration
                .getOAuth2TokenCache()
                .getAccounts(
                    null, // * wildcard
                    mPublicClientConfiguration.getClientId()
                );

        return accountsInCache;
    }

    @Override
    @Nullable
    public IAccount getAccount(@NonNull final String homeAccountIdentifier,
                               @Nullable final String authority) {
        final String methodName = ":getAccount";

        ApiDispatcher.initializeDiagnosticContext();

        String realm = StringUtil.getTenantInfo(homeAccountIdentifier).second;

        Authority authorityObj = Authority.getAuthorityFromAuthorityUrl(authority);

        if (authorityObj instanceof AzureActiveDirectoryAuthority) {
            final AzureActiveDirectoryAuthority aadAuthority = (AzureActiveDirectoryAuthority) authorityObj;
            final AzureActiveDirectoryAudience audience = aadAuthority.getAudience();
            realm = audience.getTenantId();
        } else {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                TAG + methodName,
                "Provided authority was not AAD - defaulting to parsed home_account_id"
            );
        }

        AccountRecord accountToReturn = null;

        if (null != realm) {
            accountToReturn = AccountAdapter.getAccountInternal(
                mPublicClientConfiguration.getClientId(),
                mPublicClientConfiguration.getOAuth2TokenCache(),
                homeAccountIdentifier,
                realm
            );
        } else {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                TAG + methodName,
                "Realm could not be resolved. Returning null."
            );
        }

        return null == accountToReturn ? null : AccountAdapter.adapt(accountToReturn);
    }

    @Override
    public void removeAccount(@Nullable final IAccount account, final AccountRemovedListener callback) {
        ApiDispatcher.initializeDiagnosticContext();
        if (null == account
            || null == account.getHomeAccountIdentifier()
            || StringUtil.isEmpty(account.getHomeAccountIdentifier().getIdentifier())) {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                TAG,
                "Requisite IAccount or IAccount fields were null. Insufficient criteria to remove IAccount."
            );

            callback.onAccountRemoved(false);
        }

        // FEATURE SWITCH: Set to false to allow deleting Accounts in a tenant-specific way.
        final boolean deleteAccountsInAllTenants = true;

        final String realm = deleteAccountsInAllTenants ? null : getRealm(account);

        final boolean localRemoveAccountSuccess = !mPublicClientConfiguration
            .getOAuth2TokenCache()
            .removeAccount(
                account.getEnvironment(),
                mPublicClientConfiguration.getClientId(),
                account.getHomeAccountIdentifier().getIdentifier(),
                realm
            ).isEmpty();

        if (MSALControllerFactory.brokerEligible(
            mPublicClientConfiguration.getAppContext(),
            mPublicClientConfiguration.getDefaultAuthority(),
            mPublicClientConfiguration)) {

            //Remove the account from Broker
            new BrokerMsalController().removeAccountFromBrokerCache(
                account,
                mPublicClientConfiguration,
                callback
            );
        } else {
            callback.onAccountRemoved(localRemoveAccountSuccess);
        }
    }
}
