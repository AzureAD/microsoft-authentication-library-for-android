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
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.configuration.AccountMode;
import com.microsoft.identity.client.configuration.LoggerConfiguration;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalDeclinedScopeException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.client.helper.BrokerHelperActivity;
import com.microsoft.identity.client.internal.AsyncResult;
import com.microsoft.identity.client.internal.CommandParametersAdapter;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.common.AndroidPlatformComponents;
import com.microsoft.identity.common.adal.internal.tokensharing.ITokenShareResultInternal;
import com.microsoft.identity.common.adal.internal.tokensharing.TokenShareUtility;
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.cache.IShareSingleSignOnState;
import com.microsoft.identity.common.internal.cache.IMultiTypeNameValueStorage;
import com.microsoft.identity.common.java.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.java.util.SchemaUtil;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.commands.CommandCallback;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommand;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommandCallback;
import com.microsoft.identity.common.internal.commands.GenerateShrCommand;
import com.microsoft.identity.common.internal.commands.GetDeviceModeCommand;
import com.microsoft.identity.common.internal.commands.InteractiveTokenCommand;
import com.microsoft.identity.common.internal.commands.SilentTokenCommand;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.controllers.ExceptionAdapter;
import com.microsoft.identity.common.internal.controllers.LocalMSALController;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.internal.eststelemetry.PublicApiId;
import com.microsoft.identity.common.internal.migration.AdalMigrationAdapter;
import com.microsoft.identity.common.internal.migration.TokenMigrationCallback;
import com.microsoft.identity.common.internal.migration.TokenMigrationUtility;
import com.microsoft.identity.common.internal.net.cache.HttpCache;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.result.GenerateShrResult;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.logging.Logger;
import com.microsoft.identity.msal.BuildConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.microsoft.identity.client.PublicClientApplicationConfigurationFactory.initializeConfiguration;
import static com.microsoft.identity.client.exception.MsalClientException.SAPCA_USE_WITH_MULTI_POLICY_B2C;
import static com.microsoft.identity.client.exception.MsalClientException.UNKNOWN_ERROR;
import static com.microsoft.identity.client.internal.CommandParametersAdapter.createGenerateShrCommandParameters;
import static com.microsoft.identity.client.internal.MsalUtils.throwOnMainThread;
import static com.microsoft.identity.client.internal.MsalUtils.validateNonNullArg;
import static com.microsoft.identity.client.internal.MsalUtils.validateNonNullArgument;
import static com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter.msalExceptionFromBaseException;
import static com.microsoft.identity.common.java.exception.ClientException.TOKEN_CACHE_ITEM_NOT_FOUND;
import static com.microsoft.identity.common.java.exception.ClientException.TOKEN_SHARING_DESERIALIZATION_ERROR;
import static com.microsoft.identity.common.java.exception.ClientException.TOKEN_SHARING_MSA_PERSISTENCE_ERROR;
import static com.microsoft.identity.common.java.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_CODE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ON_SHARED_DEVICE_ERROR_MESSAGE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.MULTIPLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.SINGLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_CODE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.SINGLE_ACCOUNT_PCA_INIT_FAIL_UNKNOWN_REASON_ERROR_MESSAGE;
import static com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience.isHomeTenantAlias;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.PCA_GENERATE_SIGNED_HTTP_REQUEST;
import static com.microsoft.identity.common.internal.eststelemetry.PublicApiId.PCA_GENERATE_SIGNED_HTTP_REQUEST_ASYNC;
import static com.microsoft.identity.common.java.providers.microsoft.MicrosoftIdToken.TENANT_ID;
import static com.microsoft.identity.common.java.util.StringUtil.isUuid;

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
        static final String REDIRECT_URI = "redirect_uri";
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
     * @see PublicClientApplication#create(Context, String, String, String, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, int)
     */
    public static void create(@NonNull final Context context,
                              final int configFileResourceId,
                              @NonNull final ApplicationCreatedListener listener) {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        runOnBackground(new Runnable() {
            @Override
            public void run() {
                create(
                        initializeConfiguration(context, configFileResourceId),
                        null, // client id
                        null, // authority
                        null, // redirect uri
                        listener
                );
            }
        });
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
     * @see PublicClientApplication#create(Context, String, String, String, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, int)
     */
    public static void create(@NonNull final Context context,
                              @NonNull final File configFile,
                              @NonNull final ApplicationCreatedListener listener) {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);
        runOnBackground(new Runnable() {
            @Override
            public void run() {
                create(
                        initializeConfiguration(context, configFile),
                        null, // client id
                        null, // authority
                        null, // redirect uri
                        listener
                );
            }
        });
    }

    /**
     * {@link PublicClientApplication#create(Context, String, String, ApplicationCreatedListener)}
     * allows the client id and authority to be passed instead of providing them through metadata.
     *
     * @param context     Application's {@link Context}. The sdk requires the application context to
     *                    be passed in
     *                    {@link PublicClientApplication}. Cannot be null.
     *                    <p>
     *                    Note: The {@link Context} should be the application context instead of
     *                    an running activity's context, which could potentially make the sdk hold a
     *                    strong reference to the activity, thus preventing correct garbage
     *                    collection and causing bugs.
     *                    </p>
     * @param clientId    The application client id. Cannot be null.
     * @param authority   The default authority to be used for the authority. If this is null, the default authority will be used.
     * @param redirectUri The redirect URI of the application.
     * @param listener    a callback to be invoked when the object is successfully created.
     *                    Cannot be null.
     * @see PublicClientApplication#create(Context, int, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, File, ApplicationCreatedListener)
     * @see PublicClientApplication#create(Context, int)
     */
    public static void create(@NonNull final Context context,
                              @NonNull final String clientId,
                              @Nullable final String authority,
                              @NonNull final String redirectUri,
                              @NonNull final ApplicationCreatedListener listener) {
        validateNonNullArgument(context, NONNULL_CONSTANTS.CONTEXT);
        validateNonNullArgument(clientId, NONNULL_CONSTANTS.CLIENT_ID);
        validateNonNullArgument(redirectUri, NONNULL_CONSTANTS.REDIRECT_URI);
        validateNonNullArgument(listener, NONNULL_CONSTANTS.LISTENER);

        runOnBackground(new Runnable() {
            @Override
            public void run() {
                create(
                        initializeConfiguration(context),
                        clientId,
                        authority,
                        redirectUri,
                        listener
                );
            }
        });
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
     * @see PublicClientApplication#create(Context, String, String, String, ApplicationCreatedListener)
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

        runOnBackground(new Runnable() {
            @Override
            public void run() {
                createMultipleAccountPublicClientApplication(
                        initializeConfiguration(context, configFileResourceId),
                        listener
                );
            }
        });
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

        runOnBackground(new Runnable() {
            @Override
            public void run() {
                createMultipleAccountPublicClientApplication(
                        initializeConfiguration(context, configFile),
                        listener
                );
            }
        });
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

        runOnBackground(new Runnable() {
            @Override
            public void run() {
                createSingleAccountPublicClientApplication(
                        initializeConfiguration(context, configFileResourceId),
                        listener
                );
            }
        });
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

        runOnBackground(new Runnable() {
            @Override
            public void run() {
                createSingleAccountPublicClientApplication(
                        initializeConfiguration(context, configFile),
                        listener
                );
            }
        });
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
                null, // redirectUri
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
        try {
            AsyncResult<IPublicClientApplication> result = future.get();

            if (!result.getSuccess()) {
                //Exception thrown
                MsalException ex = result.getException();
                throw ex;
            }

            return result.getResult();
        } catch (final ExecutionException e) {
            // Shouldn't be thrown.
            throw new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unexpected error while initializing PCA.",
                    e
            );
        }
    }

    @WorkerThread
    private static IMultipleAccountPublicClientApplication createMultipleAccountPublicClientApplication(
            @NonNull final PublicClientApplicationConfiguration configuration)
            throws InterruptedException, MsalException {
        if (configuration.getAccountMode() != AccountMode.MULTIPLE) {
            throw new MsalClientException(
                    MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_CODE,
                    MULTIPLE_ACCOUNT_PCA_INIT_FAIL_ACCOUNT_MODE_ERROR_MESSAGE
            );
        }

        final IPublicClientApplication application = create(configuration);

        if (application instanceof IMultipleAccountPublicClientApplication) {
            return (IMultipleAccountPublicClientApplication) application;
        } else {
            if (configuration.getAccountMode() == AccountMode.MULTIPLE && application.isSharedDevice()) {
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
            if (configuration.getAccountMode() != AccountMode.SINGLE) {
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
                               @Nullable final String redirectUri,
                               @NonNull final ApplicationCreatedListener listener) {
        if (clientId != null) {
            config.setClientId(clientId);
        }

        if (authority != null) {
            config.getAuthorities().clear();

            final Authority authorityObject = Authority.getAuthorityFromAuthorityUrl(authority);
            authorityObject.setDefault(true);
            config.getAuthorities().add(authorityObject);
        }

        if (redirectUri != null) {
            config.setRedirectUri(redirectUri);
        }

        try {
            validateAccountModeConfiguration(config);
        } catch (final MsalClientException e) {
            listener.onError(e);
            return;
        }

        final CommandParameters params = CommandParametersAdapter.createCommandParameters(config, config.getOAuth2TokenCache());

        final BaseController controller;
        try {
            controller = MSALControllerFactory.getDefaultController(
                    config.getAppContext(),
                    config.getDefaultAuthority(),
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

                        try {
                            if (config.getAccountMode() == AccountMode.SINGLE || isSharedDevice) {
                                listener.onCreated(new SingleAccountPublicClientApplication(config));
                            } else {
                                listener.onCreated(new MultipleAccountPublicClientApplication(config));
                            }
                        } catch (final MsalClientException e) {
                            listener.onError(e);
                        }
                    }

                    @Override
                    public void onCancel() {
                        // Should not be reached.
                    }
                },
                PublicApiId.PCA_GET_DEVICE_MODE
        );

        CommandDispatcher.submitSilent(command);
    }

    private static void validateAccountModeConfiguration(@NonNull final PublicClientApplicationConfiguration config) throws MsalClientException {
        if (config.getAccountMode() == AccountMode.SINGLE
                && null != config.getDefaultAuthority()
                && config.getDefaultAuthority() instanceof AzureActiveDirectoryB2CAuthority) {
            Logger.warn(
                    TAG,
                    "Warning! B2C applications should use MultipleAccountPublicClientApplication. "
                            + "Use of SingleAccount mode with multiple IEF policies is unsupported."
            );

            if (config.getAuthorities().size() > 1) {
                throw new MsalClientException(SAPCA_USE_WITH_MULTI_POLICY_B2C);
            }
        }
    }

    private static void createMultipleAccountPublicClientApplication(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final IMultipleAccountApplicationCreatedListener listener) {
        create(configuration,
                null, // client id
                null, // authority
                null, // redirect uri
                new ApplicationCreatedListener() {
                    @Override
                    public void onCreated(@NonNull final IPublicClientApplication application) {
                        if (application instanceof IMultipleAccountPublicClientApplication) {
                            listener.onCreated((IMultipleAccountPublicClientApplication) application);
                        } else {
                            if (application.getConfiguration().getAccountMode() == AccountMode.MULTIPLE
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
                null, // redirect uri
                new ApplicationCreatedListener() {
                    @Override
                    public void onCreated(final IPublicClientApplication application) {
                        if (application instanceof ISingleAccountPublicClientApplication) {
                            listener.onCreated((ISingleAccountPublicClientApplication) application);
                        } else {
                            if (application.getConfiguration().getAccountMode() != AccountMode.SINGLE) {
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

    protected PublicClientApplication(@NonNull final PublicClientApplicationConfiguration configFile) throws MsalClientException {
        mPublicClientConfiguration = configFile;
        initializeApplication();
    }

    private void initializeApplication() throws MsalClientException {
        final String methodName = ":initializeApplication";

        final Context context = mPublicClientConfiguration.getAppContext();
        setupTelemetry(context, mPublicClientConfiguration);

        AzureActiveDirectory.setEnvironment(mPublicClientConfiguration.getEnvironment());
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());

        initializeLoggerSettings(mPublicClientConfiguration.getLoggerConfiguration());

        initializeTokenSharingLibrary();

        mPublicClientConfiguration.checkIntentFilterAddedToAppManifestForBrokerFlow();

        // Since network request is sent from the sdk, if calling app doesn't declare the internet
        // permission in the manifest, we cannot make the network call.
        checkInternetPermission();

        // Init HTTP cache
        HttpCache.initialize(context.getCacheDir());

        Logger.info(TAG + methodName, "Create new public client application.");
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

    private void initializeTokenSharingLibrary() {
        if (mPublicClientConfiguration.getOAuth2TokenCache() instanceof MsalOAuth2TokenCache) {
            mTokenShareUtility = new TokenShareUtility(
                    mPublicClientConfiguration.getClientId(),
                    mPublicClientConfiguration.getRedirectUri(),
                    (MsalOAuth2TokenCache) mPublicClientConfiguration.getOAuth2TokenCache()
            );
        } else {
            throw new IllegalStateException("TSL support mandates use of the MsalOAuth2TokenCache");
        }
    }

    private void setupTelemetry(@NonNull final Context context,
                                @NonNull final PublicClientApplicationConfiguration developerConfig) {
        if (null != developerConfig.getTelemetryConfiguration()) {
            Logger.verbose(TAG, "Telemetry configuration is set. Telemetry is enabled.");
        } else {
            Logger.verbose(TAG, "Telemetry configuration is null. Telemetry is disabled.");
        }

        new com.microsoft.identity.common.internal.telemetry.Telemetry.Builder()
                .withContext(context)
                .defaultConfiguration(developerConfig.getTelemetryConfiguration())
                .build();
    }

    @Override
    public TokenShareResult getOrgIdFamilyRefreshTokenWithMetadata(@NonNull final String identifier) throws MsalClientException {
        validateNonNullArgument(identifier, "identifier");
        validateBrokerNotInUse();

        try {
            final ITokenShareResultInternal resultInternal = mTokenShareUtility.getOrgIdFamilyRefreshTokenWithMetadata(identifier);
            return new TokenShareResult(resultInternal);
        } catch (final Exception e) {
            throw new MsalClientException(
                    TOKEN_CACHE_ITEM_NOT_FOUND,
                    TSM_MSG_FAILED_TO_RETRIEVE,
                    e
            );
        }
    }

    @Override
    public String getOrgIdFamilyRefreshToken(@NonNull final String identifier) throws MsalClientException {
        return getOrgIdFamilyRefreshTokenWithMetadata(identifier).getRefreshToken();
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
    public TokenShareResult getMsaFamilyRefreshTokenWithMetadata(@NonNull final String identifier) throws MsalClientException {
        validateNonNullArgument(identifier, "identifier");
        validateBrokerNotInUse();

        try {
            final ITokenShareResultInternal resultInternal = mTokenShareUtility.getMsaFamilyRefreshTokenWithMetadata(identifier);
            return new TokenShareResult(resultInternal);
        } catch (final Exception e) {
            throw new MsalClientException(
                    TOKEN_CACHE_ITEM_NOT_FOUND,
                    TSM_MSG_FAILED_TO_RETRIEVE,
                    e
            );
        }
    }

    @Override
    public String getMsaFamilyRefreshToken(@NonNull final String identifier) throws MsalClientException {
        return getMsaFamilyRefreshTokenWithMetadata(identifier).getRefreshToken();
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

    /**
     * Presents an activity that includes the package name, signature, redirect URI and manifest entry required for your application
     *
     * @param activity
     */
    public static void showExpectedMsalRedirectUriInfo(Activity activity) {
        activity.startActivity(BrokerHelperActivity.createStartIntent(activity.getApplicationContext()));
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
    public String generateSignedHttpRequest(@NonNull final IAccount account,
                                            @NonNull final PoPAuthenticationScheme popParameters) throws MsalException {
        final ResultFuture<AsyncResult<GenerateShrResult>> future = new ResultFuture<>();

        final GenerateShrCommand generateShrCommand = createGenerateShrCommand(
                account,
                popParameters,
                new CommandCallback<GenerateShrResult, BaseException>() {
                    @Override
                    public void onCancel() {
                        // Not cancellable
                    }

                    @Override
                    public void onError(@NonNull final BaseException error) {
                        future.setResult(
                                new AsyncResult<GenerateShrResult>(
                                        null,
                                        baseExceptionToMsalException(error)
                                )
                        );
                    }

                    @Override
                    public void onTaskCompleted(@NonNull final GenerateShrResult generateShrResult) {
                        future.setResult(new AsyncResult<>(generateShrResult, null));
                    }
                },
                PCA_GENERATE_SIGNED_HTTP_REQUEST
        );

        // Execute this command silently...
        CommandDispatcher.submitSilent(generateShrCommand);

        try {
            final AsyncResult<GenerateShrResult> asyncResult = future.get();

            if (asyncResult.getSuccess()) {
                return asyncResult.getResult().getShr();
            } else {
                throw asyncResult.getException();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unexpected error while generating SHR.",
                    e
            );
        }
    }

    @Override
    public void generateSignedHttpRequest(@NonNull final IAccount account,
                                          @NonNull final PoPAuthenticationScheme popParameters,
                                          @NonNull final SignedHttpRequestRequestCallback callback) {
        try {
            final GenerateShrCommand generateShrCommand = createGenerateShrCommand(
                    account,
                    popParameters,
                    new CommandCallback<GenerateShrResult, BaseException>() {
                        @Override
                        public void onCancel() {
                            // Not cancellable
                        }

                        @Override
                        public void onError(@NonNull final BaseException error) {
                            callback.onError(baseExceptionToMsalException(error));
                        }

                        @Override
                        public void onTaskCompleted(@NonNull final GenerateShrResult generateShrResult) {
                            callback.onTaskCompleted(generateShrResult.getShr());
                        }
                    },
                    PCA_GENERATE_SIGNED_HTTP_REQUEST_ASYNC
            );

            // Execute this command silently...
            CommandDispatcher.submitSilent(generateShrCommand);
        } catch (final MsalClientException e) {
            final MsalClientException clientException = new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unexpected error while generating SHR.",
                    e
            );
            callback.onError(clientException);
        }
    }

    private GenerateShrCommand createGenerateShrCommand(@NonNull final IAccount account,
                                                        @NonNull final PoPAuthenticationScheme popParams,
                                                        @NonNull final CommandCallback<GenerateShrResult, BaseException> cmdCallback,
                                                        @NonNull final String publicApiId) throws MsalClientException {
        final GenerateShrCommandParameters cmdParams = createGenerateShrCommandParameters(
                mPublicClientConfiguration,
                mPublicClientConfiguration.getOAuth2TokenCache(),
                ((Account) account).getHomeAccountId(),
                popParams
        );

        return new GenerateShrCommand(
                cmdParams,
                MSALControllerFactory.getAllControllers(
                        mPublicClientConfiguration.getAppContext(),
                        mPublicClientConfiguration.getDefaultAuthority(),
                        mPublicClientConfiguration
                ),
                cmdCallback,
                publicApiId
        );
    }

    private MsalException baseExceptionToMsalException(@NonNull final BaseException exception) {
        if (GenerateShrResult.Errors.NO_ACCOUNT_FOUND.equalsIgnoreCase(exception.getErrorCode())) {
            return new MsalUiRequiredException(
                    GenerateShrResult.Errors.NO_ACCOUNT_FOUND,
                    "The supplied account could not be located."
            );
        }

        return new MsalClientException(exception.getErrorCode(), exception.getMessage());
    }

    @Override
    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final String[] scopes,
                             @NonNull final AuthenticationCallback callback) {
        AcquireTokenParameters acquireTokenParameters = buildAcquireTokenParameters(
                activity,
                null,
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

    AcquireTokenParameters buildAcquireTokenParameters(
            @NonNull final Activity activity,
            @Nullable final Fragment fragment,
            @NonNull final String[] scopes,
            @Nullable final IAccount account,
            @Nullable final Prompt uiBehavior,
            @Nullable final List<Map.Entry<String, String>> extraQueryParameters,
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
                .withFragment(fragment)
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

                    final InteractiveTokenCommandParameters params = CommandParametersAdapter.
                            createInteractiveTokenCommandParameters(
                                    mPublicClientConfiguration,
                                    mPublicClientConfiguration.getOAuth2TokenCache(),
                                    acquireTokenParameters
                            );

                    final InteractiveTokenCommand command = new InteractiveTokenCommand(
                            params,
                            MSALControllerFactory.getDefaultController(
                                    mPublicClientConfiguration.getAppContext(),
                                    params.getAuthority(),
                                    mPublicClientConfiguration
                            ),
                            localAuthenticationCallback,
                            publicApiId
                    );

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

                    final SilentTokenCommandParameters params =
                            CommandParametersAdapter.createSilentTokenCommandParameters(
                                    mPublicClientConfiguration,
                                    mPublicClientConfiguration.getOAuth2TokenCache(),
                                    acquireTokenSilentParameters
                            );


                    final SilentTokenCommand silentTokenCommand = new SilentTokenCommand(
                            params,
                            MSALControllerFactory.getAllControllers(
                                    mPublicClientConfiguration.getAppContext(),
                                    params.getAuthority(),
                                    mPublicClientConfiguration
                            ),
                            callback,
                            publicApiId
                    );

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
            throws ServiceException, ClientException {
        // If not authority was provided in the request, fallback to the default authority...
        if (TextUtils.isEmpty(tokenParameters.getAuthority())) {
            tokenParameters.setAuthority(
                    pcaConfig
                            .getDefaultAuthority()
                            .getAuthorityURL()
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
            String tenantId = aadAuthority.getAudience().getTenantId();

            // The AccountRecord we'll use to request a token...
            final AccountRecord accountRecord = new AccountRecord();
            accountRecord.setEnvironment(multiTenantAccount.getEnvironment());
            accountRecord.setHomeAccountId(multiTenantAccount.getHomeAccountId());

            final boolean isUuid = isUuid(tenantId);

            if (!isUuid && !isHomeTenantAlias(tenantId)) {
                tenantId = ((AzureActiveDirectoryAuthority) authority)
                        .getAudience()
                        .getTenantUuidForAlias(
                                authority.getAuthorityURL().toString()
                        );
            }
            // Set the tenant id obtained for the accountRecord
            accountRecord.setRealm(tenantId);

            IAccount accountForRequest;

            if (isHomeTenantAlias(tenantId)
                    || isAccountHomeTenant(multiTenantAccount.getClaims(), tenantId)) {
                accountForRequest = (multiTenantAccount.getClaims() != null) ? multiTenantAccount : null;
            } else {
                accountForRequest = multiTenantAccount.getTenantProfiles().get(tenantId);
            }

            // If we are not able to get the AccountRecord for the requested tenanted authority,
            // it means the user is attempting to get a token for a new tenant for the existing IAccount.
            // For silent request, use home account if available or any of the tenant profiles and pass it
            // along as accountForRequest. Controllers will use this account to get refresh token and acquire
            // a new access token for the requested tenant.
            if (null == accountForRequest) { // We did not find a profile to use
                final boolean isSilent = tokenParameters instanceof AcquireTokenSilentParameters;
                if (isSilent) {
                    if (rootAccount.getClaims() != null) {
                        accountForRequest = rootAccount;
                    } else {
                        for (ITenantProfile tenantProfile : multiTenantAccount.getTenantProfiles().values()) {
                            if (tenantProfile.getClaims() != null) {
                                accountForRequest = tenantProfile;
                                break;
                            }
                        }
                    }
                }
            }
            // We should never hit this flow as IAccount should always have a home profile or at least one tenant profile on it.
            if (accountForRequest == null) {
                Logger.warnPII(TAG,
                        "No account record found for IAccount with request tenantId: " + tenantId
                );
                throw new ClientException(
                        ErrorStrings.NO_ACCOUNT_FOUND,
                        "No account record found for IAccount"
                );
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

        try {
            AsyncResult<IAuthenticationResult> result = future.get();

            if (result.getSuccess()) {
                return result.getResult();
            } else {
                throw result.getException();
            }
        } catch (final ExecutionException e) {
            // Shouldn't be thrown.
            throw new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unexpected error while acquiring token.",
                    e
            );
        }
    }

    public void acquireTokenWithDeviceCode(@Nullable String[] scopes, @NonNull final DeviceCodeFlowCallback callback) {
        // Create a DeviceCodeFlowCommandParameters object that takes in the desired scopes and the callback object
        // Use CommandParametersAdapter
        final DeviceCodeFlowCommandParameters commandParameters = CommandParametersAdapter
                .createDeviceCodeFlowCommandParameters(
                        mPublicClientConfiguration,
                        mPublicClientConfiguration.getOAuth2TokenCache(),
                        scopes);

        // Create a CommandCallback object from the DeviceCodeFlowCallback object
        final DeviceCodeFlowCommandCallback deviceCodeFlowCommandCallback = getDeviceCodeFlowCommandCallback(callback);

        // Create a DeviceCodeFlowCommand object
        // Pass the command parameters, default controller, and command callback
        // Telemetry with DEVICE_CODE_FLOW_CALLBACK
        final DeviceCodeFlowCommand deviceCodeFlowCommand = new DeviceCodeFlowCommand(
                commandParameters,
                new LocalMSALController(),
                deviceCodeFlowCommandCallback,
                PublicApiId.DEVICE_CODE_FLOW_WITH_CALLBACK
        );

        CommandDispatcher.submitSilent(deviceCodeFlowCommand);
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

    private DeviceCodeFlowCommandCallback getDeviceCodeFlowCommandCallback(@NonNull final DeviceCodeFlowCallback callback) {
        return new DeviceCodeFlowCommandCallback<LocalAuthenticationResult, BaseException>() {

            @Override
            public void onUserCodeReceived(@NonNull final String vUri,
                                           @NonNull final String userCode,
                                           @NonNull final String message,
                                           @NonNull final Date sessionExpirationDate) {
                callback.onUserCodeReceived(vUri, userCode, message, sessionExpirationDate);
            }

            @Override
            public void onTaskCompleted(LocalAuthenticationResult tokenResult) {
                // Convert tokenResult to an AuthenticationResult object
                final IAuthenticationResult convertedResult = AuthenticationResultAdapter.adapt(
                        tokenResult);

                // Type cast the interface object
                final AuthenticationResult authResult = (AuthenticationResult) convertedResult;

                callback.onTokenReceived(authResult);
            }

            @Override
            public void onError(BaseException error) {
                final MsalException msalException;

                if (error instanceof ServiceException) {
                    msalException = new MsalServiceException(
                            error.getErrorCode(),
                            error.getMessage(),
                            ((ServiceException) error).getHttpStatusCode(),
                            error
                    );
                } else {
                    msalException = new MsalClientException(
                            error.getErrorCode(),
                            error.getMessage(),
                            error
                    );
                }

                callback.onError(msalException);
            }

            @Override
            public void onCancel() {
                // Do nothing
                // No current plans for allowing cancellation of DCF
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
        return MsalOAuth2TokenCache.create(AndroidPlatformComponents.createFromContext(mPublicClientConfiguration.getAppContext()));
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
                        if (!TextUtils.isEmpty(profileEntry.getValue().getId()) &&
                                localAccountId.contains(profileEntry.getValue().getId())) {
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

        try {
            final AsyncResult<IAuthenticationResult> result = future.get();

            if (result.getSuccess()) {
                return result.getResult();
            } else {
                throw result.getException();
            }
        } catch (final ExecutionException e) {
            throw new MsalClientException(
                    UNKNOWN_ERROR,
                    "Unexpected error while acquiring token.",
                    e
            );
        }
    }

    void performMigration(@NonNull final TokenMigrationCallback callback) {
        final Map<String, String> redirects = new HashMap<>();
        redirects.put(
                mPublicClientConfiguration.getClientId(), // Our client id
                mPublicClientConfiguration.getRedirectUri() // Our redirect uri
        );

        final AdalMigrationAdapter adalMigrationAdapter = new AdalMigrationAdapter(
                mPublicClientConfiguration.getAppContext(),
                redirects,
                false
        );

        if (adalMigrationAdapter.getMigrationStatus()) {
            callback.onMigrationFinished(0);
        } else {
            // Create the SharedPreferencesFileManager for the legacy accounts/credentials
            final IMultiTypeNameValueStorage sharedPreferencesFileManager =
                    new SharedPreferencesFileManager(
                            mPublicClientConfiguration.getAppContext(),
                            "com.microsoft.aad.adal.cache",
                            new AndroidAuthSdkStorageEncryptionManager(
                                    mPublicClientConfiguration.getAppContext(), null)
                    );

            // Load the old TokenCacheItems as key/value JSON
            final Map<String, String> credentials = sharedPreferencesFileManager.getAll();

            new TokenMigrationUtility<MicrosoftAccount, MicrosoftRefreshToken>()._import(
                    adalMigrationAdapter,
                    credentials,
                    (IShareSingleSignOnState<MicrosoftAccount, MicrosoftRefreshToken>) mPublicClientConfiguration.getOAuth2TokenCache(),
                    callback
            );
        }
    }

    private static void runOnBackground(@NonNull final Runnable runnable) {
        new Thread(runnable).start();
    }

    private static boolean isAccountHomeTenant(@Nullable final Map<String, ?> claims,
                                               @NonNull final String tenantId) {
        boolean isAccountHomeTenant = false;

        if (null != claims && !claims.isEmpty()) {
            isAccountHomeTenant = claims.get(TENANT_ID).equals(tenantId);
        }

        return isAccountHomeTenant;
    }

}
