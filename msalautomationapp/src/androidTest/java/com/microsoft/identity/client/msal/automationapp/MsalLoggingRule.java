package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.ui.automation.logging.FileLogger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class MsalLoggingRule implements TestRule {

    final static String LOG_FOLDER_NAME = "automation";

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final FileLogger msalLogStrategy = turnOnMsalLogging(description);

                base.evaluate();

                CommonUtils.copyFileToFolderInSdCard(
                        msalLogStrategy.getLogFile(),
                        LOG_FOLDER_NAME
                );
            }
        };
    }

    private FileLogger turnOnMsalLogging(final Description description) {
        final String msalLogFileName = description.getMethodName() + "-msal.log";
        final FileLogger msalfileLogger = new FileLogger(msalLogFileName);
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
        Logger.getInstance().setEnableLogcatLog(true);
        Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                switch (logLevel) {
                    case VERBOSE:
                        msalfileLogger.v(tag, message);
                    case INFO:
                        msalfileLogger.i(tag, message);
                    case ERROR:
                        msalfileLogger.e(tag, message);
                    case WARNING:
                        msalfileLogger.w(tag, message);
                }
            }
        });

        return msalfileLogger;
    }
}
