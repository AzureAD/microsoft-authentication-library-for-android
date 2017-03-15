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
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static android.provider.Settings.Secure;
import static com.microsoft.identity.client.EventConstants.EventProperty;

abstract class BaseEvent extends ArrayList<Pair<String, String>> {

    private static String sApplicationName = null;

    private static String sApplicationVersion = "NA";

    private static String sClientId = "NA";

    private static String sDeviceId = "NA";

    private Telemetry.RequestId mRequestId;

    private EventName mEventName;

    BaseEvent() {
        // Keying off Application name not being null to decide if the defaults have been set
        if (sApplicationName != null) {
            setProperty(EventProperty.APPLICATION_NAME, sApplicationName);
            setProperty(EventProperty.APPLICATION_VERSION, sApplicationVersion);
            setProperty(EventProperty.CLIENT_ID, sClientId);
            setProperty(EventProperty.DEVICE_ID, sDeviceId);
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
            sDeviceId = MSALUtils.createHash(Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            sDeviceId = "";
        }

        if (size() == 0) {
            setProperty(EventProperty.APPLICATION_NAME, sApplicationName);
            setProperty(EventProperty.APPLICATION_VERSION, sApplicationVersion);
            setProperty(EventProperty.CLIENT_ID, sClientId);
            setProperty(EventProperty.DEVICE_ID, sDeviceId);
        }
    }

    void setCorrelationId(final String correlationId) {
        add(0, new Pair<>(EventProperty.CORRELATION_ID, correlationId));
    }

    void setRequestId(final Telemetry.RequestId requestId) {
        mRequestId = requestId;
        add(0, new Pair<>(EventProperty.REQUEST_ID, requestId.value));
    }

    final Telemetry.RequestId getRequestId() {
        return mRequestId;
    }

    final void setEventName(EventName eventName) {
        mEventName = eventName;
    }

    final EventName getEventName() {
        return mEventName;
    }

}
