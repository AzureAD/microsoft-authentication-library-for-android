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

import static com.microsoft.identity.common.internal.logging.Logger.setAllowLogcat;
import static com.microsoft.identity.common.internal.logging.Logger.setAllowPii;

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

    private ILoggerCallback mExternalLogger;

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
        final com.microsoft.identity.common.internal.logging.Logger logger =
                com.microsoft.identity.common.internal.logging.Logger.getInstance();

        switch (logLevel) {
            case ERROR:
                logger.setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.ERROR);
                break;

            case WARNING:
                logger.setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.WARN);
                break;

            case INFO:
                logger.setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.INFO);
                break;

            case VERBOSE:
                logger.setLogLevel(com.microsoft.identity.common.internal.logging.Logger.LogLevel.VERBOSE);
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
    public synchronized void setExternalLogger(final ILoggerCallback externalLogger) {
        if (externalLogger == null) {
            return;
        }

        if (null != mExternalLogger) {
            throw new IllegalStateException("External logger is already set, cannot be set again.");
        }

        // If mExternalLogger is not set. Then implement the ILoggerCallback interface in common-core.
        final com.microsoft.identity.common.internal.logging.Logger logger =
                com.microsoft.identity.common.internal.logging.Logger.getInstance();

        logger.setExternalLogger(new com.microsoft.identity.common.internal.logging.ILoggerCallback() {
            @Override
            public void log(String tag, com.microsoft.identity.common.internal.logging.Logger.LogLevel logLevel, String message, boolean containsPII) {
                switch (logLevel) {
                    case ERROR:
                        mExternalLogger.log(tag, LogLevel.ERROR, message, containsPII);
                        break;

                    case WARN:
                        mExternalLogger.log(tag, LogLevel.WARNING, message, containsPII);
                        break;

                    case VERBOSE:
                        mExternalLogger.log(tag, LogLevel.VERBOSE, message, containsPII);
                        break;

                    case INFO:
                        mExternalLogger.log(tag, LogLevel.INFO, message, containsPII);
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown logLevel");
                }
            }
        });

        mExternalLogger = externalLogger;
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk enables it.
     *
     * @param enableLogcatLog True if enabling the logcat logging, false otherwise.
     */
    public void setEnableLogcatLog(final boolean enableLogcatLog) {
        setAllowLogcat(enableLogcatLog);
    }

    /**
     * Enable log message with PII (personal identifiable information) info.
     * By default, MSAL doesn't log any PII.
     *
     * @param enablePII True if enabling PII info to be logged, false otherwise.
     */
    public void setEnablePII(final boolean enablePII) {
        setAllowPii(enablePII);
    }
}
