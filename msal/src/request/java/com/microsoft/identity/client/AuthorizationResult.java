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

import android.content.Intent;
import android.net.Uri;

import java.util.Map;

/**
 * MSAL internal response for handling the result from authorize endpoint.
 */
final class AuthorizationResult {

    private final String mAuthCode;
    private final String mState;
    private final AuthorizationStatus mAuthorizationStatus;
    private final String mError;
    private final String mErrorDescription;

    private AuthorizationResult(final String authCode, final String state) {
        mAuthorizationStatus = AuthorizationStatus.SUCCESS;
        mAuthCode = authCode;
        mState = state;

        mError = null;
        mErrorDescription = null;
    }

    private AuthorizationResult(final AuthorizationStatus status, final String error, final String errorDescription) {
        mAuthorizationStatus = status;
        mError = error;
        mErrorDescription = errorDescription;

        mAuthCode = null;
        mState = null;
    }

    public static AuthorizationResult create(int resultCode, final Intent data) {
        if (data == null) {
            return new AuthorizationResult(AuthorizationStatus.FAIL,
                    Constants.MSALError.AUTHORIZATION_FAILED, "receives null intent");
        }

        if (resultCode == Constants.UIResponse.CANCEL) {
            return AuthorizationResult.getAuthorizationResultWithUserCancel();
        } else if (resultCode == Constants.UIResponse.AUTH_CODE_COMPLETE) {
            final String url = data.getStringExtra(Constants.AUTHORIZATION_FINAL_URL);
            return AuthorizationResult.parseAuthorizationResponse(url);
            //CHECKSTYLE:OFF: checkstyle:EmptyBlock
        } else if (resultCode == Constants.UIResponse.AUTH_CODE_ERROR) {
            // TODO: handle to code error case.
            //CHECKSTYLE:ON: checkstyle:EmptyBlock
        }

        return new AuthorizationResult(AuthorizationStatus.FAIL,
                Constants.MSALError.AUTHORIZATION_FAILED, "Unknown result code" + resultCode);
    }

    public static AuthorizationResult parseAuthorizationResponse(final String returnUri) {
        final Uri responseUri = Uri.parse(returnUri);
        final String result = responseUri.getQuery();

        final AuthorizationResult authorizationResult;
        if (MSALUtils.isEmpty(result)) {
            authorizationResult = getAuthorizationResultWithInvalidServerResponse();
        } else {
            final Map<String, String> urlParameters = MSALUtils.decodeUrlToMap(result, "&");
            if (urlParameters.containsKey(OauthConstants.TokenResponseClaim.CODE)) {
                final String state = urlParameters.get(OauthConstants.TokenResponseClaim.STATE);
                if (MSALUtils.isEmpty(state)) {
                    authorizationResult = new AuthorizationResult(AuthorizationStatus.FAIL, Constants.MSALError.AUTHORIZATION_FAILED,
                            Constants.MSALErrorMessage.STATE_NOT_RETURNED);
                } else {
                    authorizationResult = new AuthorizationResult(urlParameters.get(
                            OauthConstants.Oauth2Parameters.CODE), state);
                }
            } else if (urlParameters.containsKey(OauthConstants.Authorize.ERROR)) {
                final String error = urlParameters.get(OauthConstants.Authorize.ERROR);
                final String errorDescription = urlParameters.get(OauthConstants.Authorize.ERROR_DESCRIPTION);

                // TODO: finalize the error handling.
                authorizationResult = new AuthorizationResult(AuthorizationStatus.FAIL, error,
                        errorDescription);
            } else {
                authorizationResult = getAuthorizationResultWithInvalidServerResponse();
            }
        }

        return authorizationResult;
    }

    /**
     * @return The auth code in the redirect uri.
     */
    String getAuthCode() {
        return mAuthCode;
    }

    /**
     * @return The state returned in the authorize redirect with code.
     */
    String getState() {
        return mState;
    }

    /**
     * @return The {@link AuthorizationStatus} indicating the auth status for the request sent to authorize endopoint.
     */
    AuthorizationStatus getAuthorizationStatus() {
        return mAuthorizationStatus;
    }

    /**
     * @return The error string in the query string of the redirect if applicable.
     */
    String getError() {
        return mError;
    }

    String getErrorDescription() {
        return mErrorDescription;
    }

    /**
     * @return {@link AuthorizationResult} with invalid server response when the query string in redirect doesn't contain
     * either code or error.
     */
    static AuthorizationResult getAuthorizationResultWithInvalidServerResponse() {
        return new AuthorizationResult(AuthorizationStatus.FAIL, Constants.MSALError.AUTHORIZATION_FAILED,
                Constants.MSALErrorMessage.AUTHORIZATION_SERVER_INVALID_RESPONSE);
    }

    /**
     * @return {@link AuthorizationResult} indicating that user cancels the flow. If user press the device back button or
     * user clicks on the cancel displayed in the browser.
     */
    static AuthorizationResult getAuthorizationResultWithUserCancel() {
        return new AuthorizationResult(AuthorizationStatus.USER_CANCEL, Constants.MSALError.USER_CANCEL,
                Constants.MSALErrorMessage.USER_CANCELLED_FLOW);
    }

    /**
     * Enum for representing different authorization status.
     */
    enum AuthorizationStatus {
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
        FAIL,

        /**
         * AuthenticationActivity detects the invalid request.
         */
        INVALID_REQUEST
        //TODO:  Investigate how chrome tab returns http timeout error
    }
}
