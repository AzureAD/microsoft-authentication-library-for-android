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

/**
 * Constant value for MSAL internal use.
 */
final class OauthConstants {

    static final class Oauth2Parameters {
        static final String RESPONSE_TYPE = "response_type";
        static final String GRANT_TYPE = "grant_type";
        static final String CLIENT_ID = "client_id";
        static final String REFRESH_TOKEN = "refresh_token";
        static final String REDIRECT_URI = "redirect_uri";
        static final String CODE = "code";
        static final String SCOPE = "scope";
        static final String LOGIN_HINT = "login_hint";
        static final String PROMPT = "prompt";
        static final String RESTRICT_TO_HINT = "restrict_to_hint";
        static final String POLICY = "p";
        static final String HAS_CHROME = "haschrome";
        static final String STATE = "state";
    }

    static final class Oauth2ResponseType {
        static final String CODE = "code";
    }

    static final class Oauth2GrantType {
        static final String AUTHORIZATION_CODE = "authorization_code";
        static final String REFRESH_TOKEN = "refresh_token";
    }

    static final class Oauth2Value {
        static final String SCOPE_EMAIL = "email";
        static final String SCOPE_PROFILE = "profile";
        static final String[] RESERVED_SCOPES = {"openid", SCOPE_EMAIL, SCOPE_PROFILE, "offline_access"};
    }

    static final class PromptValue {
        static final String LOGIN = "login";
        static final String SELECT_ACCOUNT = "select_account";
        // TODO: what do we send for select_account and act_as_current_user?
    }

    /**
     * {@link OauthHeader} contains the constant value for headers related to Oauth2 sent in the http request.
     */
    static final class OauthHeader {

        /** String representing the correlation_id sent in the header. */
        static final String CORRELATION_ID = "client-request-id";

        /** String representing the correlation id returned from server response. */
        static final String CORRELATION_ID_IN_RESPONSE = "return-client-request-id";
    }

    static final class Authorize {
        static final String ERROR = "error";
        static final String ERROR_DESCRIPTION = "error_description";
        static final String ERROR_SUBCODE = "error_subcode";
        static final String CANCEL = "cancel";
    }

    static final class TokenResponseClaim {
        static final String CODE = "code";
        static final String TOKEN_TYPE = "token_type";
        static final String ACCESS_TOKEN = "access_token";
        static final String REFRESH_TOKEN = "refresh_token";
        static final String SCOPE = "scope";
        static final String FAMILY_ID = "foci";
        static final String ID_TOKEN = "id_token";
        static final String EXPIRES_IN = "expires_in";
        static final String ID_TOKEN_EXPIRES_IN = "id_token_expires_in";
        static final String EXTENDED_EXPIRES_IN = "ext_expires_in";
        static final String ERROR = "error";
        static final String ERROR_DESCRIPTION = "error_description";
        static final String ERROR_CODES = "error_codes";
        static final String CORRELATION_ID = "correlation_id";
        static final String STATE = "state";
    }

    static final class ErrorCode {
        static final String INVALID_GRANT = "invalid_grant";
    }
}
