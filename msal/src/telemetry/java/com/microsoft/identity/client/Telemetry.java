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

/**
 * Collects and publishes telemetry key/value pairs to subscribers.
 */
public final class Telemetry implements ITelemetry {

    private static final Telemetry INSTANCE = new Telemetry();

    private static boolean sDisableForTest;

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

    /**
     * Returns a new Telemetry instance.
     * ** This is for testing purposes only. **
     *
     * @return a new Telemetry instance.
     */
    static Telemetry getTestInstance() {
        return new Telemetry();
    }

    /**
     * Returns the Telemetry singleton.
     *
     * @return the Telemetry singleton.
     */
    public static Telemetry getInstance() {
        return INSTANCE;
    }

    /**
     * Generates a new {@link RequestId}.
     *
     * @return a random UUID (in String format).
     */
    static RequestId generateNewRequestId() {
        return new RequestId(UUID.randomUUID().toString());
    }

    /**
     * Mark that the system is running under test conditions and that Telemetry should be disabled.
     */
    static void disableForTest(final boolean disabled) {
        sDisableForTest = disabled;
    }

    @Override
    public synchronized void registerReceiver(MsalEventReceiver receiver) {
        // check to make sure we're not already dispatching elsewhere
        if (null != mPublisher) {
            throw new IllegalStateException(
                    MsalEventReceiver.class.getSimpleName()
                            + " instances are not swappable at this time."
            );
        }

        // set this dispatcher
        mPublisher = new EventDispatcher(receiver);
    }

    @Override
    public void setTelemetryOnFailureOnly(final boolean onFailure) {
        mTelemetryOnFailureOnly = onFailure;
    }

    /**
     * Stars recording a new Event, based on {@link Telemetry.RequestId}
     *
     * @param requestId the RequestId used to track this Event.
     * @param eventName the name of the Event which is to be tracked.
     */
    void startEvent(final Telemetry.RequestId requestId, final EventName eventName) {
        if (null == mPublisher || sDisableForTest) {
            return;
        }

        mEventsInProgress.put(new Pair<>(requestId, eventName), new EventStartTime(Long.toString(System.currentTimeMillis())));
    }

    /**
     * Stops a previously started Event using its Event.Builder.
     *
     * @param requestId    the RequestId of the Event to stop.
     * @param eventBuilder the Event.Builder used to create the Event.
     */
    void stopEvent(final Telemetry.RequestId requestId, final Event.Builder eventBuilder) {
        stopEvent(requestId, eventBuilder.getEventName(), eventBuilder.build());
    }

    /**
     * Stops a previously started Event.
     *
     * @param requestId   the RequestId of the Event to stop.
     * @param eventName   the name of the Event to stop.
     * @param eventToStop the Event data.
     */
    void stopEvent(final Telemetry.RequestId requestId, final EventName eventName, final IEvent eventToStop) {
        if (null == mPublisher || sDisableForTest) {
            return;
        }

        final Pair<RequestId, EventName> eventKey = new Pair<>(requestId, eventName);

        // Compute execution time
        final EventStartTime eventStartTime = mEventsInProgress.get(eventKey);
        final long startTimeL = Long.parseLong(eventStartTime.toString());
        final long stopTimeL = System.currentTimeMillis();
        final long diffTime = stopTimeL - startTimeL;
        final String stopTime = Long.toString(stopTimeL);

        // Set execution time properties on the event
        eventToStop.setProperty(EventProperty.START_TIME, eventStartTime.toString());
        eventToStop.setProperty(EventProperty.STOP_TIME, stopTime);
        eventToStop.setProperty(EventProperty.ELAPSED_TIME, Long.toString(diffTime));

        if (null == mCompletedEvents.get(requestId)) {
            // if this is the first event associated to this
            // RequestId we need to initialize a new List to hold
            // all of sibling events
            final List<IEvent> events = new ArrayList<>();
            events.add(eventToStop);
            mCompletedEvents.put(
                    requestId,
                    events
            );
        } else {
            // if this event shares a RequestId with other events
            // just add it to the List
            mCompletedEvents.get(requestId).add(eventToStop);
        }

        // Mark this event as no longer in progress
        mEventsInProgress.remove(eventKey);
    }

    /**
     * Flushes collected Events matching the supplied {@link RequestId} to the receiver.
     *
     * @param requestId Events matching the supplied RequestId will be flushed.
     */
    void flush(final RequestId requestId) {
        // check for orphaned events...
        List<IEvent> orphanedEvents = new ArrayList<>();
        collateOrphanedEvents(requestId, orphanedEvents);
        // Add the OrphanedEvents to the existing IEventList
        if (null != mCompletedEvents.get(requestId)) {
            mCompletedEvents.get(requestId).addAll(orphanedEvents);
        }

        final List<IEvent> eventsToFlush = mCompletedEvents.remove(requestId);

        if (mTelemetryOnFailureOnly) {
            // iterate over Events, if the ApiEvent was successful, don't dispatch
            boolean shouldRemoveEvents = false;

            for (IEvent event : eventsToFlush) {
                if (event instanceof IApiEvent) {
                    IApiEvent apiEvent = (IApiEvent) event;
                    shouldRemoveEvents = apiEvent.wasSuccessful();
                    break;
                }
            }

            if (shouldRemoveEvents) {
                eventsToFlush.clear();
            }
        }

        if (null != mPublisher && !eventsToFlush.isEmpty()) {
            eventsToFlush.add(0, new DefaultEvent.Builder().build());
            mPublisher.dispatch(eventsToFlush);
        }
    }

    private void collateOrphanedEvents(RequestId requestId, List<IEvent> orphanedEvents) {
        for (Pair<RequestId, EventName> key : mEventsInProgress.keySet()) {
            if (key.first.equals(requestId)) {
                final EventName orphanedEventName = key.second;
                final String orphanedEventStartTime =
                        mEventsInProgress.remove(key).toString(); // remove() this entry (clean up!)
                // Build the OrphanedEvent...
                IEvent orphanedEvent = new OrphanedEvent.Builder(orphanedEventName, orphanedEventStartTime).build();
                orphanedEvents.add(orphanedEvent);
            }
        }
    }

    /**
     * Abstract container class for String values.
     */
    abstract static class ValueTypeDef {

        private final String mValue;

        @Override
        public String toString() {
            return mValue;
        }

        ValueTypeDef(final String v) {
            this.mValue = v;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ValueTypeDef that = (ValueTypeDef) o;

            return mValue != null ? mValue.equals(that.mValue) : that.mValue == null;
        }

        @Override
        public int hashCode() {
            return mValue != null ? mValue.hashCode() : 0;
        }
    }

    /**
     * Container for Event request UUIDs.
     */
    static final class RequestId extends ValueTypeDef {

        RequestId(final String v) {
            super(v);
        }

        static boolean isValid(final String requestIdValue) {
            boolean isValid;
            try {
                UUID uuid = UUID.fromString(requestIdValue);
                isValid = true;
            } catch (IllegalArgumentException e) {
                isValid = false;
            }
            return isValid;
        }

        static boolean isValid(final RequestId requestId) {
            return null != requestId && isValid(requestId.toString());
        }

    }

    /**
     * Container for Event start times.
     */
    private static final class EventStartTime extends ValueTypeDef {

        private EventStartTime(final String value) {
            super(value);
        }

    }
}
