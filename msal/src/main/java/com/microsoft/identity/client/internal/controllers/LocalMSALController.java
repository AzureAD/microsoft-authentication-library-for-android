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
package com.microsoft.identity.client.internal.controllers;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.MsalClientException;
import com.microsoft.identity.client.MsalUiRequiredException;
import com.microsoft.identity.client.internal.authorities.Authority;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class LocalMSALController extends MSALController {

    private AuthorizationStrategy mAuthorizationStrategy = null;
    private AuthorizationRequest mAuthorizationRequest = null;

    @Override
    public AcquireTokenResult acquireToken(final MSALAcquireTokenOperationParameters parameters)
            throws ExecutionException, InterruptedException, ClientException, IOException, MsalClientException {
        final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();

        //0) Get known authority result
        throwIfNetworkNotAvailable(parameters.getAppContext());
        Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parameters.getAuthority());

        //0.1 If not known throw resulting exception
        if (!authorityResult.getKnown()) {
            throw authorityResult.getMsalClientException();
        }

        //1) Get oAuth2Strategy for Authority Type
        final OAuth2Strategy oAuth2Strategy = parameters.getAuthority().createOAuth2Strategy();

        //2) Request authorization interactively
        final AuthorizationResult result = performAuthorizationRequest(oAuth2Strategy, parameters);
        acquireTokenResult.setAuthorizationResult(result);

        if (result.getAuthorizationStatus().equals(AuthorizationStatus.SUCCESS)) {
            //3) Exchange authorization code for token
            final TokenResult tokenResult = performTokenRequest(oAuth2Strategy, mAuthorizationRequest, result.getAuthorizationResponse(), parameters);
            acquireTokenResult.setTokenResult(tokenResult);
            if (tokenResult != null && tokenResult.getSuccess()) {
                //4) Save tokens in token cache
                final ICacheRecord cacheRecord = saveTokens(oAuth2Strategy, mAuthorizationRequest, tokenResult.getTokenResponse(), parameters.getTokenCache());
                acquireTokenResult.setAuthenticationResult(new AuthenticationResult(cacheRecord));
            }
        }

        return acquireTokenResult;
    }

    private AuthorizationResult performAuthorizationRequest(final OAuth2Strategy strategy,
                                                            final MSALAcquireTokenOperationParameters parameters)
            throws ExecutionException, InterruptedException, MsalClientException {
        throwIfNetworkNotAvailable(parameters.getAppContext());

        mAuthorizationStrategy = AuthorizationStrategyFactory.getInstance().getAuthorizationStrategy(parameters.getActivity(), parameters.getAuthorizationAgent());
        mAuthorizationRequest = getAuthorizationRequest(strategy, parameters);

        Future<AuthorizationResult> future = strategy.requestAuthorization(mAuthorizationRequest, mAuthorizationStrategy);

        //We could implement Timeout Here if we wish instead of blocking indefinitely
        //future.get(10, TimeUnit.MINUTES);  // Need to handle timeout exception in the scenario it doesn't return within a reasonable amount of time
        AuthorizationResult result = future.get();

        return result;
    }

    private AuthorizationRequest getAuthorizationRequest(final OAuth2Strategy strategy,
                                                         final MSALOperationParameters parameters) {
        AuthorizationRequest.Builder builder = strategy.createAuthorizationRequestBuilder();

        List<String> msalScopes = new ArrayList<>();
        msalScopes.add("openid");
        msalScopes.add("profile");
        msalScopes.add("offline_access");
        msalScopes.addAll(parameters.getScopes());

        AuthorizationRequest.Builder request = builder
                .setClientId(parameters.getClientId())
                .setRedirectUri(parameters.getRedirectUri())
                .setScope(StringUtil.join(' ', msalScopes));

        if (parameters instanceof MSALAcquireTokenOperationParameters) {
            MSALAcquireTokenOperationParameters acquireTokenOperationParameters = (MSALAcquireTokenOperationParameters) parameters;
            msalScopes.addAll(acquireTokenOperationParameters.getExtraScopesToConsent());

            // Add additional fields to the AuthorizationRequest.Builder to support interactive
            request.setLoginHint(
                    acquireTokenOperationParameters.getLoginHint()
            ).setExtraQueryParam(
                    acquireTokenOperationParameters.getExtraQueryStringParameters()
            ).setPrompt(
                    acquireTokenOperationParameters.getUIBehavior().toString()
            );
        }

        return request.build();
    }

    private TokenResult performTokenRequest(final OAuth2Strategy strategy,
                                            final AuthorizationRequest request,
                                            final AuthorizationResponse response,
                                            final MSALAcquireTokenOperationParameters parameters)
            throws IOException, MsalClientException {
        throwIfNetworkNotAvailable(parameters.getAppContext());

        TokenRequest tokenRequest = strategy.createTokenRequest(request, response);
        tokenRequest.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        TokenResult tokenResult = null;

        tokenResult = strategy.requestToken(tokenRequest);

        return tokenResult;
    }

    void throwIfNetworkNotAvailable(final Context context) throws MsalClientException {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new MsalClientException(
                    MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE,
                    "Device network connection is not available."
            );
        }
    }

    private ICacheRecord saveTokens(final OAuth2Strategy strategy,
                                    final AuthorizationRequest request,
                                    final TokenResponse tokenResponse,
                                    final OAuth2TokenCache tokenCache) throws ClientException {
        return tokenCache.save(strategy, request, tokenResponse);
    }

    @Override
    public void completeAcquireToken(final int requestCode,
                                     final int resultCode,
                                     final Intent data) {
        mAuthorizationStrategy.completeAuthorization(requestCode, resultCode, data);
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(
            final MSALAcquireTokenSilentOperationParameters parameters)
            throws MsalClientException, IOException, ClientException {
        final AcquireTokenResult acquireTokenSilentResult = new AcquireTokenResult();
        final OAuth2TokenCache tokenCache = parameters.getTokenCache();

        final String clientId = parameters.getClientId();
        final String homeAccountId =
                parameters
                        .getAccount()
                        .getHomeAccountIdentifier()
                        .getIdentifier();

        final Account targetAccount = tokenCache.getAccount(
                null, // wildcard (*) - The request environment may not match due to aliasing
                clientId,
                homeAccountId
        );

        final OAuth2Strategy strategy = parameters.getAuthority().createOAuth2Strategy();
        final ICacheRecord cacheRecord = tokenCache.load(
                clientId,
                TextUtils.join(" ", parameters.getScopes()),
                targetAccount
        );

        if (accessTokenIsNull(cacheRecord)
                || refreshTokenIsNull(cacheRecord)
                || parameters.getForceRefresh()) {
            if (!refreshTokenIsNull(cacheRecord)) {
                // No AT found, but the RT checks out, so we'll use it
                parameters.setRefreshToken(cacheRecord.getRefreshToken());

                final TokenResult tokenResult = performSilentTokenRequest(strategy, parameters);
                acquireTokenSilentResult.setTokenResult(tokenResult);

                // TODO Do I need to check the success state of the TokenResult?
                final ICacheRecord savedRecord = tokenCache.save(
                        strategy,
                        getAuthorizationRequest(strategy, parameters),
                        tokenResult.getTokenResponse()
                );

                // Create a new AuthenticationResult to hold the saved record
                final AuthenticationResult authenticationResult = new AuthenticationResult(savedRecord);

                // Set the AuthenticationResult on the final result object
                acquireTokenSilentResult.setAuthenticationResult(authenticationResult);
            } else {
                throw new MsalClientException(MsalUiRequiredException.NO_TOKENS_FOUND, "No refresh token was found. ");
            }
        } else if (cacheRecord.getAccessToken().isExpired()) {
            tokenCache.removeCredential(cacheRecord.getAccessToken());

            parameters.setRefreshToken(cacheRecord.getRefreshToken());

            final TokenResult tokenResult = performSilentTokenRequest(strategy, parameters);
            acquireTokenSilentResult.setTokenResult(tokenResult);

            // TODO Do I need to check the success state of the TokenResult?
            final ICacheRecord savedRecord = tokenCache.save(
                    strategy,
                    getAuthorizationRequest(strategy, parameters),
                    tokenResult.getTokenResponse()
            );

            final AuthenticationResult authenticationResult = new AuthenticationResult(savedRecord);

            acquireTokenSilentResult.setAuthenticationResult(authenticationResult);
        } else {
            // the result checks out, return that....
            acquireTokenSilentResult.setAuthenticationResult(
                    new AuthenticationResult(cacheRecord)
            );
        }

        return acquireTokenSilentResult;
    }

    private boolean refreshTokenIsNull(ICacheRecord cacheRecord) {
        return null == cacheRecord.getRefreshToken();
    }

    private boolean accessTokenIsNull(ICacheRecord cacheRecord) {
        return null == cacheRecord.getAccessToken();
    }

    private TokenResult performSilentTokenRequest(final OAuth2Strategy strategy,
                                                  final MSALAcquireTokenSilentOperationParameters parameters) throws MsalClientException, IOException {
        throwIfNetworkNotAvailable(parameters.getAppContext());
        return strategy.requestToken(
                strategy.createRefreshTokenRequest(
                        parameters.getRefreshToken()
                )
        );
    }
}
