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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static com.microsoft.identity.client.Telemetry.EventName;

class BaseEvent extends ArrayList<Pair<String, String>> {

    /**
     * Prefixes all event names
     */
    private static final String EVENT_PREFIX = "Microsoft.MSAL.";

    static class Names {
        static final EventName API_EVENT = new EventName(EVENT_PREFIX + "api_event");
        static final EventName AUTHORITY_VALIDATION_EVENT = new EventName(EVENT_PREFIX + "authority_validation");
        static final EventName HTTP_EVENT = new EventName(EVENT_PREFIX + "http_event");
        static final EventName BROKER_EVENT = new EventName(EVENT_PREFIX + "broker_event");
        static final EventName UI_EVENT = new EventName(EVENT_PREFIX + "ui_event");
        static final EventName TOKEN_CACHE_LOOKUP = new EventName(EVENT_PREFIX + "token_cache_lookup");
        static final EventName TOKEN_CACHE_WRITE = new EventName(EVENT_PREFIX + "token_cache_write");
        static final EventName TOKEN_CACHE_DELETE = new EventName(EVENT_PREFIX + "token_cache_delete");
    }

    static class Properties {
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
        static final String NTLM = EVENT_PREFIX + "ntlm";
        static final String USER_CANCEL = EVENT_PREFIX + "user_cancel";
        static final String BROKER_APP = EVENT_PREFIX + "broker_app";
        static final String BROKER_VERSION = EVENT_PREFIX + "broker_version";
        static final String BROKER_APP_USED = EVENT_PREFIX + "broker_app_used";
        static final String TOKEN_TYPE = EVENT_PREFIX + "token_type";
        static final String TOKEN_TYPE_IS_RT = EVENT_PREFIX + "is_rt";
        static final String TOKEN_TYPE_IS_MRRT = EVENT_PREFIX + "is_mrrt";
        static final String TOKEN_TYPE_IS_FRT = EVENT_PREFIX + "is_frt";
        static final String TOKEN_TYPE_RT = EVENT_PREFIX + "rt"; // Android only
        static final String TOKEN_TYPE_MRRT = EVENT_PREFIX + "mrrt"; // Android only
        static final String TOKEN_TYPE_FRT = EVENT_PREFIX + "frt"; // Android only
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

        static class Values {
            static final String AUTHORITY_TYPE_ADFS = EVENT_PREFIX + "adfs";
            static final String AUTHORITY_TYPE_AAD = EVENT_PREFIX + "aad";
            static final String AUTHORITY_VALIDATION_SUCCESS = EVENT_PREFIX + "authority_validation_status_success";
            static final String AUTHORITY_VALIDATION_FAILURE = EVENT_PREFIX + "authority_validation_status_failure";
            static final String AUTHORITY_VALIDATION_NOT_DONE = EVENT_PREFIX + "authority_validation_status_not_done";
        }
    }

    private static String sApplicationName = null;

    private static String sApplicationVersion = "NA";

    private static String sClientId = "NA";

    private static String sDeviceId = "NA";

    private Telemetry.RequestId mRequestId;

    BaseEvent() {
        // Keying off Application name not being null to decide if the defaults have been set
        if (sApplicationName != null) {
            setProperty(Properties.APPLICATION_NAME, sApplicationName);
            setProperty(Properties.APPLICATION_VERSION, sApplicationVersion);
            setProperty(Properties.CLIENT_ID, sClientId);
            setProperty(Properties.DEVICE_ID, sDeviceId);
        }
    }

    void setProperty(final String propertyName, final String propertyValue) {
        add(new Pair<>(propertyName, propertyValue));
    }

    String getProperty(final String propertyName) {
        String propertyValue = null;
        for (Pair<String, String> property : this) {
            if (property.first.equals(propertyName)) {
                propertyValue = property.first;
                break;
            }
        }
        return propertyValue;
    }

    int getPropertyCount() {
        return size();
    }

    @SuppressLint("HardwareIds")
    void setDefaults(final Context context, final String clientId) {
        sClientId = clientId;
        sApplicationName = context.getPackageName();
        try {
            sApplicationVersion = context.getPackageManager().getPackageInfo(sApplicationName, 0).versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            sApplicationVersion = "NA";
        }

        try {
            sDeviceId = MSALUtils.createHash(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            sDeviceId = "";
        }

        if (size() == 0) {
            setProperty(Properties.APPLICATION_NAME, sApplicationName);
            setProperty(Properties.APPLICATION_VERSION, sApplicationVersion);
            setProperty(Properties.CLIENT_ID, sClientId);
            setProperty(Properties.DEVICE_ID, sDeviceId);
        }
    }

    void setCorrelationId(final String correlationId) {
        add(0, new Pair<>(Properties.CORRELATION_ID, correlationId));
    }

    void setRequestId(final Telemetry.RequestId requestId) {
        mRequestId = requestId;
        add(0, new Pair<>(Properties.REQUEST_ID, requestId.value));
    }

    Telemetry.RequestId getRequestId() {
        return mRequestId;
    }

}
