// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.ui.automation.logging.LogLevel;
import com.microsoft.identity.client.ui.automation.logging.appender.FileAppender;
import com.microsoft.identity.client.ui.automation.logging.formatter.LogcatLikeFormatter;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;

/**
 * A Junit Rule to enable MSAL logging during automation and set external logger to dump these logs
 * to a separate file.
 */
public class MsalLoggingRule implements TestRule {

    final static String LOG_FOLDER_NAME = "automation";

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final FileAppender msalLogFileAppender = turnOnMsalLogging(description);

                base.evaluate();

                msalLogFileAppender.closeWriter();

                CommonUtils.copyFileToFolderInSdCard(
                        msalLogFileAppender.getLogFile(),
                        LOG_FOLDER_NAME
                );
            }
        };
    }

    private FileAppender turnOnMsalLogging(final Description description) throws IOException {
        final String msalLogFileName = description.getMethodName() + "-msal.log";
        final FileAppender msalfileLogAppender = new FileAppender(msalLogFileName, new LogcatLikeFormatter());
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
        Logger.getInstance().setEnableLogcatLog(false);
        Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                switch (logLevel) {
                    case VERBOSE:
                        msalfileLogAppender.append(LogLevel.VERBOSE, tag, message, null);
                        break;
                    case INFO:
                        msalfileLogAppender.append(LogLevel.INFO, tag, message, null);
                        break;
                    case ERROR:
                        msalfileLogAppender.append(LogLevel.ERROR, tag, message, null);
                        break;
                    case WARNING:
                        msalfileLogAppender.append(LogLevel.WARN, tag, message, null);
                        break;
                }
            }
        });

        return msalfileLogAppender;
    }
}
