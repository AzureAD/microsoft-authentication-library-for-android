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

import androidx.test.runner.AndroidJUnit4;

import com.microsoft.identity.client.internal.telemetry.ApiEvent;
import com.microsoft.identity.client.internal.telemetry.CacheEvent;
import com.microsoft.identity.client.internal.telemetry.DefaultEvent;
import com.microsoft.identity.client.internal.telemetry.Defaults;
import com.microsoft.identity.client.internal.telemetry.EventConstants;
import com.microsoft.identity.client.internal.telemetry.HttpEvent;
import com.microsoft.identity.client.internal.telemetry.PlatformIdHelper;
import com.microsoft.identity.client.internal.telemetry.UiEvent;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.identity.client.internal.telemetry.EventConstants.EventProperty;

@RunWith(AndroidJUnit4.class)
public class TelemetryTest {

    private static final String TEST_APPLICATION_NAME = "test-application-name";
    private static final String TEST_APPLICATION_VERSION = "v1.0";
    private static final String TEST_APPLICATION_CLIENT_ID = "12345";
    private static final String TEST_APPLICATION_DEVICE_ID = "678910";
    private static final int ACTUAL_RESULT_SIZE = 4;

    private Telemetry mTestInstance;

    @Before
    public void setUp() {
        DefaultEvent.initializeDefaults(
                new Defaults(TEST_APPLICATION_NAME, TEST_APPLICATION_VERSION,
                        TEST_APPLICATION_CLIENT_ID, TEST_APPLICATION_DEVICE_ID,
                        "v1.0", PlatformIdHelper.PlatformIdParameters.PRODUCT_NAME)
        );
        mTestInstance = Telemetry.getTestInstance();
        Telemetry.disableForTest(false);
    }

    @Test(expected = IllegalStateException.class)
    public void testTelemetryOnlyAllowsEventReceiverSetOnce() {
        mTestInstance.registerReceiver(new IMsalEventReceiver() {
            @Override
            public void onEventsReceived(List<Map<String, String>> events) {
                // no functionality needed
            }
        });
        mTestInstance.registerReceiver(new IMsalEventReceiver() {
            @Override
            public void onEventsReceived(List<Map<String, String>> events) {
                // no functionality needed
            }
        });
    }

    @Test
    public void testContainsDefaultEvent() {
        // mock out receiver object
        final IMsalEventReceiver mockReceiver = Mockito.mock(IMsalEventReceiver.class);

        // register it on the Telemetry instance
        mTestInstance.registerReceiver(mockReceiver);

        // create some Telemetry data
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        final HttpEvent.Builder httpEventBuilder = HttpEventTest.getTestHttpEventBuilder();
        mTestInstance.startEvent(telemetryRequestId, httpEventBuilder);
        mTestInstance.stopEvent(telemetryRequestId, httpEventBuilder);

        // flush the data to the receiver
        mTestInstance.flush(telemetryRequestId);

        // create a captor to 'catch' the results
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver, Mockito.only()).onEventsReceived(captor.capture());

        // retrive the value from the captor
        List<Map<String, String>> result = captor.getValue();

        // verify results
        Assert.assertEquals(result.size(), 2);

        // get the event entry, pos[1] (since DefaultEvent is always first)
        Map<String, String> eventData = result.get(0);

        // make sure it contains the correct event
        Assert.assertEquals(
                TEST_APPLICATION_NAME,
                eventData.get(EventProperty.APPLICATION_NAME)
        );

        Assert.assertEquals(
                TEST_APPLICATION_VERSION,
                eventData.get(EventProperty.APPLICATION_VERSION)
        );

        Assert.assertEquals(
                TEST_APPLICATION_CLIENT_ID,
                eventData.get(EventProperty.CLIENT_ID)
        );

        Assert.assertEquals(
                TEST_APPLICATION_DEVICE_ID,
                eventData.get(EventProperty.DEVICE_ID)
        );
    }

    @Test
    public void testFlushesEvents() {
        // mock out receiver object
        final IMsalEventReceiver mockReceiver = Mockito.mock(IMsalEventReceiver.class);

        // register it on the Telemetry instance
        mTestInstance.registerReceiver(mockReceiver);

        // create some Telemetry data
        final String telemetryRequestId = Telemetry.generateNewRequestId();
        final HttpEvent.Builder httpEventBuilder = HttpEventTest.getTestHttpEventBuilder();
        mTestInstance.startEvent(telemetryRequestId, httpEventBuilder);
        mTestInstance.stopEvent(telemetryRequestId, httpEventBuilder);

        // flush the data to the receiver
        mTestInstance.flush(telemetryRequestId);

        // create a captor to 'catch' the results
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver, Mockito.only()).onEventsReceived(captor.capture());

        // retrive the value from the captor
        List<Map<String, String>> result = captor.getValue();

        // verify results
        Assert.assertEquals(result.size(), 2);

        // get the event entry, pos[1] (since DefaultEvent is always first)
        Map<String, String> eventData = result.get(1);

        // make sure it contains the correct event
        Assert.assertEquals(
                EventConstants.EventName.HTTP_EVENT.toString(),
                eventData.get(EventProperty.EVENT_NAME)
        );
    }

    @Test
    public void testFlushesOnlyDesiredEvents() {
        // mock out receiver object
        final IMsalEventReceiver mockReceiver = Mockito.mock(IMsalEventReceiver.class);

        // register it on the Telemetry instance
        mTestInstance.registerReceiver(mockReceiver);

        // create some Telemetry data
        final String telemetryRequestId1 = Telemetry.generateNewRequestId();
        final HttpEvent.Builder httpEventBuilder = HttpEventTest.getTestHttpEventBuilder();
        mTestInstance.startEvent(telemetryRequestId1, httpEventBuilder);
        mTestInstance.stopEvent(telemetryRequestId1, httpEventBuilder);

        final String telemetryRequestId2 = Telemetry.generateNewRequestId();
        final CacheEvent.Builder cacheEventBuilder = CacheEventTest.getTestCacheEventBuilder(EventConstants.EventName.TOKEN_CACHE_LOOKUP, "bearer");
        mTestInstance.startEvent(telemetryRequestId2, cacheEventBuilder);
        mTestInstance.stopEvent(telemetryRequestId2, cacheEventBuilder);

        // flush the data to the receiver
        mTestInstance.flush(telemetryRequestId2);

        // create a captor to 'catch' the results
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver, Mockito.only()).onEventsReceived(captor.capture());

        // retrive the value from the captor
        List<Map<String, String>> result = captor.getValue();

        // verify results
        Assert.assertEquals(result.size(), 2);

        // get the event entry, pos[1] (since DefaultEvent is always first)
        Map<String, String> eventData = result.get(1);

        // make sure it contains the correct event
        Assert.assertEquals(
                EventConstants.EventName.TOKEN_CACHE_LOOKUP.toString(),
                eventData.get(EventProperty.EVENT_NAME)
        );
    }

    @Test
    public void testTelemetryDataSentOnlyOnFailure() {
        // Do not dispatch events if successful....
        mTestInstance.setTelemetryOnFailureOnly(true);

        // create the mock receiver
        final IMsalEventReceiver mockReceiver1 = Mockito.mock(IMsalEventReceiver.class);

        // register the mock receiver
        mTestInstance.registerReceiver(mockReceiver1);

        // Create some Telemetry data where the parent IApiEvent was successful
        final String telemetryRequestId1 = Telemetry.generateNewRequestId();
        final ApiEvent.Builder apiEventBuilder = new ApiEvent.Builder(telemetryRequestId1)
                .setApiId(EventConstants.ApiId.API_ID_ACQUIRE)
                .setCorrelationId(UUID.randomUUID())
                .setApiCallWasSuccessful(true);
        final UiEvent.Builder uiEventBuilder = new UiEvent.Builder();
        final CacheEvent.Builder cacheEventBuilder = new CacheEvent.Builder(EventConstants.EventName.TOKEN_CACHE_LOOKUP)
                .setTokenType("bearer");

        mTestInstance.startEvent(telemetryRequestId1, apiEventBuilder);
        mTestInstance.startEvent(telemetryRequestId1, uiEventBuilder);
        mTestInstance.startEvent(telemetryRequestId1, cacheEventBuilder);
        mTestInstance.stopEvent(telemetryRequestId1, cacheEventBuilder);
        mTestInstance.stopEvent(telemetryRequestId1, uiEventBuilder);
        mTestInstance.stopEvent(telemetryRequestId1, apiEventBuilder);

        mTestInstance.flush(telemetryRequestId1);

        // Assert that the mock receiver wasn't called
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver1, Mockito.never()).onEventsReceived(captor.capture());

        // renew the test instance....
        mTestInstance = Telemetry.getTestInstance();
        mTestInstance.setTelemetryOnFailureOnly(false);

        // create the mock receiver
        final IMsalEventReceiver mockReceiver2 = Mockito.mock(IMsalEventReceiver.class);

        // register the mock receiver
        mTestInstance.registerReceiver(mockReceiver2);

        // Create some Telemetry data where the parent IApiEvent was successful
        final String telemetryRequestId2 = Telemetry.generateNewRequestId();
        final ApiEvent.Builder apiEventBuilder2 = new ApiEvent.Builder(telemetryRequestId2)
                .setApiId(EventConstants.ApiId.API_ID_ACQUIRE)
                .setCorrelationId(UUID.randomUUID())
                .setApiCallWasSuccessful(true);
        final UiEvent.Builder uiEventBuilder2 = new UiEvent.Builder();
        final CacheEvent.Builder cacheEventBuilder2 = new CacheEvent.Builder(EventConstants.EventName.TOKEN_CACHE_LOOKUP)
                .setTokenType("bearer");

        mTestInstance.startEvent(telemetryRequestId2, apiEventBuilder2);
        mTestInstance.startEvent(telemetryRequestId2, uiEventBuilder2);
        mTestInstance.startEvent(telemetryRequestId2, cacheEventBuilder2);
        mTestInstance.stopEvent(telemetryRequestId2, cacheEventBuilder2);
        mTestInstance.stopEvent(telemetryRequestId2, uiEventBuilder2);
        mTestInstance.stopEvent(telemetryRequestId2, apiEventBuilder2);

        mTestInstance.flush(telemetryRequestId2);

        // Assert that the mock receiver was called
        final ArgumentCaptor<List> captor2 = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver2, Mockito.only()).onEventsReceived(captor2.capture());

        List<Map<String, String>> result = captor2.getValue();

        // verify results
        Assert.assertEquals(result.size(), ACTUAL_RESULT_SIZE);
    }

    @Test
    public void testOrphanedEventsHandled() {
        final IMsalEventReceiver mockReceiver = Mockito.mock(IMsalEventReceiver.class);

        mTestInstance.registerReceiver(mockReceiver);

        // Create some Telemetry data, purposefully orphan the UiEvent
        final String telemetryRequestId1 = Telemetry.generateNewRequestId();
        final ApiEvent.Builder apiEventBuilder = new ApiEvent.Builder(telemetryRequestId1)
                .setApiId(EventConstants.ApiId.API_ID_ACQUIRE)
                .setCorrelationId(UUID.randomUUID())
                .setApiCallWasSuccessful(true);
        final UiEvent.Builder uiEventBuilder = new UiEvent.Builder();
        final CacheEvent.Builder cacheEventBuilder = new CacheEvent.Builder(EventConstants.EventName.TOKEN_CACHE_LOOKUP)
                .setTokenType("bearer").setIsAT(true);

        mTestInstance.startEvent(telemetryRequestId1, apiEventBuilder);
        mTestInstance.startEvent(telemetryRequestId1, uiEventBuilder);
        mTestInstance.startEvent(telemetryRequestId1, cacheEventBuilder);
        mTestInstance.stopEvent(telemetryRequestId1, cacheEventBuilder);
        // Do not stop the UiEvent...
        //mTestInstance.stopEvent(uiEventBuilder.build());
        mTestInstance.stopEvent(telemetryRequestId1, apiEventBuilder);

        mTestInstance.flush(telemetryRequestId1);

        // create a captor to 'catch' the results
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver, Mockito.only()).onEventsReceived(captor.capture());

        // retrive the value from the captor
        List<Map<String, String>> result = captor.getValue();

        // verify results, should contain 4 events, one orphaned...
        Assert.assertEquals(result.size(), ACTUAL_RESULT_SIZE);

        boolean orphanedEventVerified = false;
        for (final Map<String, String> event : result) {
            if (event.get(EventProperty.EVENT_NAME) != null && event.get(EventProperty.EVENT_NAME).equals(EventConstants.EventName.UI_EVENT.toString())) {
                Assert.assertEquals(event.get(EventProperty.STOP_TIME), "-1");
                orphanedEventVerified = true;
                break;
            }
        }
        Assert.assertTrue(orphanedEventVerified);
    }

}
