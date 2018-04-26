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
public final class Telemetry {

    private static final String TAG = Telemetry.class.getSimpleName();

    private static final Telemetry INSTANCE = new Telemetry();

    private static boolean sDisableForTest;

    private final Map<Pair<String, String>, Long> mEventsInProgress;

    private final Map<String, List<Event>> mCompletedEvents;

    private EventDispatcher mPublisher;

    private boolean mTelemetryOnFailureOnly = false;

    private Telemetry() {
        mEventsInProgress = Collections.synchronizedMap(
                new LinkedHashMap<Pair<String, String>, Long>()
        );
        mCompletedEvents = Collections.synchronizedMap(
                new LinkedHashMap<String, List<Event>>()
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
     * Generates a new requestId.
     *
     * @return a random UUID (in String format).
     */
    static String generateNewRequestId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Mark that the system is running under test conditions and that Telemetry should be disabled.
     */
    static void disableForTest(final boolean disabled) {
        sDisableForTest = disabled;
    }

    public synchronized void registerReceiver(IMsalEventReceiver receiver) {
        if (null == receiver) {
            throw new IllegalArgumentException("Receiver instance cannot be null");
        }

        // check to make sure we're not already dispatching elsewhere
        if (null != mPublisher) {
            throw new IllegalStateException(
                    IMsalEventReceiver.class.getSimpleName()
                            + " instances are not swappable at this time."
            );
        }

        // set this dispatcher
        mPublisher = new EventDispatcher(receiver);
    }

    public void setTelemetryOnFailureOnly(final boolean onFailure) {
        mTelemetryOnFailureOnly = onFailure;
    }

    /**
     * Starts recording a new Event, based on requestId
     *
     * @param requestId the RequestId used to track this Event.
     * @param eventName the name of the Event which is to be tracked.
     */
    void startEvent(final String requestId, final String eventName) {
        if (null == mPublisher || sDisableForTest) {
            return;
        }

        synchronized (this) {
            mEventsInProgress.put(new Pair<>(requestId, eventName), System.currentTimeMillis());
        }
    }

    /**
     * Stops a previously started Event using its Event.Builder.
     *
     * @param requestId    the RequestId of the Event to stop.
     * @param eventBuilder the Event.Builder used to create the Event.
     */
    void stopEvent(final String requestId, final Event.Builder eventBuilder) {
        stopEvent(requestId, eventBuilder.getEventName(), eventBuilder.build());
    }

    /**
     * Stops a previously started Event.
     *
     * @param requestId   the RequestId of the Event to stop.
     * @param eventName   the name of the Event to stop.
     * @param eventToStop the Event data.
     */
    void stopEvent(final String requestId, final String eventName, final Event eventToStop) {
        if (null == mPublisher || sDisableForTest) {
            return;
        }

        final Pair<String, String> eventKey = new Pair<>(requestId, eventName);

        // Compute execution time
        final Long eventStartTime;
        synchronized (this) {
            eventStartTime = mEventsInProgress.get(eventKey);
        }

        // If we did not get anything back from the dictionary, most likely its a bug that stopEvent
        // was called without a corresponding startEvent
        if (null == eventStartTime) {
            Logger.warning(TAG, null, "Stop Event called without a corresponding start_event");
            return;
        }

        final long startTimeL = Long.parseLong(eventStartTime.toString());
        final long stopTimeL = System.currentTimeMillis();
        final long diffTime = stopTimeL - startTimeL;
        final String stopTime = Long.toString(stopTimeL);

        // Set execution time properties on the event
        eventToStop.setProperty(EventProperty.START_TIME, eventStartTime.toString());
        eventToStop.setProperty(EventProperty.STOP_TIME, stopTime);
        eventToStop.setProperty(EventProperty.ELAPSED_TIME, Long.toString(diffTime));

        synchronized (this) {
            if (null == mCompletedEvents.get(requestId)) {
                // if this is the first event associated to this
                // RequestId we need to initialize a new List to hold
                // all of sibling events
                final List<Event> events = new ArrayList<>();
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
    }

    /**
     * Flushes collected Events matching the supplied requestId to the receiver.
     *
     * @param requestId Events matching the supplied RequestId will be flushed.
     */
    void flush(final String requestId) {
        if (null == mPublisher) {
            return;
        }

        synchronized (this) {
            // check for orphaned events...
            final List<Event> orphanedEvents = collateOrphanedEvents(requestId);
            // Add the OrphanedEvents to the existing IEventList
            if (null == mCompletedEvents.get(requestId)) {
                Logger.warning(TAG, null, "No completed Events returned for RequestId.");
                return;
            }

            mCompletedEvents.get(requestId).addAll(orphanedEvents);

            final List<Event> eventsToFlush = mCompletedEvents.remove(requestId);

            if (mTelemetryOnFailureOnly) {
                // iterate over Events, if the ApiEvent was successful, don't dispatch
                boolean shouldRemoveEvents = false;

                for (Event event : eventsToFlush) {
                    if (event instanceof ApiEvent) {
                        ApiEvent apiEvent = (ApiEvent) event;
                        shouldRemoveEvents = apiEvent.wasSuccessful();
                        break;
                    }
                }

                if (shouldRemoveEvents) {
                    eventsToFlush.clear();
                }
            }

            if (!eventsToFlush.isEmpty()) {
                eventsToFlush.add(0, new DefaultEvent.Builder().build());
                mPublisher.dispatch(eventsToFlush);
            }
        }
    }

    private List<Event> collateOrphanedEvents(String requestId) {
        final List<Event> orphanedEvents = new ArrayList<>();
        for (Pair<String, String> key : mEventsInProgress.keySet()) {
            if (key.first.equals(requestId)) {
                final String orphanedEventName = key.second;
                final Long orphanedEventStartTime =
                        mEventsInProgress.remove(key); // remove() this entry (clean up!)
                // Build the OrphanedEvent...
                final Event orphanedEvent = new OrphanedEvent.Builder(orphanedEventName, orphanedEventStartTime).build();
                orphanedEvents.add(orphanedEvent);
            }
        }
        return orphanedEvents;
    }
}
