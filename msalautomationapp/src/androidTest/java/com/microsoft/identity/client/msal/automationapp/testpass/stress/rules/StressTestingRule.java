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
package com.microsoft.identity.client.msal.automationapp.testpass.stress.rules;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.performance.CPUMonitor;
import com.microsoft.identity.client.ui.automation.performance.DeviceMonitor;
import com.microsoft.identity.client.ui.automation.performance.MemoryMonitor;
import com.microsoft.identity.client.ui.automation.performance.NetworkUsageMonitor;
import com.microsoft.identity.client.ui.automation.performance.PerformanceProfile;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class StressTestingRule implements TestRule {
    private static final String TAG = StressTestingRule.class.getSimpleName();

    // Sets the interval duration in seconds to which the device performance will be monitored.
    private static final long DEVICE_MONITOR_INTERVAL = TimeUnit.SECONDS.toMillis(1);
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private final AtomicLong testsPassed = new AtomicLong(0);
    private final AtomicLong testsFailed = new AtomicLong(0);
    private final int numberOfThreads;
    private final long timeLimitMinutes;
    private final String outputFileName;

    private FileWriter fileWriter;
    private volatile Exception executionException;

    public StressTestingRule(final String outputFileName, final int numberOfThreads, final long timeLimitMinutes) {
        this.outputFileName = outputFileName;
        this.numberOfThreads = numberOfThreads;
        this.timeLimitMinutes = timeLimitMinutes;

        DeviceMonitor.setProfiler(PerformanceProfile.CPU, new CPUMonitor());
        DeviceMonitor.setProfiler(PerformanceProfile.MEMORY, new MemoryMonitor());
        DeviceMonitor.setProfiler(PerformanceProfile.NETWORK, new NetworkUsageMonitor());
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final File outputFile = createOutputFile(outputFileName);
                fileWriter = new FileWriter(outputFile, false);

                final ExecutorService executorService = Executors.newFixedThreadPool(2);

                executorService.execute(getStatsCollector());
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            base.evaluate();
                        } catch (Throwable throwable) {
                            if (executionException != null) {
                                executionException = new Exception(throwable);
                            }
                        }
                    }
                });

                executorService.shutdown();
                executorService.awaitTermination(timeLimitMinutes, TimeUnit.MINUTES);
                executorService.shutdownNow();

                fileWriter.flush();
                fileWriter.close();

                CommonUtils.copyFileToFolderInSdCard(outputFile, "automation");

                if (executionException != null) {
                    throw executionException;
                }
            }
        };
    }


    private Runnable getStatsCollector() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        // time   cpu_usage   memory_usage   data_received   data_sent   num_threads   time_limit   device_memory   device_name   tests_passed   tests_failed
                        final String[] row = new String[12];
                        row[0] = new SimpleDateFormat(DATE_FORMAT).format(new Date());
                        row[1] = String.valueOf(DeviceMonitor.getCpuUsage());
                        row[2] = String.valueOf(DeviceMonitor.getMemoryUsage());
                        row[3] = String.valueOf(DeviceMonitor.getNetworkTrafficInfo().getDiffBytesReceived());
                        row[4] = String.valueOf(DeviceMonitor.getNetworkTrafficInfo().getDiffBytesSent());
                        row[5] = String.valueOf(numberOfThreads);
                        row[6] = String.valueOf(timeLimitMinutes);
                        row[7] = String.valueOf(DeviceMonitor.getTotalMemory());
                        row[8] = DeviceMonitor.getDeviceName();
                        row[9] = String.valueOf(testsPassed.get());
                        row[10] = String.valueOf(testsFailed.get());

                        for (int i = 0; i < row.length; i++) {
                            String cellData = row[i];
                            fileWriter.write(cellData == null ? "" : cellData);

                            if (i != row.length - 1) {
                                fileWriter.write(",");
                            }
                        }
                        fileWriter.write("\n");
                        fileWriter.flush();

                        TimeUnit.MILLISECONDS.sleep(DEVICE_MONITOR_INTERVAL);
                    }
                } catch (Exception exception) {
                    Log.e(TAG, "Error saving statistics", exception);
                    executionException = exception;
                }
            }
        };
    }

    private File createOutputFile(@NonNull final String filename) throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        final File directory = context.getFilesDir();
        final File outputFile = new File(directory, filename);

        if (!outputFile.exists()) {
            final boolean fileCreated = outputFile.createNewFile();
            if (!fileCreated) {
                throw new IOException("Unable to create new log file :(");
            }
        }

        return outputFile;
    }


    public synchronized void updatePassRate(final boolean passed) {
        if (passed) {
            testsPassed.incrementAndGet();
        } else {
            testsFailed.incrementAndGet();
        }
    }

    public synchronized void setExecutionException(Exception executionException) {
        this.executionException = executionException;
    }

    public synchronized boolean executionFailed() {
        return this.executionException != null;
    }
}
