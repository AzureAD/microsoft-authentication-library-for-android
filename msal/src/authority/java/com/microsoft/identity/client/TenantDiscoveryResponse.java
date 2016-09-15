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
 * MSAL internal class for representing the tenant discovery response.
 */
final class TenantDiscoveryResponse extends BaseOauth2Response {

    private final String mAuthorizationEndpoint;
    private final String mTokenEndpoint;

    /**
     * Constructor for creating the {@link TenantDiscoveryResponse} with success response including the authorize
     * endpoint and token endpoint.
     */
    TenantDiscoveryResponse(final String authorizationEndpoint, final String tokenEndpoint) {
        super(null, null, null);
        mAuthorizationEndpoint = authorizationEndpoint;
        mTokenEndpoint = tokenEndpoint;
    }

    /**
     * Constructor for creating {@link TenantDiscoveryResponse} with error response including error and error description.
     */
    TenantDiscoveryResponse(final String error, final String errorDescription, final String[] errorCodes) {
        super(error, errorDescription, errorCodes);

        mAuthorizationEndpoint = null;
        mTokenEndpoint = null;
    }

    /**
     * Constructor for creating the {@link TenantDiscoveryResponse} with error.
     * @param response
     */
    TenantDiscoveryResponse(final BaseOauth2Response response) {
        this(response.getError(), response.getErrorDescription(), response.getErrorCodes());
    }

    /**
     * @return The authorization endpoint after tenant discovery.
     */
    String getAuthorizationEndpoint() {
        return mAuthorizationEndpoint;
    }

    /**
     * @return The token endpoint after tenant discovery.
     */
    String getTokenEndpoint() {
        return mTokenEndpoint;
    }

    /**
     * Create the success {@link TenantDiscoveryResponse} with authorization and token endpoint.
     */
    static TenantDiscoveryResponse createSuccessTenantDiscoveryResponse(final Map<String, String> responseClaims) {
        final String authorizationEndpoint = responseClaims.get(OauthConstants.TenantDiscoveryClaim.AUTHORIZATION_ENDPOINT);
        final String tokenEndpoint = responseClaims.get(OauthConstants.TenantDiscoveryClaim.TOKEN_ENDPOINT);

        return new TenantDiscoveryResponse(authorizationEndpoint, tokenEndpoint);
    }
}
