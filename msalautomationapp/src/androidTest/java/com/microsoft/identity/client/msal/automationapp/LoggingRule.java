package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.ui.automation.logging.FileLogStrategy;
import com.microsoft.identity.client.ui.automation.logging.LogLevel;
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
                final FileLogStrategy automationLogStrategy = turnOnAutomationLogging(description);
                final FileLogStrategy msalLogStrategy = turnOnMsalLogging(description);

                base.evaluate();

                copyLogFilesToSdCard(automationLogStrategy);
                copyLogFilesToSdCard(msalLogStrategy);

            }
        };
    }

    private FileLogStrategy turnOnMsalLogging(final Description description) {
        final String msalLogFileName = description.getMethodName() + "-msal.log";
        final FileLogStrategy msalfileLogStrategy = new FileLogStrategy(msalLogFileName);
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
        Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                msalfileLogStrategy.log(LogLevel.VERBOSE, tag, message, null);
            }
        });

        return msalfileLogStrategy;
    }

    private FileLogStrategy turnOnAutomationLogging(final Description description) {
        final String automationLogFileName = description.getMethodName() + "-automation.log";
        final FileLogStrategy automationFileLogStrategy = new FileLogStrategy(
                automationLogFileName
        );
        com.microsoft.identity.client.ui.automation.logging.Logger.addStrategy(
                automationFileLogStrategy
        );

        return automationFileLogStrategy;
    }

    private void copyLogFilesToSdCard(final FileLogStrategy fileLogStrategy) throws IOException {
        final String filePath = fileLogStrategy.getLogFilePath();
        final File dir = new File("/sdcard/automation");
        final File destFile = new File(dir, fileLogStrategy.getLogFile().getName());
        final String destFilePath = destFile.getAbsolutePath();
        AdbShellUtils.copyToSdCard(filePath, destFilePath);
    }
}
