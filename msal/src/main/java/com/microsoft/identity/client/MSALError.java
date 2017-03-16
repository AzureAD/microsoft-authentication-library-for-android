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
 * Constants class represents all the errors that could be returned by the sdk.
 */
public final class MsalError {

    // Error codes for MsalUiRequiredException
    public final static String INVALID_GRANT = "invalid_grant";

    public final static String CACHE_MISS = "cache_miss";

    // Error codes for MsalClientException
    public final static String MULTIPLE_CACHE_ENTRY_FOUND = "multiple_cache_entry_found";

    public final static String DEVICE_NETWORK_NOT_AVAILABLE = "device_network_not_available";

    public final static String JSON_PARSE_FAILURE = "json_parse_failure";

    public final static String IO_ERROR = "io_error";

    public final static String UNSUPPORTED_ENCODING = "unsupported_encoding";

    public final static String NO_SUCH_ALGORITHM = "no_such_algorithm";

    public final static String INVALID_JWT = "invalid_jwt";

    public final static String STATE_NOT_MATCH = "state_not_match";

    public final static String UNRESOLVABLE_INTENT = "unresolvable_intent";

    public final static String UNSUPPORTED_URL = "unsupported_url";

    public final static String UNSUPPORTED_AUTHORITY_VALIDATION_INSTANCE = "unsupported_authority_validation_instance";

    final static String ADFS_AUTHORITY_VALIDATION_FAILED = "adfs_authority_validation_failed";

    // Errors codes for MsalServiceException
    public final static String INVALID_REQUEST = "invalid_request";

    public final static String UNAUTHORIZED_CLIENT = "unauthorized_client";

    public final static String ACCESS_DENIED = "access_denied";

    public final static String INVALID_SCOPE = "invalid_scope";

    public final static String SERVER_ERROR = "server_error";

    public final static String UNKNOWN_ERROR = "unknown_error";

//    /**
//     * Encounter network error and retry fails with 500/503/504.
//     */
//    RETRY_FAILED_WITH_SERVER_ERROR("Retry failed with 500/503/504"),
//
//    /**
//     * Indicates the general server error for post request to token endpoint.
//     */
//    SERVER_ERROR("Server error"),
//
//    /**
//     * Indicates the error when failing to parse the returned id token.
//     */
//    IDTOKEN_PARSING_FAILURE("Fail to parse Id token"),
//
//    /**
//     * Indicates the the encoding scheme is not supported.
//     */
//    UNSUPPORTED_ENCODING("Encoding is not supported"),
//
//    /**
//     * Indicates the failure to parse the returned JSON response.
//     */
//    JSON_PARSE_FAILURE("Failed to parse the Json response"),
//
//    /**
//     * Indicates the general error for authentication failure.
//     */
//    AUTH_FAILED("Authentication failed"),
//
//    /**
//     * Indicates the general error for post request to token endpoint fails with oauth error.
//     */
//    OAUTH_ERROR("Auth failed with oath error"),
//
//    /**
//     * Indicates the general error for silent request fails.
//     */
//    INTERACTION_REQUIRED("Ui is required for authentication to succeed."),
//
//    /**
//     * Indicates that there are multiple cache entries found in the cache.
//     */
//    MULTIPLE_CACHE_ENTRY_FOUND("multiple cache entries found"),
//
//    /**
//     * Indicates that device is not connected to the network.
//     */
//    DEVICE_CONNECTION_NOT_AVAILABLE("Device network connection not available"),
//
//    /**
//     * Indicates the failure for authority validation.
//     */
//    AUTHORITY_VALIDATION_FAILED("Authority validation failed"),
//
//    /**
//     * Indicates that authority validation is not supported for the passed in authority.
//     */
//    UNSUPPORTED_AUTHORITY_VALIDATION("Unsupported authority validation"),
//
//    /**
//     * Indicates the failure for tenant discovery.
//     */
//    TENANT_DISCOVERY_FAILED("Tenant discovery failed"),
//
//    /**
//     * Indicates PKCE Challenge could not be created due to unimplemented hash.
//     */
//    NO_SUCH_ALGORITHM("PKCE Challenge could not be completed because device lacks SHA-256 digest algorithm implementation");
}
