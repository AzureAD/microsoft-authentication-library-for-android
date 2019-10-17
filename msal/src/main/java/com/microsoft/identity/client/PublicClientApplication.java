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
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.configuration.AccountMode;
import com.microsoft.identity.client.configuration.HttpConfiguration;
import com.microsoft.identity.client.configuration.LoggerConfiguration;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalDeclinedScopeException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.AsyncResult;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.common.adal.internal.tokensharing.TokenShareUtility;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandCallback;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.controllers.ExceptionAdapter;
import com.microsoft.identity.common.internal.controllers.GetDeviceModeCommand;
import com.microsoft.identity.common.internal.controllers.InteractiveTokenCommand;
import com.microsoft.identity.common.internal.controllers.TokenCommand;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.internal.eststelemetry.PublicApiId;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.net.HttpRequest;
import com.microsoft.identity.common.internal.net.cache.HttpCache;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdProviderConfiguration;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdProviderConfigurationClient;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.internal.result.ResultFuture;
import com.microsoft.identity.msal.BuildConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.microsoft.identity.client.PublicClientApplicationConfigurationFactory.initializeConfiguration;
import static com.microsoft.identity.client.internal.MsalUtils.throwOnMainThread;
import static com.microsoft.identity.client.internal.MsalUtils.validateNonNullArg;
import static com.microsoft.identity.client.internal.MsalUtils.validateNonNullArgument;
import static com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter.msalExceptionFromBaseException;
import static com.microsoft.identity.client.internal.controllers.OperationParametersAdapter.isAccountHomeTenant;
import static com.microsoft.identity.client.internal.controllers.OperationParametersAdapter.isHomeTenantEquivalent;
import static com.microsoft.identity.client.internal.controllers.OperationParametersAdapter.validateClaimsExistForTenant;
import static com.microsoft.identity.common.exception.ClientException.TOKEN_CACHE_ITEM_NOT_FOUND;
import static com.microsoft.identity.common.exception.ClientException.TOKEN_SHARING_DESERIALIZATION_ERROR;
import static com.microsoft.identity.common.exception.ClientException.TOKEN_SHARING_MSA_PERSISTENCE_ERROR;
import static com.microsoft.identity.common.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE;
import static com.microsoft.identity.common.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE;
import static com.microsoft.identity.common.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_CODE;
import static com.microsoft.identity.common.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_MESSAGE;
import static com.microsoft.identity.common.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE;
import static com.microsoft.identity.common.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE;
import static com.microsoft.identity.common.exception.ErrorStrings.SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE;
import static com.microsoft.identity.common.exception.ErrorStrings.SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE;
import static com.microsoft.identity.common.exception.ErrorStrings.SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE;
import static com.microsoft.identity.common.exception.ErrorStrings.SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE;

/**
 * <p>
 * This is the entry point for developer to create public native applications and make API calls to
 * acquire tokens.
 * <p><b>Client ID:</b> The clientID of your application is a unique identifier which can be
 * obtained from the app registration portal.</p>
 * <p><b>AuthorityMetadata:</b> A URL indicating a directory that MSAL can use to obtain tokens.
 * In Azure AD it is of the form https://<[nstance]/[tenant], where [instance] is the directory
 * host (e.g. https://login.microsoftonline.com) and [tenant] is an identifier within the directory
 * itself (e.g. a domain associated to the tenant, such as contoso.onmicrosoft.com, or the GUID
 * representing the  TenantID property of the directory)
 * <p>
 * For B2C, it is of the form https://[instance]/tfp/[tenant]/[policy] where instance and tenant
 * are same as Azure AD, and [policy] is a string like signup</p>
 * MSAL {@link PublicClientApplication} provides three constructors allowing the client id to be
 * set either via AndroidManifest.xml metadata or using constructor parameters.
 * Similarly, if developer chooses not to use the default authority
 * https://login.microsoftonline.com, an alternate can also be configured using the manifest,
 * constructor parameters, or in acquire token calls.
 * </p>
 * <p>
 * Redirect is auto-generated in the library in the format of msal<client-id>://auth, and it cannot
 * be overridden.
 * </p>
 * <p>
 * Developer <b>MUST</b> have {@link BrowserTabActivity} declared in their manifest, which must
 * have the correct intent-filter configured. If the wrong scheme and host is provided, the sdk
 * will fail the {@link PublicClientApplication} creation.
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
 * <p><b>Scopes:</b>Permissions that the developers wants included in the access token received .
 * Not all scopes are guaranteed to be included in the access token returned.
 * </p>
 * <p>
 * <b>Login Hint:</b> Usually an email, to pass to the service at the beginning of the
 * interactive authentication flow.
 * </p>
 * <p>
 * <b>Extra Scopes to Consent:</b>  Permissions you want the user to consent to in the same
 * authentication flow,
 * but won't be included in the returned access token.
 * </p>
 * </p>
 */
public class PublicClientApplication implements IPublicClientApplication, ITokenShare {

    private static final String TAG = PublicClientApplication.class.getSimpleName();
    private static final String INTERNET_PERMISSION = "android.permission.INTERNET";
    private static final String ACCESS_NETWORK_STATE_PERMISSION = "android.permission.ACCESS_NETWORK_STATE";
    private static final ExecutorService sBackgroundExecutor = Executors.newCachedThreadPool();

    static class NONNULL_CONSTANTS {
        static final String CONTEXT = "context";
        static final String LISTENER = "listener";
        static final String CALLBACK = "callback";
        static final String CLIENT_ID = "client_id";
        static final String AUTHORITY = "authority";
        static final String CONFIG_FILE = "config_file";
        static final String ACTIVITY = "activity";
        static final String SCOPES = "scopes";
        static final String ACCOUNT = "account";

        static final String NULL_ERROR_SUFFIX = " cannot be null or empty";
    }


    /**
     * Constant used to signal a home account's tenant id should be used when performing cache
     * lookups relative to creating OperationParams.
     */

    private static final String TSL_MSG_FAILED_TO_SAVE
            = "Failed to save FRT - see getCause() for additional Exception info";

    private static final String TSM_MSG_FAILED_TO_RETRIEVE
            = "Failed to retrieve FRT - see getCause() for additional Exception info";

    protected PublicClientApplicationConfiguration mPublicClientConfiguration;
    protected TokenShareUtility mTokenShareUtility;

    //region PCA factory methods

    /**
     * {@link PublicClientApplication#create(Context, int, ApplicationCreatedListener)} will read
     * the client id and other configuration settings from the
     * file included in your application resources.
     *
     * @param context              Application's {@link Context}. The sdk requires the application
     *                             context to be passed in
     *                             {@link PublicClientApplication}. Cannot be null.
     *                             <p>
     *                             Note: The {@link Context} should be the application context
     *                             instead of the running activity's context, which could
     *                             potentially make the sdk hold a
     *                             strong reference to the activity, thus preventing correct
     *                             garbage collection and causing bugs.
     *                             </p>
     * @param configFileResourceId The resource ID of the raw file containing the JSON
     *                             configuration for the PublicClientApplication.
     *                             <p>
     *                             For more information on the schema of the MSAL config json,
     *                             please see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                             and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                             </p>
     * @param listener             a callback to be invoked when the object is successfully created.
     *                             Cannot be null.
     * @see PublicClientApplication#create(Context, File, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, String, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, String, String, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, int)
     */
    public static void create(@NonNull final Context context,
                              final int configFileResourceId,
                              @NonNull final ApplicationCreatedListener listener) {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        create(
                initializeConfiguration(context, configFileResourceId),
                null, // client id
                null, // authority
                listener
        );
    }

    /**
     * {@link PublicClientApplication#create(Context, File, ApplicationCreatedListener)}
     * will read the client id and other configuration settings from the specified file.
     *
     * @param context    Application's {@link Context}. The sdk requires the application context to
     *                   be passed in {@link PublicClientApplication}. Cannot be null.
     *                   <p>
     *                   Note: The {@link Context} should be the application context instead of the
     *                   running activity's context, which could potentially make the sdk hold a
     *                   strong reference to the activity, thus preventing correct garbage
     *                   collection and causing bugs.
     *                   </p>
     * @param configFile The file containing the JSON configuration for the PublicClientApplication.
     *                   Cannot be null.
     *                   <p>
     *                   For more information on the schema of the MSAL configuration file, please
     *                   see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                   and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                   </p>
     * @param listener   a callback to be invoked when the object is successfully created. Cannot be null.
     * @see PublicClientApplication#create(Context, int, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, String, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, String, String, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, int)
     */
    public static void create(@NonNull final Context context,
                              @Nullable final File configFile,
                              @NonNull final ApplicationCreatedListener listener) {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        create(
                initializeConfiguration(context, configFile),
                null, // client id
                null, // authority
                listener
        );
    }

    /**
     * {@link PublicClientApplication#create(Context, String, ApplicationCreatedListener)} allows
     * the client id to be passed instead of providing through the AndroidManifest metadata.
     * If this constructor is called, the default authority https://login.microsoftonline.com/common
     * will be used.
     *
     * @param context  Application's {@link Context}. The sdk requires the application context to
     *                 be passed in {@link PublicClientApplication}. Cannot be null.
     *                 <p>
     *                 Note: The {@link Context} should be the application context instead of the
     *                 running activity's context, which could potentially make the sdk hold a
     *                 strong reference to the activity, thus preventing correct garbage collection
     *                 and causing bugs.
     *                 </p>
     * @param clientId The application's client id. Cannot be null.
     * @param listener a callback to be invoked when the object is successfully created.
     *                 Cannot be null.
     * @see PublicClientApplication#create(Context, int, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, File, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, String, String, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, int)
     */
    public static void create(@NonNull final Context context,
                              @NonNull final String clientId,
                              @NonNull final ApplicationCreatedListener listener) {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(clientId, NONNULL_CONSTANTS.CLIENT_ID);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        create(
                initializeConfiguration(context),
                clientId,
                null, // authority
                listener
        );
    }

    /**
     * {@link PublicClientApplication#create(Context, String, String, ApplicationCreatedListener)}
     * allows the client id and authority to be passed instead of providing them through metadata.
     *
     * @param context   Application's {@link Context}. The sdk requires the application context to
     *                  be passed in
     *                  {@link PublicClientApplication}. Cannot be null.
     *                  <p>
     *                  Note: The {@link Context} should be the application context instead of
     *                  an running activity's context, which could potentially make the sdk hold a
     *                  strong reference to the activity, thus preventing correct garbage
     *                  collection and causing bugs.
     *                  </p>
     * @param clientId  The application client id. Cannot be null.
     * @param authority The default authority to be used for the authority. Cannot be null.
     * @param listener  a callback to be invoked when the object is successfully created.
     *                  Cannot be null.
     * @see PublicClientApplication#create(Context, int, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, File, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, String, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, int)
     */
    public static void create(@NonNull final Context context,
                              @NonNull final String clientId,
                              @NonNull final String authority,
                              @NonNull final ApplicationCreatedListener listener) {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(clientId, NONNULL_CONSTANTS.CLIENT_ID);
        validateNonNullArgument(authority, NONNULL_CONSTANTS.AUTHORITY);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        create(
                initializeConfiguration(context),
                clientId,
                authority,
                listener
        );
    }

    /**
     * {@link PublicClientApplication#create(Context, int, ApplicationCreatedListener)}
     * will read the client id and other configuration settings from the file included in your
     * application's resources.
     *
     * @param context              Application's {@link Context}. The sdk requires the application
     *                             context to be passed in
     *                             {@link PublicClientApplication}. Cannot be null.
     *                             <p>
     *                             Note: The {@link Context} should be the application context
     *                             instead of the running activity's context, which could
     *                             potentially make the sdk hold a
     *                             strong reference to the activity, thus preventing correct
     *                             garbage collection and causing bugs.
     *                             </p>
     * @param configFileResourceId The resource ID of the raw file containing the JSON
     *                             configuration for the PublicClientApplication
     *                             <p>
     *                             For more information on the schema of the MSAL config json,
     *                             please see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                             and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                             </p>
     * @return An instance of IPublicClientApplication.
     * @throws IllegalStateException if this function is invoked on the main thread.
     * @see PublicClientApplication#create(Context, int, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, File, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, String, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, String, String, ApplicationCreatedListener)
     */
    @WorkerThread
    @NonNull
    public static IPublicClientApplication create(@NonNull final Context context,
                                                  final int configFileResourceId) throws InterruptedException, MsalException {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);

        return create(initializeConfiguration(context, configFileResourceId));
    }
    //endregion

    //region Multiple Account PCA factory methods.

    /**
     * {@link PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int, IMultipleAccountApplicationCreatedListener)}
     * will read the client id and other configuration settings from the file included in your
     * application's resources.
     *
     * <p><p>This function will pass back an {@link MsalClientException} object if it is unable to
     * return {@link IMultipleAccountPublicClientApplication}.
     * For example, when the device is marked as 'shared'
     * ({@link PublicClientApplication#isSharedDevice()} is set to true) </p></p>
     *
     * @param context              Application's {@link Context}. The sdk requires the application
     *                             context to be passed in {@link PublicClientApplication}.
     *                             Cannot be null.
     *                             <p>
     *                             Note: The {@link Context} should be the application context
     *                             instead of the running activity's context, which could
     *                             potentially make the sdk hold a
     *                             strong reference to the activity, thus preventing correct
     *                             garbage collection and causing bugs.
     *                             </p>
     * @param configFileResourceId The resource ID of the raw file containing the JSON
     *                             configuration for the PublicClientApplication.
     *                             <p>
     *                             For more information on the schema of the MSAL config json,
     *                             please see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                             and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                             </p>
     * @param listener             a callback to be invoked when the object is successfully created. Cannot be null.
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, File, IMultipleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int)
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, File)
     */
    public static void createMultipleAccountPublicClientApplication(@NonNull final Context context,
                                                                    final int configFileResourceId,
                                                                    @NonNull final IMultipleAccountApplicationCreatedListener listener) {

        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        createMultipleAccountPublicClientApplication(
                initializeConfiguration(context, configFileResourceId),
                listener
        );
    }

    /**
     * {@link PublicClientApplication#createMultipleAccountPublicClientApplication(Context, File, IMultipleAccountApplicationCreatedListener)}
     * will read the client id and other configuration settings from the
     * file included in your application resources.
     *
     * <p><p>This function will pass back an {@link MsalClientException} object if it is unable to
     * return {@link IMultipleAccountPublicClientApplication}. For example, when the device is
     * marked as 'shared' ({@link PublicClientApplication#isSharedDevice()} is set to true) </p></p>
     *
     * @param context    Application's {@link Context}. The sdk requires the application context to
     *                   be passed in {@link PublicClientApplication}. Cannot be null.
     *                   <p>
     *                   Note: The {@link Context} should be the application context instead of the
     *                   running activity's context, which could potentially make the sdk hold a
     *                   strong reference to the activity, thus preventing correct garbage
     *                   collection and causing bugs.
     *                   </p>
     * @param configFile The file containing the JSON configuration for the PublicClientApplication.
     *                   Cannot be null.
     *                   <p>
     *                   For more information on the schema of the MSAL config json, please see
     *                   <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                   and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                   </p>
     * @param listener   a callback to be invoked when the object is successfully created. Cannot be null.
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int, IMultipleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int)
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, File)
     */
    public static void createMultipleAccountPublicClientApplication(@NonNull final Context context,
                                                                    @NonNull final File configFile,
                                                                    @NonNull final IMultipleAccountApplicationCreatedListener listener) {

        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        createMultipleAccountPublicClientApplication(
                initializeConfiguration(context, configFile),
                listener
        );
    }

    /**
     * {@link PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int)}
     * will read the client id and other configuration settings from the file included in your
     * application's resources.
     *
     * <p><p>This function will pass back an {@link MsalClientException} object if it is unable to
     * return {@link IMultipleAccountPublicClientApplication}. For example, when the device is
     * marked as 'shared' ({@link PublicClientApplication#isSharedDevice()} is set to true) </p></p>
     *
     * @param context              Application's {@link Context}. The sdk requires the application
     *                             context to be passed in
     *                             {@link PublicClientApplication}. Cannot be null.
     *                             <p>
     *                             Note: The {@link Context} should be the application context
     *                             instead of the running activity's context, which could
     *                             potentially make the sdk hold a
     *                             strong reference to the activity, thus preventing correct
     *                             garbage collection and causing bugs.
     *                             </p>
     * @param configFileResourceId The resource ID of the raw file containing the JSON
     *                             configuration for the PublicClientApplication.
     *                             <p>
     *                             For more information on the schema of the MSAL config json,
     *                             please see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                             and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                             </p>
     * @return An instance of IMultipleAccountPublicClientApplication.
     * @throws IllegalStateException if this function is invoked on the main thread.
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int, IMultipleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, File, IMultipleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, File)
     */
    @WorkerThread
    @NonNull
    public static IMultipleAccountPublicClientApplication createMultipleAccountPublicClientApplication(@NonNull final Context context,
                                                                                                       @NonNull final int configFileResourceId) throws MsalException, InterruptedException {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);

        return createMultipleAccountPublicClientApplication(
                initializeConfiguration(context, configFileResourceId)
        );
    }

    /**
     * {@link PublicClientApplication#createMultipleAccountPublicClientApplication(Context, File)}
     * will read the client id and other configuration settings from the
     * file included in your application resources.
     *
     * <p><p>This function will pass back an {@link MsalClientException} object if it is unable
     * to return {@link IMultipleAccountPublicClientApplication}. For example, when the device is
     * marked as 'shared' ({@link PublicClientApplication#isSharedDevice()} is set to true) </p></p>
     *
     * @param context    Application's {@link Context}. The sdk requires the application context
     *                   to be passed in {@link PublicClientApplication}. Cannot be null.
     *                   <p>
     *                   Note: The {@link Context} should be the application context instead of
     *                   the running activity's context, which could potentially make the sdk hold a
     *                   strong reference to the activity, thus preventing correct garbage
     *                   collection and causing bugs.
     *                   </p>
     * @param configFile The file containing the JSON configuration for the PublicClientApplication.
     *                   Cannot be null.
     *                   <p>
     *                   For more information on the schema of the MSAL configuration file, please
     *                   see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                   and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                   </p>
     * @throws IllegalStateException if this function is invoked on the main thread.
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int, IMultipleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, File, IMultipleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int)
     */
    @WorkerThread
    @NonNull
    public static IMultipleAccountPublicClientApplication createMultipleAccountPublicClientApplication(@NonNull final Context context,
                                                                                                       @NonNull final File configFile) throws InterruptedException, MsalException {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(configFile, "configFile");

        return createMultipleAccountPublicClientApplication(
                initializeConfiguration(context, configFile)
        );
    }

    //endregion

    //region Single Account PCA factory methods.

    /**
     * {@link PublicClientApplication#createSingleAccountPublicClientApplication(Context, int, ISingleAccountApplicationCreatedListener)}
     * will read the client id and other configuration settings from the file included in your
     * application's resources.
     *
     * <p><p>This function will pass back an {@link MsalClientException} object if it is unable to
     * return {@link ISingleAccountApplicationCreatedListener}. For example, AccountMode in
     * configuration is not set to single. </p></p>
     *
     * @param context              Application's {@link Context}. The sdk requires the application
     *                             context to be passed in {@link PublicClientApplication}.
     *                             Cannot be null.
     *                             <p>
     *                             Note: The {@link Context} should be the application context
     *                             instead of the running activity's context, which could
     *                             potentially make the sdk hold a
     *                             strong reference to the activity, thus preventing correct
     *                             garbage collection and causing bugs.
     *                             </p>
     * @param configFileResourceId The resource ID of the raw file containing the JSON
     *                             configuration for the PublicClientApplication.
     *                             <p>
     *                             For more information on the schema of the MSAL config json,
     *                             please see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                             and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                             </p>
     * @param listener             a callback to be invoked when the object is successfully created.
     *                             Cannot be null.
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, File, ISingleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, int)
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, File)
     */
    public static void createSingleAccountPublicClientApplication(@NonNull final Context context,
                                                                  final int configFileResourceId,
                                                                  @NonNull final ISingleAccountApplicationCreatedListener listener) {

        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        createSingleAccountPublicClientApplication(
                initializeConfiguration(context, configFileResourceId),
                listener
        );
    }

    /**
     * {@link PublicClientApplication#createSingleAccountPublicClientApplication(Context, int, ISingleAccountApplicationCreatedListener)}
     * will read the client id and other configuration settings from the file included in your
     * application resources.
     *
     * <p><p>This function will pass back an {@link MsalClientException} object if it is unable to
     * return {@link ISingleAccountApplicationCreatedListener}. For example, AccountMode in
     * configuration is not set to single. </p></p>
     *
     * @param context    Application's {@link Context}. The sdk requires the application context
     *                   to be passed in {@link PublicClientApplication}. Cannot be null.
     *                   <p>
     *                   Note: The {@link Context} should be the application context instead of the
     *                   running activity's context, which could potentially make the sdk hold a
     *                   strong reference to the activity, thus preventing correct garbage
     *                   collection and causing bugs.
     *                   </p>
     * @param configFile The file containing the JSON configuration for the PublicClientApplication.
     *                   Cannot be null.
     *                   <p>
     *                   For more information on the schema of the MSAL configuration file, please
     *                   see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                   and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                   </p>
     * @param listener   a callback to be invoked when the object is successfully created. Cannot be null.
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, int, ISingleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, int)
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, File)
     */
    public static void createSingleAccountPublicClientApplication(@NonNull final Context context,
                                                                  @NonNull final File configFile,
                                                                  @NonNull final ISingleAccountApplicationCreatedListener listener) {

        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(configFile, NONNULL_CONSTANTS.CONFIG_FILE);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        createSingleAccountPublicClientApplication(
                initializeConfiguration(context, configFile),
                listener
        );
    }

    /**
     * {@link PublicClientApplication#createSingleAccountPublicClientApplication(Context, int)}
     * will read the client id and other configuration settings from the file included in your
     * application's resources.
     *
     * <p><p>This function will pass back an {@link MsalClientException} object if it is unable
     * to return {@link ISingleAccountApplicationCreatedListener}. For example, AccountMode in
     * configuration is not set to single. </p></p>
     *
     * @param context              Application's {@link Context}. The sdk requires the application
     *                             context to be passed in {@link PublicClientApplication}.
     *                             Cannot be null.
     *                             <p>
     *                             Note: The {@link Context} should be the application context
     *                             instead of the running activity's context, which could
     *                             potentially make the sdk hold a strong reference to the activity,
     *                             thus preventing correct garbage collection and causing bugs.
     *                             </p>
     * @param configFileResourceId The resource ID of the raw file containing the JSON configuration
     *                             for the PublicClientApplication.
     *                             <p>
     *                             For more information on the schema of the MSAL config json,
     *                             please see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                             and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                             </p>
     * @return An instance of ISingleAccountPublicClientApplication.
     * @throws IllegalStateException if this function is invoked on the main thread.
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, File, ISingleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, int, ISingleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, File)
     */
    @WorkerThread
    @NonNull
    public static ISingleAccountPublicClientApplication createSingleAccountPublicClientApplication(
            @NonNull final Context context,
            final int configFileResourceId) throws InterruptedException, MsalException {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);

        return createSingleAccountPublicClientApplication(
                initializeConfiguration(context, configFileResourceId)
        );
    }

    /**
     * {@link PublicClientApplication#createSingleAccountPublicClientApplication(Context, int)}
     * will read the client id and other configuration settings from the file included in your
     * applications resources.
     *
     * <p><p>This function will pass back an {@link MsalClientException} object if it is unable
     * to return {@link ISingleAccountApplicationCreatedListener}. For example, AccountMode in
     * configuration is not set to single. </p></p>
     *
     * @param context    Application's {@link Context}. The sdk requires the application context
     *                   to be passed in {@link PublicClientApplication}. Cannot be null.
     *                   <p>
     *                   Note: The {@link Context} should be the application context instead of
     *                   the running activity's context, which could potentially make the sdk hold a
     *                   strong reference to the activity, thus preventing correct garbage
     *                   collection and causing bugs.
     *                   </p>
     * @param configFile The file containing the JSON configuration for the PublicClientApplication.
     *                   Cannot be null.
     *                   <p>
     *                   For more information on the schema of the MSAL configuration file,
     *                   please see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     *                   and <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     *                   </p>
     * @return An instance of ISingleAccountPublicClientApplication.
     * @throws IllegalStateException if this function is invoked on the main thread.
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, File, ISingleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, int, ISingleAccountApplicationCreatedListener)
     * @see PublicClientApplication#createSingleAccountPublicClientApplication(Context, int)
     */
    @WorkerThread
    @NonNull
    public static ISingleAccountPublicClientApplication createSingleAccountPublicClientApplication(
            @NonNull final Context context,
            @Nullable final File configFile) throws InterruptedException, MsalException {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);

        return createSingleAccountPublicClientApplication(
                initializeConfiguration(context, configFile)
        );
    }
    //endregion

    //region internal factory methods.
    @WorkerThread
    private static IPublicClientApplication create(
            @NonNull final PublicClientApplicationConfiguration configuration)
            throws MsalException, InterruptedException {
        validateNonNullArgument(configuration, "configuration");

        throwOnMainThread("createPublicClientApplication");

        final ResultFuture<AsyncResult<IPublicClientApplication>> future = new ResultFuture<>();
        create(configuration,
                null, // client id
                null, // authority
                new ApplicationCreatedListener() {
                    @Override
                    public void onCreated(final IPublicClientApplication application) {
                        future.setResult(new AsyncResult<>(application, null));
                    }

                    @Override
                    public void onError(final MsalException exception) {
                        future.setResult(new AsyncResult<IPublicClientApplication>(null, exception));
                    }
                }
        );

        //Blocking Call
        AsyncResult<IPublicClientApplication> result = future.get();

        if (!result.getSuccess()) {
            //Exception thrown
            MsalException ex = result.getException();
            throw ex;
        }

        return result.getResult();
    }

    @WorkerThread
    private static IMultipleAccountPublicClientApplication createMultipleAccountPublicClientApplication(
            @NonNull final PublicClientApplicationConfiguration configuration)
            throws InterruptedException, MsalException {
        if (configuration.mAccountMode != AccountMode.MULTIPLE) {
            throw new MsalClientException(
                    MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE,
                    MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE
            );
        }

        final IPublicClientApplication application = create(configuration);

        if (application instanceof IMultipleAccountPublicClientApplication) {
            return (IMultipleAccountPublicClientApplication) application;
        } else {
            if (configuration.mAccountMode == AccountMode.MULTIPLE && application.isSharedDevice()) {
                throw new MsalClientException(
                        MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_CODE,
                        MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_MESSAGE
                );
            }

            throw new MsalClientException(
                    MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE,
                    MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE
            );
        }
    }

    @WorkerThread
    private static ISingleAccountPublicClientApplication createSingleAccountPublicClientApplication(
            @Nullable final PublicClientApplicationConfiguration configuration)
            throws InterruptedException, MsalException {
        final IPublicClientApplication application = create(configuration);

        if (application instanceof ISingleAccountPublicClientApplication) {
            return (ISingleAccountPublicClientApplication) application;
        } else {
            if (configuration.mAccountMode != AccountMode.SINGLE) {
                throw new MsalClientException(
                        SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE,
                        SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE
                );
            }
            throw new MsalClientException(
                    SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE,
                    SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE
            );
        }
    }

    private static void create(@NonNull final PublicClientApplicationConfiguration config,
                               @Nullable final String clientId,
                               @Nullable final String authority,
                               @NonNull final ApplicationCreatedListener listener) {


        final OperationParameters params = OperationParametersAdapter.createOperationParameters(config, config.getOAuth2TokenCache());

        final BaseController controller;
        try {
            controller = MSALControllerFactory.getDefaultController(
                    config.getAppContext(),
                    params.getAuthority(),
                    config);
        } catch (MsalClientException e) {
            listener.onError(e);
            return;
        }

        final GetDeviceModeCommand command = new GetDeviceModeCommand(
                params,
                controller,
                new CommandCallback<Boolean, BaseException>() {
                    @Override
                    public void onError(BaseException error) {
                        listener.onError(MsalExceptionAdapter.msalExceptionFromBaseException(error));
                    }

                    @Override
                    public void onTaskCompleted(Boolean isSharedDevice) {
                        config.setIsSharedDevice(isSharedDevice);

                        if (config.getAccountMode() == AccountMode.SINGLE || isSharedDevice) {
                            listener.onCreated(
                                    new SingleAccountPublicClientApplication(
                                            config,
                                            clientId,
                                            authority
                                    )
                            );
                        } else {
                            listener.onCreated(
                                    new MultipleAccountPublicClientApplication(
                                            config,
                                            clientId,
                                            authority
                                    )
                            );
                        }
                    }

                    @Override
                    public void onCancel() {
                        // Should not be reached.
                    }
                }
        );

        CommandDispatcher.submitSilent(command);
    }

    private static void createMultipleAccountPublicClientApplication(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final IMultipleAccountApplicationCreatedListener listener) {
        create(configuration,
                null,
                null,
                new ApplicationCreatedListener() {
                    @Override
                    public void onCreated(@NonNull final IPublicClientApplication application) {
                        if (application instanceof IMultipleAccountPublicClientApplication) {
                            listener.onCreated((IMultipleAccountPublicClientApplication) application);
                        } else {
                            if (application.getConfiguration().mAccountMode == AccountMode.MULTIPLE
                                    && application.isSharedDevice()) {
                                listener.onError(
                                        new MsalClientException(
                                                MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_CODE,
                                                MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_MESSAGE
                                        )
                                );
                                return;
                            }
                            listener.onError(
                                    new MsalClientException(
                                            MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE,
                                            MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE
                                    )
                            );
                        }
                    }

                    @Override
                    public void onError(final MsalException exception) {
                        listener.onError(exception);
                    }
                }
        );
    }

    private static void createSingleAccountPublicClientApplication(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final ISingleAccountApplicationCreatedListener listener) {
        create(
                configuration,
                null, // client id
                null, // authority
                new ApplicationCreatedListener() {
                    @Override
                    public void onCreated(final IPublicClientApplication application) {
                        if (application instanceof ISingleAccountPublicClientApplication) {
                            listener.onCreated((ISingleAccountPublicClientApplication) application);
                        } else {
                            if (application.getConfiguration().mAccountMode != AccountMode.SINGLE) {
                                listener.onError(
                                        new MsalClientException(
                                                SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE,
                                                SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE
                                        )
                                );
                                return;
                            }
                            listener.onError(
                                    new MsalClientException(
                                            SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE,
                                            SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE
                                    )
                            );
                        }
                    }

                    @Override
                    public void onError(final MsalException exception) {
                        listener.onError(exception);
                    }
                }
        );
    }
    //endregion

    protected PublicClientApplication(@NonNull final PublicClientApplicationConfiguration configFile,
                                      @Nullable final String clientId,
                                      @Nullable final String authority) {

        mPublicClientConfiguration = configFile;

        if (clientId != null) {
            mPublicClientConfiguration.mClientId = clientId;
        }

        if (authority != null) {
            mPublicClientConfiguration.getAuthorities().clear();

            Authority authorityObject = Authority.getAuthorityFromAuthorityUrl(authority);
            authorityObject.setDefault(true);
            mPublicClientConfiguration.getAuthorities().add(authorityObject);
        }

        initializeApplication();
    }

    private void initializeApplication() {
        final String methodName = ":initializeApplication";

        final Context context = mPublicClientConfiguration.getAppContext();
        EstsTelemetry.getInstance().setupLastRequestTelemetryCache(context);
        setupTelemetry(context, mPublicClientConfiguration);

        AzureActiveDirectory.setEnvironment(mPublicClientConfiguration.getEnvironment());
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());

        initializeHttpSettings(mPublicClientConfiguration.getHttpConfiguration());
        initializeLoggerSettings(mPublicClientConfiguration.getLoggerConfiguration());

        initializeTokenSharingLibrary();

        mPublicClientConfiguration.checkIntentFilterAddedToAppManifestForBrokerFlow();

        // Since network request is sent from the sdk, if calling app doesn't declare the internet
        // permission in the manifest, we cannot make the network call.
        checkInternetPermission();

        // Init HTTP cache
        HttpCache.initialize(context.getCacheDir());

        com.microsoft.identity.common.internal.logging.Logger.info(
                TAG + methodName,
                "Create new public client application."
        );
    }

    private void initializeLoggerSettings(@Nullable final LoggerConfiguration loggerConfig) {
        if (null != loggerConfig) {
            final com.microsoft.identity.client.Logger.LogLevel configLogLevel = loggerConfig.getLogLevel();
            final boolean configPiiState = loggerConfig.isPiiEnabled();
            final boolean configLogcatState = loggerConfig.isLogcatEnabled();

            final com.microsoft.identity.client.Logger logger = com.microsoft.identity.client.Logger.getInstance();

            if (null != configLogLevel) {
                logger.setLogLevel(configLogLevel);
            }

            logger.setEnablePII(configPiiState);
            logger.setEnableLogcatLog(configLogcatState);
        }
    }

    private void initializeHttpSettings(@Nullable final HttpConfiguration httpConfiguration) {
        final String methodName = ":initializeHttpSettings";

        if (null == httpConfiguration) {
            Logger.info(
                    TAG + methodName,
                    "HttpConfiguration not provided - using defaults."
            );

            return;
        }

        final int readTimeout = httpConfiguration.getReadTimeout();
        final int connectTimeout = httpConfiguration.getConnectTimeout();

        // Configured values must be >= 0

        if (readTimeout >= 0) {
            HttpRequest.READ_TIMEOUT = readTimeout;
        }

        if (connectTimeout >= 0) {
            HttpRequest.CONNECT_TIMEOUT = connectTimeout;
        }
    }

    private void initializeTokenSharingLibrary() {
        if (mPublicClientConfiguration.getOAuth2TokenCache() instanceof MsalOAuth2TokenCache) {
            mTokenShareUtility = new TokenShareUtility(
                    mPublicClientConfiguration.getClientId(),
                    mPublicClientConfiguration.getRedirectUri(),
                    mPublicClientConfiguration.getDefaultAuthority().getAuthorityURL().toString(),
                    (MsalOAuth2TokenCache) mPublicClientConfiguration.getOAuth2TokenCache()
            );
        } else {
            throw new IllegalStateException("TSL support mandates use of the MsalOAuth2TokenCache");
        }
    }

    private void setupTelemetry(@NonNull final Context context,
                                @NonNull final PublicClientApplicationConfiguration developerConfig) {
        if (null != developerConfig.getTelemetryConfiguration()) {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG,
                    "Telemetry configuration is set. Telemetry is enabled."
            );
        } else {
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG,
                    "Telemetry configuration is null. Telemetry is disabled."
            );
        }

        new com.microsoft.identity.common.internal.telemetry.Telemetry.Builder()
                .withContext(context)
                .defaultConfiguration(developerConfig.getTelemetryConfiguration())
                .build();
    }

    @Override
    public String getOrgIdFamilyRefreshToken(@NonNull final String identifier) throws MsalClientException {
        validateNonNullArgument(identifier, "identifier");
        validateBrokerNotInUse();

        try {
            return mTokenShareUtility.getOrgIdFamilyRefreshToken(identifier);
        } catch (final Exception e) {
            throw new MsalClientException(
                    TOKEN_CACHE_ITEM_NOT_FOUND,
                    TSM_MSG_FAILED_TO_RETRIEVE,
                    e
            );
        }
    }

    @Override
    public void saveOrgIdFamilyRefreshToken(@NonNull final String ssoStateSerializerBlob) throws MsalClientException {
        validateNonNullArgument(ssoStateSerializerBlob, "SsoStateSerializerBlob");
        validateBrokerNotInUse();

        try {
            mTokenShareUtility.saveOrgIdFamilyRefreshToken(ssoStateSerializerBlob);
        } catch (final Exception e) {
            throw new MsalClientException(
                    TOKEN_SHARING_DESERIALIZATION_ERROR,
                    TSL_MSG_FAILED_TO_SAVE,
                    e
            );
        }
    }

    @Override
    public String getMsaFamilyRefreshToken(@NonNull final String identifier) throws MsalClientException {
        validateNonNullArgument(identifier, "identifier");
        validateBrokerNotInUse();

        try {
            return mTokenShareUtility.getMsaFamilyRefreshToken(identifier);
        } catch (final Exception e) {
            throw new MsalClientException(
                    TOKEN_CACHE_ITEM_NOT_FOUND,
                    TSM_MSG_FAILED_TO_RETRIEVE,
                    e
            );
        }
    }

    @Override
    public void saveMsaFamilyRefreshToken(@NonNull final String refreshToken) throws MsalClientException {
        validateNonNullArgument(refreshToken, "refreshToken");
        validateBrokerNotInUse();

        try {
            mTokenShareUtility.saveMsaFamilyRefreshToken(refreshToken);
        } catch (final Exception e) {
            throw new MsalClientException(
                    TOKEN_SHARING_MSA_PERSISTENCE_ERROR,
                    TSL_MSG_FAILED_TO_SAVE,
                    e
            );
        }
    }

    private void validateBrokerNotInUse() throws MsalClientException {
        if (MSALControllerFactory.brokerEligible(
                mPublicClientConfiguration.getAppContext(),
                mPublicClientConfiguration.getDefaultAuthority(),
                mPublicClientConfiguration
        )) {
            throw new MsalClientException(
                    "Cannot perform this action - broker is enabled."
            );
        }
    }

    /**
     * Listener callback for asynchronous loading of MSAL mode retrieval.
     */
    public interface BrokerDeviceModeCallback {
        /**
         * Called once MSAL mode is retrieved from Broker.
         * If Broker is not installed, this will fall back to the BROKER_ACCOUNT_MODE_MULTIPLE_ACCOUNT mode.
         */
        void onGetMode(final boolean isSharedDevice);

        /**
         * Called once MSAL mode can't be retrieved from Broker.
         */
        void onError(final MsalException exception);
    }

    /**
     * @return The current version for the sdk.
     */
    public static String getSdkVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public PublicClientApplicationConfiguration getConfiguration() {
        return mPublicClientConfiguration;
    }

    @Override
    public boolean isSharedDevice() {
        return mPublicClientConfiguration.getIsSharedDevice();
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @NonNull final AuthenticationCallback callback) {
        AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
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

        acquireTokenInternal(acquireTokenParameters, PublicApiId.PCA_ACQUIRE_TOKEN_WITH_ACTIVITY_SCOPES_CALLBACK);
    }

    AcquireTokenParameters buildAcquireTokenParameters(@NonNull final Activity activity,
                                     @NonNull final String[] scopes,
                                     @Nullable final IAccount account,
                                     @Nullable final Prompt uiBehavior,
                                     @Nullable final List<Pair<String, String>> extraQueryParameters,
                                     @Nullable final String[] extraScopesToConsent,
                                     @Nullable final String authority,
                                     @NonNull final AuthenticationCallback callback,
                                     @Nullable final String loginHint,
                                     @Nullable final ClaimsRequest claimsRequest) {

        validateNonNullArgument(activity, NONNULL_CONSTANTS.ACTIVITY);
        validateNonNullArgument(scopes, NONNULL_CONSTANTS.SCOPES);
        validateNonNullArgument(callback, NONNULL_CONSTANTS.CALLBACK);


        AcquireTokenParameters.Builder builder = new AcquireTokenParameters.Builder();
        AcquireTokenParameters acquireTokenParameters = builder.startAuthorizationFromActivity(activity)
                .forAccount(account)
                .withScopes(Arrays.asList(scopes))
                .withPrompt(uiBehavior)
                .withAuthorizationQueryStringParameters(extraQueryParameters)
                .withOtherScopesToAuthorize(
                        Arrays.asList(
                                null == extraScopesToConsent
                                        ? new String[]{}
                                        : extraScopesToConsent
                        )
                )
                .fromAuthority(authority)
                .withCallback(callback)
                .withLoginHint(loginHint)
                .withClaims(claimsRequest)
                .build();


        return acquireTokenParameters;
    }

    protected void validateAcquireTokenParameters(AcquireTokenParameters parameters) throws MsalArgumentException {
        final Activity activity = parameters.getActivity();
        final List scopes = parameters.getScopes();
        final AuthenticationCallback callback = parameters.getCallback();

        validateNonNullArg(activity, NONNULL_CONSTANTS.ACTIVITY);
        validateNonNullArg(scopes, NONNULL_CONSTANTS.SCOPES);
        validateNonNullArg(callback, NONNULL_CONSTANTS.CALLBACK);
    }

    protected void validateAcquireTokenSilentParameters(AcquireTokenSilentParameters parameters) throws MsalArgumentException {
        final String authority = parameters.getAuthority();
        final IAccount account = parameters.getAccount();
        final List scopes = parameters.getScopes();
        final SilentAuthenticationCallback callback = parameters.getCallback();
        validateNonNullArg(authority, NONNULL_CONSTANTS.AUTHORITY);
        validateNonNullArg(account, NONNULL_CONSTANTS.ACCOUNT);
        validateNonNullArg(callback, NONNULL_CONSTANTS.CALLBACK);
        validateNonNullArg(scopes, NONNULL_CONSTANTS.SCOPES);
    }

    public void acquireToken(@NonNull final AcquireTokenParameters acquireTokenParameters) {
        acquireTokenInternal(acquireTokenParameters, PublicApiId.PCA_ACQUIRE_TOKEN_WITH_PARAMETERS);
    }

    void acquireTokenInternal(@NonNull final AcquireTokenParameters acquireTokenParameters, @NonNull final String publicApiId) {
        // In order to support use of named tenants (such as contoso.onmicrosoft.com), we need
        // to be able to query OpenId Provider Configuration Metadata - for this reason, we will
        // build-up the acquireTokenOperationParams on a background thread.
        sBackgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final CommandCallback localAuthenticationCallback =
                        getCommandCallback(
                                acquireTokenParameters.getCallback(),
                                acquireTokenParameters
                        );
                try {
                    validateAcquireTokenParameters(acquireTokenParameters);

                    acquireTokenParameters.setAccountRecord(
                            selectAccountRecordForTokenRequest(
                                    mPublicClientConfiguration,
                                    acquireTokenParameters
                            )
                    );

                    final AcquireTokenOperationParameters params = OperationParametersAdapter.
                            createAcquireTokenOperationParameters(
                                    acquireTokenParameters,
                                    mPublicClientConfiguration,
                                    mPublicClientConfiguration.getOAuth2TokenCache()
                            );


                    final InteractiveTokenCommand command = new InteractiveTokenCommand(
                            params,
                            MSALControllerFactory.getDefaultController(
                                    mPublicClientConfiguration.getAppContext(),
                                    params.getAuthority(),
                                    mPublicClientConfiguration
                            ),
                            localAuthenticationCallback
                    );

                    command.setPublicApiId(publicApiId);
                    CommandDispatcher.beginInteractive(command);
                } catch (final Exception exception) {
                    // convert exception to BaseException
                    final BaseException baseException = ExceptionAdapter.baseExceptionFromException(exception);
                    // If there is an Exception, post it to the main thread...
                    final Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            localAuthenticationCallback.onError(baseException);
                        }
                    });
                }
            }
        });
    }

    protected AcquireTokenSilentParameters buildAcquireTokenSilentParameters(@NonNull final String[] scopes,
                                      @NonNull final IAccount account,
                                      @NonNull final String authority,
                                      final boolean forceRefresh,
                                      @Nullable final ClaimsRequest claimsRequest,
                                      @NonNull final SilentAuthenticationCallback callback) {
        validateNonNullArgument(account, NONNULL_CONSTANTS.ACCOUNT);
        validateNonNullArgument(callback, NONNULL_CONSTANTS.CALLBACK);

        AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder();
        AcquireTokenSilentParameters acquireTokenSilentParameters =
                builder.withScopes(Arrays.asList(scopes))
                        .forAccount(account)
                        .fromAuthority(authority)
                        .forceRefresh(forceRefresh)
                        .withClaims(claimsRequest)
                        .withCallback(callback)
                        .build();

        return acquireTokenSilentParameters;
    }

    @Override
    public void acquireTokenSilentAsync(
            @NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) {
        acquireTokenSilentAsyncInternal(acquireTokenSilentParameters, PublicApiId.PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS);
    }

    void acquireTokenSilentAsyncInternal(
            @NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters,
            @NonNull final String publicApiId) {
        sBackgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final CommandCallback callback = getCommandCallback(
                        acquireTokenSilentParameters.getCallback(),
                        acquireTokenSilentParameters
                );

                try {
                    validateAcquireTokenSilentParameters(acquireTokenSilentParameters);

                    acquireTokenSilentParameters.setAccountRecord(
                            selectAccountRecordForTokenRequest(
                                    mPublicClientConfiguration,
                                    acquireTokenSilentParameters
                            )
                    );

                    final AcquireTokenSilentOperationParameters params =
                            OperationParametersAdapter.createAcquireTokenSilentOperationParameters(
                                    acquireTokenSilentParameters,
                                    mPublicClientConfiguration,
                                    mPublicClientConfiguration.getOAuth2TokenCache()
                            );


                    final TokenCommand silentTokenCommand = new TokenCommand(
                            params,
                            MSALControllerFactory.getAllControllers(
                                    mPublicClientConfiguration.getAppContext(),
                                    params.getAuthority(),
                                    mPublicClientConfiguration
                            ),
                            callback
                    );

                    silentTokenCommand.setPublicApiId(publicApiId);
                    CommandDispatcher.submitSilent(silentTokenCommand);
                } catch (final Exception exception) {
                    // convert exception to BaseException
                    final BaseException baseException = ExceptionAdapter.baseExceptionFromException(exception);

                    // There was an error, shuttle it back to the main thread...
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(baseException);
                        }
                    });
                }
            }
        });
    }


    private AccountRecord selectAccountRecordForTokenRequest(
            @NonNull final PublicClientApplicationConfiguration pcaConfig,
            @NonNull final TokenParameters tokenParameters)
            throws ServiceException, MsalClientException {
        // If not authority was provided in the request, fallback to the default authority...
        if (TextUtils.isEmpty(tokenParameters.getAuthority())) {
            tokenParameters.setAuthority(
                    pcaConfig
                            .getDefaultAuthority()
                            .getAuthorityUri()
                            .toString()
            );
        }

        if (null == tokenParameters.getAccount()) {
            return null; // No account was set!
        }

        // The root account we'll be fetching tokens for...
        final IAccount rootAccount = tokenParameters.getAccount();
        final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) rootAccount;
        final String requestAuthority = tokenParameters.getAuthority();
        final Authority authority = Authority.getAuthorityFromAuthorityUrl(requestAuthority);

        if (authority instanceof AzureActiveDirectoryB2CAuthority) {
            // use home account - b2c is not compatible with broker, so no need to construct
            // the account used in the request...
            return AccountAdapter.getAccountInternal(
                    mPublicClientConfiguration.getClientId(),
                    mPublicClientConfiguration.getOAuth2TokenCache(),
                    multiTenantAccount.getHomeAccountId(),
                    multiTenantAccount.getTenantId()
            );
        } else if (authority instanceof AzureActiveDirectoryAuthority) {
            final AzureActiveDirectoryAuthority aadAuthority = (AzureActiveDirectoryAuthority) authority;

            // Although the below call implies the returned value will be a tenantId, this isn't
            // strictly true: it can also be an alias such as 'common', 'consumers', or
            // 'organizations'. Additionally, if the developer has used a named tenant it could end
            // up being something like <tenant_name>.onmicrosoft.com
            //
            // If the tenant is a GUID, we need to choose the account/profile that corresponds to it
            //
            // If the tenant is named 'common', 'consumers', or 'organizations' we need to use the
            // home account
            //
            // If the account is named like <tenant_name>.onmicrosoft.com we need to query the OpenId
            // Provider Configuration Metadata in order to get the tenant id. Once we have the
            // tenant id, we must then select the appropriate home or profile.
            String tenantIdNameOrAlias = aadAuthority.getAudience().getTenantId();

            // The AccountRecord we'll use to request a token...
            final AccountRecord accountRecord = new AccountRecord();
            accountRecord.setEnvironment(multiTenantAccount.getEnvironment());
            accountRecord.setHomeAccountId(multiTenantAccount.getHomeAccountId());

            final boolean isUuid = isUuid(tenantIdNameOrAlias);

            if (!isUuid && !isHomeTenantEquivalent(tenantIdNameOrAlias)) {
                final OpenIdProviderConfiguration providerConfiguration =
                        loadOpenIdProviderConfigurationMetadata(requestAuthority);

                final String issuer = providerConfiguration.getIssuer();
                final Uri issuerUri = Uri.parse(issuer);
                final List<String> paths = issuerUri.getPathSegments();

                if (paths.isEmpty()) {
                    final String errMsg = "OpenId Metadata did not contain a path to the tenant";

                    com.microsoft.identity.common.internal.logging.Logger.error(
                            TAG,
                            errMsg,
                            null
                    );

                    throw new MsalClientException(errMsg);
                }

                tenantIdNameOrAlias = paths.get(0);
            }

            final IAccount accountForRequest;

            if (isHomeTenantEquivalent(tenantIdNameOrAlias)
                    || isAccountHomeTenant(multiTenantAccount.getClaims(), tenantIdNameOrAlias)) {
                accountForRequest = multiTenantAccount;
            } else {
                accountForRequest = multiTenantAccount.getTenantProfiles().get(tenantIdNameOrAlias);
            }

            if (null == accountForRequest) { // We did not find a profile to use
                final boolean isSilent = tokenParameters instanceof AcquireTokenSilentParameters;

                if (isSilent) {
                    validateClaimsExistForTenant(tenantIdNameOrAlias, null);
                } else {
                    // We didn't find an Account but the request is interactive so we'll
                    // return null and let the user sort it out.
                    return null;
                }
            }

            accountRecord.setLocalAccountId(accountForRequest.getId());
            accountRecord.setUsername(accountForRequest.getUsername());

            return accountRecord;
        } else {
            // Unrecognized authority type
            throw new UnsupportedOperationException(
                    "Unsupported Authority type: "
                            + authority
                            .getClass()
                            .getSimpleName()
            );
        }
    }

    private OpenIdProviderConfiguration loadOpenIdProviderConfigurationMetadata(
            @NonNull final String requestAuthority) throws ServiceException {
        final String methodName = ":loadOpenIdProviderConfigurationMetadata";

        com.microsoft.identity.common.internal.logging.Logger.info(
                TAG + methodName,
                "Loading OpenId Provider Metadata..."
        );

        final OpenIdProviderConfigurationClient client =
                new OpenIdProviderConfigurationClient(requestAuthority);

        return client.loadOpenIdProviderConfiguration();
    }

    private static boolean isUuid(@NonNull final String tenantIdNameOrAlias) {
        try {
            UUID.fromString(tenantIdNameOrAlias);
            return true;
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public IAuthenticationResult acquireTokenSilent(
            @NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters)
            throws InterruptedException, MsalException {
        return acquireTokenSilentInternal(acquireTokenSilentParameters, PublicApiId.PCA_ACQUIRE_TOKEN_SILENT_WITH_PARAMETERS);
    }

    IAuthenticationResult acquireTokenSilentInternal(
            @NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters,
            @NonNull final String publicApiId)
            throws InterruptedException, MsalException {

        if (acquireTokenSilentParameters.getCallback() != null) {
            throw new IllegalArgumentException("Do not provide callback for synchronous methods");
        }

        final ResultFuture<AsyncResult<IAuthenticationResult>> future = new ResultFuture<>();

        acquireTokenSilentParameters.setCallback(new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                future.setResult(new AsyncResult<>(authenticationResult, null));
            }

            @Override
            public void onError(MsalException exception) {
                future.setResult(new AsyncResult<IAuthenticationResult>(null, exception));
            }
        });

        acquireTokenSilentAsyncInternal(acquireTokenSilentParameters, publicApiId);

        AsyncResult<IAuthenticationResult> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
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

    static CommandCallback<List<ICacheRecord>, BaseException> getLoadAccountsCallback(
            final LoadAccountsCallback loadAccountsCallback) {
        return new CommandCallback<List<ICacheRecord>, BaseException>() {
            @Override
            public void onTaskCompleted(final List<ICacheRecord> result) {
                if (null == result) {
                    loadAccountsCallback.onTaskCompleted(null);
                } else {
                    loadAccountsCallback.onTaskCompleted(
                            AccountAdapter.adapt(result)
                    );
                }
            }

            @Override
            public void onError(final BaseException exception) {
                loadAccountsCallback.onError(msalExceptionFromBaseException(exception));
            }

            @Override
            public void onCancel() {
                //Do nothing
            }

        };
    }


    protected CommandCallback getCommandCallback(
            @NonNull final SilentAuthenticationCallback authenticationCallback,
            @NonNull final TokenParameters tokenParameters) {

        return new CommandCallback<ILocalAuthenticationResult, BaseException>() {

            @Override
            public void onTaskCompleted(ILocalAuthenticationResult localAuthenticationResult) {

                postAuthResult(localAuthenticationResult, tokenParameters, authenticationCallback);
            }

            @Override
            public void onError(BaseException exception) {
                MsalException msalException = msalExceptionFromBaseException(exception);
                if (authenticationCallback == null) {
                    throw new IllegalStateException(NONNULL_CONSTANTS.CALLBACK + NONNULL_CONSTANTS.NULL_ERROR_SUFFIX);
                } else {
                    authenticationCallback.onError(msalException);
                }
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

    /**
     * Helper method to post authentication result.
     */
    protected void postAuthResult(@NonNull final ILocalAuthenticationResult localAuthenticationResult,
                                  @NonNull final TokenParameters requestParameters,
                                  @NonNull final SilentAuthenticationCallback authenticationCallback) {

        if (authenticationCallback == null) {
            throw new IllegalStateException(NONNULL_CONSTANTS.CALLBACK + NONNULL_CONSTANTS.NULL_ERROR_SUFFIX);
        }

        // Check if any of the requested scopes are declined by the server, if yes throw a MsalDeclinedScope exception
        final List<String> declinedScopes = AuthenticationResultAdapter.getDeclinedScopes(
                Arrays.asList(localAuthenticationResult.getScope()),
                requestParameters.getScopes()
        );

        if (!declinedScopes.isEmpty()) {
            final MsalDeclinedScopeException declinedScopeException =
                    AuthenticationResultAdapter.declinedScopeExceptionFromResult(
                            localAuthenticationResult,
                            declinedScopes,
                            requestParameters
                    );
            authenticationCallback.onError(declinedScopeException);
        } else {
            IAuthenticationResult authenticationResult = AuthenticationResultAdapter.adapt(localAuthenticationResult);
            authenticationCallback.onSuccess(authenticationResult);
        }
    }

    private OAuth2TokenCache<?, ?, ?> getOAuth2TokenCache() {
        return MsalOAuth2TokenCache.create(mPublicClientConfiguration.getAppContext());
    }

    protected class AccountMatcher {

        private final AccountMatcher[] mDelegateMatchers;

        AccountMatcher() {
            // Intentionally blank...
            mDelegateMatchers = new AccountMatcher[]{};
        }

        AccountMatcher(@NonNull final AccountMatcher... delegateMatchers) {
            mDelegateMatchers = delegateMatchers;
        }

        boolean matches(@NonNull final String identifier,
                        @NonNull final IAccount account) {
            boolean matches = false;

            for (final AccountMatcher matcher : mDelegateMatchers) {
                matches = matcher.matches(identifier, account);

                if (matches) {
                    break;
                }
            }

            return matches;
        }
    }

    protected AccountMatcher homeAccountMatcher = new AccountMatcher() {
        @Override
        boolean matches(@NonNull final String homeAccountId,
                        @NonNull final IAccount account) {
            return homeAccountId.contains(account.getId());
        }
    };

    protected AccountMatcher localAccountMatcher = new AccountMatcher() {
        @Override
        boolean matches(@NonNull final String localAccountId,
                        @NonNull final IAccount account) {
            // First, inspect the root account...
            if (localAccountId.contains(account.getId())) {
                return true;
            } else if (account instanceof MultiTenantAccount) {
                // We need to look at the profiles...
                final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) account;
                final Map<String, ITenantProfile> tenantProfiles = multiTenantAccount.getTenantProfiles();

                if (null != tenantProfiles && !tenantProfiles.isEmpty()) {
                    for (final Map.Entry<String, ITenantProfile> profileEntry : tenantProfiles.entrySet()) {
                        if (localAccountId.contains(profileEntry.getValue().getId())) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    };

    protected AccountMatcher usernameMatcher = new AccountMatcher() {
        @Override
        boolean matches(@NonNull final String username,
                        @NonNull final IAccount account) {
            // Put all of the IdToken we can inspect in a List...
            final List<IClaimable> thingsWithClaims
                    = new ArrayList<>();

            if (null != account.getClaims()) {
                thingsWithClaims.add(account);
            }

            if (account instanceof MultiTenantAccount) {
                final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) account;
                final Map<String, ITenantProfile> profiles = multiTenantAccount.getTenantProfiles();

                for (final Map.Entry<String, ITenantProfile> profileEntry : profiles.entrySet()) {
                    if (null != profileEntry.getValue().getClaims()) {
                        thingsWithClaims.add(profileEntry.getValue());
                    }
                }
            }

            for (final IClaimable thingWithClaims : thingsWithClaims) {
                if (null != thingWithClaims.getClaims()
                        && username.equalsIgnoreCase(
                        SchemaUtil.getDisplayableId(
                                thingWithClaims.getClaims()
                        )
                )) {
                    return true;
                }
            }

            return false;
        }
    };

    IAuthenticationResult acquireTokenSilentSyncInternal(@NonNull final String[] scopes,
                                                         @NonNull final String authority,
                                                         @NonNull final IAccount account,
                                                         final boolean forceRefresh,
                                                         @NonNull final String publicApiId) throws MsalException, InterruptedException {

        throwOnMainThread("acquireTokenSilent");

        final ResultFuture<AsyncResult<IAuthenticationResult>> future = new ResultFuture<>();

        final AcquireTokenSilentParameters acquireTokenSilentParameters = buildAcquireTokenSilentParameters(
                scopes,
                account,
                authority, // authority
                forceRefresh, // forceRefresh
                null, // claimsRequest
                new SilentAuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        future.setResult(new AsyncResult<>(authenticationResult, null));
                    }

                    @Override
                    public void onError(MsalException exception) {
                        future.setResult(new AsyncResult<IAuthenticationResult>(null, exception));
                    }
                }
        );

        acquireTokenSilentAsyncInternal(acquireTokenSilentParameters, publicApiId);

        final AsyncResult<IAuthenticationResult> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
        }
    }
}
