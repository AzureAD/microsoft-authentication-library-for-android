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
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.configuration.AccountMode;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalUserCancelException;
import com.microsoft.identity.client.internal.AsyncResult;
import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.client.internal.configuration.LogLevelDeserializer;
import com.microsoft.identity.client.internal.controllers.BrokerMsalController;
import com.microsoft.identity.client.internal.controllers.MSALControllerFactory;
import com.microsoft.identity.client.internal.controllers.MsalExceptionAdapter;
import com.microsoft.identity.client.internal.controllers.OperationParametersAdapter;
import com.microsoft.identity.client.internal.telemetry.DefaultEvent;
import com.microsoft.identity.client.internal.telemetry.Defaults;
import com.microsoft.identity.common.adal.internal.tokensharing.TokenShareUtility;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AuthorityDeserializer;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudienceDeserializer;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.controllers.ApiDispatcher;
import com.microsoft.identity.common.internal.controllers.InteractiveTokenCommand;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.controllers.TokenCommand;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.net.cache.HttpCache;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.ILocalAuthenticationCallback;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.internal.result.ResultFuture;
import com.microsoft.identity.msal.BuildConfig;
import com.microsoft.identity.msal.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.client.internal.controllers.OperationParametersAdapter.isHomeTenantEquivalent;
import static com.microsoft.identity.common.exception.ClientException.TOKEN_CACHE_ITEM_NOT_FOUND;
import static com.microsoft.identity.common.exception.ClientException.TOKEN_SHARING_DESERIALIZATION_ERROR;
import static com.microsoft.identity.common.exception.ClientException.TOKEN_SHARING_MSA_PERSISTENCE_ERROR;

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
public class PublicClientApplication implements IPublicClientApplication, ITokenShare {
    private static final String TAG = PublicClientApplication.class.getSimpleName();

    private static final String INTERNET_PERMISSION = "android.permission.INTERNET";
    private static final String ACCESS_NETWORK_STATE_PERMISSION = "android.permission.ACCESS_NETWORK_STATE";

    /**
     * Constant used to signal a home account's tenant id should be used when performing cache lookups
     * relative to creating OperationParams.
     */
    private static final String FORCE_HOME_LOOKUP = "force_home_lookup";

    private static final String TSL_MSG_FAILED_TO_SAVE
            = "Failed to save FRT - see getCause() for additional Exception info";

    private static final String TSM_MSG_FAILED_TO_RETRIEVE
            = "Failed to retrieve FRT - see getCause() for additional Exception info";

    protected boolean mIsSharedDevice;

    protected PublicClientApplicationConfiguration mPublicClientConfiguration;
    protected TokenShareUtility mTokenShareUtility;

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
     * @param listener             a callback to be invoked when the object is successfully created.
     *                             <p>
     *                             For more information on the schema of the MSAL config json please
     * @param listener             a callback to be invoked when the object is successfully created.
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     */
    public static void create(@NonNull final Context context,
                              final int configFileResourceId,
                              @NonNull final ApplicationCreatedListener listener) {
        validateNonNullArgument(context, "context");
        validateNonNullArgument(listener, "listener");

        create(context,
                loadConfiguration(context, configFileResourceId),
                listener
        );
    }


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
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     */
    @WorkerThread
    public static IPublicClientApplication create(@NonNull final Context context,
                                                  final int configFileResourceId) throws InterruptedException, MsalException {
        validateNonNullArgument(context, "context");

        final PublicClientApplicationConfiguration configuration = loadConfiguration(context, configFileResourceId);
        return createPublicClientApplication(context, configuration);
    }


    /**
     * {@link PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int, ApplicationCreatedListener)} will read the client id and other configuration settings from the
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
     * @param listener             a callback to be invoked when the object is successfully created.
     *                             <p>
     *                             For more information on the schema of the MSAL config json please
     * @param listener             a callback to be invoked when the object is successfully created.
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     */
    public static void createMultipleAccountPublicClientApplication(@NonNull final Context context,
                                                                    final int configFileResourceId,
                                                                    @NonNull final ApplicationCreatedListener listener) {

        validateNonNullArgument(context, "context");

        create(context,
                loadConfiguration(context, configFileResourceId),
                listener
        );
    }

    /**
     * {@link PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int)} will read the client id and other configuration settings from the
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
     * @return An instance of IMultiAccountPublicClientApplication
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     */
    @WorkerThread
    public static IMultipleAccountPublicClientApplication createMultipleAccountPublicClientApplication(@NonNull final Context context,
                                                                                                       final int configFileResourceId) throws MsalException, InterruptedException {
        validateNonNullArgument(context, "context");

        final PublicClientApplicationConfiguration configuration = loadConfiguration(context, configFileResourceId);

        if (configuration.mAccountMode != AccountMode.MULTIPLE) {
            throw new MsalClientException("AccountMode in configuration is not set to multiple");
        }

        IPublicClientApplication application = createPublicClientApplication(context, configuration);
        if (application instanceof IMultipleAccountPublicClientApplication) {
            return (IMultipleAccountPublicClientApplication) application;
        } else {
            if (configuration.mAccountMode == AccountMode.MULTIPLE && application.isSharedDevice()) {
                throw new MsalClientException("AccountMode in configuration is set to multiple; however the device is marked as shared");
            }
            throw new MsalClientException("A multiple account public client application could not be created for unknown reasons");
        }
    }

    /**
     * {@link PublicClientApplication#createMultipleAccountPublicClientApplication(Context, int, ApplicationCreatedListener)} will read the client id and other configuration settings from the
     * file included in your applications resources.
     * <p>
     * For more information on adding configuration files to your applications resources please
     *
     * @param context    Application's {@link Context}. The sdk requires the application context to be passed in
     *                   {@link PublicClientApplication}. Cannot be null.
     *                   <p>
     *                   Note: The {@link Context} should be the application context instead of the running activity's context, which could potentially make the sdk hold a
     *                   strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                   </p>
     * @param configFile The file containing the JSON configuration for the PublicClientApplication
     * @return An instance of MultiAccountPublicClientApplication
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     */
    @WorkerThread
    public static IMultipleAccountPublicClientApplication createMultipleAccountPublicClientApplication(@NonNull final Context context,
                                                                                                       @NonNull final File configFile) throws InterruptedException, MsalException {

        validateNonNullArgument(context, "context");
        validateNonNullArgument(configFile, "configFile");

        PublicClientApplicationConfiguration configuration = loadConfiguration(configFile);

        if (configuration.mAccountMode != AccountMode.MULTIPLE) {
            throw new MsalClientException("AccountMode in configuration is not set to multiple");
        }

        IPublicClientApplication application = createPublicClientApplication(context, configuration);

        if (application instanceof IMultipleAccountPublicClientApplication) {
            return (IMultipleAccountPublicClientApplication) application;
        } else {
            if (configuration.mAccountMode == AccountMode.MULTIPLE && application.isSharedDevice()) {
                throw new MsalClientException("AccountMode in configuration is set to multiple; however the device is marked as shared");
            }
            throw new MsalClientException("A multiple account public client application could not be created for unknown reasons");
        }
    }

    private static IPublicClientApplication createPublicClientApplication(@NonNull final Context context,
                                                                          final PublicClientApplicationConfiguration configuration) throws MsalException, InterruptedException {

        validateNonNullArgument(context, "context");
        validateNonNullArgument(configuration, "configuration");

        throwOnMainThread("createMultipleAccountPublicClientApplication");

        final ResultFuture<AsyncResult<IPublicClientApplication>> future = new ResultFuture<>();
        create(context,
                configuration,
                new ApplicationCreatedListener() {
                    @Override
                    public void onCreated(IPublicClientApplication application) {
                        future.setResult(new AsyncResult<IPublicClientApplication>(application, null));
                    }

                    @Override
                    public void onError(MsalException exception) {
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

    /**
     * {@link PublicClientApplication#createSingleAccountPublicClientApplication(Context, int, ApplicationCreatedListener)} will read the client id and other configuration settings from the
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
     * @param listener             a callback to be invoked when the object is successfully created.
     *                             <p>
     *                             For more information on the schema of the MSAL config json please
     * @param listener             a callback to be invoked when the object is successfully created.
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     */
    public static void createSingleAccountPublicClientApplication(@NonNull final Context context,
                                                                  final int configFileResourceId,
                                                                  @NonNull final ApplicationCreatedListener listener) {

        validateNonNullArgument(context, "context");
        validateNonNullArgument(listener, "listener");
        if (context == null) {
            throw new IllegalArgumentException("context is null.");
        }

        create(context,
                loadConfiguration(context, configFileResourceId),
                listener
        );
    }

    /**
     * {@link PublicClientApplication#createSingleAccountPublicClientApplication(Context, int)} will read the client id and other configuration settings from the
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
     * @return An instance of ISingleAccountPublicClientApplication
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     */
    @WorkerThread
    public static ISingleAccountPublicClientApplication createSingleAccountPublicClientApplication(@NonNull final Context context,
                                                                                                   final int configFileResourceId) throws InterruptedException, MsalException {
        validateNonNullArgument(context, "context");

        final PublicClientApplicationConfiguration configuration = loadConfiguration(context, configFileResourceId);

        if (configuration.mAccountMode != AccountMode.SINGLE) {
            throw new MsalClientException("AccountMode in configuration is not set to single");
        }

        IPublicClientApplication application = createPublicClientApplication(context, configuration);
        if (application instanceof ISingleAccountPublicClientApplication) {
            return (ISingleAccountPublicClientApplication) application;
        } else {
            throw new MsalClientException("A multiple single public client application could not be created for unknown reasons");
        }
    }

    /**
     * {@link PublicClientApplication#createSingleAccountPublicClientApplication(Context, int)} will read the client id and other configuration settings from the
     * file included in your applications resources.
     * <p>
     * For more information on adding configuration files to your applications resources please
     *
     * @param context    Application's {@link Context}. The sdk requires the application context to be passed in
     *                   {@link PublicClientApplication}. Cannot be null.
     *                   <p>
     *                   Note: The {@link Context} should be the application context instead of the running activity's context, which could potentially make the sdk hold a
     *                   strong reference to the activity, thus preventing correct garbage collection and causing bugs.
     *                   </p>
     * @param configFile The file containing the JSON configuration for the PublicClientApplication
     * @return An instance of ISingleAccountPublicClientApplication
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     */
    @WorkerThread
    public static ISingleAccountPublicClientApplication createSingleAccountPublicClientApplication(@NonNull final Context context,
                                                                                                   final File configFile) throws InterruptedException, MsalException {

        validateNonNullArgument(context, "context");

        final PublicClientApplicationConfiguration configuration = loadConfiguration(configFile);
        IPublicClientApplication application = createPublicClientApplication(context, configuration);

        if (application instanceof ISingleAccountPublicClientApplication) {
            return (ISingleAccountPublicClientApplication) application;
        } else {
            if (configuration.mAccountMode != AccountMode.SINGLE) {
                throw new MsalClientException("AccountMode in configuration is not set to single");
            }
            throw new MsalClientException("A single account public client application could not be created for unknown reasons");
        }
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
     * @param listener   a callback to be invoked when the object is successfully created.
     * @see <a href="https://developer.android.com/guide/topics/resources/providing-resources">Android app resource overview</a>
     * <p>
     * For more information on the schema of the MSAL config json please
     * @see <a href="https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki">MSAL Github Wiki</a>
     */
    public static void create(@NonNull final Context context,
                              final File configFile,
                              @NonNull final ApplicationCreatedListener listener) {

        validateNonNullArgument(context, "context");

        create(context,
                loadConfiguration(configFile),
                listener);
    }

    /**
     * @param methodName
     */
    protected static void throwOnMainThread(String methodName) {

        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("method: " + methodName + " may not be called from main thread.");
        }
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

        validateNonNullArgument(context, "context");
        validateNonNullArgument(context, "listener");


        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("client id is empty or null");
        }

        new BrokerMsalController().getBrokerDeviceMode(context, new BrokerDeviceModeCallback() {
            @Override
            public void onGetMode(boolean isSharedDevice) {
                if (isSharedDevice) {
                    listener.onCreated(new SingleAccountPublicClientApplication(context, clientId, isSharedDevice));
                } else {
                    listener.onCreated(new MultipleAccountPublicClientApplication(context, clientId));
                }
            }

            @Override
            public void onError(MsalException exception) {
                listener.onError(exception);
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

        validateNonNullArgument(context, "context");

        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("client id is empty or null");
        }

        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("authority is empty or null");
        }

        new BrokerMsalController().getBrokerDeviceMode(context, new BrokerDeviceModeCallback() {
            @Override
            public void onGetMode(boolean isSharedDevice) {
                if (isSharedDevice) {
                    listener.onCreated(new SingleAccountPublicClientApplication(context, clientId, authority, isSharedDevice));
                } else {
                    listener.onCreated(new MultipleAccountPublicClientApplication(context, clientId, authority));
                }
            }

            @Override
            public void onError(MsalException exception) {
                listener.onError(exception);
            }
        });
    }

    private static void create(@NonNull final Context context,
                               final PublicClientApplicationConfiguration developerConfig,
                               @NonNull final ApplicationCreatedListener listener) {
        new BrokerMsalController().getBrokerDeviceMode(context, new BrokerDeviceModeCallback() {
            @Override
            public void onGetMode(boolean isSharedDevice) {
                if (isSharedDevice) {
                    listener.onCreated(new SingleAccountPublicClientApplication(context, developerConfig, isSharedDevice));
                } else {
                    if (developerConfig.getAccountMode() == AccountMode.SINGLE) {
                        listener.onCreated(new SingleAccountPublicClientApplication(context, developerConfig, false));
                    } else {
                        listener.onCreated(new MultipleAccountPublicClientApplication(context, developerConfig));
                    }
                }
            }

            @Override
            public void onError(MsalException exception) {
                listener.onError(exception);
            }
        });
    }

    protected PublicClientApplication(@NonNull final Context context,
                                      @Nullable final PublicClientApplicationConfiguration developerConfig) {
        initializeConfiguration(context, developerConfig);

        AzureActiveDirectory.setEnvironment(mPublicClientConfiguration.getEnvironment());
        Authority.addKnownAuthorities(mPublicClientConfiguration.getAuthorities());

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

    protected PublicClientApplication(@NonNull final Context context,
                                      @NonNull final String clientId) {
        this(context, (PublicClientApplicationConfiguration) null);
        mPublicClientConfiguration.mClientId = clientId;
        initializeApplication();
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
        final Context context = mPublicClientConfiguration.getAppContext();

        // Init Events with defaults (application-wide)
        DefaultEvent.initializeDefaults(
                Defaults.forApplication(
                        context,
                        mPublicClientConfiguration.getClientId()
                )
        );

        mPublicClientConfiguration.mRedirectUri
                = BrokerValidator.getBrokerRedirectUri(
                mPublicClientConfiguration.getAppContext(),
                mPublicClientConfiguration.getAppContext().getPackageName());

        checkIntentFilterAddedToAppManifest();

        // Since network request is sent from the sdk, if calling app doesn't declare the internet permission in the
        // manifest, we cannot make the network call.
        checkInternetPermission();

        // Init HTTP cache
        HttpCache.initialize(context.getCacheDir());

        com.microsoft.identity.common.internal.logging.Logger.info(TAG, "Create new public client application.");
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
     * Listener callback for asynchronous initialization of IPublicClientApplication object.
     */
    public interface ApplicationCreatedListener {
        /**
         * Called once an IPublicClientApplication is successfully created.
         */
        void onCreated(final IPublicClientApplication application);

        /**
         * Called once IPublicClientApplication can't be created.
         */
        void onError(final MsalException exception);
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
        return mIsSharedDevice;
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

    protected void acquireToken(@NonNull final Activity activity,
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


        acquireToken(acquireTokenParameters);
    }

    protected static void validateNonNullArgument(@Nullable Object o,
                                                  @NonNull String argName) {
        if (null == o
                || (o instanceof CharSequence) && TextUtils.isEmpty((CharSequence) o)) {
            throw new IllegalArgumentException(
                    argName
                            + " cannot be null or empty"
            );
        }
    }

    /**
     * For the provided configuration and TokenParameters, determine the tenant id that should be
     * used to lookup the proper AccountRecord.
     *
     * @param config        The application configuration to inspect if no request authority is provided.
     * @param requestParams The request parameters to inspect for an authority.
     * @return The tenantId **OR** a magic constant signalling that home should be used.
     */
    private String getRequestTenantId(@NonNull final PublicClientApplicationConfiguration config,
                                      @NonNull final TokenParameters requestParams) {
        final String result;

        // First look at the request
        if (TextUtils.isEmpty(requestParams.getAuthority())) {
            // Authority was not provided in the request - fallback to the default authority
            requestParams.setAuthority(
                    config
                            .getDefaultAuthority()
                            .getAuthorityUri()
                            .toString()
            );
        }

        final Authority authority = Authority.getAuthorityFromAuthorityUrl(requestParams.getAuthority());

        if (authority instanceof AzureActiveDirectoryAuthority) {
            final AzureActiveDirectoryAuthority aadAuthority = (AzureActiveDirectoryAuthority) authority;
            final String tenantId = aadAuthority.getAudience().getTenantId();

            if (isHomeTenantEquivalent(tenantId)) { // something like /consumers, /orgs, /common
                result = FORCE_HOME_LOOKUP;
            } else {
                // Use the specific tenant
                result = tenantId;
            }
        } else if (authority instanceof AzureActiveDirectoryB2CAuthority) {
            result = FORCE_HOME_LOOKUP;
        } else {
            // Unrecognized authority type
            throw new UnsupportedOperationException(
                    "Unsupported Authority type: "
                            + authority
                            .getClass()
                            .getSimpleName()
            );
        }

        return result;

    }

    protected void validateAcquireTokenParameters(AcquireTokenParameters parameters) {
        return;
    }

    protected void validateAcquireTokenSilentParameters(AcquireTokenSilentParameters parameters) {
        if (TextUtils.isEmpty(parameters.getAuthority())) {
            throw new IllegalArgumentException(
                    "Authority must be specified for acquireTokenSilent"
            );
        }

        return;
    }

    @Override
    public void acquireToken(@NonNull final AcquireTokenParameters acquireTokenParameters) {
        acquireTokenParameters.setAccountRecord(
                getAccountRecord(
                        acquireTokenParameters.getAccount(),
                        getRequestTenantId(
                                mPublicClientConfiguration,
                                acquireTokenParameters
                        )
                )
        );

        validateAcquireTokenParameters(acquireTokenParameters);

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

    protected AccountRecord getAccountRecord(@Nullable final IAccount account,
                                             @NonNull String tenantId) {
        final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) account;

        if (null != multiTenantAccount) {

            if (FORCE_HOME_LOOKUP.equals(tenantId)) {
                tenantId = account.getTenantId();
            }

            return AccountAdapter.getAccountInternal(
                    mPublicClientConfiguration.getClientId(),
                    mPublicClientConfiguration.getOAuth2TokenCache(),
                    multiTenantAccount.getHomeAccountId(),
                    tenantId
            );
        }

        return null;
    }

    protected void acquireTokenSilent(@NonNull final String[] scopes,
                                      @NonNull final IAccount account,
                                      @NonNull final String authority,
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
    public void acquireTokenSilentAsync(
            @NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters) {
        acquireTokenSilentParameters.setAccountRecord(
                getAccountRecord(
                        acquireTokenSilentParameters.getAccount(),
                        getRequestTenantId(
                                mPublicClientConfiguration,
                                acquireTokenSilentParameters
                        )
                )
        );

        String requestEnvironment = null;
        String requestHomeAccountId = null;

        if (null != acquireTokenSilentParameters.getAccountRecord()) {
            final AccountRecord requestAccount = acquireTokenSilentParameters.getAccountRecord();
            requestEnvironment = requestAccount.getEnvironment();
            requestHomeAccountId = requestAccount.getHomeAccountId();
        }

        validateAcquireTokenSilentParameters(acquireTokenSilentParameters);

        final String finalRequestEnvironment = requestEnvironment;
        final String finalRequestHomeAccountId = requestHomeAccountId;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final AcquireTokenSilentOperationParameters params =
                        OperationParametersAdapter.createAcquireTokenSilentOperationParameters(
                                acquireTokenSilentParameters,
                                mPublicClientConfiguration,
                                finalRequestEnvironment,
                                finalRequestHomeAccountId
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
        }).start();
    }

    @Override
    public IAuthenticationResult acquireTokenSilent(@NonNull AcquireTokenSilentParameters acquireTokenSilentParameters) throws InterruptedException, MsalException {
        if (acquireTokenSilentParameters.getCallback() != null) {
            throw new IllegalArgumentException("Do not provide callback for synchronous methods");
        }

        final ResultFuture<AsyncResult<IAuthenticationResult>> future = new ResultFuture<>();

        acquireTokenSilentParameters.setCallback(new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                future.setResult(new AsyncResult<IAuthenticationResult>(authenticationResult, null));
            }

            @Override
            public void onError(MsalException exception) {
                future.setResult(new AsyncResult<IAuthenticationResult>(null, exception));
            }

            @Override
            public void onCancel() {
                future.setResult(new AsyncResult<IAuthenticationResult>(null, new MsalUserCancelException()));
            }
        });

        acquireTokenSilentAsync(acquireTokenSilentParameters);

        AsyncResult<IAuthenticationResult> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
        }

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

    private void initializeConfiguration(@NonNull Context context,
                                         @Nullable PublicClientApplicationConfiguration developerConfig) {
        final PublicClientApplicationConfiguration defaultConfig = loadDefaultConfiguration(context);

        if (developerConfig != null) {
            defaultConfig.mergeConfiguration(developerConfig);
            defaultConfig.validateConfiguration();
        }

        mPublicClientConfiguration = defaultConfig;
        mPublicClientConfiguration.setAppContext(context);
        mPublicClientConfiguration.setOAuth2TokenCache(getOAuth2TokenCache());

        setupTelemetry(context, mPublicClientConfiguration);
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

    static TaskCompletedCallbackWithError<List<ICacheRecord>, BaseException> getLoadAccountsCallback(
            final LoadAccountsCallback loadAccountsCallback) {
        return new TaskCompletedCallbackWithError<List<ICacheRecord>, BaseException>() {
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
                loadAccountsCallback.onError(MsalExceptionAdapter.msalExceptionFromBaseException(exception));
            }
        };
    }

    protected ILocalAuthenticationCallback getLocalAuthenticationCallback(final AuthenticationCallback authenticationCallback) {

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

    protected IAuthenticationResult acquireTokenSilentSync(@NonNull final String[] scopes,
                                                           @NonNull final String authority,
                                                           @NonNull final IAccount account,
                                                           final boolean forceRefresh) throws MsalException, InterruptedException {

        throwOnMainThread("acquireTokenSilent");

        final ResultFuture<AsyncResult<IAuthenticationResult>> future = new ResultFuture<>();

        acquireTokenSilent(
                scopes,
                account,
                authority, // authority
                forceRefresh, // forceRefresh
                null, // claimsRequest
                new AuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        future.setResult(new AsyncResult<IAuthenticationResult>(authenticationResult, null));
                    }

                    @Override
                    public void onError(MsalException exception) {
                        future.setResult(new AsyncResult<IAuthenticationResult>(null, exception));
                    }

                    @Override
                    public void onCancel() {
                        future.setResult(new AsyncResult<IAuthenticationResult>(null, new MsalUserCancelException()));
                    }
                }
        );

        AsyncResult<IAuthenticationResult> result = future.get();

        if (result.getSuccess()) {
            return result.getResult();
        } else {
            throw result.getException();
        }
    }
}
