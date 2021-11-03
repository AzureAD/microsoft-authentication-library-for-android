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

package com.microsoft.identity.client.exception;

import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.exception.ErrorStrings;

/**
 * This exception indicates that UI is required for authentication to succeed.
 * <p>
 * Error codes that can be returned from this exception:
 * <ul>
 * <li>invalid_grant: The refresh token used to redeem access token is invalid, expired or revoked. </li>
 * <li>no_tokens_found: Access token doesn't exist and no refresh token can be found to redeem access token. </li>
 * </ul>
 * </p>
 */
public final class MsalUiRequiredException extends MsalException {
    /**
     * The refresh token used to redeem access token is invalid, expired, revoked.
     */
    public static final String INVALID_GRANT = OAuth2ErrorCode.INVALID_GRANT;

    /**
     * Access token doesn't exist and there is no refresh token can be found to redeem access token.
     */
    public static final String NO_TOKENS_FOUND = ErrorStrings.NO_TOKENS_FOUND;

    /**
     * The supplied Account cannot be found in the cache.
     */
    public static final String NO_ACCOUNT_FOUND = ErrorStrings.NO_ACCOUNT_FOUND;

    public MsalUiRequiredException(final String errorCode) {
        super(errorCode);
    }

    public MsalUiRequiredException(final String errorCode, final String errorMessage) {
        super(errorCode, errorMessage);
    }

    public MsalUiRequiredException(
            final String errorCode, final String errorMessage, final Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }
}
