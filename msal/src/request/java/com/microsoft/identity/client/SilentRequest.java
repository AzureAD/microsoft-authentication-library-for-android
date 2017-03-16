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

import android.content.Context;

/**
 * Request handling silent flow. Silent flow will try to find a valid RT, if no valid AT exists, it will
 * try to find a RT(all the RTs are multi-scoped), there will only be one entry per authority, clientid and user.
 */
final class SilentRequest extends BaseRequest {
    private static final String TAG = SilentRequest.class.getSimpleName();

    private RefreshTokenCacheItem mRefreshTokenCacheItem;
    private final boolean mForceRefresh;
    private final User mUser;
    private AuthenticationResult mAuthResult;

    SilentRequest(final Context appContext, final AuthenticationRequestParameters authRequestParams,
                  final boolean forceRefresh, final User user) {
        super(appContext, authRequestParams);

        mForceRefresh = forceRefresh;
        mUser = user;
    }

    @Override
    void preTokenRequest() throws MsalException {
        final TokenCache tokenCache = mAuthRequestParameters.getTokenCache();

        // lookup AT first.
        if (!mForceRefresh) {
            final AccessTokenCacheItem accessTokenCacheItem = tokenCache.findAccessToken(mAuthRequestParameters, mUser);
            if (accessTokenCacheItem != null) {
                Logger.info(TAG, mAuthRequestParameters.getRequestContext(), "Access token is found, returning cached AT.");
                mAuthResult = new AuthenticationResult(accessTokenCacheItem);
                return;
            }
        } else {
            Logger.info(TAG, mAuthRequestParameters.getRequestContext(), "ForceRefresh is set to true, skipping AT lookup.");
        }

        mRefreshTokenCacheItem = tokenCache.findRefreshToken(mAuthRequestParameters, mUser);
        if (mRefreshTokenCacheItem == null) {
            Logger.info(TAG, mAuthRequestParameters.getRequestContext(), "No refresh token item is found.");
            throw new MsalUiRequiredException(MsalError.CACHE_MISS, "No refresh token was found. ");
        }
    }

    @Override
    void setAdditionalOauthParameters(final Oauth2Client oauth2Client) {
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.GRANT_TYPE,
                OauthConstants.Oauth2GrantType.REFRESH_TOKEN);
        oauth2Client.addBodyParameter(OauthConstants.Oauth2Parameters.REFRESH_TOKEN, mRefreshTokenCacheItem.getRefreshToken());
    }

    /**
     * For silent request, we check if there is an valid access token first. If there is an valid AT in the cache, no actual
     * perform token request. Otherwise, use the base performTokenRequest. Resiliency feather will be enabled here, if we
     * get the SERVER_ERROR, check for the extended_expires_on and if the token is still valid with extended expires on,
     * return the token.
     */
    @Override
    void performTokenRequest() throws MsalServiceException, MsalClientException {
        // There is an access token returned, don't perform any token request. PostTokenRequest will the stored valid
        // access token.
        if (mAuthResult != null) {
            return;
        }

        // TODO: Support resilency. No need for #BUILD
        super.performTokenRequest();
    }

    /**
     * Return the valid AT. If error happens for request sent to token endpoint, remove the stored refresh token if
     * receiving invalid_grant, and re-wrap the exception with high level error as Interaction_required.
     * @return {@link AuthenticationResult} containing the auth token.
     */
    @Override
    AuthenticationResult postTokenRequest() throws MsalServiceException, MsalUiRequiredException, MsalClientException {
        // if there is an valid access token returned, mAuthResult will already be set
        if (mAuthResult != null) {
            return mAuthResult;
        }

        if (!isAccessTokenReturned()) {
            removeToken();
            throwExceptionFromTokenResponse(mTokenResponse);
        }

        return super.postTokenRequest();
    }

    /**
     * Check the returned token response, if invalid grant is returned, remove the refresh token from cache.
     */
    private void removeToken() {
        final String errorCode = mTokenResponse.getError();
        if (!OauthConstants.ErrorCode.INVALID_GRANT.equalsIgnoreCase(errorCode)) {
            Logger.verbose(TAG, mAuthRequestParameters.getRequestContext(), "Received invalid_grant, removing refresh token");
            return;
        }

        mAuthRequestParameters.getTokenCache().deleteRT(mRefreshTokenCacheItem);
    }
}
