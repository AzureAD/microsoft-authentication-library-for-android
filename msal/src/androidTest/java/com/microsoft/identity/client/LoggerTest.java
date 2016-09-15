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
    static final LogResponse sLogResponse = new LogResponse();

    @BeforeClass
    public static void setUp() {
        Logger.getInstance().setExternalLogger(new Logger.ILogger() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, String additionalMessage) {
                sLogResponse.setTag(tag);
                sLogResponse.setLogLevel(logLevel);
                sLogResponse.setMessage(message);
                sLogResponse.setAdditionalMessage(additionalMessage);
            }
        });
    }

    @After
    public void tearDown() {
        sLogResponse.reset();
    }

    /**
     * Verify that if logger is turned on at verbose level, all level logs will be generated and sent to the external logger.
     */
    @Test
    public void testVerboseLevelLogging() {
        // set as verbose level logging
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
        Logger.verbose(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(sLogResponse);

        // log an empty message
        Logger.verbose(TAG, CORRELATION_ID, "", "");
        Assert.assertTrue(sLogResponse.getMessage().contains("-" + CORRELATION_ID.toString() + "-N/A ver: "
                + PublicClientApplication.getSdkVersion()));
        Assert.assertTrue(sLogResponse.getAdditionalMessage().equals(""));
        sLogResponse.reset();

        // log null additional message
        Logger.verbose(TAG, CORRELATION_ID, MESSAGE, null);
        Assert.assertTrue(sLogResponse.getAdditionalMessage().equals(""));
        sLogResponse.reset();

        // verify info logs are generated
        verifyLogMessageEmpty(sLogResponse);
        Logger.info(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(sLogResponse);

        // verify warning logs are generated
        verifyLogMessageEmpty(sLogResponse);
        Logger.warning(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(sLogResponse);

        // verify error level logs are generated
        verifyLogMessageEmpty(sLogResponse);
        Logger.error(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE, null);
        verifyLogMessage(sLogResponse);
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
        verifyLogMessageEmpty(sLogResponse);

        // Do a info level logging
        Logger.info(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(sLogResponse);

        // do a warning level logging
        Logger.warning(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(sLogResponse);

        // do a error level logging
        Logger.error(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE, null);
        verifyLogMessage(sLogResponse);
    }

    /**
     * Verify that if logging are turned on at warning level, only warning and error logs will be generated.
     */
    @Test
    public void testWarningLevelLogging() {
        Logger.getInstance().setLogLevel(Logger.LogLevel.WARNING);

        // Perform verbose level logging
        verifyLogMessageEmpty(sLogResponse);
        Logger.verbose(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(sLogResponse);

        // Perform info level logging
        verifyLogMessageEmpty(sLogResponse);
        Logger.info(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(sLogResponse);

        // perform warning level logging
        verifyLogMessageEmpty(sLogResponse);
        Logger.warning(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessage(sLogResponse);

        // perform error level logging
        verifyLogMessageEmpty(sLogResponse);
        Logger.error(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE, null);
        verifyLogMessage(sLogResponse);
    }

    /**
     * Verify that if logging is turned on error level, only error level logs are generated. Stacktrace for throwable are
     * correctly appended in the logs.
     */
    @Test
    public void testErrorLevelLogging() {
        Logger.getInstance().setLogLevel(Logger.LogLevel.ERROR);

        // perform error log
        verifyLogMessageEmpty(sLogResponse);
        Throwable throwable = null;
        try {
            final String testString = null;
            testString.length();
            Assert.fail();
        } catch (final NullPointerException e) {
            throwable = e;
        }

        Logger.error(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE, throwable);
        Assert.assertTrue(sLogResponse.getAdditionalMessage().equals(ADDITIONAL_MESSAGE + ' ' + Log.getStackTraceString(throwable)));
        verifyLogMessage(sLogResponse);

        // perform warning level logging
        verifyLogMessageEmpty(sLogResponse);
        Logger.warning(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(sLogResponse);

        // perform info level logging
        verifyLogMessageEmpty(sLogResponse);
        Logger.info(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(sLogResponse);

        // perform verbose level logging
        verifyLogMessageEmpty(sLogResponse);
        Logger.verbose(TAG, CORRELATION_ID, MESSAGE, ADDITIONAL_MESSAGE);
        verifyLogMessageEmpty(sLogResponse);
    }

    private void verifyLogMessage(final LogResponse response) {
        Assert.assertTrue(response.getTag().equals(TAG));
        Assert.assertTrue(response.getMessage().contains("-" + CORRELATION_ID.toString() + "-" + MESSAGE
                + " ver: " + PublicClientApplication.getSdkVersion()));
        Assert.assertTrue(response.getAdditionalMessage().contains(ADDITIONAL_MESSAGE));

        response.reset();
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

        void setTag (final String tag) {
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
