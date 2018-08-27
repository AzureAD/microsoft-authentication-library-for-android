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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.authorities.Authority;
import com.microsoft.identity.client.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.client.internal.configuration.AuthorityDeserializer;
import com.microsoft.identity.client.internal.configuration.AzureActiveDirectoryAudienceDeserializer;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.msal.BuildConfig;
import com.microsoft.identity.msal.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.microsoft.identity.client.EventConstants.ApiId.ACQUIRE_TOKEN_SILENT_ASYNC_WITH_USER;
import static com.microsoft.identity.client.EventConstants.ApiId.ACQUIRE_TOKEN_SILENT_ASYNC_WITH_USER_AUTHORITY_AND_FORCE_REFRESH;
import static com.microsoft.identity.client.EventConstants.ApiId.API_ID_ACQUIRE;
import static com.microsoft.identity.client.EventConstants.ApiId.API_ID_ACQUIRE_WITH_HINT;
import static com.microsoft.identity.client.EventConstants.ApiId.API_ID_ACQUIRE_WITH_HINT_BEHAVIOR_AND_PARAMETERS;
import static com.microsoft.identity.client.EventConstants.ApiId.API_ID_ACQUIRE_WITH_HINT_BEHAVIOR_PARAMETERS_AND_AUTHORITY;
import static com.microsoft.identity.client.EventConstants.ApiId.API_ID_ACQUIRE_WITH_USER_BEHAVIOR_AND_PARAMETERS;
import static com.microsoft.identity.client.EventConstants.ApiId.API_ID_ACQUIRE_WITH_USER_BEHAVIOR_PARAMETERS_AND_AUTHORITY;

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
    //private static final String CONFIGURATION = "com.microsoft.identity.client.Configuration";
    private static final String INTERNET_PERMISSION = "android.permission.INTERNET";
    private static final String ACCESS_NETWORK_STATE_PERMISSION = "android.permission.ACCESS_NETWORK_STATE";
    private static final String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

    private final Context mAppContext;
    private final TokenCache mTokenCache;

    /**
     * The authority the application will use to obtain tokens.
     */
    private String mAuthorityString;

    /**
     * The client ID of the application. This should come from the app developer portal.
     */
    private String mClientId;

    /**
     * Unique String identifier used in logging/telemetry callbacks to identify.
     * component in the application using MSAL
     */
    private String mComponent;

    /**
     * The redirect URI for the application.
     */
    private String mRedirectUri;

    /**
     * When set to true (default), MSAL will compare the application's authority against well-known URL
     * templates representing well-formed authorities. It is useful when the authority is obtained at
     * run time to prevent MSAL from displaying authentication prompts from malicious pages.
     */
    private boolean mValidateAuthority = true;
    private String mSliceParameters = "";

    private PublicClientApplicationConfiguration mPublicClientConfiguration;

    /**
     * @deprecated
     * This constructor has been replaced with one that leverages a configuration file.
     * <p> Use {@link PublicClientApplication#PublicClientApplication(Context, int)}</p> instead.
     *
     *
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
     *
     * @param context Application's {@link Context}. The sdk requires the application context to be passed in
     *                {@link PublicClientApplication}. Cannot be null.
     *                <p>
     *                Note: The {@link Context} should be the application context instead of the running activity's context, which could potentially make the sdk hold a
     *                strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                </p>
     */
    @Deprecated
    public PublicClientApplication(@NonNull final Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null.");
        }

        mAppContext = context;
        setupConfiguration();
        //Deprecating configuration in Metadata for now will copy and provided state to config object
        loadMetaDataFromManifest();
        mTokenCache = new TokenCache(mAppContext);

        initializeApplication();
    }

    /**
     * {@link PublicClientApplication#PublicClientApplication(Context, int)} will read the client id and other configuration settings from the
     * file included in your applications resources.
     *
     * For more information on adding configuration files to your applications resources please
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *
     * For more information on the schema of the MSAL config json please
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *
     * @param context Application's {@link Context}. The sdk requires the application context to be passed in
     *                {@link PublicClientApplication}. Cannot be null.
     *                <p>
     *                Note: The {@link Context} should be the application context instead of the running activity's context, which could potentially make the sdk hold a
     *                strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                </p>
     *
     * @param configFileResourceId The resource ID of the raw file containing the JSON configuration for the PublicClientApplication
     */
    public PublicClientApplication(@NonNull final Context context, final int configFileResourceId){
        if (context == null) {
            throw new IllegalArgumentException("context is null.");
        }

        mAppContext = context;
        mTokenCache = new TokenCache(mAppContext);
        setupConfiguration(configFileResourceId);

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
        mTokenCache = new TokenCache(mAppContext);
        mClientId = clientId;
        mAuthorityString = DEFAULT_AUTHORITY;
        setupConfiguration();
        mPublicClientConfiguration.mClientId = clientId;

        initializeApplication();
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
    public PublicClientApplication(@NonNull final Context context, @NonNull final String clientId, @NonNull final String authority) {
        this(context, clientId);

        if (MsalUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("authority is empty or null");
        }

        mAuthorityString = authority;

        mPublicClientConfiguration.getAuthorities().clear();
        mPublicClientConfiguration.getAuthorities().add(Authority.getAuthorityFromAuthorityUrl(authority));
    }

    private void initializeApplication() {
        // Init Events with defaults (application-wide)
        DefaultEvent.initializeDefaults(
                Defaults.forApplication(mAppContext, mClientId)
        );

        mRedirectUri = createRedirectUri(mClientId);
        validateInputParameters();

        // Since network request is sent from the sdk, if calling app doesn't declare the internet permission in the
        // manifest, we cannot make the network call.
        checkInternetPermission();
        Logger.info(TAG, null, "Create new public client application.");
    }


    /**
     * @return The current version for the sdk.
     */
    public static String getSdkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * @deprecated
     * The use of this property setter is no longer required.  Authorities will be considered valid
     * if they are asserted by the developer via configuration or if Microsoft recognizes the cloud within which the authority exists.
     *
     * This setter no longer controls MSAL behavior.
     *
     * By Default, authority validation is turned on. To turn on authority validation, set
     * {@link PublicClientApplication#setValidateAuthority(boolean)} to false.
     *
     * @param validateAuthority True if authority validation is on, false otherwise. By default, authority
     *                          validation is turned on.
     */
    @Deprecated
    public void setValidateAuthority(final boolean validateAuthority) {
        mValidateAuthority = validateAuthority;
    }

    /**
     * Returns the PublicClientConfiguration for this instance of PublicClientApplication
     * Configuration is based on the defaults established for MSAl and can be overridden by creating the
     * PublicClientApplication using {@link PublicClientApplication#PublicClientApplication(Context, int)}
     * @return
     */
    public PublicClientApplicationConfiguration getConfiguration() {
        return mPublicClientConfiguration;
    }

    /**
     * Specify the string identifier to identify the component that consumes MSAL.
     * This is intended for libraries that consume MSAL which are embedded in apps that might also be using MSAL
     * as well. This allows logging, telemetry apps, or library developers to differentiate MSAL usage
     * from the app to MSAL usage by component libraries.
     *
     * @param component The component identifier string passed into MSAL when creating the application object
     */
    public void setComponent(final String component) {
        mComponent = component;
    }

    /**
     * @deprecated
     * If you're a Micorosft developer who needs to target a specific slice please refer to the AAD Onboarding documentation for instruction on how to do so.
     *
     * Custom query parameters which maybe sent to the STS for dogfood testing. This parameter should not be set by developers as it may
     * have adverse effect on the application.
     *
     * @param sliceParameters The custom query parameters(for dogfood testing) sent to token and authorize endpoint.
     */
    @Deprecated
    public void setSliceParameters(final String sliceParameters) {
        mSliceParameters = sliceParameters;
    }

    /**
     * Returns the list of {@link User}s we have tokens in the cache.
     *
     * @return Immutable List of {@link User}.
     * @throws MsalClientException If failed to retrieve users from the cache.
     */
    public List<User> getUsers() throws MsalClientException {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        final ApiEvent.Builder apiEventBuilder = new ApiEvent.Builder(telemetryRequestId);
        final URL authorityURL = MsalUtils.getUrl(mAuthorityString);
        apiEventBuilder.setAuthority(authorityURL.getProtocol() + "://" + authorityURL.getHost());
        Telemetry.getInstance().startEvent(telemetryRequestId, apiEventBuilder);

        final UUID correlationId = UUID.randomUUID();
        initializeDiagnosticContext(correlationId.toString());

        List<User> users = mTokenCache.getUsers(
                AuthorityMetadata.getAuthorityHost(mAuthorityString, mValidateAuthority),
                mClientId,
                new RequestContext(
                        correlationId,
                        mComponent,
                        telemetryRequestId
                )
        );

        apiEventBuilder.setApiCallWasSuccessful(true);
        stopTelemetryEventAndFlush(apiEventBuilder);
        return users;
    }

    /**
     * Returns the {@link User} that is matching the provided user identifier.
     *
     * @param userIdentifier The unique identifier for a {@link User} across tenant.
     * @return The {@link User} matching the provided user identifier.
     * @throws MsalClientException If failed to retrieve users from the cache.
     */
    public User getUser(final String userIdentifier) throws MsalClientException {
        if (MsalUtils.isEmpty(userIdentifier)) {
            throw new IllegalArgumentException("Empty or null userIdentifier");
        }

        final List<User> users = getUsers();
        for (final User user : users) {
            if (user.getUserIdentifier().equals(userIdentifier)) {
                return user;
            }
        }

        return null;
    }

    /**
     * MSAL requires the calling app to pass an {@link Activity} which <b> MUST </b> call this method to get the auth
     * code passed back correctly.
     *
     * @param requestCode The request code for interactive request.
     * @param resultCode  The result code for the request to get auth code.
     * @param data        {@link Intent} either contains the url with auth code as query string or the errors.
     */
    public void handleInteractiveRequestRedirect(int requestCode, int resultCode, final Intent data) {
        InteractiveRequest.onActivityResult(requestCode, resultCode, data);
    }

    // Interactive APIs. Will launch the system browser with web UI.

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
    public void acquireToken(@NonNull final Activity activity, @NonNull final String[] scopes, @NonNull final AuthenticationCallback callback) {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        ApiEvent.Builder apiEventBuilder = createApiEventBuilder(telemetryRequestId, API_ID_ACQUIRE);

        acquireTokenInteractive(activity, scopes, "", UiBehavior.SELECT_ACCOUNT, "", null, "", null,
                wrapCallbackForTelemetryIntercept(apiEventBuilder, callback), telemetryRequestId, apiEventBuilder);
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
    public void acquireToken(@NonNull final Activity activity, @NonNull final String[] scopes, final String loginHint,
                             @NonNull final AuthenticationCallback callback) {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        ApiEvent.Builder apiEventBuilder = createApiEventBuilder(telemetryRequestId, API_ID_ACQUIRE_WITH_HINT);

        acquireTokenInteractive(activity, scopes, loginHint, UiBehavior.SELECT_ACCOUNT, "", null, "", null,
                wrapCallbackForTelemetryIntercept(apiEventBuilder, callback), telemetryRequestId, apiEventBuilder);
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
    public void acquireToken(@NonNull final Activity activity, @NonNull final String[] scopes, final String loginHint, final UiBehavior uiBehavior,
                             final String extraQueryParameters, @NonNull final AuthenticationCallback callback) {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        ApiEvent.Builder apiEventBuilder = createApiEventBuilder(telemetryRequestId, API_ID_ACQUIRE_WITH_HINT_BEHAVIOR_AND_PARAMETERS);

        acquireTokenInteractive(activity, scopes, loginHint, uiBehavior == null ? UiBehavior.SELECT_ACCOUNT : uiBehavior,
                extraQueryParameters, null, "", null, wrapCallbackForTelemetryIntercept(apiEventBuilder, callback), telemetryRequestId, apiEventBuilder);
    }

    /**
     * Acquire token interactively, will pop-up webUI. Interactive flow will skip the cache lookup.
     * Default value for {@link UiBehavior} is {@link UiBehavior#SELECT_ACCOUNT}.
     *
     * @param activity            Non-null {@link Activity} that will be used as the parent activity for launching the {@link AuthenticationActivity}.
     *                            All the apps doing interactive request are required to call the
     *                            {@link PublicClientApplication#handleInteractiveRequestRedirect(int, int, Intent)} within the calling
     *                            activity {@link Activity#onActivityResult(int, int, Intent)}.
     * @param scopes              The non-null array of scopes to be requested for the access token.
     *                            MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param user                Optional. If provided, will be used to force the session continuation.  If user tries to sign in with a different user,
     *                            error will be returned.
     * @param uiBehavior          The {@link UiBehavior} for prompting behavior. By default, the sdk use {@link UiBehavior#SELECT_ACCOUNT}.
     * @param extraQueryParameter Optional. The extra query parameter sent to authorize endpoint.
     * @param callback            The Non-null {@link AuthenticationCallback} to receive the result back.
     *                            1) If user cancels the flow by pressing the device back button, the result will be sent
     *                            back via {@link AuthenticationCallback#onCancel()}.
     *                            2) If the sdk successfully receives the token back, result will be sent back via
     *                            {@link AuthenticationCallback#onSuccess(AuthenticationResult)}
     *                            3) All the other errors will be sent back via
     *                            {@link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireToken(@NonNull final Activity activity, @NonNull final String[] scopes, final User user, final UiBehavior uiBehavior,
                             final String extraQueryParameter, @NonNull final AuthenticationCallback callback) {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        ApiEvent.Builder apiEventBuilder = createApiEventBuilder(telemetryRequestId, API_ID_ACQUIRE_WITH_USER_BEHAVIOR_AND_PARAMETERS);

        acquireTokenInteractive(activity, scopes, "", uiBehavior, extraQueryParameter, null, "", user, wrapCallbackForTelemetryIntercept(
                apiEventBuilder, callback), telemetryRequestId, apiEventBuilder);
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
     * @param extraQueryParams     Optional. The extra query parameter sent to authorize endpoint.
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
    public void acquireToken(@NonNull final Activity activity, @NonNull final String[] scopes, final String loginHint, final UiBehavior uiBehavior,
                             final String extraQueryParams, final String[] extraScopesToConsent, final String authority,
                             @NonNull final AuthenticationCallback callback) {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        ApiEvent.Builder apiEventBuilder = createApiEventBuilder(telemetryRequestId, API_ID_ACQUIRE_WITH_HINT_BEHAVIOR_PARAMETERS_AND_AUTHORITY);

        acquireTokenInteractive(activity, scopes, loginHint, uiBehavior == null ? UiBehavior.SELECT_ACCOUNT : uiBehavior,
                extraQueryParams, extraScopesToConsent, authority, null, wrapCallbackForTelemetryIntercept(apiEventBuilder, callback), telemetryRequestId, apiEventBuilder);
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
     * @param user                 Optional. If provided, will be used to force the session continuation.  If user tries to sign in with a different user, error
     *                             will be returned.
     * @param uiBehavior           The {@link UiBehavior} for prompting behavior. By default, the sdk use {@link UiBehavior#SELECT_ACCOUNT}.
     * @param extraQueryParams     Optional. The extra query parameter sent to authorize endpoint.
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
    public void acquireToken(@NonNull final Activity activity, @NonNull final String[] scopes, final User user, final UiBehavior uiBehavior,
                             final String extraQueryParams, final String[] extraScopesToConsent, final String authority,
                             @NonNull final AuthenticationCallback callback) {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        ApiEvent.Builder apiEventBuilder = createApiEventBuilder(telemetryRequestId, API_ID_ACQUIRE_WITH_USER_BEHAVIOR_PARAMETERS_AND_AUTHORITY);

        acquireTokenInteractive(activity, scopes, "", uiBehavior == null ? UiBehavior.SELECT_ACCOUNT : uiBehavior, extraQueryParams, extraScopesToConsent,
                authority, user, wrapCallbackForTelemetryIntercept(apiEventBuilder, callback), telemetryRequestId, apiEventBuilder);
    }

    // Silent call APIs.

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param scopes   The non-null array of scopes to be requested for the access token.
     *                 MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param user     {@link User} represents the user to silently request tokens.
     * @param callback {@link AuthenticationCallback} that is used to send the result back. The success result will be
     *                 sent back via {@link AuthenticationCallback#onSuccess(AuthenticationResult)}.
     *                 Failure case will be sent back via {
     * @link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireTokenSilentAsync(@NonNull final String[] scopes, @NonNull final User user,
                                        @NonNull final AuthenticationCallback callback) {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        ApiEvent.Builder apiEventBuilder = createApiEventBuilder(telemetryRequestId, ACQUIRE_TOKEN_SILENT_ASYNC_WITH_USER);

        acquireTokenSilent(scopes, user, "", false, wrapCallbackForTelemetryIntercept(apiEventBuilder, callback), telemetryRequestId, apiEventBuilder);
    }

    /**
     * Perform acquire token silent call. If there is a valid access token in the cache, the sdk will return the access token; If
     * no valid access token exists, the sdk will try to find a refresh token and use the refresh token to get a new access token. If refresh token does not exist
     * or it fails the refresh, exception will be sent back via callback.
     *
     * @param scopes       The non-null array of scopes to be requested for the access token.
     *                     MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     * @param user         {@link User} represents the user to silently request tokens.
     * @param authority    Optional. Can be passed to override the configured authority.
     * @param forceRefresh True if the request is forced to refresh, false otherwise.
     * @param callback     {@link AuthenticationCallback} that is used to send the result back. The success result will be
     *                     sent back via {@link AuthenticationCallback#onSuccess(AuthenticationResult)}.
     *                     Failure case will be sent back via {
     * @link AuthenticationCallback#onError(MsalException)}.
     */
    public void acquireTokenSilentAsync(@NonNull final String[] scopes, @NonNull final User user, final String authority,
                                        final boolean forceRefresh,
                                        @NonNull final AuthenticationCallback callback) {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        ApiEvent.Builder apiEventBuilder = createApiEventBuilder(telemetryRequestId, ACQUIRE_TOKEN_SILENT_ASYNC_WITH_USER_AUTHORITY_AND_FORCE_REFRESH);

        acquireTokenSilent(scopes, user, authority, forceRefresh, wrapCallbackForTelemetryIntercept(apiEventBuilder, callback), telemetryRequestId, apiEventBuilder);
    }


    /**
     * Deletes all matching tokens (access & refresh tokens) for the {@link User} instance from the application cache.
     *
     * @param user {@link User} whose tokens should be deleted.
     */
    public void remove(final User user) {
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        final ApiEvent.Builder apiEventBuilder = new ApiEvent.Builder(telemetryRequestId);
        final URL authorityURL = MsalUtils.getUrl(mAuthorityString);
        apiEventBuilder.setAuthority(authorityURL.getProtocol() + "://" + authorityURL.getHost());
        Telemetry.getInstance().startEvent(telemetryRequestId, apiEventBuilder);

        final UUID correlationId = UUID.randomUUID();
        initializeDiagnosticContext(correlationId.toString());

        final RequestContext requestContext = new RequestContext(correlationId, mComponent, telemetryRequestId);
        mTokenCache.deleteRefreshTokenByUser(user, requestContext);
        mTokenCache.deleteAccessTokenByUser(user, requestContext);

        apiEventBuilder.setApiCallWasSuccessful(true);
        stopTelemetryEventAndFlush(apiEventBuilder);
    }

    /**
     * Keep this method internal only to make it easy for MS apps to do serialize/deserialize on the family tokens.
     *
     * @return The {@link TokenCache} that is used to persist token items for the running app.
     */
    TokenCache getTokenCache() {
        return mTokenCache;
    }

    private void loadMetaDataFromManifest() {
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

        // TODO: Comment out for now. As discussed, redirect should be computed during runtime, developer needs to put
//        final String redirectUri = applicationInfo.metaData.getString(REDIRECT_META_DATA);
//        if (!MsalUtils.isEmpty(redirectUri)) {
//            mRedirectUri = redirectUri;
//        }
    }

    private void setupConfiguration(final int configResourceId) {
        PublicClientApplicationConfiguration developerConfig = loadConfiguration(configResourceId);
        PublicClientApplicationConfiguration defaultConfig = loadDefaultConfiguration();
        defaultConfig.mergeConfiguration(developerConfig);
        mPublicClientConfiguration = defaultConfig;
    }

    private void setupConfiguration() {
        mPublicClientConfiguration = loadDefaultConfiguration();
    }

    private PublicClientApplicationConfiguration loadConfiguration(final int configResourceId)  {

        InputStream configStream = mAppContext.getResources().openRawResource(configResourceId);
        byte[] buffer;

        try {
            buffer = new byte[configStream.available()];
            configStream.read(buffer);
        }catch(IOException e){
            if(configResourceId == R.raw.msal_default_config) {
                throw new IllegalStateException("Unable to open default configuration file.  MSAL module may be incomplete.");
            }else{
                throw new IllegalArgumentException("Provided config file resource id could not be accessed");
            }
        }

        String config = new String(buffer);

        Gson gson = getGsonForLoadingConfiguration();

        PublicClientApplicationConfiguration configObject = gson.fromJson(config, PublicClientApplicationConfiguration.class);

        return configObject;


    }

    private PublicClientApplicationConfiguration loadDefaultConfiguration() {
        return loadConfiguration(R.raw.msal_default_config);
    }


    private Gson getGsonForLoadingConfiguration(){

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Authority.class, new AuthorityDeserializer())
                .registerTypeAdapter(AzureActiveDirectoryAudience.class, new AzureActiveDirectoryAudienceDeserializer())
                .create();

        return gson;

    }


    // TODO: if no more input validation is needed, this could be moved back to the constructor.
    private void validateInputParameters() {
        if (!MsalUtils.hasCustomTabRedirectActivity(mAppContext, mRedirectUri)) {
            //TODO: Fix this error message to be more clear
            throw new IllegalStateException("App doesn't have the correct configuration for "
                    + BrowserTabActivity.class.getSimpleName() + ".");
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
     * Redirect uri will the in the format of msauth-clientid://appPackageName.
     * The sdk will comupte the redirect when the PublicClientApplication is initialized.
     */
    private String createRedirectUri(final String clientId) {
        return "msal" + clientId + "://auth";
    }


    private void acquireTokenInteractive(final Activity activity, final String[] scopes, final String loginHint, final UiBehavior uiBehavior,
                                         final String extraQueryParams, final String[] extraScopesToConsent,
                                         final String authority, final User user, final AuthenticationCallback callback,
                                         final String telemetryRequestId, final ApiEvent.Builder apiEventBuilder) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }

        final AuthenticationRequestParameters requestParameters = getRequestParameters(authority, scopes, loginHint,
                extraQueryParams, uiBehavior, user, telemetryRequestId);

        // add properties to our telemetry data
        apiEventBuilder
                .setAuthorityType(requestParameters.getAuthority().mAuthorityType)
                .setLoginHint(loginHint)
                .setUiBehavior(uiBehavior.name())
                .setCorrelationId(requestParameters.getRequestContext().getCorrelationId());

        Logger.info(TAG, requestParameters.getRequestContext(), "Preparing a new interactive request");
        final BaseRequest request = new InteractiveRequest(activity, requestParameters, extraScopesToConsent);
        request.getToken(callback);
    }

    private void acquireTokenSilent(final String[] scopes, final User user, final String authority,
                                    final boolean forceRefresh,
                                    final AuthenticationCallback callback,
                                    final String telemetryRequestId,
                                    final ApiEvent.Builder apiEventBuilder) {
        if (callback == null) {
            throw new IllegalArgumentException("callback is null");
        }

        final AuthorityMetadata authorityForRequest = MsalUtils.isEmpty(authority) ? AuthorityMetadata.createAuthority(mAuthorityString, mValidateAuthority)
                : AuthorityMetadata.createAuthority(authority, mValidateAuthority);

        // Initialize Logging & RequestContext
        final UUID correlationId = UUID.randomUUID();
        initializeDiagnosticContext(correlationId.toString());

        final RequestContext requestContext = new RequestContext(correlationId, mComponent, telemetryRequestId);
        final Set<String> scopesAsSet = MsalUtils.convertArrayToSet(scopes);
        final AuthenticationRequestParameters requestParameters = AuthenticationRequestParameters.create(authorityForRequest, mTokenCache,
                scopesAsSet, mClientId, mSliceParameters, requestContext);

        // add properties to our telemetry data
        apiEventBuilder
                .setAuthorityType(requestParameters.getAuthority().mAuthorityType)
                .setLoginHint(requestParameters.getLoginHint())
                .setCorrelationId(requestParameters.getRequestContext().getCorrelationId());

        if (null != requestParameters.getUiBehavior()) {
            apiEventBuilder.setUiBehavior(requestParameters.getUiBehavior().name());
        }

        Logger.info(TAG, requestContext, "Preparing a new silent request");
        final SilentRequest request = new SilentRequest(mAppContext, requestParameters, forceRefresh, user);
        request.setIsAuthorityProvided(!MsalUtils.isEmpty(authority));
        request.getToken(callback);
    }

    static void initializeDiagnosticContext(String correlationIdStr) {
        final com.microsoft.identity.common.internal.logging.RequestContext rc =
                new com.microsoft.identity.common.internal.logging.RequestContext();
        rc.put(AuthenticationConstants.AAD.CORRELATION_ID, correlationIdStr);
        DiagnosticContext.setRequestContext(rc);
    }

    private AuthenticationRequestParameters getRequestParameters(final String authority, final String[] scopes,
                                                                 final String loginHint, final String extraQueryParam,
                                                                 final UiBehavior uiBehavior, final User user, final String telemetryRequestId) {
        final AuthorityMetadata authorityForRequest = MsalUtils.isEmpty(authority) ? AuthorityMetadata.createAuthority(mAuthorityString, mValidateAuthority)
                : AuthorityMetadata.createAuthority(authority, mValidateAuthority);

        // Set up the correlationId for logging + request tracking
        final UUID correlationId = UUID.randomUUID();
        initializeDiagnosticContext(correlationId.toString());
        final Set<String> scopesAsSet = MsalUtils.convertArrayToSet(scopes);

        return AuthenticationRequestParameters.create(
                authorityForRequest,
                mTokenCache,
                scopesAsSet,
                mClientId,
                mRedirectUri,
                loginHint,
                extraQueryParam,
                uiBehavior,
                user,
                mSliceParameters,
                new RequestContext(
                        correlationId,
                        mComponent,
                        telemetryRequestId
                )
        );
    }

    private ApiEvent.Builder createApiEventBuilder(final String telemetryRequestId, final String apiId) {
        // Create the ApiEvent.Builder
        ApiEvent.Builder eventBuilder =
                new ApiEvent.Builder(telemetryRequestId)
                        .setApiId(apiId)
                        .setAuthority(mAuthorityString);

        // Start the Event on our Telemetry instance
        Telemetry.getInstance().startEvent(telemetryRequestId, eventBuilder);

        // Return the Builder
        return eventBuilder;
    }

    /**
     * Wraps {@link AuthenticationCallback} instances to bind Telemetry actions.
     *
     * @param eventBinding           the {@link com.microsoft.identity.client.ApiEvent.Builder}
     *                               monitoring this request.
     * @param authenticationCallback the original consuming callback
     * @return the wrapped {@link AuthenticationCallback} instance
     */
    private AuthenticationCallback wrapCallbackForTelemetryIntercept(
            final ApiEvent.Builder eventBinding, final AuthenticationCallback authenticationCallback) {
        if (null == authenticationCallback) {
            throw new IllegalArgumentException("callback is null");
        }
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(final AuthenticationResult authenticationResult) {
                eventBinding.setApiCallWasSuccessful(true);
                stopTelemetryEventAndFlush(eventBinding);
                authenticationCallback.onSuccess(authenticationResult);
            }

            @Override
            public void onError(final MsalException exception) {
                eventBinding.setApiCallWasSuccessful(false);
                eventBinding.setApiErrorCode(exception.getErrorCode());
                stopTelemetryEventAndFlush(eventBinding);
                authenticationCallback.onError(exception);
            }

            @Override
            public void onCancel() {
                stopTelemetryEventAndFlush(eventBinding);
                authenticationCallback.onCancel();
            }
        };
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void stopTelemetryEventAndFlush(final ApiEvent.Builder builder) {
        final ApiEvent event = builder.build();
        Telemetry.getInstance().stopEvent(event.getRequestId(), builder);
        Telemetry.getInstance().flush(event.getRequestId());
    }
}