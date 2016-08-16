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

import android.net.Uri;
import android.util.Base64;

import java.util.Map;

/**
 * MSAL internal response for handling the result from authorize endpoint.
 */
final class AuthorizationResult {

    private final String mAuthCode;
    private final AuthorizationStatus mAuthorizationStatus;
    private final String mError;
    private final String mErrorDescription;

    private AuthorizationResult(final String authCode) {
        mAuthorizationStatus = AuthorizationStatus.SUCCESS;
        mAuthCode = authCode;

        mError = null;
        mErrorDescription = null;
    }

    private AuthorizationResult(final AuthorizationStatus status, final String error, final String errorDescription) {
        mAuthorizationStatus = status;
        mError = error;
        mErrorDescription = errorDescription;

        mAuthCode = null;
    }

    public static AuthorizationResult parseAuthorizationResponse(final String returnUri) {
        final Uri responseUri = Uri.parse(returnUri);
        final String result = responseUri.getQuery();

        final AuthorizationResult authorizationResult;
        if (MSALUtils.isEmpty(result)) {
            authorizationResult = getAuthorizationResultWithInvalidServerResponse();
        } else {
            final Map<String, String> urlParameters = MSALUtils.decodeUrlToMap(result, "&");
//            if (!urlParameters.containsKey("state")) {
//                authorizationResult = getAuthorizationResultWithInvalidServerResponse();
//            }else
            // TODO: append state
            if (urlParameters.containsKey(OauthConstants.TokenResponseClaim.CODE)) {
                authorizationResult = new AuthorizationResult(urlParameters.get(OauthConstants.Oauth2Parameters.CODE));
            } else if (urlParameters.containsKey(OauthConstants.TokenResponseClaim.ERROR)) {
                final String error = urlParameters.get(OauthConstants.TokenResponseClaim.ERROR);
                final String errorDescription = urlParameters.get(OauthConstants.TokenResponseClaim.ERROR_DESCRIPTION);
                authorizationResult = new AuthorizationResult(AuthorizationStatus.PROTOCOL_ERROR, error,
                        errorDescription);
            } else {
                authorizationResult = getAuthorizationResultWithInvalidServerResponse();
            }
        }

        return authorizationResult;
    }

    String getAuthCode() {
        return mAuthCode;
    }

    AuthorizationStatus getAuthorizationStatus() {
        return mAuthorizationStatus;
    }

    String getError() {
        return mError;
    }

    String getErrorDescription() {
        return mErrorDescription;
    }

    static AuthorizationResult getAuthorizationResultWithInvalidServerResponse() {
        return new AuthorizationResult(AuthorizationStatus.UNKNOWN, Constants.MSALError.AUTHORIZATION_FAILED,
                Constants.MSALErrorMessage.AUTHORIZATION_SERVER_INVALID_RESPONSE);
    }

    static AuthorizationResult getAuthorizationResultWithUserCancel() {
        return new AuthorizationResult(AuthorizationStatus.USER_CANCEL, Constants.MSALError.USER_CANCEL,
                Constants.MSALErrorMessage.USER_CANCELLED_FLOW);
    }

    private String decodeState(final String encodedState) {
        if (MSALUtils.isEmpty(encodedState)) {
            return null;
        }

        final byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING |Base64.URL_SAFE);
        return new String(stateBytes);
    }

    static enum AuthorizationStatus {
        /**
         * Code is successfully returned.
         */
        SUCCESS,

        /**
         * User press device back button.
         */
        USER_CANCEL,

        /**
         * Returned URI contains error.
         */
        PROTOCOL_ERROR,

        UNKNOWN
        //TODO:  Investigate how chrome tab returns http timeout error
    }
}
