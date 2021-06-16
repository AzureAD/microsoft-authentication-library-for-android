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
package com.microsoft.identity.client.msal.automationapp.testpass.stress;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.logging.appender.FileAppender;
import com.microsoft.identity.client.ui.automation.logging.formatter.LogcatLikeFormatter;
import com.microsoft.identity.client.ui.automation.performance.DeviceMonitor;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Before;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public abstract class AbstractMsalUiStressTest<T, S> extends AbstractMsalUiTest {

    // Sets the interval duration in seconds to which the device performance will be monitored.
    private static final long DEVICE_MONITOR_INTERVAL = TimeUnit.SECONDS.toMillis(1);
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private Exception executionException;

    @Override
    @Before
    public void setup() {
        super.setup();
        executionException = null;
    }

    /**
     * Run the stress tests
     */
    public void run() throws Exception {
        final T prerequisites = this.prepare();

        final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(getNumberOfThreads());
        final RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();

        final ExecutorService executorService = new ThreadPoolExecutor(
                1,
                getNumberOfThreads() + 1,
                0L,
                TimeUnit.MILLISECONDS,
                blockingQueue,
                rejectedExecutionHandler
        );


        final long startTime = System.currentTimeMillis();
        final long timeLimit = TimeUnit.MINUTES.toMillis(getTimeLimit());

        // use the execution thread pool to collect device stats
        executorService.submit(getPerformanceStatsCollector());


        while (System.currentTimeMillis() - startTime < timeLimit && executionException == null) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        S result = execute(prerequisites);

                        assertResult(result);
                    } catch (Exception ex) {
                        executionException = ex;
                    }
                }
            });
            TimeUnit.MILLISECONDS.sleep(50);
        }

        executorService.shutdown();


        if (executionException != null) {
            throw executionException;
        }
    }

    private Runnable getPerformanceStatsCollector() {
        writeFile(String.format("%d\n%d", getNumberOfThreads(), getTimeLimit()));

        return new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String line = String.format(
                                "%s,%s,%s,%s,%s",
                                new SimpleDateFormat(DATE_FORMAT).format(new Date()),
                                DeviceMonitor.getCpuUsage(),
                                DeviceMonitor.getMemoryUsage(),
                                DeviceMonitor.getNetworkTrafficInfo().getDiffBytesReceived(),
                                DeviceMonitor.getNetworkTrafficInfo().getDiffBytesSent()
                        );

                        System.out.println("Result: " + line);
                        writeFile(line);

                        TimeUnit.MILLISECONDS.sleep(DEVICE_MONITOR_INTERVAL);
                    }
                } catch (Exception ex) {
                    executionException = ex;
                }
            }
        };
    }

    private synchronized void writeFile(final String output) {
        try {
            final FileAppender fileAppender = new FileAppender(getOutputFileName(), new LogcatLikeFormatter());
            fileAppender.append(output);

            CommonUtils.copyFileToFolderInSdCard(fileAppender.getLogFile(), "automation");
        } catch (IOException e) {
            executionException = e;
        }
    }


    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_webview;
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;

        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }

    public abstract void assertResult(S result);


    /**
     * Prepare all the tests for execution
     *
     * @return a result that will be used in the stress test execution
     */
    public abstract T prepare();

    /**
     * Run the stress test
     *
     * @param prerequisites result from the {@link AbstractMsalUiStressTest#prepare()}
     * @return the result of the test
     */
    public abstract S execute(T prerequisites) throws Exception;

    /**
     * Provide the number of threads that will be running
     *
     * @return the number of threads
     */
    public abstract int getNumberOfThreads();


    /**
     * Provide the time limit for the tests in minutes
     *
     * @return the time limit in minutes
     */
    public abstract long getTimeLimit();


    /**
     * Return the output file name
     *
     * @return the name of the output file
     */
    public abstract String getOutputFileName();

}
