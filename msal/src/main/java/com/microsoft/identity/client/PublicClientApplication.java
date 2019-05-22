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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.client.internal.configuration.LogLevelDeserializer;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.client.internal.telemetry.DefaultEvent;
import com.microsoft.identity.client.internal.telemetry.Defaults;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AuthorityDeserializer;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudienceDeserializer;
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
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.controllers.InteractiveTokenCommand;
import com.microsoft.identity.common.internal.controllers.LoadAccountCommand;
import com.microsoft.identity.common.internal.controllers.RemoveAccountCommand;
import com.microsoft.identity.common.internal.controllers.TokenCommand;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.migration.AdalMigrationAdapter;
import com.microsoft.identity.common.internal.migration.TokenMigrationCallback;
import com.microsoft.identity.common.internal.migration.TokenMigrationUtility;
import com.microsoft.identity.common.internal.net.cache.HttpCache;
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
import com.microsoft.identity.common.internal.request.OperationParameters;
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
public final class PublicClientApplication {
    private static final String TAG = PublicClientApplication.class.getSimpleName();

    private static final String CLIENT_ID_META_DATA = "com.microsoft.identity.client.ClientId";
    private static final String AUTHORITY_META_DATA = "com.microsoft.identity.client.AuthorityMetadata";
    private static final String INTERNET_PERMISSION = "android.permission.INTERNET";
    private static final String ACCESS_NETWORK_STATE_PERMISSION = "android.permission.ACCESS_NETWORK_STATE";

    private PublicClientApplicationConfiguration mPublicClientConfiguration;

    /**
     * @param context Application's {@link Context}. The sdk requires the application context to be passed in
     *                {@link PublicClientApplication}. Cannot be null.
     *                <p>
     *                Note: The {@link Context} should be the application context instead of the running activity's context, which could potentially make the sdk hold a
     *                strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                </p>
     * @deprecated This constructor has been replaced with one that leverages a configuration file.
     * <p> Use {@link PublicClientApplication#PublicClientApplication(Context, int)}</p> instead.
     * <p>
     * <p>
     * {@link PublicClientApplication#PublicClientApplication(Context)} will read the client id (which must be set) from manifest, and if authority
     * is not set, default authority(https://login.microsoftonline.com/common) will be used.
     * <p>
     * Client id <b>MUST</b> be set in the manifest as the meta data({@link IllegalArgumentException} will be thrown
     * if client id is not provided), name for client id in the metadata is: "com.microsoft.identity.client.ClientId".
     * <p>
     * Redirect uri <b>MUST</b> be set in the manifest as the meta data({@link IllegalArgumentException} will be thrown
     * if client id is not provided), name for redirect uri in metadata is: "com.microsoft.identity.client.RedirectUri".
     * <p>
     * AuthorityMetadata can be set in the meta data, if not provided, the sdk will use the default authority https://login.microsoftonline.com/common.
     * </p>
     */
    @Deprecated
    public PublicClientApplication(@NonNull final Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null.");
        }

        setupConfiguration(context);
        loadMetaDataFromManifest();
        initializeApplication();
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());
    }

    /**
     * {@link PublicClientApplication#PublicClientApplication(Context, int)} will read the client id and other configuration settings from the
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
     */
    public PublicClientApplication(@NonNull final Context context, final int configFileResourceId) {
        if (context == null) {
            throw new IllegalArgumentException("context is null.");
        }

        final PublicClientApplicationConfiguration developerConfig = loadConfiguration(context, configFileResourceId);
        setupConfiguration(context, developerConfig);
        initializeApplication();
        AzureActiveDirectory.setEnvironment(mPublicClientConfiguration.getEnvironment());
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());
    }

    /**
     * {@link PublicClientApplication#PublicClientApplication(Context, File)} will read the client id and other configuration settings from the
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
     */
    public PublicClientApplication(@NonNull final Context context,
                                   @NonNull final File configFile) {
        if (null == context) {
            throw new IllegalArgumentException("context is null.");
        }

        if (null == configFile) {
            throw new IllegalArgumentException("config is null.");
        }

        final PublicClientApplicationConfiguration developerConfig = loadConfiguration(configFile);
        setupConfiguration(context, developerConfig);
        initializeApplication();
        AzureActiveDirectory.setEnvironment(mPublicClientConfiguration.getEnvironment());
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());
    }

    /**
     * {@link PublicClientApplication#PublicClientApplication(Context, String)} allows the client id to be passed instead of
     * providing through the AndroidManifest metadata. If this constructor is called, the default authority https://login.microsoftonline.com/common will be used.
     *
     * @param context  Application's {@link Context}. The sdk requires the application context to be passed in
     *                 {@link PublicClientApplication}. Cannot be null.
     *                 <p>
     *                 Note: The {@link Context} should be the application context instead of the running activity's context, which could potentially make the sdk hold a
     *                 strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                 </p>
     * @param clientId The application's client id.
     */
    public PublicClientApplication(@NonNull final Context context, @NonNull final String clientId) {
        if (context == null) {
            throw new IllegalArgumentException("Context is null");
        }

        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("client id is empty or null");
        }

        setupConfiguration(context);
        mPublicClientConfiguration.mClientId = clientId;
        initializeApplication();
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());
    }

    /**
     * {@link PublicClientApplication#PublicClientApplication(Context, String, String)} allows the client id and authority to be passed instead of
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
     */
    public PublicClientApplication(@NonNull final Context context,
                                   @NonNull final String clientId,
                                   @NonNull final String authority) {
        this(context, clientId);

        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("authority is empty or null");
        }

        mPublicClientConfiguration.getAuthorities().clear();
        Authority authorityObject = Authority.getAuthorityFromAuthorityUrl(authority);
        authorityObject.setDefault(true);
        mPublicClientConfiguration.getAuthorities().add(authorityObject);
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());
        initializeApplication();
    }

    private void initializeApplication() {
        final Context context = mPublicClientConfiguration.getAppContext();

        // Init Events with defaults (application-wide)
        DefaultEvent.initializeDefaults(
                Defaults.forApplication(
                        context,
                        mPublicClientConfiguration.getClientId()
                )
        );

        mPublicClientConfiguration.mRedirectUri = createRedirectUri(mPublicClientConfiguration.getClientId());
        checkIntentFilterAddedToAppManifest();

        // Since network request is sent from the sdk, if calling app doesn't declare the internet permission in the
        // manifest, we cannot make the network call.
        checkInternetPermission();

        // Init HTTP cache
        HttpCache.initialize(context.getCacheDir());

        com.microsoft.identity.common.internal.logging.Logger.info(TAG, "Create new public client application.");
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
     * PublicClientApplication using {@link PublicClientApplication#PublicClientApplication(Context, int)}
     *
     * @return
     */
    public PublicClientApplicationConfiguration getConfiguration() {
        return mPublicClientConfiguration;
    }

    public interface LoadAccountCallback extends TaskCompletedCallbackWithError<List<IAccount>, Exception> {
        /**
         * Called once succeed and pass the result object.
         *
         * @param result the success result.
         */
        void onTaskCompleted(List<IAccount> result);

        /**
         * Called once exception thrown.
         *
         * @param exception
         */
        void onError(Exception exception);
    }

    public interface GetAccountCallback extends TaskCompletedCallbackWithError<IAccount, Exception> {
        /**
         * Called once succeed and pass the result object.
         *
         * @param result the success result.
         */
        void onTaskCompleted(IAccount result);

        /**
         * Called once exception thrown.
         *
         * @param exception
         */
        void onError(Exception exception);
    }

    public interface RemoveAccountCallback extends TaskCompletedCallbackWithError<Boolean, Exception> {
        /**
         * Called once succeed and pass the result object.
         *
         * @param result the success result.
         */
        void onTaskCompleted(Boolean result);

        /**
         * Called once exception thrown.
         *
         * @param exception
         */
        void onError(Exception exception);
    }

    /**
     * Asynchronously returns a List of {@link IAccount} objects for which this application has RefreshTokens.
     *
     * @param callback The callback to notify once this action has finished.
     */
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
                            if (null == result) {
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

    /**
     * Removes the Account and Credentials (tokens) for the supplied IAccount.
     *
     * @param account The IAccount whose entry and associated tokens should be removed.
     * @return True, if the account was removed. False otherwise.
     */
    public void removeAccount(@Nullable final IAccount account, final RemoveAccountCallback callback) {
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
                    //requestAccountRecord.setEnvironment(account.getEnvironment());
                    requestAccountRecord.setUsername(account.getUsername());
                    requestAccountRecord.setHomeAccountId(account.getHomeAccountIdentifier().getIdentifier());
                    requestAccountRecord.setAlternativeAccountId(account.getAccountIdentifier().getIdentifier());
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

    /**
     * MSAL requires the calling app to pass an {@link Activity} which <b> MUST </b> call this method to get the auth
     * code passed back correctly.
     *
     * @param requestCode The request code for interactive request.
     * @param resultCode  The result code for the request to get auth code.
     * @param data        {@link Intent} either contains the url with auth code as query string or the errors.
     */
    public void handleInteractiveRequestRedirect(final int requestCode,
                                                 final int resultCode,
                                                 @NonNull final Intent data) {
        ApiDispatcher.completeInteractive(requestCode, resultCode, data);
    }

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     *
     * @param activity Non-null {@link Activity} that is used as the parent activity for launching the {@link AuthenticationActivity}.
     *                 All apps doing an interactive request are required to call the
     *                 {@link PublicClientApplication#handleInteractiveRequestRedirect(int, int, Intent)} within the calling
     *                 activity {@link Activity#onActivityResult(int, int, Intent)}.
     * @param scopes   The non-null array of scopes to be requested for the access token.
     *                 MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param callback The {@link AuthenticationCallback} to receive the result back.
     *                 1) If user cancels the flow by pressing the device back button, the result will be sent
     *                 back via {@link AuthenticationCallback#onCancel()}.
     *                 2) If the sdk successfully receives the token back, result will be sent back via
     *                 {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     *                 3) All the other errors will be sent back via
     *                 {@link AuthenticationCallback#onError(MsalException)}.
     */
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

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     *
     * @param activity  Non-null {@link Activity} that will be used as the parent activity for launching the {@link AuthenticationActivity}.
     *                  All the apps doing interactive request are required to call the
     *                  {@link PublicClientApplication#handleInteractiveRequestRedirect(int, int, Intent)} within the calling
     *                  activity {@link Activity#onActivityResult(int, int, Intent)}.
     * @param scopes    The non-null array of scopes to be requested for the access token.
     *                  MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param loginHint Optional. If provided, will be used as the query parameter sent for authenticating the user,
     *                  which will have the UPN pre-populated.
     * @param callback  The Non-null {@link AuthenticationCallback} to receive the result back.
     *                  1) If user cancels the flow by pressing the device back button, the result will be sent
     *                  back via {@link AuthenticationCallback#onCancel()}.
     *                  2) If the sdk successfully receives the token back, result will be sent back via
     *                  {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     *                  3) All the other errors will be sent back via
     *                  {@link AuthenticationCallback#onError(MsalException)}.
     */
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

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     *
     * @param activity             Non-null {@link Activity} that will be used as the parent activity for launching the {@link AuthenticationActivity}.
     *                             All the apps doing interactive request are required to call the
     *                             {@link PublicClientApplication#handleInteractiveRequestRedirect(int, int, Intent)} within the calling
     *                             activity {@link Activity#onActivityResult(int, int, Intent)}.
     * @param scopes               The non-null array of scopes to be requested for the access token.
     *                             MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param loginHint            Optional. If provided, will be used as the query parameter sent for authenticating the user,
     *                             which will have the UPN pre-populated.
     * @param uiBehavior           The {@link UiBehavior} for prompting behavior. By default, the sdk use {@link UiBehavior#SELECT_ACCOUNT}.
     * @param extraQueryParameters Optional. The extra query parameters sent to authorize endpoint.
     * @param callback             The Non-null {@link AuthenticationCallback} to receive the result back.
     *                             1) If user cancels the flow by pressing the device back button, the result will be sent
     *                             back via {@link AuthenticationCallback#onCancel()}.
     *                             2) If the sdk successfully receives the token back, result will be sent back via
     *                             {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     *                             3) All the other errors will be sent back via
     *                             {@link AuthenticationCallback#onError(MsalException)}.
     */
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

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     *
     * @param activity             Non-null {@link Activity} that will be used as the parent activity for launching the {@link AuthenticationActivity}.
     *                             All the apps doing interactive request are required to call the
     *                             {@link PublicClientApplication#handleInteractiveRequestRedirect(int, int, Intent)} within the calling
     *                             activity {@link Activity#onActivityResult(int, int, Intent)}.
     * @param scopes               The non-null array of scopes to be requested for the access token.
     *                             MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param account              Optional. If provided, will be used to force the session continuation.  If user tries to sign in with a different user,
     *                             error will be returned.
     * @param uiBehavior           The {@link UiBehavior} for prompting behavior. By default, the sdk use {@link UiBehavior#SELECT_ACCOUNT}.
     * @param extraQueryParameters Optional. The extra query parameter sent to authorize endpoint.
     * @param callback             The Non-null {@link AuthenticationCallback} to receive the result back.
     *                             1) If user cancels the flow by pressing the device back button, the result will be sent
     *                             back via {@link AuthenticationCallback#onCancel()}.
     *                             2) If the sdk successfully receives the token back, result will be sent back via
     *                             {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     *                             3) All the other errors will be sent back via
     *                             {@link AuthenticationCallback#onError(MsalException)}.
     */
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

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     *
     * @param activity             Non-null {@link Activity} that will be used as the parent activity for launching the {@link AuthenticationActivity}.
     *                             All the apps doing interactive request are required to call the
     *                             {@link PublicClientApplication#handleInteractiveRequestRedirect(int, int, Intent)} within the calling
     *                             activity {@link Activity#onActivityResult(int, int, Intent)}.
     * @param scopes               The non-null array of scopes to be requested for the access token.
     *                             MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param loginHint            Optional. If provided, will be used as the query parameter sent for authenticating the user,
     *                             which will have the UPN pre-populated.
     * @param uiBehavior           The {@link UiBehavior} for prompting behavior. By default, the sdk use {@link UiBehavior#SELECT_ACCOUNT}.
     * @param extraQueryParameters Optional. The extra query parameter sent to authorize endpoint.
     * @param extraScopesToConsent Optional. The extra scopes to request consent.
     * @param authority            Optional. Can be passed to override the configured authority.
     * @param callback             The Non-null {@link AuthenticationCallback} to receive the result back.
     *                             1) If user cancels the flow by pressing the device back button, the result will be sent
     *                             back via {@link AuthenticationCallback#onCancel()}.
     *                             2) If the sdk successfully receives the token back, result will be sent back via
     *                             {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     *                             3) All the other errors will be sent back via
     *                             {@link AuthenticationCallback#onError(MsalException)}.
     */
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

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     *
     * @param activity             Non-null {@link Activity} that will be used as the parent activity for launching the {@link AuthenticationActivity}.
     *                             All the apps doing interactive request are required to call the
     *                             {@link PublicClientApplication#handleInteractiveRequestRedirect(int, int, Intent)} within the calling
     *                             activity {@link Activity#onActivityResult(int, int, Intent)}.
     * @param scopes               The non-null array of scopes to be requested for the access token.
     *                             MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param account              Optional. If provided, will be used to force the session continuation.  If user tries to sign in with a different user, error
     *                             will be returned.
     * @param uiBehavior           The {@link UiBehavior} for prompting behavior. By default, the sdk use {@link UiBehavior#SELECT_ACCOUNT}.
     * @param extraQueryParameters Optional. The extra query parameter sent to authorize endpoint.
     * @param extraScopesToConsent Optional. The extra scopes to request consent.
     * @param authority            Optional. Can be passed to override the configured authority.
     * @param callback             The Non-null {@link AuthenticationCallback} to receive the result back.
     *                             1) If user cancels the flow by pressing the device back button, the result will be sent
     *                             back via {@link AuthenticationCallback#onCancel()}.
     *                             2) If the sdk successfully receives the token back, result will be sent back via
     *                             {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     *                             3) All the other errors will be sent back via
     *                             {@link AuthenticationCallback#onError(MsalException)}.
     */
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

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     * <p>
     * Convey parameters via the AquireTokenParameters object
     *
     * @param acquireTokenParameters
     */
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

        try {
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
        } catch (final BaseException exception) {
            localAuthenticationCallback.onError(exception);
        }
    }

    private AccountRecord getAccountRecord(@Nullable final IAccount account) {
        if (account != null) {
            return AccountAdapter.getAccountInternal(
                    mPublicClientConfiguration.getClientId(),
                    mPublicClientConfiguration.getOAuth2TokenCache(),
                    account.getHomeAccountIdentifier().getIdentifier(),
                    AccountAdapter.getRealm(account)
            );
        }

        return null;
    }

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param scopes   The non-null array of scopes to be requested for the access token.
     *                 MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param account  {@link IAccount} represents the account to silently request tokens.
     * @param callback {@link AuthenticationCallback} that is used to send the result back. The success result will be
     *                 sent back via {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}.
     *                 Failure case will be sent back via {
     * @link AuthenticationCallback#onError(MsalException)}.
     */
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

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param scopes       The non-null array of scopes to be requested for the access token.
     *                     MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param account      {@link IAccount} represents the account to silently request tokens.
     * @param authority    Optional. Can be passed to override the configured authority.
     * @param forceRefresh True if the request is forced to refresh, false otherwise.
     * @param callback     {@link AuthenticationCallback} that is used to send the result back. The success result will be
     *                     sent back via {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}.
     *                     Failure case will be sent back via {
     * @link AuthenticationCallback#onError(MsalException)}.
     */
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

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param acquireTokenSilentParameters
     */
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

        try {
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
        } catch (final BaseException exception) {
            callback.onError(exception);
        }
    }

    private void loadMetaDataFromManifest() {
        final String methodName = ":loadMetaDataFromManifest";
        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Loading metadata from manifest..."
        );
        final ApplicationInfo applicationInfo = MsalUtils.getApplicationInfo(mPublicClientConfiguration.getAppContext());
        if (applicationInfo == null || applicationInfo.metaData == null) {
            throw new IllegalArgumentException("No meta-data exists");
        }

        // read authority from manifest.
        final String authority = applicationInfo.metaData.getString(AUTHORITY_META_DATA);

        if (!MsalUtils.isEmpty(authority)) {
            mPublicClientConfiguration.getAuthorities().clear();
            mPublicClientConfiguration.getAuthorities().add(Authority.getAuthorityFromAuthorityUrl(authority));
        }

        // read client id from manifest
        final String clientId = applicationInfo.metaData.getString(CLIENT_ID_META_DATA);

        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("client id missing from manifest");
        }

        mPublicClientConfiguration.mClientId = clientId;
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

    private void setupConfiguration(Context context) {
        mPublicClientConfiguration = loadDefaultConfiguration(context);
        mPublicClientConfiguration.setAppContext(context);
        mPublicClientConfiguration.setOAuth2TokenCache(getOAuth2TokenCache());
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

    private void setupConfiguration(@NonNull Context context, PublicClientApplicationConfiguration developerConfig) {
        final PublicClientApplicationConfiguration defaultConfig = loadDefaultConfiguration(context);
        defaultConfig.mergeConfiguration(developerConfig);
        defaultConfig.validateConfiguration();
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

    private static TaskCompletedCallbackWithError<List<AccountRecord>, Exception> getLoadAccountsCallback (
            final LoadAccountCallback loadAccountsCallback) {
        return new TaskCompletedCallbackWithError<List<AccountRecord>, Exception>() {
            @Override
            public void onTaskCompleted(final List<AccountRecord> result) {
                if (null == result) {
                    loadAccountsCallback.onTaskCompleted(null);
                } else {
                    final List<IAccount> resultAccounts = new ArrayList<>();
                    for (AccountRecord accountRecord : result) {
                        resultAccounts.add(AccountAdapter.adapt(accountRecord));
                    }
                    loadAccountsCallback.onTaskCompleted(resultAccounts);
                }
            }

            @Override
            public void onError(final Exception exception) {
                loadAccountsCallback.onError(exception);
            }
        };
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
