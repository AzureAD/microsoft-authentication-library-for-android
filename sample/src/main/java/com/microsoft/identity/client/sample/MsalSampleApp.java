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

import com.microsoft.identity.client.ILogger;
import com.microsoft.identity.client.Logger;

/**
 * Created by weij on 3/22/2017.
 */

public class MsalSampleApp extends Application {
    private StringBuffer mLogs;

    @Override
    public void onCreate() {
        super.onCreate();
        mLogs = new StringBuffer();

        Logger.getInstance().setExternalLogger(new ILogger() {
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
