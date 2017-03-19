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

import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.identity.client.EventConstants.EventProperty;

public class Telemetry implements ITelemetry {

    private static final Telemetry INSTANCE = new Telemetry();

    private final Map<Pair<RequestId, EventName>, EventStartTime> mEventsInProgress;

    private final Map<RequestId, List<IEvent>> mCompletedEvents;

    private EventDispatcher mPublisher;

    private boolean mTelemetryOnFailureOnly = false;

    private Telemetry() {
        mEventsInProgress = Collections.synchronizedMap(
                new LinkedHashMap<Pair<RequestId, EventName>, EventStartTime>()
        );
        mCompletedEvents = Collections.synchronizedMap(
                new LinkedHashMap<RequestId, List<IEvent>>()
        );
    }

    static Telemetry getTestInstance() {
        return new Telemetry();
    }

    static Telemetry getInstance() {
        return INSTANCE;
    }

    static RequestId generateNewRequestId() {
        return new RequestId(UUID.randomUUID().toString());
    }

    @Override
    public synchronized void registerReceiver(MsalEventReceiver dispatcher) {
        // check to make sure we're not already dispatching elsewhere
        if (null != mPublisher) {
            throw new IllegalStateException(
                    MsalEventReceiver.class.getSimpleName()
                            + " instances are not swappable at this time."
            );
        }

        // set this dispatcher
        mPublisher = new EventDispatcher(dispatcher);
    }

    @Override
    public void setTelemetryOnFailure(final boolean onFailure) {
        mTelemetryOnFailureOnly = onFailure;
    }

    void startEvent(final RequestId requestId, final EventName eventName) {
        if (null == mPublisher) {
            // no publisher, abort
            return;
        }

        mEventsInProgress.put( // add the new event
                new Pair<>( // create the composite key
                        requestId,
                        eventName
                ),
                new EventStartTime( // create the value using systime
                        Long.toString(
                                System.currentTimeMillis()
                        )
                )
        );
    }

    void stopEvent(final RequestId requestId, final EventName eventName, final IEvent event) {
        final Pair<RequestId, EventName> eventKey = new Pair<>(requestId, eventName);

        // Compute execution time
        final EventStartTime eventStartTime = mEventsInProgress.get(eventKey);
        final long startTimeL = Long.parseLong(eventStartTime.value);
        final long stopTimeL = System.currentTimeMillis();
        final long diffTime = stopTimeL - startTimeL;
        final String stopTime = Long.toString(stopTimeL);

        // Set execution time properties on the event
        event.setProperty(EventProperty.START_TIME, eventStartTime.value);
        event.setProperty(EventProperty.STOP_TIME, stopTime);
        event.setProperty(EventProperty.RESPONSE_TIME, Long.toString(diffTime));

        if (null == mCompletedEvents.get(requestId)) {
            // if this is the first event associated to this
            // RequestId we need to initialize a new List to hold
            // all of sibling events
            mCompletedEvents.put(
                    requestId,
                    new ArrayList<IEvent>() {{
                        add(event);
                    }}
            );
        } else {
            // if this event shares a RequestId with other events
            // just add it to the List
            mCompletedEvents.get(requestId).add(event);
        }

        // Mark this event as no longer in progress
        mEventsInProgress.remove(eventKey);
    }

    void flush(final RequestId requestId) {
        // check for orphaned events...
        for (Pair<RequestId, EventName> key : mEventsInProgress.keySet()) {
            if (key.first.equals(requestId)) {
                // this event was orphaned...
                final String orphanedRequestId = key.first.value;
                final String orphanedEventName = key.second.value;
                final String orphanedEventStartTime =
                        mEventsInProgress.remove(key).value; // remove() this entry
                // TODO what should I do with this information?
            }
        }
        final List<IEvent> eventsToFlush = mCompletedEvents.remove(requestId);
        mPublisher.dispatch(eventsToFlush);
    }

    static abstract class ValueTypeDef {

        final String value;

        ValueTypeDef(final String value) {
            this.value = value;
        }
    }

    static final class RequestId extends ValueTypeDef {

        RequestId(final String value) {
            super(value);
        }

    }

    private static final class EventStartTime extends ValueTypeDef {

        private EventStartTime(final String value) {
            super(value);
        }

    }
}
