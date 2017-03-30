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
import java.util.UUID;

import static com.microsoft.identity.client.EventConstants.EventProperty;

@RunWith(AndroidJUnit4.class)
public class TelemetryTest {

    private Telemetry mTestInstance;

    @Before
    public void setUp() {
        mTestInstance = Telemetry.getTestInstance();
        Telemetry.disableForTest(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testTelemetryOnlyAllowsEventReceiverSetOnce() {
        mTestInstance.registerReceiver(new MsalEventReceiver() {
            @Override
            public void onEventsReceived(List<Map<String, String>> events) {
                // no functionality needed
            }
        });
        mTestInstance.registerReceiver(new MsalEventReceiver() {
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
        mTestInstance.registerReceiver(mockReceiver);

        // create some Telemetry data
        final Telemetry.RequestId requestId = Telemetry.generateNewRequestId();
        mTestInstance.startEvent(requestId, EventName.HTTP_EVENT);
        mTestInstance.stopEvent(requestId, EventName.HTTP_EVENT, HttpEventTest.getTestHttpEvent(requestId));

        // flush the data to the receiver
        mTestInstance.flush(requestId);

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
                EventName.HTTP_EVENT.toString(),
                eventData.get(EventProperty.EVENT_NAME)
        );
    }

    @Test
    public void testFlushesOnlyDesiredEvents() {
        // mock out receiver object
        final MsalEventReceiver mockReceiver = Mockito.mock(MsalEventReceiver.class);

        // register it on the Telemetry instance
        mTestInstance.registerReceiver(mockReceiver);

        // create some Telemetry data
        final Telemetry.RequestId requestId1 = Telemetry.generateNewRequestId();
        mTestInstance.startEvent(requestId1, EventName.HTTP_EVENT);
        mTestInstance.stopEvent(requestId1, EventName.HTTP_EVENT, HttpEventTest.getTestHttpEvent(requestId1));

        final Telemetry.RequestId requestId2 = Telemetry.generateNewRequestId();
        mTestInstance.startEvent(requestId2, EventName.TOKEN_CACHE_LOOKUP);
        mTestInstance.stopEvent(
                requestId2,
                EventName.TOKEN_CACHE_LOOKUP,
                CacheEventTest.getTestCacheEvent(
                        requestId2,
                        EventName.TOKEN_CACHE_LOOKUP,
                        CacheEventTest.TEST_TOKEN_TYPE_AT
                )
        );

        // flush the data to the receiver
        mTestInstance.flush(requestId2);

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
                EventName.TOKEN_CACHE_LOOKUP.toString(),
                eventData.get(EventProperty.EVENT_NAME)
        );
    }

    @Test
    public void testTelemetryDataSentOnlyOnFailure() {
        // Do not dispatch events if successful....
        mTestInstance.setTelemetryOnFailure(true);

        // create the mock receiver
        final MsalEventReceiver mockReceiver1 = Mockito.mock(MsalEventReceiver.class);

        // register the mock receiver
        mTestInstance.registerReceiver(mockReceiver1);

        // Create some Telemetry data where the parent IApiEvent was successful
        final Telemetry.RequestId requestId1 = Telemetry.generateNewRequestId();
        final ApiEvent.Builder apiEventBuilder = new ApiEvent.Builder(requestId1)
                .setApiId(EventConstants.ApiId.API_ID_ACQUIRE)
                .setCorrelationId(UUID.randomUUID())
                .setApiCallWasSuccessful(true);
        final UiEvent.Builder uiEventBuilder = new UiEvent.Builder(requestId1)
                .setRedirectCount(0);
        final CacheEvent.Builder cacheEventBuilder = new CacheEvent.Builder(requestId1, EventName.TOKEN_CACHE_LOOKUP)
                .setTokenType(EventProperty.Value.TOKEN_TYPE_AT);

        mTestInstance.startEvent(apiEventBuilder);
        mTestInstance.startEvent(uiEventBuilder);
        mTestInstance.startEvent(cacheEventBuilder);
        mTestInstance.stopEvent(cacheEventBuilder.build());
        mTestInstance.stopEvent(uiEventBuilder.build());
        mTestInstance.stopEvent(apiEventBuilder.build());

        mTestInstance.flush(requestId1);

        // Assert that the mock receiver wasn't called
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver1, Mockito.never()).onEventsReceived(captor.capture());

        // renew the test instance....
        mTestInstance = Telemetry.getTestInstance();
        mTestInstance.setTelemetryOnFailure(false);

        // create the mock receiver
        final MsalEventReceiver mockReceiver2 = Mockito.mock(MsalEventReceiver.class);

        // register the mock receiver
        mTestInstance.registerReceiver(mockReceiver2);

        // Create some Telemetry data where the parent IApiEvent was successful
        final Telemetry.RequestId requestId2 = Telemetry.generateNewRequestId();
        final ApiEvent.Builder apiEventBuilder2 = new ApiEvent.Builder(requestId2)
                .setApiId(EventConstants.ApiId.API_ID_ACQUIRE)
                .setCorrelationId(UUID.randomUUID())
                .setApiCallWasSuccessful(true);
        final UiEvent.Builder uiEventBuilder2 = new UiEvent.Builder(requestId2)
                .setRedirectCount(0);
        final CacheEvent.Builder cacheEventBuilder2 = new CacheEvent.Builder(requestId2, EventName.TOKEN_CACHE_LOOKUP)
                .setTokenType(EventProperty.Value.TOKEN_TYPE_AT);

        mTestInstance.startEvent(apiEventBuilder2);
        mTestInstance.startEvent(uiEventBuilder2);
        mTestInstance.startEvent(cacheEventBuilder2);
        mTestInstance.stopEvent(cacheEventBuilder2.build());
        mTestInstance.stopEvent(uiEventBuilder2.build());
        mTestInstance.stopEvent(apiEventBuilder2.build());

        mTestInstance.flush(requestId2);

        // Assert that the mock receiver wasn called
        final ArgumentCaptor<List> captor2 = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver2, Mockito.only()).onEventsReceived(captor2.capture());

        List<Map<String, String>> result = captor2.getValue();

        // verify results
        Assert.assertEquals(result.size(), 3);
    }

    @Test
    public void testOrphanedEventsHandled() {
        final MsalEventReceiver mockReceiver = Mockito.mock(MsalEventReceiver.class);

        mTestInstance.registerReceiver(mockReceiver);

        // Create some Telemetry data, purposefully orphan the UiEvent
        final Telemetry.RequestId requestId1 = Telemetry.generateNewRequestId();
        final ApiEvent.Builder apiEventBuilder = new ApiEvent.Builder(requestId1)
                .setApiId(EventConstants.ApiId.API_ID_ACQUIRE)
                .setCorrelationId(UUID.randomUUID())
                .setApiCallWasSuccessful(true);
        final UiEvent.Builder uiEventBuilder = new UiEvent.Builder(requestId1)
                .setRedirectCount(0);
        final CacheEvent.Builder cacheEventBuilder = new CacheEvent.Builder(requestId1, EventName.TOKEN_CACHE_LOOKUP)
                .setTokenType(EventProperty.Value.TOKEN_TYPE_AT);

        mTestInstance.startEvent(apiEventBuilder);
        mTestInstance.startEvent(uiEventBuilder);
        mTestInstance.startEvent(cacheEventBuilder);
        mTestInstance.stopEvent(cacheEventBuilder.build());
        // Do not stop the UiEvent...
        //mTestInstance.stopEvent(uiEventBuilder.build());
        mTestInstance.stopEvent(apiEventBuilder.build());

        mTestInstance.flush(requestId1);

        // create a captor to 'catch' the results
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver, Mockito.only()).onEventsReceived(captor.capture());

        // retrive the value from the captor
        List<Map<String, String>> result = captor.getValue();

        // verify results, should contain 3 events, one orphaned...
        Assert.assertEquals(result.size(), 3);

        boolean orphanedEventVerified = false;
        for (final Map<String, String> event : result) {
            if (event.get(EventProperty.EVENT_NAME).equals(EventName.UI_EVENT.toString())) {
                Assert.assertEquals(event.get(EventProperty.STOP_TIME), "-1");
                orphanedEventVerified = true;
                break;
            }
        }
        Assert.assertTrue(orphanedEventVerified);
    }

}
