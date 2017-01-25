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
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.util.Log;

import junit.framework.Assert;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

/**
 * Tests for {@link Logger}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class LoggerTest {
    static final UUID CORRELATION_ID = UUID.randomUUID();
    static final String TAG = "someTestTag";
    static final String MESSAGE = "test message";
    static final String ADDITIONAL_MESSAGE = "additional test message";
    static final LogResponse LOG_RESPONSE = new LogResponse();

    @BeforeClass
    public static void setUp() {
        Logger.getInstance().setExternalLogger(new ILogger() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, String additionalMessage) {
                LOG_RESPONSE.setTag(tag);
                LOG_RESPONSE.setLogLevel(logLevel);
                LOG_RESPONSE.setMessage(message);
                LOG_RESPONSE.setAdditionalMessage(additionalMessage);
            }
        });
    }

    @After
    public void tearDown() {
        LOG_RESPONSE.reset();
    }

    /**
     * Verify that if logger is turned on at verbose level, all level logs will be generated and sent to the external logger.
     */
    @Test
    public void testVerboseLevelLogging() {
        // set as verbose level logging
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
        Logger.verbose(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(LOG_RESPONSE, null);

        // log an empty message
        Logger.verbose(TAG, CORRELATION_ID, "", "");
        verifyLogMessageFormat(LOG_RESPONSE, "N/A", CORRELATION_ID, null);
        Assert.assertTrue(LOG_RESPONSE.getAdditionalMessage().equals(""));
        LOG_RESPONSE.reset();

        // log null additional message
        Logger.verbose(TAG, CORRELATION_ID, MESSAGE, null);
        Assert.assertTrue(LOG_RESPONSE.getAdditionalMessage().equals(""));
        LOG_RESPONSE.reset();

        // verify info logs are generated
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.info(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(LOG_RESPONSE, null);

        // verify warning logs are generated
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.warning(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(LOG_RESPONSE, null);

        // verify error level logs are generated
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.error(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE, null);
        verifyLogMessage(LOG_RESPONSE, null);

        // verify the log message is correctly formatted when no correlation id exists.
        Logger.verbose(TAG, null, MESSAGE, "");
        verifyLogMessageFormat(LOG_RESPONSE, MESSAGE, null, null);
        Assert.assertTrue(LOG_RESPONSE.getAdditionalMessage().equals(""));
        LOG_RESPONSE.reset();
    }

    /**
     * Verify that if logger is turned on at info level, verbose log will not be generated. Only error, warning and info
     * level logs are generated.
     */
    @Test
    public void testInfoLevelLogging() {
        // set as info level logging
        Logger.getInstance().setLogLevel(Logger.LogLevel.INFO);

        // perform a verbose level logging, make sure callback doesn't get the log message
        Logger.verbose(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // Do a info level logging
        Logger.info(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(LOG_RESPONSE, null);

        // do a warning level logging
        Logger.warning(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(LOG_RESPONSE, null);

        // do a error level logging
        Logger.error(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE, null);
        verifyLogMessage(LOG_RESPONSE, null);
    }

    /**
     * Verify that if logging are turned on at warning level, only warning and error logs will be generated.
     */
    @Test
    public void testWarningLevelLogging() {
        Logger.getInstance().setLogLevel(Logger.LogLevel.WARNING);

        // Perform verbose level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.verbose(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // Perform info level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.info(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // perform warning level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.warning(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(LOG_RESPONSE, null);

        // perform error level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.error(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE, null);
        verifyLogMessage(LOG_RESPONSE, null);
    }

    /**
     * Verify that if logging is turned on error level, only error level logs are generated. Stacktrace for throwable are
     * correctly appended in the logs.
     */
    @Test
    public void testErrorLevelLogging() {
        Logger.getInstance().setLogLevel(Logger.LogLevel.ERROR);

        // perform error log
        verifyLogMessageEmpty(LOG_RESPONSE);
        Throwable throwable = null;
        try {
            final String testString = null;
            testString.length();
            Assert.fail();
        } catch (final NullPointerException e) {
            throwable = e;
        }

        Logger.error(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE, throwable);
        Assert.assertTrue(LOG_RESPONSE.getAdditionalMessage().equals(ADDITIONAL_MESSAGE));
        verifyLogMessage(LOG_RESPONSE, throwable);

        // perform warning level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.warning(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // perform info level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.info(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // perform verbose level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.verbose(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);
    }

    private void verifyLogMessage(final LogResponse response, final Throwable throwable) {
        Assert.assertTrue(response.getTag().equals(TAG));
        verifyLogMessageFormat(response, MESSAGE, CORRELATION_ID, throwable);
        Assert.assertTrue(response.getAdditionalMessage().contains(ADDITIONAL_MESSAGE));

        response.reset();
    }

    private void verifyLogMessageFormat(final LogResponse response, final String message, final UUID correlationId,
                                        final Throwable throwable) {
        Assert.assertTrue(response.getMessage().contains("MSAL " + PublicClientApplication.getSdkVersion() + " Android " + Build.VERSION.SDK_INT + " ["));
        if (correlationId != null) {
            Assert.assertTrue(response.getMessage().contains(" - " + correlationId.toString() + "] " + message + getStackTrace(throwable)));
        } else {
            Assert.assertTrue(response.getMessage().contains("] " + message + getStackTrace(throwable)));
        }
    }

    private String getStackTrace(final Throwable throwable) {
        if (throwable != null) {
            return " " + Log.getStackTraceString(throwable);
        }

        return "";
    }

    private void verifyLogMessageEmpty(final LogResponse response) {
        Assert.assertNull(response.getMessage());
        Assert.assertNull(response.getAdditionalMessage());
        Assert.assertNull(response.getTag());
        Assert.assertNull(response.getLogLevel());
    }

    /**
     * Test log response class to get the log message. 
     */
    static final class LogResponse {
        private String mTag;
        private String mMessage;
        private String mAdditionalMessage;
        private Logger.LogLevel mLevel;

        public void reset() {
            mTag = null;
            mMessage = null;
            mAdditionalMessage = null;
            mLevel = null;
        }

        void setTag(final String tag) {
            mTag = tag;
        }

        String getTag() {
            return mTag;
        }

        void setMessage(final String message) {
            mMessage = message;
        }

        String getMessage() {
            return mMessage;
        }

        void setAdditionalMessage(final String additionalMessage) {
            mAdditionalMessage = additionalMessage;
        }

        String getAdditionalMessage() {
            return mAdditionalMessage;
        }

        void setLogLevel(final Logger.LogLevel logLevel) {
            mLevel = logLevel;
        }

        Logger.LogLevel getLogLevel() {
            return mLevel;
        }
    }
}
