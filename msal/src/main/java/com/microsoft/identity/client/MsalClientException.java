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
 * This exception class represents general errors that are local to the library. Given below is a table of proposed codes and a short description of each.
 * <p>
 *     Set of error codes that could be returned from this exception:
 *     <ul>
 *         <li>multiple_matching_tokens_detected: There are multiple cache entries found, the sdk cannot pick the correct access token
 *         or refresh token from the cache. When this happens, mostly likely it's an bug in the sdk for storing tokens. </li>
 *         <li>device_network_not_available: No active network is available on the device. </li>
 *         <li>json_parse_failure: Indicates that the sdk failed to parse the Json format.</li>
 *         <li>io_error: Indicates that IOException happened, could be the device/network errors. </li>
 *         <li>malformed_url: Indicates that the url is malformed. </li>
 *         <li>unsupported_encoding: Indicates that the encoding is not supported by the device. </li>
 *         <li>no_such_algorithm: Indicates the algorithm used to generate pkce challenge is not supported. </li>
 *         <li>invalid_jwt: JWT returned by the server is not valid, empty or malformed. </li>
 *         <li>state_mismatch: For authorize request, the sdk will verify the state returned from redirect and the one sent
 *         in the request. This error indicates that it doesn't match. </li>
 *         <li>unresolvable_intent: The intent to launch {@link AuthenticationActivity} is not resolvable by the OS or the intent
 *         doesn't contain the required data. </li>
 *         <li>unsupported_url: The url is not supported, Authority URL/URI must be RFC 2396 compliant to use AD FS validation. </li>
 *         <li>authority_validation_not_supported: The authority is not supported for authority validation. The sdk supports
 *         b2c authority, but we don't support b2c authority validation yet. Only well-known host will be supported. </li>
 *         <li>chrome_not_installed: Indicates that chrome is not installed on the device. The sdk uses chrome custom tab for
 *         authorize request if applicable or fall back to chrome browser. </li>
 *         <li>user_mismatch: Indicates that the user provided in the acquire token request doesn't match the user returned from server.</li>
 *     </ul>
 * </p>
 */
public final class MsalClientException extends MsalException {
    /**
     * There are multiple cache entries found, the sdk cannot pick the correct access token
     * or refresh token from the cache. When this happens, mostly likely it's a bug in the sdk for storing tokens.
     */
    public final static String MULTIPLE_MATCHING_TOKENS_DETECTED = "multiple_matching_tokens_detected";

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
    public final static String STATE_MISMATCH = "state_mismatch";

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
    public final static String AUTHORITY_VALIDATION_NOT_SUPPORTED = "authority_validation_not_supported";

    /**
     * Indicates that chrome is not installed on the device. The sdk uses chrome custom tab for authorize request if
     * applicable or fall back to chrome browser.
     */
    public final static String CHROME_NOT_INSTALLED = "chrome_not_installed";

    /**
     * Indicates that the user provided in the acquire token request doesn't match the user returned from server.
     */
    public final static String USER_MISMATCH = "user_mismatch";

    /**
     * Indicates that extra parameters set by the client app is already sent by the sdk.
     */
    public final static String DUPLICATE_QUERY_PARAMETER = "duplicate_query_parameter";

    /**
     * Temporary non-exposed error code to indicate that Adfs authority validation fails. Adfs as authority is not supported
     * for preview.
     */
    final static String ADFS_AUTHORITY_VALIDATION_FAILED = "adfs_authority_validation_failed";

    MsalClientException(final String errorCode) {
        super(errorCode);
    }

    MsalClientException(final String errorCode, final String errorMessage) {
        super(errorCode, errorMessage);
    }

    MsalClientException(final String errorCode, final String errorMessage, final Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }
}
