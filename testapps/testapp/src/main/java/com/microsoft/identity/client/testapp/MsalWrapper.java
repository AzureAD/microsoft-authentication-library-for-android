package com.microsoft.identity.client.testapp;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

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
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalDeclinedScopeException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

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

    public abstract void loadAccounts(@NonNull final INotifyOperationResultCallback<List<IAccount>> callback);

    public abstract void removeAccount(@NonNull IAccount account,
                                       @NonNull final INotifyOperationResultCallback<Void> callback);

    public void acquireToken(@NonNull final Activity activity,
                             @NonNull final RequestOptions requestOptions,
                             @NonNull final INotifyOperationResultCallback<IAuthenticationResult> callback) {

        final AcquireTokenParameters.Builder builder = new AcquireTokenParameters.Builder();
        builder.startAuthorizationFromActivity(activity)
                .withScopes(Arrays.asList(requestOptions.getScopes().toLowerCase().split(" ")))
                .withLoginHint(requestOptions.getLoginHint())
                .forAccount(requestOptions.getAccount())
                .withPrompt(requestOptions.getPrompt())
                .withOtherScopesToAuthorize(
                        Arrays.asList(
                                requestOptions
                                        .getExtraScopesToConsent()
                                        .toLowerCase()
                                        .split(" ")
                        )
                )
                .withCallback(getAuthenticationCallback(callback));

        if (requestOptions.getAuthority() != null && !requestOptions.getAuthority().isEmpty()) {
            builder.fromAuthority(requestOptions.getAuthority());
        }

        if (requestOptions.getAuthScheme() == Constants.AuthScheme.POP) {
            try {
                builder.withAuthenticationScheme(
                        PoPAuthenticationScheme.builder()
                                .withHttpMethod(HttpMethod.GET)
                                .withUrl(
                                        new URL("https://signedhttprequest.azurewebsites.net/api/validateSHR")
                                ).build()
                );
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        final AcquireTokenParameters parameters = builder.build();
        acquireTokenAsyncInternal(parameters);
    }

    public void acquireTokenWithResource(@NonNull final Activity activity,
                                         @NonNull final RequestOptions requestOptions,
                                         @NonNull final INotifyOperationResultCallback<IAuthenticationResult> callback) {
        final AcquireTokenParameters.Builder builder = new AcquireTokenParameters.Builder();
        builder.startAuthorizationFromActivity(activity)
                .withLoginHint(requestOptions.getLoginHint())
                .forAccount(requestOptions.getAccount())
                .withResource(requestOptions.getScopes().toLowerCase().trim())
                .withPrompt(requestOptions.getPrompt())
                .withAuthorizationQueryStringParameters(null)
                .withCallback(getAuthenticationCallback(callback))
                .build();

        if (requestOptions.getAuthority() != null && !requestOptions.getAuthority().isEmpty()) {
            builder.fromAuthority(requestOptions.getAuthority());
        }

        final AcquireTokenParameters parameters = builder.build();
        acquireTokenAsyncInternal(parameters);
    }

    abstract void acquireTokenAsyncInternal(@NonNull final AcquireTokenParameters parameters);

    public void acquireTokenSilent(@NonNull RequestOptions requestOptions,
                                   @NonNull INotifyOperationResultCallback<IAuthenticationResult> callback) {
        if (requestOptions.getAccount() == null) {
            callback.showMessage("Account is null.");
            return;
        }

        final AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder();
        builder.withScopes(Arrays.asList(requestOptions.getScopes().toLowerCase().split(" ")))
                .forAccount(requestOptions.getAccount())
                .forceRefresh(requestOptions.forceRefresh())
                .withCallback(getAuthenticationCallback(callback));

        if (requestOptions.getAuthority() != null && !requestOptions.getAuthority().isEmpty()) {
            builder.fromAuthority(requestOptions.getAuthority());
        } else {
            builder.fromAuthority(requestOptions.getAccount().getAuthority());
        }

        if (requestOptions.getAuthScheme() == Constants.AuthScheme.POP) {
            try {
                builder.withAuthenticationScheme(
                        PoPAuthenticationScheme.builder()
                                .withHttpMethod(HttpMethod.GET)
                                .withUrl(
                                        new URL("https://signedhttprequest.azurewebsites.net/api/validateSHR")
                                ).build()
                );
            } catch (MalformedURLException e) {
                callback.showMessage("Unexpected error." + e.getMessage());
                return;
            }
        }

        final AcquireTokenSilentParameters parameters = builder.build();
        acquireTokenSilentAsyncInternal(parameters);
    }

    public void acquireTokenSilentWithResource(@NonNull RequestOptions requestOptions,
                                               @NonNull INotifyOperationResultCallback<IAuthenticationResult> callback) {
        if (requestOptions.getAccount() == null) {
            callback.showMessage("Account is null.");
            return;
        }

        final AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder();
        builder.withResource(requestOptions.getScopes().toLowerCase().trim())
                .forAccount(requestOptions.getAccount())
                .forceRefresh(requestOptions.forceRefresh())
                .withCallback(getAuthenticationCallback(callback))
                .build();

        if (requestOptions.getAuthority() != null && !requestOptions.getAuthority().isEmpty()) {
            builder.fromAuthority(requestOptions.getAuthority());
        } else {
            builder.fromAuthority(requestOptions.getAccount().getAuthority());
        }

        final AcquireTokenSilentParameters parameters = builder.build();
        acquireTokenSilentAsyncInternal(parameters);
    }

    abstract void acquireTokenSilentAsyncInternal(@NonNull final AcquireTokenSilentParameters parameters);

    AuthenticationCallback getAuthenticationCallback(@NonNull final INotifyOperationResultCallback<IAuthenticationResult> callback) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                callback.onSuccess(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                // Check the exception type.
                if (exception instanceof MsalClientException) {
                    // This means errors happened in the sdk itself, could be network, Json parse, etc. Check MsalError.java
                    // for detailed list of the errors.
                    callback.showMessage("MsalClientException.\n" + exception.getMessage());
                } else if (exception instanceof MsalServiceException) {
                    // This means something is wrong when the sdk is communication to the service, mostly likely it's the client
                    // configuration.
                    callback.showMessage("MsalServiceException.\n" + exception.getMessage());
                } else if (exception instanceof MsalArgumentException) {
                    callback.showMessage("MsalArgumentException.\n" + exception.getMessage());
                } else if (exception instanceof MsalUiRequiredException) {
                    // This explicitly indicates that developer needs to prompt the user, it could be refresh token is expired, revoked
                    // or user changes the password; or it could be that no token was found in the token cache.
                    callback.showMessage("MsalDeclinedScopeException.\n" + exception.getMessage());
                } else if (exception instanceof MsalDeclinedScopeException) {
                    // Declined scope implies that not all scopes requested have been granted.
                    // Developer can either continue with Authentication by calling acquireTokenSilent
                    callback.showMessage("MsalDeclinedScopeException.\n" +
                            "Granted Scope:" + ((MsalDeclinedScopeException) exception).getGrantedScopes() + "\n" +
                            "Declined Scope:" + ((MsalDeclinedScopeException) exception).getDeclinedScopes());
                }
            }

            @Override
            public void onCancel() {
                callback.showMessage("User cancelled the flow.");
            }
        };
    }
}
