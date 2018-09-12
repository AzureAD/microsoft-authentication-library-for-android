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

package com.microsoft.identity.client.internal.telemetry;

/**
 * Constants used in the handling of telemetry data.
 */
public final class EventConstants {

    private EventConstants() {
        // Utility class
    }

    /**
     * Prefixes all event names.
     */
    public static final String EVENT_PREFIX = "msal.";

    /**
     * API Ids for Telemetry.
     */
    public static final class ApiId {

        private ApiId() {
            // Utility class
        }

        public static final String API_ID_ACQUIRE = "179";
        public static final String API_ID_ACQUIRE_WITH_HINT = "180";
        public static final String API_ID_ACQUIRE_WITH_HINT_BEHAVIOR_AND_PARAMETERS = "181";
        public static final String API_ID_ACQUIRE_WITH_HINT_BEHAVIOR_PARAMETERS_AND_AUTHORITY = "182";
        public static final String API_ID_ACQUIRE_WITH_USER_BEHAVIOR_AND_PARAMETERS = "183";
        public static final String API_ID_ACQUIRE_WITH_USER_BEHAVIOR_PARAMETERS_AND_AUTHORITY = "184";
        public static final String ACQUIRE_TOKEN_SILENT_ASYNC_WITH_USER = "80";
        public static final String ACQUIRE_TOKEN_SILENT_ASYNC_WITH_USER_AUTHORITY_AND_FORCE_REFRESH = "81";
    }

    public static final class EventName {

        private EventName() {
            // Utility class
        }

        public static final String DEFAULT_EVENT = EVENT_PREFIX + "default";
        public static final String API_EVENT = EVENT_PREFIX + "api_event";
        public static final String AUTHORITY_VALIDATION_EVENT = EVENT_PREFIX + "authority_validation";
        public static final String HTTP_EVENT = EVENT_PREFIX + "http_event";
        public static final String UI_EVENT = EVENT_PREFIX + "ui_event";
        public static final String TOKEN_CACHE_LOOKUP = EVENT_PREFIX + "token_cache_lookup";
        public static final String TOKEN_CACHE_WRITE = EVENT_PREFIX + "token_cache_write";
        public static final String TOKEN_CACHE_DELETE = EVENT_PREFIX + "token_cache_delete";
    }

    /**
     * Properties used by Event key/value pairs.
     */
    public static final class EventProperty {

        private EventProperty() {
            // Utility class
        }

        //DefaultEvent
        public static final String APPLICATION_NAME = EVENT_PREFIX + "application_name";
        public static final String APPLICATION_VERSION = EVENT_PREFIX + "application_version";
        public static final String CLIENT_ID = EVENT_PREFIX + "client_id";
        public static final String DEVICE_ID = EVENT_PREFIX + "device_id";
        public static final String SDK_VERSION = EVENT_PREFIX + "sdk_version";
        public static final String SDK_PLATFORM = EVENT_PREFIX + "sdk_platform";

        // Common
        public static final String START_TIME = EVENT_PREFIX + "start_time";
        public static final String STOP_TIME = EVENT_PREFIX + "stop_time";
        public static final String ELAPSED_TIME = EVENT_PREFIX + "elapsed_time";
        public static final String EVENT_NAME = EVENT_PREFIX + "event_name";

        // ApiEvent
        public static final String API_ID = EVENT_PREFIX + "api_id";
        public static final String CORRELATION_ID = EVENT_PREFIX + "correlation_id";
        public static final String REQUEST_ID = EVENT_PREFIX + "request_id";
        public static final String AUTHORITY_NAME = EVENT_PREFIX + "authority";
        public static final String AUTHORITY_TYPE = EVENT_PREFIX + "authority_type";
        public static final String AUTHORITY_VALIDATION = EVENT_PREFIX + "authority_validation_status";
        public static final String UI_BEHAVIOR = EVENT_PREFIX + "ui_behavior";
        public static final String WAS_SUCCESSFUL = EVENT_PREFIX + "is_successful";
        public static final String IDP_NAME = EVENT_PREFIX + "idp";
        public static final String TENANT_ID = EVENT_PREFIX + "tenant_id";
        public static final String LOGIN_HINT = EVENT_PREFIX + "login_hint";
        public static final String USER_ID = EVENT_PREFIX + "user_id";
        public static final String API_ERROR_CODE = EVENT_PREFIX + "api_error_code";

        // CacheEvent
        public static final String TOKEN_TYPE = EVENT_PREFIX + "token_type";
        public static final String IS_AT = EVENT_PREFIX + "is_at";
        public static final String IS_RT = EVENT_PREFIX + "is_rt";

        // HttpEvent
        public static final String OAUTH_ERROR_CODE = EVENT_PREFIX + "oauth_error_code";
        public static final String HTTP_PATH = EVENT_PREFIX + "http_path";
        public static final String HTTP_USER_AGENT = EVENT_PREFIX + "user_agent";
        public static final String HTTP_METHOD = EVENT_PREFIX + "method";
        public static final String HTTP_QUERY_PARAMETERS = EVENT_PREFIX + "query_params";
        public static final String HTTP_RESPONSE_CODE = EVENT_PREFIX + "response_code";
        public static final String HTTP_API_VERSION = EVENT_PREFIX + "api_version";
        public static final String REQUEST_ID_HEADER = EVENT_PREFIX + "x_ms_request_id";

        // UiEvent
        public static final String USER_CANCEL = EVENT_PREFIX + "user_cancel";

        /**
         * Frequently occurring values of telemetry key/value pairs.
         */
        public static final class Value {

            private Value() {
                // Utility class
            }

            public static final String AUTHORITY_TYPE_ADFS = "adfs";
            public static final String AUTHORITY_TYPE_AAD = "aad";
            public static final String AUTHORITY_TYPE_B2C = "b2c";
            public static final String AUTHORITY_TYPE_UNKNOWN = "unknown";
            public static final String AUTHORITY_VALIDATION_SUCCESS = "authority_validation_status_success";
            public static final String AUTHORITY_VALIDATION_FAILURE = "authority_validation_status_failure";
            public static final String AUTHORITY_VALIDATION_NOT_DONE = "authority_validation_status_not_done";
            public static final String HTTP_METHOD_POST = "post";
        }
    }
}
