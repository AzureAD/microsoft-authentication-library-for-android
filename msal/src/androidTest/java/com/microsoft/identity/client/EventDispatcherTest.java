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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class EventDispatcherTest {

    @Test
    public void testEventsDispatchedInOrder() {
        // mock out receiver object
        final MsalEventReceiver mockReceiver = Mockito.mock(MsalEventReceiver.class);
        final EventDispatcher dispatcher = new EventDispatcher(mockReceiver);

        // create a List to hold some events
        List<IEvent> eventList = new ArrayList<>();

        // create some test events
        final Telemetry.RequestId cacheEventRequestId = Telemetry.generateNewRequestId();
        final EventName cacheEventName = EventName.TOKEN_CACHE_LOOKUP;
        final ICacheEvent cacheEvent =
                CacheEventTest.getTestCacheEvent(
                        cacheEventRequestId,
                        cacheEventName,
                        CacheEventTest.TEST_TOKEN_TYPE_AT
                );

        final Telemetry.RequestId httpEventRequestId = Telemetry.generateNewRequestId();
        final IHttpEvent httpEvent = HttpEventTest.getTestHttpEvent(httpEventRequestId);


        final Telemetry.RequestId uiEventRequestId = Telemetry.generateNewRequestId();
        final IUiEvent uiEvent = UiEventTest.getTestUiEvent(uiEventRequestId);


        final Telemetry.RequestId apiEventRequestId = Telemetry.generateNewRequestId();
        final IApiEvent apiEvent = ApiEventTest.getTestApiEvent(apiEventRequestId);
        // add them to the list
        eventList.add(cacheEvent);
        eventList.add(httpEvent);
        eventList.add(uiEvent);
        eventList.add(apiEvent);

        // dispatch them to the receiver
        dispatcher.dispatch(eventList);

        // check the results are in order
        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(mockReceiver, Mockito.only()).onEventsReceived(captor.capture());
        List<Map<String, String>> result = captor.getValue();

        int index = 0;
        for (final IEvent event : eventList) {
            Assert.assertEquals(event.getRequestId(), new Telemetry.RequestId(result.get(index).get(EventConstants.EventProperty.REQUEST_ID)));
            index++;
        }
    }

}
