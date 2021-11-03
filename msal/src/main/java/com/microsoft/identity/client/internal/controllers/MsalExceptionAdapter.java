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

import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalIntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.client.exception.MsalUserCancelException;
import com.microsoft.identity.common.java.exception.ArgumentException;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.exception.UiRequiredException;
import com.microsoft.identity.common.java.exception.UserCancelException;

public class MsalExceptionAdapter {

    public static MsalException msalExceptionFromBaseException(final BaseException e) {
        MsalException msalException = null;

        if (e instanceof MsalException) {
            msalException = (MsalException) e;
        } else if (e instanceof ClientException) {
            final ClientException clientException = ((ClientException) e);
            msalException =
                    new MsalClientException(
                            clientException.getErrorCode(),
                            clientException.getMessage(),
                            clientException);
        } else if (e instanceof ArgumentException) {
            final ArgumentException argumentException = ((ArgumentException) e);
            msalException =
                    new MsalArgumentException(
                            argumentException.getArgumentName(),
                            argumentException.getOperationName(),
                            argumentException.getMessage(),
                            argumentException);
        } else if (e instanceof UiRequiredException) {
            final UiRequiredException uiRequiredException = ((UiRequiredException) e);
            msalException =
                    new MsalUiRequiredException(
                            uiRequiredException.getErrorCode(), uiRequiredException.getMessage());
        } else if (e instanceof IntuneAppProtectionPolicyRequiredException) {
            msalException =
                    new MsalIntuneAppProtectionPolicyRequiredException(
                            (IntuneAppProtectionPolicyRequiredException) e);
        } else if (e instanceof ServiceException) {
            final ServiceException serviceException = ((ServiceException) e);
            msalException =
                    new MsalServiceException(
                            serviceException.getErrorCode(),
                            serviceException.getMessage(),
                            serviceException.getHttpStatusCode(),
                            serviceException);
        } else if (e instanceof UserCancelException) {
            msalException = new MsalUserCancelException();
        }

        if (msalException == null) {
            msalException =
                    new MsalClientException(MsalClientException.UNKNOWN_ERROR, e.getMessage(), e);
        }

        return msalException;
    }
}
