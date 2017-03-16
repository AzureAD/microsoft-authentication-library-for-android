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
 *         <ul>server_error: This is to represent 500/503/504 or {@link java.net.SocketTimeoutException}. </ul>
 *     </li>
 * </p>
 */
public final class MsalServiceException extends MsalException {

    private final int mHttpStatusCode;
    private final String mClaims;

    /**
     * When {@link java.net.SocketTimeoutException} is thrown, no status code will be caught. Will use 0 instead.
     */
    static int DEFAULT_STATUS_CODE = 0;

    public MsalServiceException(final String errorCode, final String errorMessage, final Throwable throwable) {
        super(errorCode, errorMessage, throwable);

        mHttpStatusCode = DEFAULT_STATUS_CODE;
        mClaims = "";
    }

    public MsalServiceException(final String errorCode, final String errorMessage, final int httpStatusCode, final Throwable throwable) {
        super(errorCode, errorMessage, throwable);

        mHttpStatusCode = httpStatusCode;
        mClaims = "";
    }

    public MsalServiceException(final String errorCode, final String errorMessge, final int httpStatusCode, final String claims, final Throwable throwable) {
        super(errorCode, errorMessge, throwable);

        mHttpStatusCode = httpStatusCode;
        mClaims = claims;
    }

    public int getHttpStatusCode() {
        return mHttpStatusCode;
    }

    public String getClaims() {
        return mClaims;
    }
}
