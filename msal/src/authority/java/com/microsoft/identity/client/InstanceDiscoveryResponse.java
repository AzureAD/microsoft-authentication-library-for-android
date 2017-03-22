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
 * Internal class for representing the instance discovery response.
 */
final class InstanceDiscoveryResponse extends BaseOauth2Response {

    private final String mTenantDiscoveryEndpoint;

    /**
     * Constructor for creating the success response of instance discovery.
     * @param tenantDiscoveryEndpoint The tenant discovery endpoint.
     */
    InstanceDiscoveryResponse(final String tenantDiscoveryEndpoint) {
        super(null, null, BaseOauth2Response.DEFAULT_STATUS_CODE);

        mTenantDiscoveryEndpoint = tenantDiscoveryEndpoint;
    }

    /**
     * Constructor for creating the failure response of instance discovery.
     * @param error Error code representing the failure.
     * @param errorDescription Detailed error description.
     * @param statusCode The http status code related with the request.
     */
    InstanceDiscoveryResponse(final String error, final String errorDescription, final int statusCode) {
        super(error, errorDescription, statusCode);
        mTenantDiscoveryEndpoint = null;
    }

    /**
     * Create the {@link InstanceDiscoveryResponse} with error response.
     */
    InstanceDiscoveryResponse(final BaseOauth2Response response) {
        this(response.getError(), response.getErrorDescription(), response.getHttpStatusCode());
    }

    /**
     * @return The tenant discovery endpoint.
     */
    String getTenantDiscoveryEndpoint() {
        return mTenantDiscoveryEndpoint;
    }

    /**
     * Create the success {@link InstanceDiscoveryResponse}.
     */
    static InstanceDiscoveryResponse createSuccessInstanceDiscoveryResponse(final Map<String, String> responseClaims) {
        final String tenantDiscoveryEndpoint = responseClaims.get(OauthConstants.InstanceDiscoveryClaim.TENANT_DISCOVERY_ENDPOINT);

        return new InstanceDiscoveryResponse(tenantDiscoveryEndpoint);
    }
}
