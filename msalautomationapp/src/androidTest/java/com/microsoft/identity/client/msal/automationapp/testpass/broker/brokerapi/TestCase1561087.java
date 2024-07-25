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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.brokerapi;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LocalBrokerHostDebugUiTest;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

// Flight settings
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1561087
@SupportedBrokers(brokers = BrokerHost.class)
@LocalBrokerHostDebugUiTest
@RetryOnFailure
public class TestCase1561087 extends AbstractMsalBrokerTest {
    @Test
    public void test_1561087() {
        // Skipping this test is brokerhost is using local flights
        Assume.assumeFalse(((BrokerHost) mBroker).isLocalFlightProviderSelector());

        // Set flights and get to check if the flight information is returned
        final String flightKey = "SetFlightsTest";
        final String flightValue = "true";
        final String flightsJson = "{\""+flightKey+"\":\""+flightValue+"\"}";
        mBroker.overwriteFlights(flightsJson);
        checkIfBrokerContainsFlight(flightKey, flightValue);

        // Add flights and get to check if the flight information is returned
        final String anotherFlightKey = "AnotherFlight";
        final String anotherFlightValue = "hello";
        mBroker.setFlights(anotherFlightKey, anotherFlightValue);
        checkIfBrokerContainsFlight(anotherFlightKey, anotherFlightValue);
        checkIfBrokerContainsFlight(flightKey, flightValue);

        // Override flights and get to check if the flight information is returned
        final String flightToOverwriteKey = "SetFlightsTest";
        final String flightToOverwriteValue = "false";
        mBroker.setFlights(flightToOverwriteKey, flightToOverwriteValue);
        checkIfBrokerContainsFlight(anotherFlightKey, anotherFlightValue);
        checkIfBrokerContainsFlight(flightToOverwriteKey, flightToOverwriteValue);

        // Add flight with null value. SetFlightsTest should be removed.
        final String flightMapWithEmptyValueKey = "SetFlightsTest";
        final String flightMapWithEmptyValueValue = "";
        mBroker.setFlights(flightMapWithEmptyValueKey, flightMapWithEmptyValueValue);
        checkIfBrokerContainsFlight(anotherFlightKey, anotherFlightValue);
        Assert.assertFalse(mBroker.getFlights().contains("SetFlightsTest"));

        // Add an empty flight map. the flight map should not change.
        final String oldFlights = mBroker.getFlights();
        mBroker.setFlights("", "");
        Assert.assertEquals(oldFlights, mBroker.getFlights());

        // clear flights and get to check if the flights are cleared.
        // Looks like some defaults flights are added even after flights are cleared, so we can check that the flights we added have been deleted.
        mBroker.overwriteFlights("{}");
        Assert.assertFalse(mBroker.getFlights().contains("AnotherFlight"));
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

    /**
     * Check if the Broker contain the fligth key and value
     *
     * @param flightKey   flight key to be checked against flight set from BrokerHost
     * @param flightValue flight value to be checked against flight set from BrokerHost
     */
    private void checkIfBrokerContainsFlight(@NonNull final String flightKey, @NonNull final String flightValue) {
        final String flights = mBroker.getFlights();
        Assert.assertTrue(flights.contains(flightKey));
        Assert.assertTrue(flights.contains(flightValue));
    }
}
