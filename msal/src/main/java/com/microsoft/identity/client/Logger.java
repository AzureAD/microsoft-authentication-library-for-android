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
import java.util.concurrent.atomic.AtomicReference;

/**
 * MSAL Logger for diagnostic purpose. The sdk generates logs with both logcat logging or the external logger.
 * By default, the sdk enables logging with logcat. To turn it off logging:
 * <pre>
 * {@code
 *     Logger.getInstance().setEnableLogcatLog(false);
 * }
 * </pre>
 * To enable the custom logger, set the external logger implementing
 * {@link ILoggerCallback}.
 * <pre>
 * <code>
 *     Logger.getInstance().setExternalLogger(new Logger.ILoggerCallback() {
 *    {@literal @}Override
 *     public void log(String tag, Logger.LogLevel logLevel, String message,
 *         String additionalMessage) { }
 *     });
 * </code>
 * </pre>
 * Loglevel can be specified at {@link LogLevel#ERROR}, {@link LogLevel#WARNING}, {@link LogLevel#INFO}
 * and {@link LogLevel#VERBOSE}. The sdk enables the verbose level logging by default, to set different
 * level logging:
 * <pre>
 * {@code
 *     Logger.getInstance().setLogLevel(Loglevel)
 * }
 * </pre>
 * By default, the sdk doesn't send any log messages that contain PII (personal identifiable information) info. To enable PII
 * logging:
 * <pre>
 * {@code
 *     Logger.getInstance().setEnablePII(true);
 * }
 * </pre>
 */
public final class Logger {
    private static final Logger INSTANCE = new Logger();
    static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Turn on the verbose level logging by default.
    private LogLevel mLogLevel = LogLevel.VERBOSE;
    private AtomicReference<ILoggerCallback> mExternalLogger = new AtomicReference<>(null);
    private boolean mLogcatLogEnabled = true;
    private boolean mEnablePII = false;

    /**
     * @return The single instance of {@link Logger}.
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
     * Set the custom logger. Configures external logging to configure a callback that
     * the sdk will use to pass each log message. Overriding the logger callback is not allowed.
     * @param externalLogger The reference to the {@link ILoggerCallback} that can
     * output the logs to the designated places.
     * @throws IllegalStateException if external logger is already set, and the caller is trying to set it again.
     */
    public void setExternalLogger(final ILoggerCallback externalLogger) {
        if (externalLogger == null) {
            return;
        }

        if (mExternalLogger.get() != null) {
            throw new IllegalStateException("External logger is already set, cannot be set again.");
        }

        mExternalLogger.set(externalLogger);
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk enables it.
     * @param enableLogcatLog True if enabling the logcat logging, false otherwise.
     */
    public void setEnableLogcatLog(final boolean enableLogcatLog) {
        mLogcatLogEnabled = enableLogcatLog;
    }

    /**
     * Enable log message with PII (personal identifiable information) info. By default, MSAL doesn't log any PII.
     * @param enablePII True if enabling PII info to be logged, false otherwise.
     */
    public void setEnablePII(final boolean enablePII) {
        mEnablePII = enablePII;
    }

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     */
    static void error(final String tag, final RequestContext requestContext, final String errorMessage,
                      final Throwable exception) {
        getInstance().log(tag, LogLevel.ERROR, requestContext, errorMessage, exception, false);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message with PII.
     */
    static void errorPII(final String tag, final RequestContext requestContext, final String errorMessage,
                         final Throwable exception) {
        getInstance().log(tag, LogLevel.ERROR, requestContext, errorMessage, exception, true);
    }

    /**
     * Send a {@link LogLevel#WARNING} log message without PII.
     */
    static void warning(final String tag, final RequestContext requestContext, final String message) {
        getInstance().log(tag, LogLevel.WARNING, requestContext, message, null, false);
    }

    /**
     * Send a {@link LogLevel#WARNING} log message with PII.
     */
    static void warningPII(final String tag, final RequestContext requestContext, final String message) {
        getInstance().log(tag, LogLevel.WARNING, requestContext, message, null, true);
    }

    /**
     * Send a {@link LogLevel#INFO} log message without PII.
     */
    static void info(final String tag, final RequestContext requestContext, final String message) {
        getInstance().log(tag, LogLevel.INFO, requestContext, message, null, false);
    }

    /**
     * Send a {@link LogLevel#INFO} log message with PII.
     */
    static void infoPII(final String tag, final RequestContext requestContext, final String message) {
        getInstance().log(tag, LogLevel.INFO, requestContext, message, null, true);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     */
    static void verbose(final String tag, final RequestContext requestContext, final String message) {
        getInstance().log(tag, LogLevel.VERBOSE, requestContext, message, null, false);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message with PII.
     */
    static void verbosePII(final String tag, final RequestContext requestContext, final String message) {
        getInstance().log(tag, LogLevel.VERBOSE, requestContext, message, null, true);
    }

    /**
     * Format the log message. Depends on the developer setting, the log message could be sent to logcat
     * or the external logger set by the calling app.
     */
    private void log(final String tag, final LogLevel logLevel, final RequestContext requestContext,
                     final String message, final Throwable throwable, final boolean containsPII) {
        if (logLevel.compareTo(mLogLevel) > 0) {
            return;
        }

        // Developer turns off PII logging, if the log message contains any PII, we shouldn't send it.
        if (!mEnablePII && containsPII) {
            return;
        }

        final StringBuilder logMessage = new StringBuilder();
        logMessage.append(formatMessage(requestContext, message));

        // Adding stacktrace to message
        if (throwable != null) {
            logMessage.append(' ').append(Log.getStackTraceString(throwable));
        }

        if (mLogcatLogEnabled) {
            sendLogcatLogs(tag, logLevel, logMessage.toString());
        }

        if (mExternalLogger.get() != null) {
            mExternalLogger.get().log(tag, logLevel, logMessage.toString(), containsPII);
        }
    }

    /**
     * Send logs to logcat as the default logging if developer doesn't turn off the logcat logging.
     */
    private void sendLogcatLogs(final String tag, final LogLevel logLevel, final String message) {
        // Append additional message to the message part for logcat logging
        switch (logLevel) {
            case ERROR:
                Log.e(tag, message);
                break;
            case WARNING:
                Log.w(tag, message);
                break;
            case INFO:
                Log.i(tag, message);
                break;
            case VERBOSE:
                Log.v(tag, message);
                break;
            default:
                throw new IllegalArgumentException("Unknown loglevel");
        }
    }

    /**
     * Wrap the log message, component is optional.
     * If correlation id exists:
     * MSAL <msal_version> <platform> <platform_version> [<timestamp> - <correlation_id>] (component) <log_message>
     * If correlation id doesn't exist:
     * MSAL <msal_version> <platform> <platform_version> [<timestamp>] (component) <log_message>
     */
    private String formatMessage(final RequestContext requestContext, final String message) {
        final String logMessage = MsalUtils.isEmpty(message) ? "N/A" : message;

        return "MSAL " + PublicClientApplication.getSdkVersion() + " Android "
                + Build.VERSION.SDK_INT + " [" + getUTCDateTimeAsString() + appendCorrelationId(requestContext)
                + appendComponent(requestContext) + logMessage;
    }

    private String appendCorrelationId(final RequestContext requestContext) {
        String formatMessage = "";
        if (requestContext != null && requestContext.getCorrelationId() != null) {
            formatMessage += " - " + requestContext.getCorrelationId().toString();
        }

        return formatMessage + "] ";
    }

    private String appendComponent(final RequestContext requestContext) {
        if (requestContext != null && !MsalUtils.isEmpty(requestContext.getComponent())) {
            return "(" + requestContext.getComponent() + ") ";
        }

        return "";
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
