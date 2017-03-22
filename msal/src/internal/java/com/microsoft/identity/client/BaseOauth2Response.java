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

import java.util.Map;

/**
 * Abstract class to representing the common attributes among TokenResponse, InstanceDiscoveryResponse and TenantDiscoveryResponse.
 */
class BaseOauth2Response {
    private static final String TAG = BaseOauth2Response.class.getSimpleName();

    static final int DEFAULT_STATUS_CODE = 0;
    private final String mError;
    private final String mErrorDescription;
    private final int mHttpStatusCode;

    /**
     * Constructor for {@link BaseOauth2Response}.
     */
    BaseOauth2Response(final String error, final String errorDescription, final int httpStatusCode) {
        mError = error;
        mErrorDescription = errorDescription;
        mHttpStatusCode = httpStatusCode;
    }

    /**
     * @return Error represents the error in the JSON response.
     */
    public String getError() {
        return mError;
    }

    /**
     * @return Error descriptions representing the error_description.
     */
    public String getErrorDescription() {
        return mErrorDescription;
    }

    /**
     * @return The http status code for the error response.
     */
    public int getHttpStatusCode() {
        return mHttpStatusCode;
    }

    static BaseOauth2Response createErrorResponse(final Map<String, String> responseItems, final int httpStatusCode) {
        final String error = responseItems.get(OauthConstants.BaseOauth2ResponseClaim.ERROR);
        final String errorDescription = responseItems.get(OauthConstants.BaseOauth2ResponseClaim.ERROR_DESCRIPTION);

        return new BaseOauth2Response(error, errorDescription, httpStatusCode);
    }
}
