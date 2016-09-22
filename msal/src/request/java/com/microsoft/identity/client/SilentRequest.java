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
    void preTokenRequest() throws AuthenticationException {
        final TokenCache tokenCache = mAuthRequestParameters.getTokenCache();

        // lookup AT first.
        if (!mForceRefresh) {
            final TokenCacheItem tokenCacheItem = tokenCache.findAccessToken(mAuthRequestParameters, mUser);
            if (tokenCacheItem != null) {
                mAuthResult = new AuthenticationResult(tokenCacheItem);
                return;
            }
        }

        mRefreshTokenCacheItem = tokenCache.findRefreshToken(mAuthRequestParameters, mUser);
        if (mRefreshTokenCacheItem == null) {
            throw new AuthenticationException(MSALError.INTERACTION_REQUIRED, "RT not found");
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
     * @throws AuthenticationException
     */
    @Override
    void performTokenRequest() throws AuthenticationException {
        // There is an access token returned, don't perform any token request. PostTokenRequest will the stored valid
        // access token.
        if (mAuthResult != null) {
            return;
        }

        try {
            super.performTokenRequest();
        } catch (final AuthenticationException authenticationException) {
            // TODO: resiliency feature
            if (MSALError.SERVER_ERROR.equals(authenticationException.getErrorCode())) {
                // TODO: check if the extended_expires_on is turned on and if the token is valid with extended_expires_on
                // if so, set mAuthResult to the access token item
                return;
            }

            throw authenticationException;
        }
    }

    /**
     * Return the valid AT. If error happens for request sent to token endpoint, remove the stored refresh token if
     * receiving invalid_grant, and re-wrap the exception with high level error as Interaction_required.
     * @return {@link AuthenticationResult} containing the auth token.
     * @throws AuthenticationException
     */
    @Override
    AuthenticationResult postTokenRequest() throws AuthenticationException {
        // if there is an valid access token returned, mAuthResult will already be set
        if (mAuthResult != null) {
            return mAuthResult;
        }

        if (!isAccessTokenReturned()) {
            removeToken();
            throw new AuthenticationException(MSALError.INTERACTION_REQUIRED, "Silent request failed, interaction required",
                    new AuthenticationException(MSALError.OAUTH_ERROR, "ErrorCode: " + mTokenResponse.getError()
                            + "; ErrorDescription: " + mTokenResponse.getErrorDescription()));
        }

        return super.postTokenRequest();
    }

    /**
     * Check the returned token response, if invalid grant is returned, remove the refresh token from cache.
     */
    private void removeToken() {
        final String errorCode = mTokenResponse.getError();
        if (!OauthConstants.ErrorCode.INVALID_GRANT.equalsIgnoreCase(errorCode)) {
            return;
        }

        mAuthRequestParameters.getTokenCache().deleteRT(mRefreshTokenCacheItem);
    }
}
