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

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.ui.automation.logging.LogLevel;
import com.microsoft.identity.client.ui.automation.logging.appender.FileAppender;
import com.microsoft.identity.client.ui.automation.logging.formatter.LogcatLikeFormatter;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.common.java.util.ThreadUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * A Junit Rule to enable MSAL logging during automation and set external logger to dump these logs
 * to a separate file.
 */
public class MsalLoggingRule implements TestRule {

    final static String LOG_FOLDER_NAME = "automation";

    public static final String TAG = MsalLoggingRule.class.getSimpleName();

    private static final MsalLoggingOptions defaultLoggingOptions = new MsalLoggingOptions() {
        @Override
        public boolean logcatEnabled() {
            return false;
        }

        @Override
        public boolean logFileEnabled() {
            return true;
        }

        @Override
        public Logger.LogLevel logLevel() {
            return Logger.LogLevel.VERBOSE;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return MsalLoggingOptions.class;
        }
    };

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final MsalLoggingOptions loggingOptions = Optional
                        .of(description.getAnnotation(MsalLoggingOptions.class))
                        .orElse(defaultLoggingOptions);

                final FileAppender msalLogFileAppender = turnOnMsalLogging(description, loggingOptions);

                try {
                    base.evaluate();
                } finally {
                    // MSAL (common) logger logs using a background thread, so even though the test is
                    // finished at this point, we may still be receiving logs from the logger. If we
                    // close the stream right now we might the lose the last bit of logs and we might
                    // encounter an IoException when trying to write that last bit of logs to the file
                    // as the stream was closed.
                    // To mitigate it we would just sleep for a tiny bit of time to ensure that we grab
                    // those last bit of logs, dump them to the file and then close the writer.
                    ThreadUtils.sleepSafely(
                            Math.toIntExact(TimeUnit.SECONDS.toMillis(1)),
                            TAG,
                            "Error while sleeping during saving logs."
                    );

                    msalLogFileAppender.closeWriter();

                    CommonUtils.copyFileToFolderInSdCard(
                            msalLogFileAppender.getLogFile(),
                            LOG_FOLDER_NAME
                    );
                }
            }
        };
    }

    private FileAppender turnOnMsalLogging(
            final Description description,
            final MsalLoggingOptions loggingOptions
    ) throws IOException {
        final String msalLogFileName = description.getMethodName() + "-msal.log";
        final FileAppender msalFileLogAppender = new FileAppender(msalLogFileName, new LogcatLikeFormatter());
        Logger.getInstance().setLogLevel(loggingOptions.logLevel());
        Logger.getInstance().setEnableLogcatLog(loggingOptions.logFileEnabled());

        if (loggingOptions.logFileEnabled()) {
            Logger.getInstance().setExternalLogger(new ILoggerCallback() {
                @Override
                public void log(final String tag, final Logger.LogLevel logLevel,
                                final String message, boolean containsPII) {
                    final LogLevel level = convertMsalLogLevelToInternalLogLevel(logLevel);
                    msalFileLogAppender.append(level, tag, message, null);
                }
            });
        }

        return msalFileLogAppender;
    }

    private LogLevel convertMsalLogLevelToInternalLogLevel(
            @NonNull final Logger.LogLevel logLevel) {
        switch (logLevel) {
            case VERBOSE:
                return LogLevel.VERBOSE;
            case WARNING:
                return LogLevel.WARN;
            case ERROR:
                return LogLevel.ERROR;
            default:
                return LogLevel.INFO;
        }
    }
}
