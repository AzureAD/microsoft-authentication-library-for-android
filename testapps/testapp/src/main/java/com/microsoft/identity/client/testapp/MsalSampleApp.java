//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.
package com.microsoft.identity.client.testapp;

import android.app.Application;
import android.util.Log;

import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryAggregatedObserver;
import com.microsoft.identity.common.internal.telemetry.observers.ITelemetryDefaultObserver;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * MSAL sample app.
 */
public class MsalSampleApp extends Application {
    private static final int LOG_SIZE = 1024 * 1024;
    private StringBuilder mLogs;
    private int mLogSize;

    @Override
    public void onCreate() {
        super.onCreate();
        mLogs = new StringBuilder();

        // Logging can be turned on four different levels: error, warning, info, and verbose. By
        // default the sdk is turning on
        // verbose level logging. Any apps can use Logger.getInstance().setLogLevel(Loglevel) to
        // enable different level of logging.
        Logger.getInstance()
                .setExternalLogger(
                        new ILoggerCallback() {
                            @Override
                            public void log(
                                    String tag,
                                    Logger.LogLevel logLevel,
                                    String message,
                                    boolean containsPII) {
                                // contains PII indicates that if the log message contains PII
                                // information. If Pii logging is
                                // disabled, the sdk never returns back logs with Pii.
                                mLogSize = mLogs.toString().getBytes().length;
                                if (mLogSize + message.getBytes().length >= LOG_SIZE) {
                                    clearLogs();
                                }
                                mLogs.append(message).append('\n');
                            }
                        });

        // to add one observer
        Telemetry.getInstance().addObserver(new TelemetryAggregatedObserver());
        Telemetry.getInstance().addObserver(new TelemetryDefaultObserver());
        // to remove one type of observer
        Telemetry.getInstance().removeObserver(TelemetryDefaultObserver.class);
    }

    class TelemetryDefaultObserver implements ITelemetryDefaultObserver {
        public void onReceived(List<Map<String, String>> telemetryData) {
            for (Map properties : telemetryData) {
                final Iterator iterator = properties.entrySet().iterator();
                while (iterator.hasNext()) {
                    final Map.Entry pair = (Map.Entry) iterator.next();
                    Log.e("", pair.getKey() + ":" + pair.getValue());
                }
                Log.e("", "====================================" + '\n');
            }
        }
    }

    class TelemetryAggregatedObserver implements ITelemetryAggregatedObserver {
        public void onReceived(Map<String, String> telemetryData) {
            final Iterator iterator = telemetryData.entrySet().iterator();
            while (iterator.hasNext()) {
                final Map.Entry pair = (Map.Entry) iterator.next();
                Log.e("", pair.getKey() + ":" + pair.getValue());
            }
            Log.e("", "====================================" + '\n');
        }
    }

    String getLogs() {
        return mLogs.toString();
    }

    void clearLogs() {
        mLogs = new StringBuilder();
        mLogSize = 0;
    }
}
