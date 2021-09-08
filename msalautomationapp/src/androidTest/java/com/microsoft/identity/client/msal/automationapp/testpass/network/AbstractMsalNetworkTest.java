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
package com.microsoft.identity.client.msal.automationapp.testpass.network;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.internal.testutils.IShellCommandExecutor;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;
import com.microsoft.identity.internal.testutils.networkutils.INetworkTestRunner;
import com.microsoft.identity.internal.testutils.networkutils.NetworkTestConstants;
import com.microsoft.identity.internal.testutils.networkutils.NetworkTestResult;
import com.microsoft.identity.internal.testutils.networkutils.NetworkTestingManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractMsalNetworkTest extends AbstractMsalUiTest {

    private static final IShellCommandExecutor shellCommandExecutor = new IShellCommandExecutor() {
        @Override
        public String execute(String shellCommand) {
            return AdbShellUtils.executeShellCommand(shellCommand);
        }
    };

    @Override
    public void setup() {
        // Turn both WIFI and mobile data ON before running tests.
        resetNetworkState();
        super.setup();
    }

    private void resetNetworkState() {
        NetworkTestingManager.changeNetworkState(shellCommandExecutor, NetworkTestConstants.InterfaceType.WIFI_AND_CELLULAR, true);

        final CountDownLatch waiter = new CountDownLatch(1);
        try {
            // Wait for a few seconds for WIFI to turn ON
            waiter.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }


    public <T> void runNetworkTest(String inputFile, String expectedOutputFile, INetworkTestRunner<T> testRun) throws ClassNotFoundException, IOException {
        List<NetworkTestingManager> stateManagerList = NetworkTestingManager.readCSVFile(
                getClass(),
                shellCommandExecutor,
                inputFile, expectedOutputFile
        );


        for (NetworkTestingManager stateManager : stateManagerList) {
            submitForTestRun(stateManager, testRun);
        }
    }

    private <T> void submitForTestRun(final NetworkTestingManager networkTestingManager, final INetworkTestRunner<T> testRun) {
        final long startTime = System.currentTimeMillis();

        final Thread networkStateThread = networkTestingManager.execute();

        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(networkStateThread);

        final ResultFuture<T> testResults = new ResultFuture<>();

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                testRun.runTest(testResults);
            }
        });

        executorService.shutdown();

        final NetworkTestResult expectedResult = networkTestingManager.getTestResult();

        expectedResult.verifyResult(testResults, startTime);

        executorService.shutdownNow();

        // Reset the network state after running a test.
        resetNetworkState();
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

}
