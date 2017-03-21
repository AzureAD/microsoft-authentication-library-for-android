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

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static com.microsoft.identity.client.EventConstants.EventProperty;

@RunWith(AndroidJUnit4.class)
public class TelemetryTest {

    Telemetry testInstance;

    @Before
    public void setUp() {
        testInstance = Telemetry.getTestInstance();
        Telemetry.disableForTest(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testTelemetryOnlyAllowsEventReceiverSetOnce() {
        testInstance.registerReceiver(new MsalEventReceiver() {
            @Override
            public void onEventsReceived(List<Map<String, String>> events) {
                // no functionality needed
            }
        });
        testInstance.registerReceiver(new MsalEventReceiver() {
            @Override
            public void onEventsReceived(List<Map<String, String>> events) {
                // no functionality needed
            }
        });
    }

    @Test
    public void testFlushesEvents() {
        // mock out receiver object
        final MsalEventReceiver mockReceiver = Mockito.mock(MsalEventReceiver.class);

        // register it on the Telemetry instance
        testInstance.registerReceiver(mockReceiver);

        // create some Telemetry data
        final Telemetry.RequestId requestId = Telemetry.generateNewRequestId();
        testInstance.startEvent(requestId, EventName.HTTP_EVENT);
        testInstance.stopEvent(requestId, EventName.HTTP_EVENT, HttpEventTest.getTestHttpEvent(requestId));

        // flush the data to the receiver
        testInstance.flush(requestId);

        // create a captor to 'catch' the results
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver, Mockito.only()).onEventsReceived(captor.capture());

        // retrive the value from the captor
        List<Map<String, String>> result = captor.getValue();

        // verify results
        Assert.assertEquals(result.size(), 1);

        // get the event entry
        Map<String, String> eventData = result.get(0);

        // make sure it contains the correct event
        Assert.assertEquals(
                EventName.HTTP_EVENT.value,
                eventData.get(EventProperty.EVENT_NAME)
        );
    }

    @Test
    public void testFlushesOnlyDesiredEvents() {
        // mock out receiver object
        final MsalEventReceiver mockReceiver = Mockito.mock(MsalEventReceiver.class);

        // register it on the Telemetry instance
        testInstance.registerReceiver(mockReceiver);

        // create some Telemetry data
        final Telemetry.RequestId requestId1 = Telemetry.generateNewRequestId();
        testInstance.startEvent(requestId1, EventName.HTTP_EVENT);
        testInstance.stopEvent(requestId1, EventName.HTTP_EVENT, HttpEventTest.getTestHttpEvent(requestId1));

        final Telemetry.RequestId requestId2 = Telemetry.generateNewRequestId();
        testInstance.startEvent(requestId2, EventName.TOKEN_CACHE_LOOKUP);
        testInstance.stopEvent(
                requestId2,
                EventName.TOKEN_CACHE_LOOKUP,
                CacheEventTest.getTestCacheEvent(
                        requestId2,
                        EventName.TOKEN_CACHE_LOOKUP,
                        CacheEventTest.sTestTokenTypeAT
                )
        );

        // flush the data to the receiver
        testInstance.flush(requestId2);

        // create a captor to 'catch' the results
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver, Mockito.only()).onEventsReceived(captor.capture());

        // retrive the value from the captor
        List<Map<String, String>> result = captor.getValue();

        // verify results
        Assert.assertEquals(result.size(), 1);

        // get the event entry
        Map<String, String> eventData = result.get(0);

        // make sure it contains the correct event
        Assert.assertEquals(
                EventName.TOKEN_CACHE_LOOKUP.value,
                eventData.get(EventProperty.EVENT_NAME)
        );
    }

    @Test
    public void testTelemetryDataSentOnlyOnFailure() {
        // TODO
    }

    @Test
    public void testOrphanedEventsHandled() {
        // TODO how should they be handled?
    }

}
