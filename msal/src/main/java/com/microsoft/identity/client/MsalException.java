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

package com.microsoft.identity.client;

/**
 * {@link MsalException} that will be thrown or sent back via callback, represents the detailed exception
 * thrown by the sdk. It will contain the {@link MSALError}, error description(could be null) or
 *  throwable(could be null).
 */
public class MsalException extends Exception {
    private String mErrorCode;

    /**
     * Default constructor.
     */
    public MsalException() { }

    /**
     * Initiates the {@link MsalException} with {@link MSALError}.
     * @param errorCode The {@link MSALError} contained in the exception.
     */
    public MsalException(final String errorCode) {
        mErrorCode = errorCode;
    }

    /**
     * Initiates the {@link MsalException} with {@link MSALError} and error message.
     * @param errorCode The {@link MSALError} contained in the exception.
     * @param errorMessage The error message contained in the exception.
     */
    public MsalException(final String errorCode, final String errorMessage) {
        super(errorMessage);
        mErrorCode = errorCode;
    }

    /**
     * Initiates the {@link MsalException} with {@link MSALError}, error message and throwable.
     * @param errorCode The {@link MSALError} contained in the exception.
     * @param errorMessage The error message contained in the exception.
     * @param throwable The {@link Throwable} contains the cause for the exception.
     */
    public MsalException(final String errorCode, final String errorMessage,
                         final Throwable throwable) {
        super(errorMessage, throwable);
        mErrorCode = errorCode;
    }

    /**
     * @return The {@link MSALError} for the exception, could be null.
     */
    public String getErrorCode() {
        return mErrorCode;
    }

    /**
     * {@inheritDoc}
     * If the error description is set on the exception, the error message will be returned. Otherwise, if
     * {@link MSALError} is provided, will return the description for the error code. If neither error description nor
     * error code is provided, will return NULL.
     */
    @Override
    public String getMessage() {
        if (!MSALUtils.isEmpty(super.getMessage())) {
            return super.getMessage();
        }

        return "";
    }
}
