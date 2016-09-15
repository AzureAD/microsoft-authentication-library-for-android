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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract class to representing the common attributes among TokenResponse, InstanceDiscoveryResponse and TenantDiscoveryResponse.
 */
class BaseOauth2Response {
    private final String mError;
    private final String mErrorDescription;
    private final String[] mErrorCodes;

    /**
     * Constructor for {@link BaseOauth2Response}.
     */
    BaseOauth2Response(final String error, final String errorDescription, final String[] errorCodes) {
        mError = error;
        mErrorDescription = errorDescription;
        mErrorCodes = errorCodes;
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
     * @return Array of error codes.
     */
    public String[] getErrorCodes() {
        return mErrorCodes;
    }

    static BaseOauth2Response createErrorResponse(final Map<String, String> responseItems) throws JSONException {
        final String error = responseItems.get(OauthConstants.BaseOauth2ResponseClaim.ERROR);
        final String errorDescription = responseItems.get(OauthConstants.BaseOauth2ResponseClaim.ERROR_DESCRIPTION);

        final JSONArray errorCodesJsonArray = new JSONArray(responseItems.get(OauthConstants.BaseOauth2ResponseClaim.ERROR_CODES));
        final List<String> errorCodesList = new ArrayList<>();
        for (int i = 0; i < errorCodesJsonArray.length(); i++) {
            final String errorCode = errorCodesJsonArray.getString(i);
            errorCodesList.add(errorCode);
        }

        return new BaseOauth2Response(error, errorDescription, errorCodesList.toArray(new String[errorCodesList.size()]));
    }

    static BaseOauth2Response createSuccessResponse(final Map<String, String> responseItems) {
        if (responseItems.get(OauthConstants.InstanceDiscoveryClaim.TENANT_DISCOVERY_ENDPOINT) != null) {
            return InstanceDiscoveryResponse.createSuccessInstanceDiscoveryResponse(responseItems);
        } else if (responseItems.get(OauthConstants.TenantDiscoveryClaim.AUTHORIZATION_ENDPOINT) != null) {
            return TenantDiscoveryResponse.createSuccessTenantDiscoveryResponse(responseItems);
        } else {
            return TokenResponse.createSuccessTokenResponse(responseItems);
        }
    }
}
