package com.microsoft.identity.client.testapp;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.HttpMethod;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PoPAuthenticationScheme;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalDeclinedScopeException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.ui.PreferredAuthMethod;
import com.microsoft.identity.common.java.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/// Acting as a bridge between the result of MsalWrapper's results and the outside world.
interface INotifyOperationResultCallback<T> {
    void onSuccess(T result);

    void showMessage(String message);
}

abstract class MsalWrapper {
    public static void create(@NonNull final Context context,
                              @NonNull final int configFileResourceId,
                              @NonNull final INotifyOperationResultCallback<MsalWrapper> callback) {
        PublicClientApplication.create(context,
                configFileResourceId,
                new PublicClientApplication.ApplicationCreatedListener() {
                    @Override
                    public void onCreated(IPublicClientApplication application) {
                        if (application instanceof ISingleAccountPublicClientApplication) {
                            callback.onSuccess(new SingleAccountModeWrapper((ISingleAccountPublicClientApplication) application));
                        } else {
                            callback.onSuccess(new MultipleAccountModeWrapper((IMultipleAccountPublicClientApplication) application));
                        }
                    }

                    @Override
                    public void onError(MsalException exception) {
                        callback.showMessage("Failed to load MSAL Application: " + exception.getMessage());
                    }
                });
    }

    public abstract String getDefaultBrowser();

    public abstract String getMode();

    public abstract IPublicClientApplication getApp();

    public abstract void loadAccounts(@NonNull final INotifyOperationResultCallback<List<IAccount>> callback);

    public abstract void removeAccount(@NonNull IAccount account,
                                       @NonNull final INotifyOperationResultCallback<Void> callback);

    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final RequestOptions requestOptions,
                             @NonNull final INotifyOperationResultCallback<IAuthenticationResult> callback) {

        final AcquireTokenParameters.Builder builder = getAcquireTokenParametersBuilder(activity, requestOptions, callback);
        builder.withScopes(Arrays.asList(requestOptions.getScopes().toLowerCase().split(" ")));
        builder.withOtherScopesToAuthorize(
                Arrays.asList(
                        requestOptions
                                .getExtraScope()
                                .toLowerCase()
                                .split(" ")
                )
        );

        final AcquireTokenParameters parameters = builder.build();
        acquireTokenAsyncInternal(parameters);
    }

    public void acquireTokenWithQR(@NonNull final Activity activity,
                                   @NonNull final RequestOptions requestOptions,
                                   @NonNull final INotifyOperationResultCallback<IAuthenticationResult> callback) {

        final AcquireTokenParameters.Builder builder = getAcquireTokenParametersBuilder(activity, requestOptions, callback);
        builder.withScopes(Arrays.asList(requestOptions.getScopes().toLowerCase().split(" ")));
        builder.withPreferredAuthMethod(PreferredAuthMethod.QR);
        builder.withOtherScopesToAuthorize(
                Arrays.asList(
                        requestOptions
                                .getExtraScope()
                                .toLowerCase()
                                .split(" ")
                )
        );

        final AcquireTokenParameters parameters = builder.build();
        acquireTokenAsyncInternal(parameters);
    }

    public void acquireTokenWithResource(@NonNull final Activity activity,
                                         @NonNull final RequestOptions requestOptions,
                                         @NonNull final INotifyOperationResultCallback<IAuthenticationResult> callback) {

        final AcquireTokenParameters.Builder builder = getAcquireTokenParametersBuilder(activity, requestOptions, callback);
        builder.withAuthorizationQueryStringParameters(null);
        builder.withResource(requestOptions.getScopes().trim());

        final AcquireTokenParameters parameters = builder.build();
        acquireTokenAsyncInternal(parameters);
    }

    private AcquireTokenParameters.Builder getAcquireTokenParametersBuilder(@NonNull Activity activity,
                                                                            @NonNull RequestOptions requestOptions,
                                                                            @NonNull INotifyOperationResultCallback<IAuthenticationResult> callback) {
        final AcquireTokenParameters.Builder builder = new AcquireTokenParameters.Builder();
        builder.startAuthorizationFromActivity(activity)
                .withLoginHint(requestOptions.getLoginHint())
                .forAccount(requestOptions.getAccount())
                .withPrompt(requestOptions.getPrompt())
                .withCallback(getAuthenticationCallback(callback));

        // Create extra query parameters list
        final List<Map.Entry<String, String>> extraQP = new ArrayList<>();

        // Split given extra query parameters and add them to extra query parameter list
        // If any were passed in the RequestOptions
        final String[] requestGenericExtraQueryParams = requestOptions.getExtraQueryParams().split("&");
        for (String extraQueryParam : requestGenericExtraQueryParams) {
            if (extraQueryParam.equals("")) {
                continue;
            }
            final String[] splitParam = extraQueryParam.split("=");
            extraQP.add(new AbstractMap.SimpleEntry<>(splitParam[0], splitParam[1]));
        }

        // add "is_remote_login_allowed=true" if passed
        if (requestOptions.isAllowSignInFromOtherDevice()) {
            extraQP.add(new AbstractMap.SimpleEntry<>("is_remote_login_allowed", Boolean.toString(true)));
        }
        builder.withAuthorizationQueryStringParameters(extraQP);

        if (!StringUtil.isNullOrEmpty(requestOptions.getAuthority())) {
            builder.fromAuthority(requestOptions.getAuthority());
        }

        if (!StringUtil.isNullOrEmpty(requestOptions.getClaims())) {
            builder.withClaims(ClaimsRequest.getClaimsRequestFromJsonString(requestOptions.getClaims()));
        }

        if (requestOptions.getAuthScheme() == Constants.AuthScheme.POP) {
            try {
                builder.withAuthenticationScheme(
                        PoPAuthenticationScheme.builder()
                                .withHttpMethod(requestOptions.getPopHttpMethod())
                                .withClientClaims(requestOptions.getPoPClientClaims())
                                .withUrl(new URL(requestOptions.getPopResourceUrl()))
                                .build()
                );
            } catch (MalformedURLException e) {
                callback.showMessage("Unexpected error." + e.getMessage());
            }
        }

        return builder;
    }

    abstract void acquireTokenAsyncInternal(@NonNull final AcquireTokenParameters parameters);

    public void acquireTokenSilent(@NonNull RequestOptions requestOptions,
                                   @NonNull INotifyOperationResultCallback<IAuthenticationResult> callback) {
        if (requestOptions.getAccount() == null) {
            callback.showMessage("Account is null.");
            return;
        }

        final AcquireTokenSilentParameters.Builder builder = getAcquireTokenSilentParametersBuilder(requestOptions, callback);
        builder.withScopes(Arrays.asList(requestOptions.getScopes().toLowerCase().split(" ")));

        final AcquireTokenSilentParameters parameters = builder.build();
        acquireTokenSilentAsyncInternal(parameters);
    }

    public void acquireTokenSilentWithResource(@NonNull RequestOptions requestOptions,
                                               @NonNull INotifyOperationResultCallback<IAuthenticationResult> callback) {
        if (requestOptions.getAccount() == null) {
            callback.showMessage("Account is null.");
            return;
        }

        final AcquireTokenSilentParameters.Builder builder = getAcquireTokenSilentParametersBuilder(requestOptions, callback);
        builder.withResource(requestOptions.getScopes().toLowerCase().trim());

        final AcquireTokenSilentParameters parameters = builder.build();
        acquireTokenSilentAsyncInternal(parameters);
    }

    private AcquireTokenSilentParameters.Builder getAcquireTokenSilentParametersBuilder(@NonNull RequestOptions requestOptions,
                                                                                        @NonNull INotifyOperationResultCallback<IAuthenticationResult> callback) {
        final AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder();
        builder.forAccount(requestOptions.getAccount())
                .forceRefresh(requestOptions.isForceRefresh())
                .withCallback(getAuthenticationCallback(callback));

        if (!StringUtil.isNullOrEmpty(requestOptions.getAuthority())) {
            builder.fromAuthority(requestOptions.getAuthority());
        } else {
            builder.fromAuthority(requestOptions.getAccount().getAuthority());
        }

        if (!StringUtil.isNullOrEmpty(requestOptions.getClaims())) {
            builder.withClaims(ClaimsRequest.getClaimsRequestFromJsonString(requestOptions.getClaims()));
        }

        if (requestOptions.getAuthScheme() == Constants.AuthScheme.POP) {
            try {
                builder.withAuthenticationScheme(
                        PoPAuthenticationScheme.builder()
                                .withHttpMethod(requestOptions.getPopHttpMethod())
                                .withClientClaims(requestOptions.getPoPClientClaims())
                                .withUrl(new URL(requestOptions.getPopResourceUrl())).build()
                );
            } catch (MalformedURLException e) {
                callback.showMessage("Unexpected error." + e.getMessage());
                return null;
            }
        }

        return builder;
    }

    abstract void acquireTokenSilentAsyncInternal(@NonNull final AcquireTokenSilentParameters parameters);

    abstract String getActiveBrokerPkgName(@NonNull final Activity activity);

    public void acquireTokenWithDeviceCodeFlow(@NonNull RequestOptions requestOptions,
                                               @NonNull final INotifyOperationResultCallback<IAuthenticationResult> callback) {

        ClaimsRequest claimsRequest = null;
        if (!StringUtil.isNullOrEmpty(requestOptions.getClaims())) {
            claimsRequest = ClaimsRequest.getClaimsRequestFromJsonString(requestOptions.getClaims());
        }

        acquireTokenWithDeviceCodeFlowInternal(
                Arrays.asList(requestOptions.getScopes().toLowerCase().split(" ")),
                claimsRequest,
                new IPublicClientApplication.DeviceCodeFlowCallback() {
                    @Override
                    public void onUserCodeReceived(@NonNull String vUri,
                                                   @NonNull String userCode,
                                                   @NonNull String message,
                                                   @NonNull Date sessionExpirationDate) {
                        callback.showMessage(
                                "Uri: " + vUri + "\n" +
                                        "UserCode: " + userCode + "\n" +
                                        "Message: " + message + "\n" +
                                        "sessionExpirationDate: " + sessionExpirationDate);
                    }

                    @Override
                    public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                        callback.onSuccess(authResult);
                    }

                    @Override
                    public void onError(@NonNull MsalException e) {
                        callback.showMessage("Unexpected error." + e.getMessage());
                    }
                });
    }

    abstract void acquireTokenWithDeviceCodeFlowInternal(@NonNull List<String> scopes,
                                                         @Nullable ClaimsRequest claimsRequest,
                                                         @NonNull final IPublicClientApplication.DeviceCodeFlowCallback callback);

    AuthenticationCallback getAuthenticationCallback(@NonNull final INotifyOperationResultCallback<IAuthenticationResult> callback) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                callback.onSuccess(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                String message = "CorrelationID: " + exception.getCorrelationId() + "\n";
                // Check the exception type.
                if (exception instanceof MsalClientException) {
                    // This means errors happened in the sdk itself, could be network, Json parse, etc. Check MsalError.java
                    // for detailed list of the errors.
                    message += "MsalClientException.\n" + exception.getMessage();
                } else if (exception instanceof MsalServiceException) {
                    // This means something is wrong when the sdk is communication to the service, mostly likely it's the client
                    // configuration.
                    message += "MsalServiceException.\n" + exception.getMessage();
                } else if (exception instanceof MsalArgumentException) {
                    message += "MsalArgumentException.\n" + exception.getMessage();
                } else if (exception instanceof MsalUiRequiredException) {
                    // This explicitly indicates that developer needs to prompt the user, it could be refresh token is expired, revoked
                    // or user changes the password; or it could be that no token was found in the token cache.
                    message += "MsalUiRequiredException.\n" + exception.getMessage();
                } else if (exception instanceof MsalDeclinedScopeException) {
                    // Declined scope implies that not all scopes requested have been granted.
                    // Developer can either continue with Authentication by calling acquireTokenSilent
                    message += "MsalDeclinedScopeException.\n" +
                            "Granted Scope:" + ((MsalDeclinedScopeException) exception).getGrantedScopes() + "\n" +
                            "Declined Scope:" + ((MsalDeclinedScopeException) exception).getDeclinedScopes();
                }

                callback.showMessage(message);
            }

            @Override
            public void onCancel() {
                callback.showMessage("User cancelled the flow.");
            }
        };
    }

    public void generateSignedHttpRequest(@NonNull final RequestOptions currentRequestOptions,
                                          @NonNull final INotifyOperationResultCallback<String> generateShrCallback) {
        // Build up the params we want/need
        final IAccount currentAccount = currentRequestOptions.getAccount();
        final HttpMethod popHttpMethod = currentRequestOptions.getPopHttpMethod();
        final String resourceUrl = currentRequestOptions.getPopResourceUrl();
        final String clientClaims = currentRequestOptions.getPoPClientClaims();

        if (null == currentAccount) {
            // User must first sign-in
            generateShrCallback.showMessage("No user signed-in or selected.");
            return;
        }

        try {
            final PoPAuthenticationScheme popParams =
                    PoPAuthenticationScheme.builder()
                            .withHttpMethod(popHttpMethod)
                            .withUrl(new URL(resourceUrl))
                            .withClientClaims(clientClaims)
                            .build();

            Log.d(
                    MsalWrapper.class.getSimpleName() + ":generateSHR",
                    "Account: " + currentAccount.getUsername()
                            + "\n"
                            + "HttpMethod: " + popHttpMethod
                            + "\n"
                            + "Resource URL: " + resourceUrl
                            + "\n"
                            + "Client Claims: " + clientClaims
            );

            generateSignedHttpRequestInternal(
                    currentAccount,
                    popParams,
                    generateShrCallback
            );
        } catch (MalformedURLException e) {
            generateShrCallback.showMessage("Invalid URL.");
        }
    }

    public abstract void generateSignedHttpRequestInternal(@NonNull final IAccount account,
                                                           @NonNull final PoPAuthenticationScheme params,
                                                           @NonNull final INotifyOperationResultCallback<String> generateShrCallback
    );

    public String getPreferredAuthMethod() {
        try {
            return getApp().getPreferredAuthConfiguration().name();
        } catch (BaseException e) {
            return e.getMessage();
        }
    }
}
