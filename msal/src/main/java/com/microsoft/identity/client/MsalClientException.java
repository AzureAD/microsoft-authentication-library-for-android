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
 *     <li>
 *         <ul>multiple_cache_entry_found: There are multiple cache entries found, the sdk cannot pick the correct access token
 *         or refresh token from the cache. When this happens, mostly likely it's an bug in the sdk for storing tokens. </ul>
 *         <ul>device_network_not_available: No active network is available on the device. </ul>
 *         <ul>json_parse_failure: Indicates that the sdk failed to parse the Json format.</ul>
 *         <ul>io_error: Indicates that IOException happened, could be the device/network errors. </ul>
 *         <ul>malformed_url: Indicates that the url is malformed. </ul>
 *         <ul>unsupported_encoding: Indicates that the encoding is not supported by the device. </ul>
 *         <ul>no_such_algorithm: Indicates the algorithm used to generate pkce challenge is not supported. </ul>
 *         <ul>invalid_jwt: JWT returned by the server is not valid, empty or malformed. </ul>
 *         <ul>state_not_match: For authorize request, the sdk will verify the state returned from redirect and the one sent
 *         in the request. This error indicates that it doesn't match. </ul>
 *         <ul>unresolvable_intent: The intent to launch {@link AuthenticationActivity} is not resolvable by the OS or the intent
 *         doesn't contain the required data. </ul>
 *         <ul>unsupported_url: The url is not supported, Authority URL/URI must be RFC 2396 compliant to use AD FS validation. </ul>
 *         <ul>unsupported_authority_validation_instance: The authority is not supported for authority validation. The sdk supports
 *         b2c authority, but we don't support b2c authority validation yet. Only well-known host will be supported. </ul>
 *         <ul>chrome_not_installed: Indicates that chrome is not installed on the device. The sdk uses chrome custom tab for
 *         authorize request if applicable or fall back to chrome browser. </ul>
 *         <ul>user_mismatch: Indicates that the user provided in the acquire token request doesn't match the user returned from server.</ul>
 *     </li>
 * </p>
 */
public final class MsalClientException extends MsalException {

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
