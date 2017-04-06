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
 * Constants class represents all the errors that could be returned by the sdk.
 */
public final class MSALError {

    // Error codes for MsalUiRequiredException
    /**
     * The refresh token used to redeem access token is invalid, expired, revoked.
     */
    public final static String INVALID_GRANT = "invalid_grant";

    /**
     * Access token doesn't exist and there is no refresh token can be found to redeem access token.
     */
    public final static String NO_TOKENS_FOUND = "no_tokens_found";

    // Error codes for MsalClientException
    /**
     * There are multiple cache entries found, the sdk cannot pick the correct access token
     * or refresh token from the cache. When this happens, mostly likely it's an bug in the sdk for storing tokens.
     */
    public final static String MULTIPLE_CACHE_ENTRY_FOUND = "multiple_cache_entry_found";

    /**
     * No active network is available on the device.
     */
    public final static String DEVICE_NETWORK_NOT_AVAILABLE = "device_network_not_available";

    /**
     * Indicates that the sdk failed to parse the Json format
     */
    public final static String JSON_PARSE_FAILURE = "json_parse_failure";

    /**
     * Indicates that IOException happened, could be the device/network errors.
     */
    public final static String IO_ERROR = "io_error";

    /**
     * Indicates that the url is malformed.
     */
    public final static String MALFORMED_URL = "malformed_url";

    /**
     * Indicates that the encoding is not supported by the device.
     */
    public final static String UNSUPPORTED_ENCODING = "unsupported_encoding";

    /**
     * Indicates the algorithm used to generate pkce challenge is not supported.
     */
    public final static String NO_SUCH_ALGORITHM = "no_such_algorithm";

    /**
     * JWT returned by the server is not valid, empty or malformed.
     */
    public final static String INVALID_JWT = "invalid_jwt";

    /**
     * For authorize request, the sdk will verify the state returned from redirect and the one sent in the request.
     * This error indicates that it doesn't match.
     */
    public final static String STATE_NOT_MATCH = "state_not_match";

    /**
     * The intent to launch {@link AuthenticationActivity} is not resolvable by the OS or the intent doesn't contain the required data.
     */
    public final static String UNRESOLVABLE_INTENT = "unresolvable_intent";

    /**
     * The url is not supported, Authority URL/URI must be RFC 2396 compliant to use AD FS validation.
     */
    public final static String UNSUPPORTED_URL = "unsupported_url";

    /**
     * The authority is not supported for authority validation. The sdk supports b2c authority, but we don't support b2c authority validation yet.
     * Only well-known host will be supported.
     */
    public final static String UNSUPPORTED_AUTHORITY_VALIDATION_INSTANCE = "unsupported_authority_validation_instance";

    /**
     * Indicates that chrome is not installed on the device. The sdk uses chrome custom tab for authorize request if
     * applicable or fall back to chrome browser.
     */
    public final static String CHROME_NOT_INSTALLED = "chrome_not_installed";

    /**
     * Indicates that the user provided in the acquire token request doesn't match the user returned from server.
     */
    public final static String USER_MISMATCH = "user_mismatch";

    final static String ADFS_AUTHORITY_VALIDATION_FAILED = "adfs_authority_validation_failed";

    // Errors codes for MsalServiceException
    /**
     * This request is missing a required parameter, include an invalid parameter value, include a parameter more than
     * once or is otherwise malformed
     */
    public final static String INVALID_REQUEST = "invalid_request";

    /**
     * The client is not authorized to request an authorization code.
     */
    public final static String UNAUTHORIZED_CLIENT = "unauthorized_client";

    /**
     * The resource owner or authorization server denied the request.
     */
    public final static String ACCESS_DENIED = "access_denied";

    /**
     * The request scope is invalid, unknown or malformed.
     */
    public final static String INVALID_SCOPE = "invalid_scope";

    /**
     * This is to represent 500/503/504.
     */
    public final static String SERVICE_NOT_AVAILABLE = "service_not_available";

    /**
     * This is to represent {@link java.net.SocketTimeoutException}.
     */
    public final static String REQUEST_TIMEOUT = "request_timeout";

    /**
     * This is returned when authority validation fails.
     */
    public final static String INVALID_INSTANCE = "invalid_instance";

    /**
     * Request to server failed, but no error and error_description is returned back from the service。
     */
    public final static String UNKNOWN_ERROR = "unknown_error";
}