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
import com.microsoft.identity.client.ui.automation.network.INetworkTestRunner;
import com.microsoft.identity.client.ui.automation.network.NetworkTestConstants;
import com.microsoft.identity.client.ui.automation.network.NetworkTestResult;
import com.microsoft.identity.client.ui.automation.network.NetworkTestingManager;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Sets up a base class for running MSAL network tests.
 */
public class BaseMsalNetworkTest extends AbstractMsalUiTest {


    @Override
    public void setup() {
        // Turn both WIFI and mobile data ON before running tests.
        resetNetworkState();
        super.setup();
    }

    /**
     * Reset the network state to WIFI_AND_CELLULAR
     */
    private void resetNetworkState() {
        final NetworkTestConstants.InterfaceType previousInterface = NetworkTestingManager.getCurrentInterface();

        NetworkTestingManager.changeNetworkState(NetworkTestConstants.InterfaceType.WIFI_AND_CELLULAR);

        // If the previous interface WIFI was turned off, wait for a few seconds to turn WIFI on.
        if (previousInterface == null || !previousInterface.wifiActive()) {
            final CountDownLatch waiter = new CountDownLatch(1);
            try {
                waiter.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
        }
    }


    public Collection<DynamicTest> dynamicNetworkTests(String inputFile, String expectedOutputFile, INetworkTestRunner testRun) throws ClassNotFoundException, IOException {
        List<NetworkTestingManager> stateManagerList = NetworkTestingManager.readCSVFile(
                getClass(),
                inputFile, expectedOutputFile
        );

        final Set<DynamicTest> tests = new LinkedHashSet<>(stateManagerList.size());


        for (int i = 0; i < stateManagerList.size(); i++) {
            NetworkTestingManager stateManager = stateManagerList.get(i);

            tests.add(DynamicTest.dynamicTest(stateManager.getId(), new Executable() {
                @Override
                public void execute() throws Throwable {
                    submitForTestRun(stateManager, testRun);
                    resetNetworkState();
                }
            }));
        }

        return tests;
    }

    private void submitForTestRun(final NetworkTestingManager networkTestingManager, final INetworkTestRunner testRun) {
        final Thread networkStateThread = networkTestingManager.execute();

        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(networkStateThread);

        final ResultFuture<String, Exception> testResults = new ResultFuture<>();


        executorService.submit(new Runnable() {
            @Override
            public void run() {
                testRun.runTest(testResults);
            }
        });

        executorService.shutdown();

        final NetworkTestResult expectedResult = networkTestingManager.getTestResult();

        expectedResult.verifyResult(testResults);

        executorService.shutdownNow();
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
