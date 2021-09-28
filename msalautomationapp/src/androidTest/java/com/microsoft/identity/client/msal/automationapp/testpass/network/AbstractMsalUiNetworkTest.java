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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.network.NetworkTestConstants;
import com.microsoft.identity.client.ui.automation.network.NetworkTestResult;
import com.microsoft.identity.client.ui.automation.network.NetworkTestRunner;
import com.microsoft.identity.client.ui.automation.network.NetworkTestStateManager;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Sets up a base class for running MSAL network tests.
 */
public abstract class AbstractMsalUiNetworkTest<T> extends AbstractMsalUiTest implements NetworkTestRunner<T> {

    /**
     * Generate testing parameters for a network test
     *
     * @param inputFile          a string representing the name of the CSV file that contains the network
     *                           states.
     * @param expectedResultFile a string representing the name of the CSV file that defines the expected result after
     *                           the the test is run
     * @return a list of object arrays that contain a {@link NetworkTestStateManager} to be passed to a network test {@link AbstractMsalUiNetworkTest} and the test id
     */
    public static Iterable<Object[]> generateTestParameters(
            @NonNull final String inputFile,
            @NonNull final String expectedResultFile) throws IOException, ClassNotFoundException {
        final List<NetworkTestStateManager> stateManagers = NetworkTestStateManager.readCSVFile(AbstractMsalUiNetworkTest.class, inputFile, expectedResultFile);

        final List<Object[]> parameters = new ArrayList<>();

        int index = 0;

        for (NetworkTestStateManager networkTestStateManager : stateManagers) {
            parameters.add(new Object[]{networkTestStateManager, networkTestStateManager.getId(), index});
            index++;
        }
        return parameters;
    }

    @Parameterized.Parameter
    public NetworkTestStateManager testingManager;

    @Parameterized.Parameter(value = 1)
    public String testId;

    @Parameterized.Parameter(value = 2)
    public int testIndex;


    @Override
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();

        // Turn WIFI ON before running the first test.
        resetNetworkState();

        super.setup();
    }


    /**
     * Reset the network state to WIFI
     */
    private void resetNetworkState() {
        NetworkTestStateManager.changeNetworkState(NetworkTestConstants.InterfaceType.WIFI_AND_CELLULAR);

        final ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        final ConnectivityManager.NetworkCallback networkCallback;

        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            final CountDownLatch wifiWaiter = new CountDownLatch(1);

            connectivityManager.registerDefaultNetworkCallback(networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    wifiWaiter.countDown();
                }
            });

            try {
                // If the device is not connected to the internet, wait for WIFI to turn ON
                wifiWaiter.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }

            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }


    @Test
    public void runNetworkTest() {
        final Thread networkStateThread = testingManager.execute();

        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        final ResultFuture<String, Exception> testResults = new ResultFuture<>();

        final T prerequisites = prepare();

        final long startTime = System.currentTimeMillis();

        executorService.execute(networkStateThread);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                execute(prerequisites, testResults);
            }
        });

        executorService.shutdown();


        final NetworkTestResult expectedResult = testingManager.getTestResult();

        expectedResult.verifyResult(timeoutSeconds(), testResults, startTime);

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

    @Override
    public long timeoutSeconds() {
        return TimeUnit.MINUTES.toSeconds(10);
    }
}
