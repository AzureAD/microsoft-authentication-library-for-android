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
 * This exception class represents errors when communicating to service, could be from authorize endpoint or token endpoint. We'll directly
 * read error and error_description from the server response (redirect for authorize endpoint and JSON response from token endpoint). Those
 * errors generally indicate that they cannot be fixed even if going to UI, developer needs to check about the app configuration on the aad.
 * <p>
 *     Set of error codes that could be returned from this exception:
 *     <li>
 *         <ul>invalid_request: This request is missing a required parameter, include an invalid parameter value, include a parameter more than
 *         once or is otherwise malformed.</ul>
 *         <ul>unauthorized_client: The client is not authorized to request an authorization code. </ul>
 *         <ul>access_denied: The resource owner or authorization server denied the request.</ul>
 *         <ul>invalid_scope: The request scope is invalid, unknown or malformed. </ul>
 *         <ul>service_not_available: This is to represent 500/503/504. </ul>
 *         <ul>request_timeout: This is to represent {@link java.net.SocketTimeoutException}. </ul>
 *         <ul>invalid_instance: This is returned when authority validation fails. </ul>
 *         <ul>unknown_error: Request to server failed, but no error and error_description is returned back from the service. </ul>
 *     </li>
 * </p>
 * @Note: {@link MsalServiceException} provides one extra API:
 * <li>
 *     <ul>
 *         {@link MsalServiceException#getHttpStatusCode()} : indicates the http status code for the failed request.
 *     </ul>
 * </li>
 */
public final class MsalServiceException extends MsalException {
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

    private final int mHttpStatusCode;

    /**
     * When {@link java.net.SocketTimeoutException} is thrown, no status code will be caught. Will use 0 instead.
     */
    static int DEFAULT_STATUS_CODE = 0;

    MsalServiceException(final String errorCode, final String errorMessage, final Throwable throwable) {
        super(errorCode, errorMessage, throwable);

        mHttpStatusCode = DEFAULT_STATUS_CODE;
    }

    MsalServiceException(final String errorCode, final String errorMessage, final int httpStatusCode, final Throwable throwable) {
        super(errorCode, errorMessage, throwable);

        mHttpStatusCode = httpStatusCode;
    }

    /**
     * @return The http status code for the request sent to the service.
     */
    public int getHttpStatusCode() {
        return mHttpStatusCode;
    }
}
