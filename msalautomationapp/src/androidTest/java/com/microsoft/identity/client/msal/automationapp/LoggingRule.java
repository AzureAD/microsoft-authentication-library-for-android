package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.ui.automation.logging.FileLogger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.IOException;

public class LoggingRule implements TestRule {

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final FileLogger automationLogStrategy = turnOnAutomationLogging(description);
                final FileLogger msalLogStrategy = turnOnMsalLogging(description);

                base.evaluate();

                copyLogFilesToSdCard(automationLogStrategy);
                copyLogFilesToSdCard(msalLogStrategy);

            }
        };
    }

    private FileLogger turnOnMsalLogging(final Description description) {
        final String msalLogFileName = description.getMethodName() + "-msal.log";
        final FileLogger msalfileLogger = new FileLogger(msalLogFileName);
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
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

    private FileLogger turnOnAutomationLogging(final Description description) {
        final String automationLogFileName = description.getMethodName() + "-automation.log";
        final FileLogger automationFileLogger = new FileLogger(
                automationLogFileName
        );

        com.microsoft.identity.client.ui.automation.logging.Logger
                .getLoggerRegistry()
                .registerLogger(automationFileLogger);

        return automationFileLogger;
    }

    private void copyLogFilesToSdCard(final FileLogger fileLogStrategy) throws IOException {
        final String filePath = fileLogStrategy.getLogFilePath();
        final File dir = new File("/sdcard/automation");
        final File destFile = new File(dir, fileLogStrategy.getLogFile().getName());
        final String destFilePath = destFile.getAbsolutePath();
        AdbShellUtils.copyToSdCard(filePath, destFilePath);
    }
}
