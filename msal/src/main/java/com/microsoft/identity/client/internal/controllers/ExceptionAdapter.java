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
package com.microsoft.identity.client.internal.controllers;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.client.exception.MsalUserCancelException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;

public class ExceptionAdapter {

    private static final String TAG = ExceptionAdapter.class.getSimpleName();

    public static MsalException exceptionFromAcquireTokenResult(final AcquireTokenResult result) {
        final String methodName = ":exceptionFromAcquireTokenResult";
        final AuthorizationResult authorizationResult = result.getAuthorizationResult();

        if (null != authorizationResult) {
            final AuthorizationErrorResponse authorizationErrorResponse = authorizationResult.getAuthorizationErrorResponse();
            if (!authorizationResult.getSuccess()) {
                //THERE ARE CURRENTLY NO USAGES of INVALID_REQUEST
                switch (result.getAuthorizationResult().getAuthorizationStatus()) {
                    case FAIL:
                        return new MsalServiceException(authorizationErrorResponse.getError(), authorizationErrorResponse.getError() + ";"
                                + authorizationErrorResponse.getErrorDescription(), MsalServiceException.DEFAULT_STATUS_CODE, null);
                    case USER_CANCEL:
                        return new MsalUserCancelException();

                }
            }
        } else {
            Logger.warn(
                    TAG + methodName,
                    "AuthorizationResult was null -- expected for ATS cases."
            );
        }

        final TokenResult tokenResult = result.getTokenResult();
        final TokenErrorResponse tokenErrorResponse;

        if (!result.getTokenResult().getSuccess()) {
            tokenErrorResponse = tokenResult.getErrorResponse();

            if (tokenErrorResponse.getError().equalsIgnoreCase(MsalUiRequiredException.INVALID_GRANT)) {
                Logger.warn(
                        TAG + methodName,
                        "Received invalid_grant"
                );
                return new MsalUiRequiredException(tokenErrorResponse.getError(), tokenErrorResponse.getErrorDescription(), null);
            }

            if (StringUtil.isEmpty(tokenErrorResponse.getError())) {
                Logger.warn(
                        TAG + methodName,
                        "Received unknown error"
                );
                return new MsalServiceException(MsalServiceException.UNKNOWN_ERROR, "Request failed, but no error returned back from service.", null);
            }

            return new MsalServiceException(tokenErrorResponse.getError(), tokenErrorResponse.getErrorDescription(), null);
        }

        return null;
    }

    public static MsalException msalExceptionFromException(final Exception e) {
        MsalException msalException = null;

        if (e instanceof IOException) {
            msalException = new MsalClientException(MsalClientException.IO_ERROR, "An IO error occurred with message: " + e.getMessage(), e);
        }

        if (e instanceof ClientException) {
            msalException = new MsalClientException(((ClientException) e).getErrorCode(), e.getMessage(), e);
        }

        if (msalException == null) {
            msalException = new MsalClientException(MsalClientException.UNKNOWN_ERROR, e.getMessage(), e);
        }

        return msalException;

    }

}
