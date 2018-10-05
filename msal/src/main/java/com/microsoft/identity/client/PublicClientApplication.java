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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.client.internal.authorities.Authority;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.client.internal.configuration.AuthorityDeserializer;
import com.microsoft.identity.client.internal.configuration.AzureActiveDirectoryAudienceDeserializer;
import com.microsoft.identity.client.internal.configuration.LogLevelDeserializer;
import com.microsoft.identity.client.internal.controllers.LocalMSALController;
import com.microsoft.identity.client.internal.controllers.MSALAcquireTokenOperationParameters;
import com.microsoft.identity.client.internal.controllers.MSALAcquireTokenSilentOperationParameters;
import com.microsoft.identity.client.internal.controllers.MSALApiDispatcher;
import com.microsoft.identity.client.internal.controllers.MSALInteractiveTokenCommand;
import com.microsoft.identity.client.internal.controllers.MSALTokenCommand;
import com.microsoft.identity.client.internal.telemetry.DefaultEvent;
import com.microsoft.identity.client.internal.telemetry.Defaults;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.internal.cache.AccountCredentialCache;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.ICacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.msal.BuildConfig;
import com.microsoft.identity.msal.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.microsoft.identity.common.internal.cache.AccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;

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
    private static final String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

    private final Context mAppContext;
    private final OAuth2TokenCache mOauth2TokenCache;

    /**
     * The authority the application will use to obtain tokens.
     */
    private String mAuthorityString;

    /**
     * The client ID of the application. This should come from the app developer portal.
     */
    private String mClientId;

    /**
     * The redirect URI for the application.
     */
    private String mRedirectUri;

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

        mAppContext = context;
        mOauth2TokenCache = getOAuth2TokenCache();

        //This order matters for now...
        setupConfiguration();
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

        mAppContext = context;
        mOauth2TokenCache = getOAuth2TokenCache();
        setupConfiguration(configFileResourceId);
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

        mAppContext = context;
        mOauth2TokenCache = getOAuth2TokenCache();
        mClientId = clientId;
        setupConfiguration();
        initializeApplication();
        mAuthorityString = DEFAULT_AUTHORITY;
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

        if (MsalUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("authority is empty or null");
        }

        mAuthorityString = authority;
        mPublicClientConfiguration.getAuthorities().clear();
        mPublicClientConfiguration.getAuthorities().add(Authority.getAuthorityFromAuthorityUrl(authority));

        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());
    }

    private void initializeApplication() {
        // Init Events with defaults (application-wide)
        DefaultEvent.initializeDefaults(
                Defaults.forApplication(mAppContext, mClientId)
        );

        mRedirectUri = createRedirectUri(mClientId);
        checkIntentFilterAddedToAppManifest();

        // Since network request is sent from the sdk, if calling app doesn't declare the internet permission in the
        // manifest, we cannot make the network call.
        checkInternetPermission();
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

    /**
     * Returns a List of {@link IAccount} objects for which this application has RefreshTokens.
     *
     * @return An immutable List of IAccount objects - empty if no IAccounts exist.
     */
    public List<IAccount> getAccounts() {
        MSALApiDispatcher.initializeDiagnosticContext();
        final List<IAccount> accountsToReturn = new ArrayList<>();

        // Grab the Accounts from the common cache
        final List<AccountRecord> accountsInCache = mOauth2TokenCache.getAccounts(
                null, // * wildcard
                mClientId
        );

        // Adapt them to the MSAL model
        for (final AccountRecord account : accountsInCache) {
            accountsToReturn.add(AccountAdapter.adapt(account));
        }

        return Collections.unmodifiableList(accountsToReturn);
    }

    /**
     * Returns the IAccount object matching the supplied home_account_id.
     *
     * @param homeAccountIdentifier The home_account_id of the sought IAccount.
     * @return The IAccount stored in the cache or null, if no such matching entry exists.
     */
    public IAccount getAccount(final String homeAccountIdentifier) {
        MSALApiDispatcher.initializeDiagnosticContext();
        final AccountRecord accountToReturn = getAccountInternal(homeAccountIdentifier);
        return null == accountToReturn ? null : AccountAdapter.adapt(accountToReturn);
    }

    private AccountRecord getAccountInternal(final String homeAccountIdentifier) {
        final AccountRecord accountToReturn;

        if (!StringUtil.isEmpty(homeAccountIdentifier)) {
            accountToReturn = mOauth2TokenCache.getAccount(
                    null, // * wildcard
                    mClientId,
                    homeAccountIdentifier
            );
        } else {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                    TAG,
                    "homeAccountIdentifier was null or empty -- invalid criteria"
            );
            accountToReturn = null;
        }

        return accountToReturn;
    }

    /**
     * Removes the Account and Credentials (tokens) for the supplied IAccount.
     *
     * @param account The IAccount whose entry and associated tokens should be removed.
     * @return True, if the account was removed. False otherwise.
     */
    public boolean removeAccount(final IAccount account) {
        MSALApiDispatcher.initializeDiagnosticContext();
        if (null == account
                || null == account.getHomeAccountIdentifier()
                || StringUtil.isEmpty(account.getHomeAccountIdentifier().getIdentifier())) {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                    TAG,
                    "Requisite IAccount or IAccount fields were null. Insufficient criteria to remove IAccount."
            );

            return false;
        }

        return mOauth2TokenCache.removeAccount(
                account.getEnvironment(),
                mClientId,
                account.getHomeAccountIdentifier().getIdentifier()
        );
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
                                                 final Intent data) {
        MSALApiDispatcher.completeInteractive(requestCode, resultCode, data);
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
     *                 {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                 3) All the other errors will be sent back via
     *                 {@link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @NonNull final AuthenticationCallback callback) {
        final MSALAcquireTokenOperationParameters params =
                getInteractiveOperationParameters(
                        activity,
                        scopes,
                        null, // login hint
                        UiBehavior.SELECT_ACCOUNT,
                        null, // extra query params
                        null, // extra scopes to consent
                        null, // authority
                        null // account
                );

        final MSALInteractiveTokenCommand command =
                new MSALInteractiveTokenCommand(
                        mAppContext,
                        params,
                        new LocalMSALController(),
                        callback
                );
        MSALApiDispatcher.beginInteractive(command);
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
     *                  {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                  3) All the other errors will be sent back via
     *                  {@link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final String loginHint,
                             @NonNull final AuthenticationCallback callback) {
        final MSALAcquireTokenOperationParameters params =
                getInteractiveOperationParameters(
                        activity,
                        scopes,
                        loginHint,
                        UiBehavior.SELECT_ACCOUNT,
                        null, // extra query params
                        null, // extra scopes to consent
                        null, // authority,
                        null
                );

        final MSALInteractiveTokenCommand command =
                new MSALInteractiveTokenCommand(
                        mAppContext,
                        params,
                        new LocalMSALController(),
                        callback
                );
        MSALApiDispatcher.beginInteractive(command);
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
     *                             {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                             3) All the other errors will be sent back via
     *                             {@link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final String loginHint,
                             @NonNull final UiBehavior uiBehavior,
                             @Nullable final List<Pair<String, String>> extraQueryParameters,
                             @NonNull final AuthenticationCallback callback) {
        final MSALAcquireTokenOperationParameters params =
                getInteractiveOperationParameters(
                        activity,
                        scopes,
                        loginHint,
                        uiBehavior,
                        extraQueryParameters,
                        null,
                        null,
                        null
                );

        final MSALInteractiveTokenCommand command =
                new MSALInteractiveTokenCommand(
                        mAppContext,
                        params,
                        new LocalMSALController(),
                        callback
                );
        MSALApiDispatcher.beginInteractive(command);
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
     *                             {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                             3) All the other errors will be sent back via
     *                             {@link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @Nullable final IAccount account,
                             @NonNull final UiBehavior uiBehavior,
                             @Nullable final List<Pair<String, String>> extraQueryParameters,
                             @NonNull final AuthenticationCallback callback) {
        String loginHint = null;
        String authority = null;

        if (null != account) {
            loginHint = account.getUsername();
            authority = account.getEnvironment();
        }

        final MSALAcquireTokenOperationParameters params =
                getInteractiveOperationParameters(
                        activity,
                        scopes,
                        loginHint,
                        uiBehavior,
                        extraQueryParameters,
                        null,
                        authority,
                        account
                );

        final MSALInteractiveTokenCommand command =
                new MSALInteractiveTokenCommand(
                        mAppContext,
                        params,
                        new LocalMSALController(),
                        callback
                );
        MSALApiDispatcher.beginInteractive(command);
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
     *                             {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                             3) All the other errors will be sent back via
     *                             {@link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             final String loginHint,
                             final UiBehavior uiBehavior,
                             @Nullable final List<Pair<String, String>> extraQueryParameters,
                             final String[] extraScopesToConsent,
                             final String authority,
                             @NonNull final AuthenticationCallback callback) {
        final MSALAcquireTokenOperationParameters params =
                getInteractiveOperationParameters(
                        activity,
                        scopes,
                        loginHint,
                        uiBehavior,
                        extraQueryParameters,
                        extraScopesToConsent,
                        authority,
                        null // account
                );

        final MSALInteractiveTokenCommand command =
                new MSALInteractiveTokenCommand(
                        mAppContext,
                        params,
                        new LocalMSALController(),
                        callback
                );
        MSALApiDispatcher.beginInteractive(command);
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
     *                             {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
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
        String loginHint = null;

        if (null != account) {
            loginHint = account.getUsername();
        }

        final MSALAcquireTokenOperationParameters params =
                getInteractiveOperationParameters(
                        activity,
                        scopes,
                        loginHint,
                        uiBehavior,
                        extraQueryParameters,
                        extraScopesToConsent,
                        authority,
                        account
                );

        final MSALInteractiveTokenCommand command =
                new MSALInteractiveTokenCommand(
                        mAppContext,
                        params,
                        new LocalMSALController(),
                        callback
                );
        MSALApiDispatcher.beginInteractive(command);
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
     *                 sent back via {@link AuthenticationCallback#onSuccess(AuthenticationResult)}.
     *                 Failure case will be sent back via {
     * @link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @NonNull final IAccount account,
                                        @NonNull final AuthenticationCallback callback) {
        String requestAuthority = Authority.getAuthorityFromAccount(account);

        if (null == requestAuthority) {
            requestAuthority = mAuthorityString;
        }

        final MSALAcquireTokenSilentOperationParameters params = getSilentOperationParameters(
                scopes,
                requestAuthority,
                false,
                account
        );

        final MSALTokenCommand silentTokenCommand = new MSALTokenCommand(
                mAppContext,
                params,
                new LocalMSALController(),
                callback
        );

        MSALApiDispatcher.submitSilent(silentTokenCommand);
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
     *                     sent back via {@link AuthenticationCallback#onSuccess(AuthenticationResult)}.
     *                     Failure case will be sent back via {
     * @link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireTokenSilentAsync(@NonNull final String[] scopes,
                                        @NonNull final IAccount account,
                                        @Nullable final String authority,
                                        final boolean forceRefresh,
                                        @NonNull final AuthenticationCallback callback) {
        String requestAuthority = authority;

        if (StringUtil.isEmpty(requestAuthority)) {
            if (Authority.getAuthorityFromAuthorityUrl(mAuthorityString) instanceof AzureActiveDirectoryB2CAuthority) {
                requestAuthority = AzureActiveDirectoryB2CAuthority.getAuthorityFromAccount(account);
            } else {
                requestAuthority = Authority.getAuthorityFromAccount(account);
            }

            if (requestAuthority == null) {
                requestAuthority = mAuthorityString;
            }
        }

        final MSALAcquireTokenSilentOperationParameters params = getSilentOperationParameters(
                scopes,
                requestAuthority,
                forceRefresh,
                account
        );

        final MSALTokenCommand silentTokenCommand = new MSALTokenCommand(
                mAppContext,
                params,
                new LocalMSALController(),
                callback
        );

        MSALApiDispatcher.submitSilent(silentTokenCommand);
    }

    private MSALAcquireTokenSilentOperationParameters getSilentOperationParameters(final String[] scopes,
                                                                                   final String authorityStr,
                                                                                   final boolean forceRefresh,
                                                                                   final IAccount account) {
        final MSALAcquireTokenSilentOperationParameters parameters = new MSALAcquireTokenSilentOperationParameters();
        parameters.setAppContext(mAppContext);
        parameters.setScopes(new ArrayList<>(Arrays.asList(scopes)));
        parameters.setClientId(mClientId);
        parameters.setTokenCache(mOauth2TokenCache);
        parameters.setAuthority(Authority.getAuthorityFromAuthorityUrl(authorityStr));
        if (null != account) {
            parameters.setAccount(
                    getAccountInternal(
                            account.getHomeAccountIdentifier().getIdentifier()
                    )
            );
        }
        parameters.setForceRefresh(forceRefresh);

        return parameters;
    }

    private void loadMetaDataFromManifest() {
        final String methodName = ":loadMetaDataFromManifest";
        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Loading metadata from manifest..."
        );
        final ApplicationInfo applicationInfo = MsalUtils.getApplicationInfo(mAppContext);
        if (applicationInfo == null || applicationInfo.metaData == null) {
            throw new IllegalArgumentException("No meta-data exists");
        }

        // read authority from manifest.
        final String authority = applicationInfo.metaData.getString(AUTHORITY_META_DATA);

        if (!MsalUtils.isEmpty(authority)) {
            mAuthorityString = authority;
            mPublicClientConfiguration.getAuthorities().clear();
            mPublicClientConfiguration.getAuthorities().add(Authority.getAuthorityFromAuthorityUrl(mAuthorityString));
        } else {
            mAuthorityString = DEFAULT_AUTHORITY;
            //mPublicClientConfiguration already has the default authority configured.
        }

        // read client id from manifest
        final String clientId = applicationInfo.metaData.getString(CLIENT_ID_META_DATA);

        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("client id missing from manifest");
        }

        mClientId = clientId;
        mPublicClientConfiguration.mClientId = clientId;
    }

    private void setupConfiguration(final int configResourceId) {
        final PublicClientApplicationConfiguration developerConfig = loadConfiguration(mAppContext, configResourceId);
        final PublicClientApplicationConfiguration defaultConfig = loadDefaultConfiguration(mAppContext);
        defaultConfig.mergeConfiguration(developerConfig);
        mPublicClientConfiguration = defaultConfig;

        if (!StringUtil.isEmpty(mPublicClientConfiguration.getClientId())) {
            mClientId = mPublicClientConfiguration.getClientId();
        }

        if (!StringUtil.isEmpty(mPublicClientConfiguration.getRedirectUri())) {
            mRedirectUri = mPublicClientConfiguration.getRedirectUri();
        }

        if (mPublicClientConfiguration.isDefaultAuthorityConfigured()) {
            mAuthorityString = mPublicClientConfiguration.getDefaultAuthority().getAuthorityURL().toString();
        } else {
            mAuthorityString = DEFAULT_AUTHORITY;
        }
    }

    private void setupConfiguration() {
        mPublicClientConfiguration = loadDefaultConfiguration(mAppContext);
    }

    @VisibleForTesting
    static PublicClientApplicationConfiguration loadConfiguration(@NonNull final Context context,
                                                                  final int configResourceId) {
        InputStream configStream = context.getResources().openRawResource(configResourceId);
        byte[] buffer;

        try {
            buffer = new byte[configStream.available()];
            configStream.read(buffer);
        } catch (IOException e) {
            if (configResourceId == R.raw.msal_default_config) {
                throw new IllegalStateException("Unable to open default configuration file.  MSAL module may be incomplete.");
            } else {
                throw new IllegalArgumentException("Provided config file resource id could not be accessed");
            }
        }

        final String config = new String(buffer);
        final Gson gson = getGsonForLoadingConfiguration();

        return gson.fromJson(config, PublicClientApplicationConfiguration.class);
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
                .registerTypeAdapter(Authority.class, new AuthorityDeserializer())
                .registerTypeAdapter(AzureActiveDirectoryAudience.class, new AzureActiveDirectoryAudienceDeserializer())
                .registerTypeAdapter(Logger.LogLevel.class, new LogLevelDeserializer())
                .create();
    }

    // TODO: if no more input validation is needed, this could be moved back to the constructor.
    private void checkIntentFilterAddedToAppManifest() {
        if (!MsalUtils.hasCustomTabRedirectActivity(mAppContext, mRedirectUri)) {
            throw new IllegalStateException("Intent filter for: "
                    + BrowserTabActivity.class.getSimpleName() + " is missing.  Please refer to the MSAL readme.");
        }
    }

    private void checkInternetPermission() {
        final PackageManager packageManager = mAppContext.getPackageManager();
        if (packageManager.checkPermission(INTERNET_PERMISSION, mAppContext.getPackageName())
                != PackageManager.PERMISSION_GRANTED
                || packageManager.checkPermission(ACCESS_NETWORK_STATE_PERMISSION, mAppContext.getPackageName())
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

    private MSALAcquireTokenOperationParameters getInteractiveOperationParameters(@NonNull final Activity activity,
                                                                                  @NonNull final String[] scopes,
                                                                                  @Nullable final String loginHint,
                                                                                  final UiBehavior uiBehavior,
                                                                                  @Nullable List<Pair<String, String>> extraQueryParameters,
                                                                                  @Nullable final String[] extraScopesToConsent,
                                                                                  @Nullable final String authority,
                                                                                  @Nullable final IAccount account) {
        final String methodName = ":getInteractiveOperationParameters";
        final MSALAcquireTokenOperationParameters params = new MSALAcquireTokenOperationParameters();

        if (StringUtil.isEmpty(authority)) {
            if (mPublicClientConfiguration.isDefaultAuthorityConfigured()) {
                com.microsoft.identity.common.internal.logging.Logger.verbose(
                        TAG + methodName,
                        "Using default configured authority"
                );
                params.setAuthority(mPublicClientConfiguration.getDefaultAuthority());
            } else {
                params.setAuthority(Authority.getAuthorityFromAuthorityUrl(mAuthorityString));
            }
        } else {
            params.setAuthority(Authority.getAuthorityFromAuthorityUrl(authority));
        }

        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                TAG + methodName,
                "Using authority: [" + params.getAuthority().getAuthorityUri() + "]"
        );

        params.setScopes(new ArrayList<>(Arrays.asList(scopes)));
        params.setClientId(mClientId);
        params.setRedirectUri(mRedirectUri);
        params.setActivity(activity);
        params.setLoginHint(loginHint);
        params.setTokenCache(mOauth2TokenCache);
        params.setExtraQueryStringParameters(extraQueryParameters);
        params.setExtraScopesToConsent(
                null != extraScopesToConsent
                        ? Arrays.asList(extraScopesToConsent)
                        : new ArrayList<String>()
        );
        params.setAppContext(mAppContext);
        if (null != mPublicClientConfiguration.getAuthorizationAgent()) {
            params.setAuthorizationAgent(mPublicClientConfiguration.getAuthorizationAgent());
        } else {
            params.setAuthorizationAgent(AuthorizationAgent.DEFAULT);
        }

        if (uiBehavior == null) {
            params.setUIBehavior(UiBehavior.SELECT_ACCOUNT);
        } else {
            params.setUIBehavior(uiBehavior);
        }

        params.setAppContext(mAppContext);
        if (null != account) {
            params.setAccount(
                    getAccountInternal(
                            account.getHomeAccountIdentifier().getIdentifier()
                    )
            );
        }

        return params;
    }

    private MsalOAuth2TokenCache<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> initCommonCache(final Context context) {
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
                new AccountCredentialCache(
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

    private OAuth2TokenCache<?, ?, ?> getOAuth2TokenCache() {
        return initCommonCache(mAppContext);
    }
}
