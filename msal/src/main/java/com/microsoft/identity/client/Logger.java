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

import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.identity.msal.BuildConfig.VERSION_NAME;

/**
 * MSAL Logger for diagnostic purpose. The sdk generates logs with both logcat logging or the external logger.
 * By default, the sdk enables logging with logcat. To turn off logging:
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
 *         boolean containsPII) { }
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
    private static final Logger sINSTANCE = new Logger();

    private AtomicReference<ILoggerCallback> mExternalLogger = new AtomicReference<>(null);

    /**
     * @return The single instance of {@link Logger}.
     */
    public static Logger getInstance() {
        return sINSTANCE;
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

    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level logging.
     *
     * @param logLevel The {@link LogLevel} to be enabled for the diagnostic logging.
     */
    public void setLogLevel(final LogLevel logLevel) {
        switch (logLevel) {
            case ERROR:
                com.microsoft.identity.common.internal.logging.Logger.getInstance()
                        .setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.ERROR);
                break;
            case WARNING:
                com.microsoft.identity.common.internal.logging.Logger.getInstance()
                        .setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.WARN);
                break;
            case INFO:
                com.microsoft.identity.common.internal.logging.Logger.getInstance()
                        .setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.INFO);
                break;
            case VERBOSE:
                com.microsoft.identity.common.internal.logging.Logger.getInstance()
                        .setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.VERBOSE);
                break;
            default:
                throw new IllegalArgumentException("Unknown logLevel");
        }
    }

    /**
     * Set the custom logger. Configures external logging to configure a callback that
     * the sdk will use to pass each log message. Overriding the logger callback is not allowed.
     *
     * @param externalLogger The reference to the {@link ILoggerCallback} that can
     *                       output the logs to the designated places.
     * @throws IllegalStateException if external logger is already set, and the caller is trying to set it again.
     */
    public void setExternalLogger(final ILoggerCallback externalLogger) {
        if (externalLogger == null) {
            return;
        }

        if (mExternalLogger.get() != null) {
            throw new IllegalStateException("External logger is already set, cannot be set again.");
        }

        // If mExternalLogger is not set. Then implement the ILoggerCallback interface in common-core.
        com.microsoft.identity.common.internal.logging.Logger.getInstance().setExternalLogger(new com.microsoft.identity.common.internal.logging.ILoggerCallback() {
            @Override
            public void log(String tag, com.microsoft.identity.common.internal.logging.Logger.LogLevel logLevel, String message, boolean containsPII) {
                switch (logLevel) {
                    case ERROR:
                        mExternalLogger.get().log(tag, LogLevel.ERROR, message, containsPII);
                        break;
                    case WARN:
                        mExternalLogger.get().log(tag, LogLevel.WARNING, message, containsPII);
                        break;
                    case VERBOSE:
                        mExternalLogger.get().log(tag, LogLevel.VERBOSE, message, containsPII);
                        break;
                    case INFO:
                        mExternalLogger.get().log(tag, LogLevel.INFO, message, containsPII);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown logLevel");
                }
            }
        });

        mExternalLogger.set(externalLogger);
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk enables it.
     *
     * @param enableLogcatLog True if enabling the logcat logging, false otherwise.
     */
    public void setEnableLogcatLog(final boolean enableLogcatLog) {
        com.microsoft.identity.common.internal.logging.Logger.setAllowLogcat(enableLogcatLog);
    }

    /**
     * Enable log message with PII (personal identifiable information) info. By default, MSAL doesn't log any PII.
     *
     * @param enablePII True if enabling PII info to be logged, false otherwise.
     */
    public void setEnablePII(final boolean enablePII) {
        com.microsoft.identity.common.internal.logging.Logger.setAllowPii(enablePII);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     *
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#error(String, String, String, Throwable)} instead.
     */
    @Deprecated
    static void error(final String tag, final RequestContext requestContext, final String errorMessage,
                      final Throwable exception) {
        getInstance().commonCoreWrapper(tag, LogLevel.ERROR, requestContext, errorMessage, exception, false);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message with PII.
     *
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#errorPII(String, String, String, Throwable)} instead.
     */
    @Deprecated
    static void errorPII(final String tag, final RequestContext requestContext, final String errorMessage,
                         final Throwable exception) {
        getInstance().commonCoreWrapper(tag, LogLevel.ERROR, requestContext, errorMessage, exception, true);
    }

    /**
     * Send a {@link LogLevel#WARNING} log message without PII.
     *
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#warn(String, String, String)} instead.
     */
    @Deprecated
    static void warning(final String tag, final RequestContext requestContext, final String message) {
        getInstance().commonCoreWrapper(tag, LogLevel.WARNING, requestContext, message, null, false);
    }

    /**
     * Send a {@link LogLevel#WARNING} log message with PII.
     *
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#warnPII(String, String, String)} instead.
     */
    @Deprecated
    static void warningPII(final String tag, final RequestContext requestContext, final String message) {
        getInstance().commonCoreWrapper(tag, LogLevel.WARNING, requestContext, message, null, true);
    }

    /**
     * Send a {@link LogLevel#INFO} log message without PII.
     *
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#info(String, String, String)} instead.
     */
    @Deprecated
    static void info(final String tag, final RequestContext requestContext, final String message) {
        getInstance().commonCoreWrapper(tag, LogLevel.INFO, requestContext, message, null, false);
    }

    /**
     * Send a {@link LogLevel#INFO} log message with PII.
     *
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#infoPII(String, String, String)} instead.
     */
    @Deprecated
    static void infoPII(final String tag, final RequestContext requestContext, final String message) {
        getInstance().commonCoreWrapper(tag, LogLevel.INFO, requestContext, message, null, true);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     *
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#verbose(String, String, String)} instead.
     */
    @Deprecated
    static void verbose(final String tag, final RequestContext requestContext, final String message) {
        getInstance().commonCoreWrapper(tag, LogLevel.VERBOSE, requestContext, message, null, false);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message with PII.
     *
     * @deprecated use {@link com.microsoft.identity.common.internal.logging.Logger#verbosePII(String, String, String)} instead.
     */
    @Deprecated
    static void verbosePII(final String tag, final RequestContext requestContext, final String message) {
        getInstance().commonCoreWrapper(tag, LogLevel.VERBOSE, requestContext, message, null, true);
    }

    private String getCorrelationId(RequestContext requestContext) {
        if (requestContext != null && requestContext.getCorrelationId() != null) {
            return requestContext.getCorrelationId().toString();
        } else {
            return null;
        }
    }

    private void commonCoreWrapper(final String tag, final LogLevel logLevel, final RequestContext requestContext,
                                   final String message, final Throwable throwable, final boolean containsPII) {
        final String messageWithComponent = appendComponent(requestContext) + message + " SDK ver:" + VERSION_NAME;
        final String correlationID = getCorrelationId(requestContext);

        switch (logLevel) {
            case ERROR:
                if (containsPII) {
                    com.microsoft.identity.common.internal.logging.Logger.errorPII(tag, correlationID, messageWithComponent, throwable);
                } else {
                    com.microsoft.identity.common.internal.logging.Logger.error(tag, correlationID, messageWithComponent, throwable);
                }
                break;
            case WARNING:
                if (containsPII) {
                    com.microsoft.identity.common.internal.logging.Logger.warnPII(tag, correlationID, messageWithComponent);
                } else {
                    com.microsoft.identity.common.internal.logging.Logger.warn(tag, correlationID, messageWithComponent);
                }
                break;
            case INFO:
                if (containsPII) {
                    com.microsoft.identity.common.internal.logging.Logger.infoPII(tag, correlationID, messageWithComponent);
                } else {
                    com.microsoft.identity.common.internal.logging.Logger.info(tag, correlationID, messageWithComponent);
                }
                break;
            case VERBOSE:
                if (containsPII) {
                    com.microsoft.identity.common.internal.logging.Logger.verbosePII(tag, correlationID, messageWithComponent);
                } else {
                    com.microsoft.identity.common.internal.logging.Logger.verbose(tag, correlationID, messageWithComponent);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown logLevel");
        }
    }

    private String appendComponent(final RequestContext requestContext) {
        if (requestContext != null && !MsalUtils.isEmpty(requestContext.getComponent())) {
            return "(" + requestContext.getComponent() + ") ";
        }

        return "";
    }
}