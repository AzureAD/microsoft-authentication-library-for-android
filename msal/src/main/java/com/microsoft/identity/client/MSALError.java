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
 * Enum class representing MSAL error code.
 */
public enum MSALError {
    /** Encounter network error and retry fails with 500/503/504. */
    RETRY_FAILED_WITH_SERVER_ERROR("Retry failed with 500/503/504"),

    /** Indicates the general server error for post request to token endpoint. */
    SERVER_ERROR("Server error"),

    /** Indicates the error when failing to parse the returned id token. */
    IDTOKEN_PARSING_FAILURE("Fail to parse Id token"),

    /** Indicates the the encoding scheme is not supported. */
    UNSUPPORTED_ENCODING("Encoding is not supported"),

    /** Indicates the failure to parse the returned JSON response. */
    JSON_PARSE_FAILURE("Failed to parse the Json response"),

    /** Indicates the general error for authentication failure. */
    AUTH_FAILED("Authentication failed"),

    /** Indicates the general error for post request to token endpoint fails with oauth error. */
    OAUTH_ERROR("Auth failed with oath error"),

    /** Indicates the general error for silent request fails. */
    INTERACTION_REQUIRED("Silent request failed, interaction required"),

    /** Indicates that there are multiple cache entries found in the cache. */
    MULTIPLE_CACHE_ENTRY_FOUND("multiple cache entries found"),

    /** Indicates that device is not connected to the network. */
    DEVICE_CONNECTION_NOT_AVAILABLE("Device network connection not available"), 

    /** Indicates the failure for authority validation. */
    AUTHORITY_VALIDATION_FAILED("Authority validation failed"),

    /** Indicates the failure for tenant discovery. */
    TENANT_DISCOVERY_FAILED("Tenant discovery failed");

    private String mErrorDescription;

    /**
     * Initiates {@link MSALError} with error description.
     * @param errorDescription
     */
    MSALError(final String errorDescription) {
        mErrorDescription = errorDescription;
    }

    /**
     * @return Description for the MSAL error.
     */
    public String getDescription() {
        return mErrorDescription;
    }
}
