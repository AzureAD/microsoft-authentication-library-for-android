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
package com.microsoft.identity.client.msal.automationapp.sdk;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalUserCancelException;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;
import com.microsoft.identity.client.ui.automation.sdk.IAuthSdk;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryB2CAuthority;

import java.util.ArrayList;
import java.util.List;

/**
 * A Sdk wrapper for Microsoft Authentication Library (MSAL) which implements
 * both the methods of acquire token interactively and silently and returns back the
 * AuthResult, MSAL tests can leverage this sdk for acquiring token with specific
 * parameters and get back the final result.
 */
public class MsalSdk implements IAuthSdk<MsalAuthTestParams> {
    
    @Override
    public MsalAuthResult acquireTokenInteractive(@NonNull MsalAuthTestParams authTestParams, final OnInteractionRequired interactionRequiredCallback, @NonNull final TokenRequestTimeout tokenRequestTimeout) throws Throwable {
        final IPublicClientApplication pca = setupPCA(
                authTestParams.getActivity(),
                authTestParams.getMsalConfigResourceId()
        );

        final ResultFuture<IAuthenticationResult, Exception> future = new ResultFuture<>();

        final AcquireTokenParameters.Builder acquireTokenParametersBuilder = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(authTestParams.getActivity())
                .withLoginHint(authTestParams.getLoginHint())
                .withPrompt(authTestParams.getPromptParameter())
                .fromAuthority(authTestParams.getAuthority())
                .withCallback(getAuthCallback(future));

        if (authTestParams.getScopes() == null || authTestParams.getScopes().isEmpty()) {
            acquireTokenParametersBuilder.withResource(authTestParams.getResource());
        } else {
            acquireTokenParametersBuilder.withScopes(new ArrayList<>(authTestParams.getScopes()));
        }

        if (authTestParams.getClaims() != null) {
            acquireTokenParametersBuilder.withClaims(authTestParams.getClaims());
        }

        final AcquireTokenParameters acquireTokenParameters = acquireTokenParametersBuilder.build();

        pca.acquireToken(acquireTokenParameters);

        interactionRequiredCallback.handleUserInteraction();

        try {
            final IAuthenticationResult result = future.get(tokenRequestTimeout.getTime(), tokenRequestTimeout.getTimeUnit());
            return new MsalAuthResult(result);
        } catch (Exception exception) {
            return new MsalAuthResult(exception);
        }
    }

    @Override
    public MsalAuthResult acquireTokenSilent(@NonNull MsalAuthTestParams authTestParams, @NonNull final TokenRequestTimeout tokenRequestTimeout) throws Throwable {
        final IPublicClientApplication pca = setupPCA(
            authTestParams.getActivity(),
            authTestParams.getMsalConfigResourceId()
        );

        final ResultFuture<IAuthenticationResult, Exception> future = new ResultFuture<>();

        final Authority authority;
        if (authTestParams.getAuthority() != null) {
            authority = Authority.getAuthorityFromAuthorityUrl(authTestParams.getAuthority());
        } else {
            authority = pca.getConfiguration().getDefaultAuthority();
        }

        final IAccount account;

        if (authority instanceof AzureActiveDirectoryB2CAuthority) {
            final String policyName = ((AzureActiveDirectoryB2CAuthority) authority).getB2CPolicyName();
            account = getAccountForPolicyName((MultipleAccountPublicClientApplication) pca, policyName);
        } else {
            account = getAccount(
                    authTestParams.getActivity(),
                    authTestParams.getMsalConfigResourceId(),
                    authTestParams.getLoginHint()
            );
        }

        final AcquireTokenSilentParameters.Builder acquireTokenParametersBuilder = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .forceRefresh(authTestParams.isForceRefresh())
                .fromAuthority(authTestParams.getAuthority())
                .withCallback(getAuthCallback(future));

        if (authTestParams.getScopes() == null || authTestParams.getScopes().isEmpty()) {
            acquireTokenParametersBuilder.withResource(authTestParams.getResource());
        } else {
            acquireTokenParametersBuilder.withScopes(new ArrayList<>(authTestParams.getScopes()));
        }

        if (authTestParams.getClaims() != null) {
            acquireTokenParametersBuilder.withClaims(authTestParams.getClaims());
        }

        final AcquireTokenSilentParameters acquireTokenParameters = acquireTokenParametersBuilder.build();

        pca.acquireTokenSilentAsync(acquireTokenParameters);

        try {
            final IAuthenticationResult result = future.get(tokenRequestTimeout.getTime(), tokenRequestTimeout.getTimeUnit());
            return new MsalAuthResult(result);
        } catch (final Exception exception) {
            return new MsalAuthResult(exception);
        }
    }

    private IPublicClientApplication setupPCA(@NonNull final Context context,
                                              final int msalConfigResourceId) {
        try {
            return PublicClientApplication.create(context, msalConfigResourceId);
        } catch (InterruptedException | MsalException e) {
            throw new AssertionError(e);
        }
    }

    private AuthenticationCallback getAuthCallback(final ResultFuture<IAuthenticationResult, Exception> future) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                future.setResult(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                future.setException(exception);
            }

            @Override
            public void onCancel() {
                future.setException(new MsalUserCancelException());
            }
        };
    }

    public IAccount getAccount(@NonNull final Activity activity,
                                final int msalConfigResourceId,
                                @NonNull final String username) {
        final IPublicClientApplication pca = setupPCA(
                activity,
                msalConfigResourceId
        );

        if (pca instanceof SingleAccountPublicClientApplication) {
            return getAccountForSingleAccountPca((SingleAccountPublicClientApplication) pca);
        } else if (pca instanceof MultipleAccountPublicClientApplication) {
            return getAccountForMultipleAccountPca((MultipleAccountPublicClientApplication) pca, username);
        } else {
            throw new AssertionError("Weird");
        }
    }

    private IAccount getAccountForSingleAccountPca(@NonNull final SingleAccountPublicClientApplication pca) {
        final ResultFuture<IAccount, Exception> future = new ResultFuture<>();

        pca.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                future.setResult(activeAccount);
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                future.setResult(currentAccount);
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                future.setException(exception);
            }
        });

        try {
            return future.get();
        } catch (final Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private IAccount getAccountForMultipleAccountPca(@NonNull final MultipleAccountPublicClientApplication pca,
                                                     final String username) {
        final ResultFuture<IAccount, Exception> future = new ResultFuture<>();

        pca.getAccount(username, new IMultipleAccountPublicClientApplication.GetAccountCallback() {
            @Override
            public void onTaskCompleted(IAccount result) {
                future.setResult(result);
            }

            @Override
            public void onError(MsalException exception) {
                future.setException(exception);
            }
        });

        try {
            return future.get();
        } catch (final Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private IAccount getAccountForPolicyName(@NonNull final MultipleAccountPublicClientApplication pca, @NonNull final String policyName)
    {
        try {
            List<IAccount> accounts = pca.getAccounts();
            for(IAccount account : accounts)
            {
                if (policyName.equals(account.getClaims().get("tfp")))
                {
                    return account;
                }
            }
        } catch (final Exception exception) {
            throw new AssertionError(exception);
        }
        return null;
    }
}
