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

import android.os.Build;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * MSAL Logger for diagnostic purpose. The sdk can generate logs with both the default logcat logging or the external logger.
 * By default, the SDK enables the logcat logging. To turn it off, developer needs to explicitly do:
 * <code>
 *     Logger.getInstance().setEnableLogcatLog(false);
 * </code>
 *
 * To enable the custom logger, developer needs to explicitly set the external logger implementing
 * the {@link ILogger}.
 * <code>
 *     Logger.getInstance().setExternalLogger(new Logger.ILogger() {
 *     @Override
 *     public void log(String tag, Logger.LogLevel logLevel, String message,
 *         String additionalMessage) { }
 *     });
 * </code>
 *
 * Loglevel can be specified at {@link LogLevel#ERROR}, {@link LogLevel#WARNING}, {@link LogLevel#INFO}
 * and {@link LogLevel#VERBOSE}. The sdk enables the verbose level logging by default, to set different
 * level logging, developer needs to:
 * <code>
 *     Logger.getInstance().setLogLevel({@link LogLevel})
 * </code>
 */
public final class Logger {
    private static final Logger INSTANCE = new Logger();
    static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Turn on the verbose level logging by default.
    private LogLevel mLogLevel = LogLevel.INFO;
    private ILogger mExternalLogger;
    private boolean mLogcatLogEnabled = true;

    /**
     * @return The single instance for {@link Logger}.
     */
    public static Logger getInstance() {
        return INSTANCE;
    }

    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level logging.
     * @param logLevel The {@link LogLevel} to be enabled for the diagnostic logging.
     */
    public void setLogLevel(final LogLevel logLevel) {
        mLogLevel = logLevel;
    }

    /**
     * Set the custom logger. Developer can configure external logging to configure a callback that
     * the sdk will use to hand off each log message as it is generated.
     * @param externalLogger The reference to the {@link ILogger} that the developer can config to
     *                       generate the log to the designated places.
     */
    public void setExternalLogger(final ILogger externalLogger) {
        mExternalLogger = externalLogger;
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk enables it.
     * @param enableLogcatLog True if enabling the logcat logging, false otherwise.
     */
    public void setEnableLogcatLog(final boolean enableLogcatLog) {
        mLogcatLogEnabled = enableLogcatLog;
    }

    /**
     * Send an {@link LogLevel#ERROR} log message.
     */
    static void error(final String tag, final UUID correlationId, final String errorMessage,
                      final String additionalMessage, final Throwable exception) {
        getInstance().log(tag, LogLevel.ERROR, correlationId, errorMessage, additionalMessage, exception);
    }

    /**
     * Send a {@link LogLevel#WARNING} log message.
     */
    static void warning(final String tag, final UUID correlationId, final String message,
                        final String additionalMessage) {
        getInstance().log(tag, LogLevel.WARNING, correlationId, message, additionalMessage, null);
    }

    /**
     * Send a {@link LogLevel#INFO} log message.
     */
    static void info(final String tag, final UUID correlationId, final String message,
                     final String additionalMessage) {
        getInstance().log(tag, LogLevel.INFO, correlationId, message, additionalMessage, null);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message.
     */
    static void verbose(final String tag, final UUID correlationId, final String message,
                        final String additionalMessage) {
        getInstance().log(tag, LogLevel.VERBOSE, correlationId, message, additionalMessage, null);
    }

    /**
     * Format the log message. Depends on the developer setting, the log message could be sent to logcat
     * or the external logger set by the calling app.
     */
    private void log(final String tag, final LogLevel logLevel, final UUID correlationId,
                     final String message, final String additionalMessage,
                     final Throwable throwable) {
        if (logLevel.compareTo(mLogLevel) > 0) {
            return;
        }

        final String updatedAdditionalMessage = MSALUtils.isEmpty(additionalMessage) ? "" : additionalMessage;
        final StringBuilder logMessage = new StringBuilder();
        logMessage.append(formatMessage(correlationId, message));

        // Adding stacktrace to message
        if (throwable != null) {
            logMessage.append(' ').append(Log.getStackTraceString(throwable));
        }

        if (mLogcatLogEnabled) {
            sendLogcatLogs(tag, logLevel, logMessage.toString(), updatedAdditionalMessage);
        }

        mExternalLogger.log(tag, logLevel, logMessage.toString(), updatedAdditionalMessage);
    }

    /**
     * Send logs to logcat as the default logging if developer doesn't turn of the logcat logging.
     */
    private void sendLogcatLogs(final String tag, final LogLevel logLevel, final String message,
                                final String additionalMessage) {
        // Append additional message to the message part for logcat logging
        final String logcatMessage = message + (MSALUtils.isEmpty(additionalMessage)
                ? "N/A" : " " + additionalMessage);
        switch (logLevel) {
            case ERROR:
                Log.e(tag, logcatMessage);
                break;
            case WARNING:
                Log.w(tag, logcatMessage);
                break;
            case INFO:
                Log.i(tag, logcatMessage);
                break;
            case VERBOSE:
                Log.v(tag, logcatMessage);
                break;
            default:
                throw new IllegalArgumentException("Unknown loglevel");
        }
    }

    /**
     * Wrap the log message.
     * If correlation id exists, log message will be in the format of :
     * MSAL <msal_version> <platform> <platform_version> [<timestamp> - <correlation_id>] <log_message>
     * If correlation id doesn't exist, log message will be in the format of:
     * MSAL <msal_version> <platform> <platform_version> [<timestamp>] <log_message>
     */
    private String formatMessage(final UUID correlationId, final String message) {
        final String logMessage = MSALUtils.isEmpty(message) ? "N/A" : message;

        String formattedMessage = "MSAL " + PublicClientApplication.getSdkVersion() + " Android "
                + Build.VERSION.SDK_INT + " [" + getUTCDateTimeAsString();
        if (correlationId != null) {
            formattedMessage += " - " + correlationId.toString();
        }

        formattedMessage += "] " + logMessage;

        return formattedMessage;
    }

    private static String getUTCDateTimeAsString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(new Date());
    }

    /**
     * Enum class for LogLevel that the sdk recognizes. 
     */
    public enum LogLevel {
        /**
         * Error level logging.
         */
        ERROR,
        /**
         * Warning level logging.
         */
        WARNING,
        /**
         * Info level logging.
         */
        INFO,
        /**
         * Verbose level logging.
         */
        VERBOSE
    }
}
