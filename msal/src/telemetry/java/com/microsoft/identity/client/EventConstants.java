package com.microsoft.identity.client;

final class EventConstants {

    /**
     * Prefixes all event names
     */
    private static final String EVENT_PREFIX = "Microsoft.MSAL.";

    static class EventName {
        static final Telemetry.EventName API_EVENT = new Telemetry.EventName(EVENT_PREFIX + "api_event");
        static final Telemetry.EventName AUTHORITY_VALIDATION_EVENT = new Telemetry.EventName(EVENT_PREFIX + "authority_validation");
        static final Telemetry.EventName HTTP_EVENT = new Telemetry.EventName(EVENT_PREFIX + "http_event");
        static final Telemetry.EventName UI_EVENT = new Telemetry.EventName(EVENT_PREFIX + "ui_event");
        static final Telemetry.EventName TOKEN_CACHE_LOOKUP = new Telemetry.EventName(EVENT_PREFIX + "token_cache_lookup");
        static final Telemetry.EventName TOKEN_CACHE_WRITE = new Telemetry.EventName(EVENT_PREFIX + "token_cache_write");
        static final Telemetry.EventName TOKEN_CACHE_DELETE = new Telemetry.EventName(EVENT_PREFIX + "token_cache_delete");
    }

    static class EventProperty {
        static final String API_ID = EVENT_PREFIX + "api_id";
        static final String START_TIME = EVENT_PREFIX + "start_time";
        static final String STOP_TIME = EVENT_PREFIX + "stop_time";
        static final String RESPONSE_TIME = EVENT_PREFIX + "response_time";
        static final String APPLICATION_NAME = EVENT_PREFIX + "application_name";
        static final String APPLICATION_VERSION = EVENT_PREFIX + "application_version";
        static final String CLIENT_ID = EVENT_PREFIX + "client_id";
        static final String DEVICE_ID = EVENT_PREFIX + "device_id";
        static final String CORRELATION_ID = EVENT_PREFIX + "correlation_id";
        static final String REQUEST_ID = EVENT_PREFIX + "request_id";
        static final String EVENT_NAME = EVENT_PREFIX + "event_name";
        static final String AUTHORITY_NAME = EVENT_PREFIX + "authority";
        static final String AUTHORITY_TYPE = EVENT_PREFIX + "authority_type";
        static final String API_DEPRECATED = EVENT_PREFIX + "is_deprecated"; // Android only
        static final String AUTHORITY_VALIDATION = EVENT_PREFIX + "authority_validation_status";
        static final String PROMPT_BEHAVIOR = EVENT_PREFIX + "prompt_behavior";
        static final String EXTENDED_EXPIRES_ON_SETTING = EVENT_PREFIX + "extended_expires_on_setting";
        static final String WAS_SUCCESSFUL = EVENT_PREFIX + "is_successful";
        static final String API_ERROR_CODE = EVENT_PREFIX + "api_error_code";
        static final String OAUTH_ERROR_CODE = EVENT_PREFIX + "oauth_error_code";
        static final String IDP_NAME = EVENT_PREFIX + "idp";
        static final String TENANT_ID = EVENT_PREFIX + "tenant_id";
        static final String LOGIN_HINT = EVENT_PREFIX + "login_hint";
        static final String USER_ID = EVENT_PREFIX + "user_id";
        static final String REDIRECT_COUNT = EVENT_PREFIX + "redirect_count"; // Android only
        static final String USER_CANCEL = EVENT_PREFIX + "user_cancel";
        static final String TOKEN_TYPE = EVENT_PREFIX + "token_type";
        static final String TOKEN_TYPE_IS_RT = EVENT_PREFIX + "is_rt";
        static final String TOKEN_TYPE_RT = EVENT_PREFIX + "rt"; // Android only
        static final String CACHE_EVENT_COUNT = EVENT_PREFIX + "cache_event_count";
        static final String UI_EVENT_COUNT = EVENT_PREFIX + "ui_event_count";
        static final String HTTP_EVENT_COUNT = EVENT_PREFIX + "http_event_count";
        static final String HTTP_PATH = EVENT_PREFIX + "http_path";
        static final String HTTP_USER_AGENT = EVENT_PREFIX + "user_agent";
        static final String HTTP_METHOD = EVENT_PREFIX + "method";
        static final String HTTP_METHOD_POST = EVENT_PREFIX + "post";
        static final String HTTP_QUERY_PARAMETERS = EVENT_PREFIX + "query_params";
        static final String HTTP_RESPONSE_CODE = EVENT_PREFIX + "response_code";
        static final String HTTP_API_VERSION = EVENT_PREFIX + "api_version";
        static final String REQUEST_ID_HEADER = EVENT_PREFIX + "x_ms_request_id";

        static class Value {
            static final String AUTHORITY_TYPE_ADFS = EVENT_PREFIX + "adfs";
            static final String AUTHORITY_TYPE_AAD = EVENT_PREFIX + "aad";
            static final String AUTHORITY_VALIDATION_SUCCESS = EVENT_PREFIX + "authority_validation_status_success";
            static final String AUTHORITY_VALIDATION_FAILURE = EVENT_PREFIX + "authority_validation_status_failure";
            static final String AUTHORITY_VALIDATION_NOT_DONE = EVENT_PREFIX + "authority_validation_status_not_done";
        }
    }
}
