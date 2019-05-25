package com.microsoft.identity.client;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.cache.IShareSingleSignOnState;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.ApiDispatcher;
import com.microsoft.identity.common.internal.controllers.LoadAccountCommand;
import com.microsoft.identity.common.internal.controllers.RemoveAccountCommand;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.migration.AdalMigrationAdapter;
import com.microsoft.identity.common.internal.migration.TokenMigrationCallback;
import com.microsoft.identity.common.internal.migration.TokenMigrationUtility;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.util.StringUtil;

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

    /**
     * Asynchronously returns a List of {@link IAccount} objects for which this application has RefreshTokens.
     *
     * @param callback The callback to notify once this action has finished.
     */
    @Override
    public void getAccounts(@NonNull final LoadAccountCallback callback) {
        ApiDispatcher.initializeDiagnosticContext();
        final String methodName = ":getAccounts";
        final List<AccountRecord> accounts =
                mPublicClientConfiguration
                        .getOAuth2TokenCache()
                        .getAccounts(
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
            final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration);
            final LoadAccountCommand command = new LoadAccountCommand(
                    params,
                    MSALControllerFactory.getAcquireTokenController(
                            mPublicClientConfiguration.getAppContext(),
                            params.getAuthority(),
                            mPublicClientConfiguration
                    ),
                    getLoadAccountsCallback(callback)
            );

            ApiDispatcher.getAccounts(command);
        } catch (final MsalClientException e) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(e);
                }
            });
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
        final String methodName = ":getAccount";

        ApiDispatcher.initializeDiagnosticContext();

        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Get account with the identifier."
        );

        try {
            final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration);
            final LoadAccountCommand command = new LoadAccountCommand(
                    params,
                    MSALControllerFactory.getAcquireTokenController(
                            mPublicClientConfiguration.getAppContext(),
                            params.getAuthority(),
                            mPublicClientConfiguration
                    ),
                    new TaskCompletedCallbackWithError<List<AccountRecord>, Exception>() {
                        @Override
                        public void onTaskCompleted(final List<AccountRecord> result) {
                            if (null == result || result.size() == 0) {
                                com.microsoft.identity.common.internal.logging.Logger.verbose(
                                        TAG + methodName,
                                        "No account found.");
                                callback.onTaskCompleted(null);
                            } else {
                                for (AccountRecord accountRecord : result) {
                                    if (accountRecord.getHomeAccountId().equalsIgnoreCase(identifier.trim())) {
                                        com.microsoft.identity.common.internal.logging.Logger.verbose(
                                                TAG + methodName,
                                                "One account found matching the homeAccountIdentifier provided.");
                                        callback.onTaskCompleted(AccountAdapter.adapt(accountRecord));
                                        return;
                                    } else if (accountRecord.getLocalAccountId().equalsIgnoreCase(identifier.trim())) {
                                        com.microsoft.identity.common.internal.logging.Logger.verbose(
                                                TAG + methodName,
                                                "One account found matching the localAccountIdentifier provided.");
                                        callback.onTaskCompleted(AccountAdapter.adapt(accountRecord));
                                        return;
                                    } else if (accountRecord.getUsername().equalsIgnoreCase(identifier.trim())) {
                                        com.microsoft.identity.common.internal.logging.Logger.verbose(
                                                TAG + methodName,
                                                "One account found matching the username provided.");
                                        callback.onTaskCompleted(AccountAdapter.adapt(accountRecord));
                                        return;
                                    } else {
                                        com.microsoft.identity.common.internal.logging.Logger.verbose(
                                                TAG + methodName,
                                                "No account found.");
                                        callback.onTaskCompleted(null);
                                    }
                                }
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
    public void removeAccount(@Nullable final IAccount account,
                              @NonNull final RemoveAccountCallback callback) {
        ApiDispatcher.initializeDiagnosticContext();
        //create the parameter
        try {
            if (null == account
                    || null == account.getHomeAccountIdentifier()
                    || StringUtil.isEmpty(account.getHomeAccountIdentifier().getIdentifier())) {
                com.microsoft.identity.common.internal.logging.Logger.warn(
                        TAG,
                        "Requisite IAccount or IAccount fields were null. Insufficient criteria to remove IAccount."
                );

                callback.onTaskCompleted(false);
            } else {
                final OperationParameters params = OperationParametersAdapter.createOperationParameters(mPublicClientConfiguration);
                if (null == getAccountRecord(account)) {
                    // If could not find the account record in local msal cache
                    // Create the pass along account record object to broker
                    final AccountRecord requestAccountRecord = new AccountRecord();
                    requestAccountRecord.setEnvironment(account.getEnvironment());
                    requestAccountRecord.setUsername(account.getUsername());
                    requestAccountRecord.setHomeAccountId(account.getHomeAccountIdentifier().getIdentifier());
                    requestAccountRecord.setLocalAccountId(account.getAccountIdentifier().getIdentifier());
                    params.setAccount(requestAccountRecord);
                } else {
                    params.setAccount(getAccountRecord(account));
                }

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
            }
        } catch (final MsalClientException e) {
            callback.onError(e);
        }
    }
}
