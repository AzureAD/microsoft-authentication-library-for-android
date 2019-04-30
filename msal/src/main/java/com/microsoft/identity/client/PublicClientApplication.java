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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.client.internal.configuration.LogLevelDeserializer;
import com.microsoft.identity.client.internal.controllers.BrokerMsalController;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.client.internal.telemetry.DefaultEvent;
import com.microsoft.identity.client.internal.telemetry.Defaults;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AuthorityDeserializer;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudienceDeserializer;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.ICacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IShareSingleSignOnState;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.controllers.ApiDispatcher;
import com.microsoft.identity.common.internal.controllers.InteractiveTokenCommand;
import com.microsoft.identity.common.internal.controllers.TokenCommand;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.migration.AdalMigrationAdapter;
import com.microsoft.identity.common.internal.migration.TokenMigrationCallback;
import com.microsoft.identity.common.internal.migration.TokenMigrationUtility;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.ILocalAuthenticationCallback;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.msal.BuildConfig;
import com.microsoft.identity.msal.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.internal.cache.SharedPreferencesAccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;

/**
 * <p>
 * This is the entry point for developer to create public native applications and make API calls to acquire tokens.
 * <p><b>Client ID:</b> The clientID of your application is a unique identifier which can be obtained from the app registration portal.</p>
 * <p><b>AuthorityMetadata:</b> A URL indicating a directory that MSAL can use to obtain tokens. In Azure AD
 * it is of the form https://<[nstance]/[tenant], where [instance] is the directory host (e.g. https://login.microsoftonline.com)
 * and [tenant] is an identifier within the directory itself (e.g. a domain associated to the
 * tenant, such as contoso.onmicrosoft.com, or the GUID representing the  TenantID property of the directory)
 * For B2C, it is of the form https://[instance]/tfp/[tenant]/[policy] where instance and tenant are same as Azure AD, and [policy] is a string like signup</p>
 * MSAL {@link PublicClientApplication} provides three constructors allowing the client id to be set either via AndroidManifest.xml metadata or using constructor parameters.
 * Similarly, if developer chooses not to use the default authority https://login.microsoftonline.com, an alternate can also be configured using the manifest, constructor parameters, or in acquire token calls.
 * </p>
 * <p>
 * Redirect is auto-generated in the library in the format of msal<client-id>://auth, and it cannot be overridden.
 * </p>
 * <p>
 * Developer <b>MUST</b> have {@link BrowserTabActivity} declared in their manifest, which must have the correct intent-filter configured. If the wrong scheme and host is provided, the sdk will fail the {@link PublicClientApplication} creation.
 * <p>
 * Expected format will be:
 * <pre>
 * &lt;activity
 *     android:name="com.microsoft.identity.client.BrowserTabActivity"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="android.intent.action.VIEW" /&gt;
 *         &lt;category android:name="android.intent.category.DEFAULT" /&gt;
 *         &lt;category android:name="android.intent.category.BROWSABLE" /&gt;
 *         &lt;data android:scheme="msal&lt;AppClientId&gt;"
 *              android:host="auth" /&gt;
 *     &lt;/intent-filter&gt;
 * &lt;/activity&gt;
 * </pre>
 * </p>
 * <p>Other Terminology:</p>
 * <p>
 * <p><b>Scopes:</b>Permissions that the developers wants included in the access token received . Not all scopes are
 * guaranteed to be included in the access token returned.
 * </p>
 * <p>
 * <b>Login Hint:</b> Usually an email, to pass to the service at the beginning of the interactive authentication flow.
 * </p>
 * <p>
 * <b>Extra Scopes to Consent:</b>  Permissions you want the user to consent to in the same authentication flow,
 * but won't be included in the returned access token.
 * </p>
 * </p>
 */
public class PublicClientApplication implements IPublicClientApplication, IMultipleAccountPublicClientApplication {
    private static final String TAG = PublicClientApplication.class.getSimpleName();

    private static final String INTERNET_PERMISSION = "android.permission.INTERNET";
    private static final String ACCESS_NETWORK_STATE_PERMISSION = "android.permission.ACCESS_NETWORK_STATE";

    private PublicClientApplicationConfiguration mPublicClientConfiguration;

    /**
     * {@link PublicClientApplication#create(Context, int, ApplicationCreatedListener)} will read the client id and other configuration settings from the
     * file included in your applications resources.
     * <p>
     * For more information on adding configuration files to your applications resources please
     *
     * @param context              Application's {@link Context}. The sdk requires the application context to be passed in
     *                             {@link PublicClientApplication}. Cannot be null.
     *                             <p>
     *                             Note: The {@link Context} should be the application context instead of the running activity's context, which could potentially make the sdk hold a
     *                             strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                             </p>
     * @param configFileResourceId The resource ID of the raw file containing the JSON configuration for the PublicClientApplication
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * <p>
     * For more information on the schema of the MSAL config json please
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     * @param listener a callback to be invoked when the object is successfully created.
     */
    public static void create(@NonNull final Context context,
                              final int configFileResourceId,
                              @NonNull final ApplicationCreatedListener listener){
        if (context == null) {
            throw new IllegalArgumentException("context is null.");
        }

        create(context,
            loadConfiguration(context, configFileResourceId),
            listener);
    }

    /**
     * {@link PublicClientApplication#create(Context, File, ApplicationCreatedListener)} will read the client id and other configuration settings from the
     * specified file.
     *
     * @param context    Application's {@link Context}. The sdk requires the application context to be passed in
     *                   {@link PublicClientApplication}. Cannot be null.
     *                   <p>
     *                   Note: The {@link Context} should be the application context instead of the running activity's context, which could potentially make the sdk hold a
     *                   strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                   </p>
     * @param configFile The file containing the JSON configuration for the PublicClientApplication
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * <p>
     * For more information on the schema of the MSAL config json please
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     * @param listener a callback to be invoked when the object is successfully created.
     */
    public static void create(@NonNull final Context context,
                              final File configFile,
                              @NonNull final ApplicationCreatedListener listener){
        create(context,
            loadConfiguration(configFile),
            listener);
    }

    /**
     * {@link PublicClientApplication#create(Context, String, ApplicationCreatedListener)} allows the client id to be passed instead of
     * providing through the AndroidManifest metadata. If this constructor is called, the default authority https://login.microsoftonline.com/common will be used.
     *
     * @param context  Application's {@link Context}. The sdk requires the application context to be passed in
     *                 {@link PublicClientApplication}. Cannot be null.
     *                 <p>
     *                 Note: The {@link Context} should be the application context instead of the running activity's context, which could potentially make the sdk hold a
     *                 strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                 </p>
     * @param clientId The application's client id.
     * @param listener a callback to be invoked when the object is successfully created.
     */
    public static void create(@NonNull final Context context,
                              @NonNull final String clientId,
                              @NonNull final ApplicationCreatedListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("Context is null.");
        }

        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("client id is empty or null");
        }
        new BrokerMsalController().getBrokerAccountMode(context, new BrokerAccountModeCallback() {
            @Override
            public void onGetMode(String mode) {
                if (AuthenticationConstants.Broker.BROKER_ACCOUNT_MODE_SINGLE_ACCOUNT.equalsIgnoreCase(mode)) {
                    // TODO: return SingleAccountPublicClientApplication
                    listener.onCreated(new PublicClientApplication(context, clientId));
                } else {
                    // TODO: return MultipleAccountPublicClientApplication
                    listener.onCreated(new PublicClientApplication(context, clientId));
                }
            }
        });
    }

    /**
     * {@link PublicClientApplication#create(Context, String, String, ApplicationCreatedListener)} allows the client id and authority to be passed instead of
     * providing them through metadata.
     *
     * @param context   Application's {@link Context}. The sdk requires the application context to be passed in
     *                  {@link PublicClientApplication}. Cannot be null.
     *                  <p>
     *                  Note: The {@link Context} should be the application context instead of an running activity's context, which could potentially make the sdk hold a
     *                  strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                  </p>
     * @param clientId  The application client id.
     * @param authority The default authority to be used for the authority.
     * @param listener  a callback to be invoked when the object is successfully created.
     */
    public static void create(@NonNull final Context context,
                              @NonNull final String clientId,
                              @NonNull final String authority,
                              @NonNull final ApplicationCreatedListener listener) {

        if (context == null) {
            throw new IllegalArgumentException("Context is null.");
        }

        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("client id is empty or null");
        }

        if (authority == null) {
            throw new IllegalArgumentException("authority is null");
        }

        new BrokerMsalController().getBrokerAccountMode(context, new BrokerAccountModeCallback() {
            @Override
            public void onGetMode(String mode) {
                if (AuthenticationConstants.Broker.BROKER_ACCOUNT_MODE_SINGLE_ACCOUNT.equalsIgnoreCase(mode)) {
                    // TODO: return SingleAccountPublicClientApplication
                    listener.onCreated(new PublicClientApplication(context, clientId, authority));
                } else {
                    // TODO: return MultipleAccountPublicClientApplication
                    listener.onCreated(new PublicClientApplication(context, clientId, authority));
                }
            }
        });
    }

    private static void create(@NonNull final Context context,
                               final PublicClientApplicationConfiguration developerConfig,
                               @NonNull final ApplicationCreatedListener listener){
        new BrokerMsalController().getBrokerAccountMode(context, new BrokerAccountModeCallback() {
            @Override
            public void onGetMode(String mode) {
                if (AuthenticationConstants.Broker.BROKER_ACCOUNT_MODE_SINGLE_ACCOUNT.equalsIgnoreCase(mode)) {
                    // TODO: return SingleAccountPublicClientApplication
                    listener.onCreated(new PublicClientApplication(context, developerConfig));
                } else {
                    // TODO: return MultipleAccountPublicClientApplication
                    listener.onCreated(new PublicClientApplication(context,  developerConfig));
                }
            }
        });
    }

    protected PublicClientApplication(@NonNull final Context context,
                                      @NonNull final PublicClientApplicationConfiguration developerConfig) {
        setupConfiguration(context, developerConfig);
        AzureActiveDirectory.setEnvironment(mPublicClientConfiguration.getEnvironment());
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());
    }

    protected PublicClientApplication(@NonNull final Context context,
                                      @NonNull final String clientId) {
        setupConfiguration(context, null);
        mPublicClientConfiguration.mClientId = clientId;
        initializeApplication();
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());
    }

    protected PublicClientApplication(@NonNull final Context context,
                                      @NonNull final String clientId,
                                      @NonNull final String authority) {

        this(context, clientId);

        mPublicClientConfiguration.getAuthorities().clear();

        if (authority != null) {
            Authority authorityObject = Authority.getAuthorityFromAuthorityUrl(authority);
            authorityObject.setDefault(true);
            mPublicClientConfiguration.getAuthorities().add(authorityObject);
        }

        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());
    }

    private void initializeApplication() {
        // Init Events with defaults (application-wide)
        DefaultEvent.initializeDefaults(
                Defaults.forApplication(
                        mPublicClientConfiguration.getAppContext(),
                        mPublicClientConfiguration.getClientId()
                )
        );

        mPublicClientConfiguration.mRedirectUri = createRedirectUri(mPublicClientConfiguration.getClientId());
        checkIntentFilterAddedToAppManifest();

        // Since network request is sent from the sdk, if calling app doesn't declare the internet permission in the
        // manifest, we cannot make the network call.
        checkInternetPermission();
        com.microsoft.identity.common.internal.logging.Logger.info(TAG, "Create new public client application.");
    }

    /**
     * Listener callback for asynchronous initialization of IPublicClientApplication object.
     */
    public interface ApplicationCreatedListener {
        /**
         * Called once an IPublicClientApplication is successfully created.
         */
        void onCreated(final IPublicClientApplication application);
    }

    /**
     * Listener callback for asynchronous loading of MSAL mode retrieval.
     */
    public interface BrokerAccountModeCallback {
        /**
         * Called once MSAL mode is retrieved from Broker.
         * If the value can't be retrieved, this will fall back to the BROKER_ACCOUNT_MODE_MULTIPLE_ACCOUNT mode.
         */
        void onGetMode(String mode);
    }

    /**
     * @return The current version for the sdk.
     */
    public static String getSdkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * Returns the PublicClientConfiguration for this instance of PublicClientApplication
     * Configuration is based on the defaults established for MSAl and can be overridden by creating the
     * PublicClientApplication using {@link PublicClientApplication#create(Context, int, ApplicationCreatedListener)}
     *
     * @return
     */
    public PublicClientApplicationConfiguration getConfiguration() {
        return mPublicClientConfiguration;
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
            new BrokerMsalController().removeBrokerAccount(
                    account,
                    mPublicClientConfiguration,
                    callback
            );
        } else {
            callback.onAccountRemoved(localRemoveAccountSuccess);
        }
    }

    @Nullable
    private static String getRealm(@NonNull IAccount account) {
        String realm = null;

        if (null != account.getAccountIdentifier() // This is an AAD account w/ tenant info
                && account.getAccountIdentifier() instanceof AzureActiveDirectoryAccountIdentifier) {
            final AzureActiveDirectoryAccountIdentifier identifier = (AzureActiveDirectoryAccountIdentifier) account.getAccountIdentifier();
            realm = identifier.getTenantIdentifier();
        }

        return realm;
    }

    @Override
    public void handleInteractiveRequestRedirect(final int requestCode,
                                                 final int resultCode,
                                                 @NonNull final Intent data) {
        ApiDispatcher.completeInteractive(requestCode, resultCode, data);
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @NonNull final AuthenticationCallback callback) {
        acquireToken(
                activity,
                scopes,
                null, // account
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
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final String loginHint,
                             @NonNull final AuthenticationCallback callback) {
        acquireToken(
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
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final String loginHint,
                             @NonNull final UiBehavior uiBehavior,
                             @Nullable final List<Pair<String, String>> extraQueryParameters,
                             @NonNull final AuthenticationCallback callback) {
        acquireToken(
                activity,
                scopes,
                null, // account
                uiBehavior,
                extraQueryParameters,
                null, // extraScopes
                null, // authority
                callback,
                loginHint,
                null // claimsRequest
        );
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final IAccount account,
                             @NonNull final UiBehavior uiBehavior,
                             @Nullable final List<Pair<String, String>> extraQueryParameters,
                             @NonNull final AuthenticationCallback callback) {
        acquireToken(
                activity,
                scopes,
                account,
                uiBehavior,
                extraQueryParameters,
                null, // extraScopes
                null, // authority
                callback,
                null, // loginHint
                null // claimsRequest
        );
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final String loginHint,
                             @Nullable final UiBehavior uiBehavior,
                             @Nullable final List<Pair<String, String>> extraQueryParameters,
                             @Nullable final String[] extraScopesToConsent,
                             @Nullable final String authority,
                             @NonNull final AuthenticationCallback callback) {
        acquireToken(
                activity,
                scopes,
                null, // account
                uiBehavior,
                extraQueryParameters,
                extraScopesToConsent,
                authority,
                callback,
                loginHint,
                null // claimsRequest
        );
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final IAccount account,
                             @NonNull final UiBehavior uiBehavior,
                             @Nullable final List<Pair<String, String>> extraQueryParameters,
                             @Nullable final String[] extraScopesToConsent,
                             @Nullable final String authority,
                             @NonNull final AuthenticationCallback callback) {
        acquireToken(
                activity,
                scopes,
                account,
                uiBehavior,
                extraQueryParameters,
                extraScopesToConsent,
                authority,
                callback,
                null, // loginHint
                null //claimsRequest
        );
    }

    private void acquireToken(@NonNull final Activity activity,
                              @NonNull final String[] scopes,
                              @Nullable final IAccount account,
                              @Nullable final UiBehavior uiBehavior,
                              @Nullable final List<Pair<String, String>> extraQueryParameters,
                              @Nullable final String[] extraScopesToConsent,
                              @Nullable final String authority,
                              @NonNull final AuthenticationCallback callback,
                              @Nullable final String loginHint,
                              @Nullable final ClaimsRequest claimsRequest) {
        validateNonNullArgument(activity, "Activity");
        validateNonNullArgument(callback, "Callback");

        AcquireTokenParameters.Builder builder = new AcquireTokenParameters.Builder();
        AcquireTokenParameters acquireTokenParameters = builder.startAuthorizationFromActivity(activity)
                .forAccount(account)
                .withScopes(Arrays.asList(scopes))
                .withUiBehavior(uiBehavior)
                .withAuthorizationQueryStringParameters(extraQueryParameters)
                .withOtherScopesToAuthorize(
                        Arrays.asList(
                                null == extraScopesToConsent
                                        ? new String[]{}
                                        : extraScopesToConsent
                        )
                )
                .fromAuthority(authority)
                .callback(callback)
                .withLoginHint(loginHint)
                .withClaims(claimsRequest)
                .build();

        acquireTokenAsync(acquireTokenParameters);
    }

    private static void validateNonNullArgument(@Nullable Object o,
                                                @NonNull String argName) {
        if (null == o) {
            throw new IllegalArgumentException(
                    argName
                            + " cannot be null or empty"
            );
        }
    }

    @Override
    public void acquireTokenAsync(@NonNull final AcquireTokenParameters acquireTokenParameters) {
        acquireTokenParameters.setAccountRecord(
                getAccountRecord(acquireTokenParameters.getAccount())
        );

        final AcquireTokenOperationParameters params = OperationParametersAdapter.
                createAcquireTokenOperationParameters(
                        acquireTokenParameters,
                        mPublicClientConfiguration
                );

        ILocalAuthenticationCallback localAuthenticationCallback =
                getLocalAuthenticationCallback(
                        acquireTokenParameters.getCallback()
                );

        final InteractiveTokenCommand command = new InteractiveTokenCommand(
                params,
                MSALControllerFactory.getAcquireTokenController(
                        mPublicClientConfiguration.getAppContext(),
                        params.getAuthority(),
                        mPublicClientConfiguration
                ),
                localAuthenticationCallback
        );
        ApiDispatcher.beginInteractive(command);
    }

    private AccountRecord getAccountRecord(@Nullable final IAccount account) {
        if (account != null) {
            return AccountAdapter.getAccountInternal(
                    mPublicClientConfiguration.getClientId(),
                    mPublicClientConfiguration.getOAuth2TokenCache(),
                    account.getHomeAccountIdentifier().getIdentifier(),
                    getRealm(account)
            );
        }

        return null;
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @NonNull final IAccount account,
                                        @NonNull final AuthenticationCallback callback) {
        acquireTokenSilent(
                scopes,
                account,
                null, // authority
                false, // forceRefresh
                null, // claimsRequest
                callback
        );
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @NonNull final IAccount account,
                                        @Nullable final String authority,
                                        final boolean forceRefresh,
                                        @NonNull final AuthenticationCallback callback) {
        acquireTokenSilent(
                scopes,
                account,
                authority,
                forceRefresh,
                null, // claimsRequest
                callback
        );
    }

    private void acquireTokenSilent(@NonNull final String[] scopes,
                                    @NonNull final IAccount account,
                                    @Nullable final String authority,
                                    final boolean forceRefresh,
                                    @Nullable final ClaimsRequest claimsRequest,
                                    @NonNull final AuthenticationCallback callback) {
        validateNonNullArgument(account, "Account");
        validateNonNullArgument(callback, "Callback");

        AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder();
        AcquireTokenSilentParameters acquireTokenSilentParameters =
                builder.withScopes(Arrays.asList(scopes))
                        .forAccount(account)
                        .fromAuthority(authority)
                        .forceRefresh(forceRefresh)
                        .withClaims(claimsRequest)
                        .callback(callback)
                        .build();

        acquireTokenSilentAsync(acquireTokenSilentParameters);
    }

    @Override
    public void acquireTokenSilentAsync(@NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) {
        acquireTokenSilentParameters.setAccountRecord(
                getAccountRecord(
                        acquireTokenSilentParameters.getAccount()
                )
        );

        final AcquireTokenSilentOperationParameters params =
                OperationParametersAdapter.createAcquireTokenSilentOperationParameters(
                        acquireTokenSilentParameters,
                        mPublicClientConfiguration
                );

        ILocalAuthenticationCallback callback = getLocalAuthenticationCallback(acquireTokenSilentParameters.getCallback());

        final TokenCommand silentTokenCommand = new TokenCommand(
                params,
                MSALControllerFactory.getAcquireTokenSilentControllers(
                        mPublicClientConfiguration.getAppContext(),
                        params.getAuthority(),
                        mPublicClientConfiguration
                ),
                callback
        );

        ApiDispatcher.submitSilent(silentTokenCommand);
    }

    @VisibleForTesting
    static PublicClientApplicationConfiguration loadConfiguration(@NonNull final Context context,
                                                                  final int configResourceId) {
        InputStream configStream = context.getResources().openRawResource(configResourceId);
        boolean useDefaultConfigResourceId = configResourceId == R.raw.msal_default_config;
        return loadConfiguration(configStream, useDefaultConfigResourceId);
    }

    @VisibleForTesting
    static PublicClientApplicationConfiguration loadConfiguration(@NonNull File configFile) {
        try {
            return loadConfiguration(new FileInputStream(configFile), false);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Provided configuration file path=" + configFile.getPath() + " not found.");
        }
    }

    private static PublicClientApplicationConfiguration loadConfiguration(InputStream configStream, boolean isDefaultConfiguration) {
        byte[] buffer;

        try {
            buffer = new byte[configStream.available()];
            configStream.read(buffer);
        } catch (IOException e) {
            if (isDefaultConfiguration) {
                throw new IllegalStateException("Unable to open default configuration file.", e);
            } else {
                throw new IllegalArgumentException("Unable to open provided configuration file.", e);
            }
        } finally {
            try {
                configStream.close();
            } catch (IOException e) {
                if (isDefaultConfiguration) {
                    com.microsoft.identity.common.internal.logging.Logger.warn(
                            TAG + "loadConfiguration",
                            "Unable to close default configuration file. This can cause memory leak."
                    );
                } else {
                    com.microsoft.identity.common.internal.logging.Logger.warn(
                            TAG + "loadConfiguration",
                            "Unable to close provided configuration file. This can cause memory leak."
                    );
                }
            }
        }

        final String config = new String(buffer);
        final Gson gson = getGsonForLoadingConfiguration();

        return gson.fromJson(config, PublicClientApplicationConfiguration.class);
    }

    private void setupConfiguration(@NonNull Context context,
                                    @Nullable PublicClientApplicationConfiguration developerConfig) {
        final PublicClientApplicationConfiguration defaultConfig = loadDefaultConfiguration(context);
        if (developerConfig != null) {
            defaultConfig.mergeConfiguration(developerConfig);
        }

        mPublicClientConfiguration = defaultConfig;
        mPublicClientConfiguration.setAppContext(context);
        mPublicClientConfiguration.setOAuth2TokenCache(getOAuth2TokenCache());
    }

    private PublicClientApplicationConfiguration loadDefaultConfiguration(@NonNull final Context context) {
        final String methodName = ":loadDefaultConfiguration";
        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Loading default configuration"
        );
        return loadConfiguration(context, R.raw.msal_default_config);
    }

    private static Gson getGsonForLoadingConfiguration() {
        return new GsonBuilder()
                .registerTypeAdapter(
                        Authority.class,
                        new AuthorityDeserializer()
                )
                .registerTypeAdapter(
                        AzureActiveDirectoryAudience.class,
                        new AzureActiveDirectoryAudienceDeserializer()
                )
                .registerTypeAdapter(
                        Logger.LogLevel.class,
                        new LogLevelDeserializer()
                )
                .create();
    }

    // TODO: if no more input validation is needed, this could be moved back to the constructor.
    private void checkIntentFilterAddedToAppManifest() {
        if (!MsalUtils.hasCustomTabRedirectActivity(mPublicClientConfiguration.getAppContext(), mPublicClientConfiguration.getRedirectUri())) {
            throw new IllegalStateException("Intent filter for: "
                    + BrowserTabActivity.class.getSimpleName() + " is missing.  Please refer to the MSAL readme.");
        }
    }

    private void checkInternetPermission() {
        final PackageManager packageManager = mPublicClientConfiguration.getAppContext().getPackageManager();
        if (packageManager.checkPermission(INTERNET_PERMISSION, mPublicClientConfiguration.getAppContext().getPackageName())
                != PackageManager.PERMISSION_GRANTED
                || packageManager.checkPermission(ACCESS_NETWORK_STATE_PERMISSION, mPublicClientConfiguration.getAppContext().getPackageName())
                != PackageManager.PERMISSION_GRANTED) {
            throw new IllegalStateException("android.permission.Internet or android.permission.ACCESS_NETWORK_STATE is missing");
        }
    }

    /**
     * By default redirect uri will the in the format of msauth-clientid://appPackageName.
     * Otherwise the library will use the configured redirect URI.
     */
    private String createRedirectUri(final String clientId) {
        final String methodName = ":createRedirectUri";
        if (!StringUtil.isEmpty(mPublicClientConfiguration.getRedirectUri())) {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Returning redirectUri from configuration"
            );
            return mPublicClientConfiguration.getRedirectUri();
        } else {
            return "msal" + clientId + "://auth";
        }
    }

    private MsalOAuth2TokenCache<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> initCommonCache(@NonNull final Context context) {
        final String methodName = ":initCommonCache";
        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Initializing common cache"
        );
        // Init the new-schema cache
        final ICacheKeyValueDelegate cacheKeyValueDelegate = new CacheKeyValueDelegate();
        final IStorageHelper storageHelper = new StorageHelper(context);
        final ISharedPreferencesFileManager sharedPreferencesFileManager =
                new SharedPreferencesFileManager(
                        context,
                        DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES,
                        storageHelper
                );
        final IAccountCredentialCache accountCredentialCache =
                new SharedPreferencesAccountCredentialCache(
                        cacheKeyValueDelegate,
                        sharedPreferencesFileManager
                );
        final MicrosoftStsAccountCredentialAdapter accountCredentialAdapter =
                new MicrosoftStsAccountCredentialAdapter();

        return new MsalOAuth2TokenCache<>(
                context,
                accountCredentialCache,
                accountCredentialAdapter
        );
    }

    private static ILocalAuthenticationCallback getLocalAuthenticationCallback(final AuthenticationCallback authenticationCallback) {

        return new ILocalAuthenticationCallback() {

            @Override
            public void onSuccess(ILocalAuthenticationResult localAuthenticationResult) {
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

    private OAuth2TokenCache<?, ?, ?> getOAuth2TokenCache() {
        return initCommonCache(mPublicClientConfiguration.getAppContext());
    }
}
