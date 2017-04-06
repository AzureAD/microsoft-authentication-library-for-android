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

package com.microsoft.identity.client.sample;

import android.app.Application;

import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;

/**
 * MSAL sample app.
 */

public class MsalSampleApp extends Application {
    private StringBuffer mLogs;

    @Override
    public void onCreate() {
        super.onCreate();
        mLogs = new StringBuffer();

        // Logging can be turned on four different levels: error, warning, info, and verbose. By default the sdk is turning on
        // verbose level logging. Any apps can use Logger.getInstance().setLogLevel(Loglevel) to enable different level of logging.
        Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                // contains PII indicates that if the log message contains PII information. If Pii logging is
                // disabled, the sdk never returns back logs with Pii.
                mLogs.append(message).append('\n');
            }
        });
    }

    String getLogs() {
        return mLogs.toString();
    }

    void clearLogs() {
        mLogs = new StringBuffer();
    }
}
