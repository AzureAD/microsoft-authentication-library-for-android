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

class BaseEvent extends ArrayList<Pair<String, String>> {

    static class Properties {
        /**
         * Prefixes all event names
         */
        private static final String EVENT_PREFIX = "Microsoft.MSAL.";

        /////////////////
        // The properties
        /////////////////
        static final String START_TIME = EVENT_PREFIX + "start_time";
        static final String STOP_TIME = EVENT_PREFIX + "stop_time";
        static final String RESPONSE_TIME = EVENT_PREFIX + "response_time";
        static final String APPLICATION_NAME = EVENT_PREFIX + "application_name";
        static final String APPLICATION_VERSION = EVENT_PREFIX + "application_version";
        static final String CLIENT_ID = EVENT_PREFIX + "client_id";
        static final String DEVICE_ID = EVENT_PREFIX + "device_id";
        static final String CORRELATION_ID = EVENT_PREFIX + "correlation_id";
        static final String REQUEST_ID = EVENT_PREFIX + "request_id";
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

}
