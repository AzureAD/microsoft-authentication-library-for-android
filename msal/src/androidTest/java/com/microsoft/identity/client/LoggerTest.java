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
    static final String COMPONENT_NAME = "test component";
    static final String TAG = "someTestTag";
    static final String MESSAGE = "test message";
    static final LogResponse LOG_RESPONSE = new LogResponse();
    static final RequestContext REQUEST_CONTEXT_WITH_COMPONENT = new RequestContext(CORRELATION_ID, COMPONENT_NAME);
    static final RequestContext REQUEST_CONTEXT_NO_COMPONENT = new RequestContext(CORRELATION_ID, null);

    @BeforeClass
    public static void setUp() {
        Logger.getInstance().setExternalLogger(new ILogger() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                LOG_RESPONSE.setTag(tag);
                LOG_RESPONSE.setLogLevel(logLevel);
                LOG_RESPONSE.setMessage(message);
                LOG_RESPONSE.setContainsPII(containsPII);
            }
        });
    }

    @After
    public void tearDown() {
        LOG_RESPONSE.reset();
        Logger.getInstance().setEnablePII(false);
    }

    /**
     * Verify that if logger is turned on at verbose level, all level logs will be generated and sent to the external logger.
     */
    @Test
    public void testVerboseLevelLogging() {
        // set as verbose level logging
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);

        Logger.verbose(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, false, MESSAGE, null);
        LOG_RESPONSE.reset();

        // test verbose with PII
        Logger.verbosePII(TAG, REQUEST_CONTEXT_NO_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);
        LOG_RESPONSE.reset();

        // log an empty message
        Logger.verbose(TAG, REQUEST_CONTEXT_WITH_COMPONENT, "");
        verifyLogMessageFormat(LOG_RESPONSE, "N/A", REQUEST_CONTEXT_WITH_COMPONENT, null);
        LOG_RESPONSE.reset();

        // verify info logs are generated
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.info(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, false, MESSAGE, null);

        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.infoPII(TAG, REQUEST_CONTEXT_NO_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // verify warning logs are generated
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.warning(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, false, MESSAGE, null);

        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.warningPII(TAG, REQUEST_CONTEXT_NO_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // verify error level logs are generated
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.error(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE, null);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, false, MESSAGE, null);

        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.errorPII(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE, null);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // verify the log message is correctly formatted when no correlation id exists.
        final RequestContext requestContext = new RequestContext(null, COMPONENT_NAME);
        Logger.verbose(TAG, new RequestContext(null, COMPONENT_NAME), MESSAGE);
        verifyLogMessageFormat(LOG_RESPONSE, MESSAGE, requestContext, null);

        // verify the log message is correctly formatted when no request context exists.
        Logger.verbose(TAG, null, MESSAGE);
        verifyLogMessageFormat(LOG_RESPONSE, MESSAGE, null, null);

        // verify the log message is correctly formatted when neither correlation nor component exists
        final RequestContext requestContextNoCorrelationNoComponent = new RequestContext(null, null);
        Logger.verbose(TAG, requestContextNoCorrelationNoComponent, MESSAGE);
        verifyLogMessageFormat(LOG_RESPONSE, MESSAGE, requestContextNoCorrelationNoComponent, null);
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
        Logger.verbose(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // Do a info level logging
        Logger.info(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, false, MESSAGE, null);

        // since no PII is turned on, no log mesaage should be logged.
        Logger.infoPII(TAG, REQUEST_CONTEXT_NO_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // do a warning level logging
        Logger.warning(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, false, MESSAGE, null);

        Logger.warningPII(TAG, REQUEST_CONTEXT_NO_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // do a error level logging
        Logger.error(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE, null);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, false, MESSAGE, null);

        Logger.errorPII(TAG, REQUEST_CONTEXT_NO_COMPONENT, MESSAGE, null);
        verifyLogMessageEmpty(LOG_RESPONSE);
    }

    @Test
    public void testEnablePII() {
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
        Logger.getInstance().setEnablePII(true);

        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.verbosePII(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, true, MESSAGE, null);

        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.infoPII(TAG, REQUEST_CONTEXT_NO_COMPONENT, MESSAGE);
        verifyLogMessage(REQUEST_CONTEXT_NO_COMPONENT, LOG_RESPONSE, true, MESSAGE, null);

        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.warningPII(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, true, MESSAGE, null);

        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.errorPII(TAG, REQUEST_CONTEXT_NO_COMPONENT, MESSAGE, null);
        verifyLogMessage(REQUEST_CONTEXT_NO_COMPONENT, LOG_RESPONSE, true, MESSAGE, null);
    }

    /**
     * Verify that if logging are turned on at warning level, only warning and error logs will be generated.
     */
    @Test
    public void testWarningLevelLogging() {
        Logger.getInstance().setLogLevel(Logger.LogLevel.WARNING);

        // Perform verbose level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.verbose(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // Perform info level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.info(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // perform warning level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.warning(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, false, MESSAGE, null);

        // perform error level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.error(TAG, REQUEST_CONTEXT_NO_COMPONENT, MESSAGE, null);
        verifyLogMessage(REQUEST_CONTEXT_NO_COMPONENT, LOG_RESPONSE, false, MESSAGE, null);
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

        Logger.error(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE, throwable);
        verifyLogMessage(REQUEST_CONTEXT_WITH_COMPONENT, LOG_RESPONSE, false, MESSAGE, throwable);

        // perform warning level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.warning(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // perform info level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.info(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);

        // perform verbose level logging
        verifyLogMessageEmpty(LOG_RESPONSE);
        Logger.verbose(TAG, REQUEST_CONTEXT_WITH_COMPONENT, MESSAGE);
        verifyLogMessageEmpty(LOG_RESPONSE);
    }

    private void verifyLogMessage(final RequestContext requestContext, final LogResponse response, boolean containsPII,
                                  final String logMessage, final Throwable throwable) {
        Assert.assertTrue(response.getTag().equals(TAG));
        verifyLogMessageFormat(response, logMessage, requestContext, throwable);
        Assert.assertEquals(LOG_RESPONSE.containsPII(), containsPII);

        response.reset();
    }

    private void verifyLogMessageFormat(final LogResponse response, final String message, final RequestContext requestContext,
                                        final Throwable throwable) {
        Assert.assertTrue(response.getMessage().contains("MSAL " + PublicClientApplication.getSdkVersion() + " Android " + Build.VERSION.SDK_INT + " ["));
        if (requestContext != null && (requestContext.getCorrelationId() != null || !MsalUtils.isEmpty(requestContext.getComponent()))) {
            if (requestContext.getCorrelationId() != null && !MsalUtils.isEmpty(requestContext.getComponent())) {
                Assert.assertTrue(response.getMessage().contains(" - " + requestContext.getCorrelationId().toString() + "] (" + requestContext.getComponent()
                        + ") " + message + getStackTrace(throwable)));
            } else if (requestContext.getCorrelationId() != null) {
                Assert.assertTrue(response.getMessage().contains(" - " + requestContext.getCorrelationId().toString() + "] " + message + getStackTrace(throwable)));
            } else {
                Assert.assertTrue(response.getMessage().contains("] (" + requestContext.getComponent() + ") " + message + getStackTrace(throwable)));
            }
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
        private boolean mContainsPII;

        public void reset() {
            mTag = null;
            mMessage = null;
            mAdditionalMessage = null;
            mLevel = null;
            mContainsPII = false;
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

        void setContainsPII(final boolean containsPII) {
            mContainsPII = containsPII;
        }

        boolean containsPII() {
            return mContainsPII;
        }
    }
}
