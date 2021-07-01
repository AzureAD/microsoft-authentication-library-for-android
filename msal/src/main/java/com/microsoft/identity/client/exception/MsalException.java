// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.identity.client.exception;

import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.common.java.exception.BaseException;

/**
 * {@link MsalException} thrown or sent back via callback, representing the detailed exception
 * thrown by the sdk. It will contain the error code, error description (could be null) or
 * throwable (could be null).
 */
public class MsalException extends BaseException {
    /**
     * Default constructor.
     */
    MsalException() {
        super();
    }

    /**
     * Initiates the detailed error code.
     *
     * @param errorCode The error code contained in the exception.
     */
    MsalException(final String errorCode) {
        super(errorCode);
    }

    /**
     * Initiates the {@link MsalException} with error code and error message.
     *
     * @param errorCode    The error code contained in the exception.
     * @param errorMessage The error message contained in the exception.
     */
    MsalException(final String errorCode, final String errorMessage) {
        super(errorCode, errorMessage);
    }

    /**
     * Initiates the {@link MsalException} with error code, error message and throwable.
     *
     * @param errorCode    The error code contained in the exception.
     * @param errorMessage The error message contained in the exception.
     * @param throwable    The {@link Throwable} contains the cause for the exception.
     */
    MsalException(final String errorCode, final String errorMessage,
                         final Throwable throwable) {
        super(errorCode, errorMessage, throwable);
    }

    /**
     * @return The error code for the exception, could be null. {@link MsalException} is the top level base exception, for the
     * constants value of all the error code.
     */
    @Override
    public String getErrorCode() {
        return super.getErrorCode();
    }

    /**
     * {@inheritDoc}
     * Return the detailed description explaining why the exception is returned back.
     */
    @Override
    public String getMessage() {
        if (!MsalUtils.isEmpty(super.getMessage())) {
            return super.getMessage();
        }

        return "";
    }
}
