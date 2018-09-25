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

import com.microsoft.identity.client.internal.telemetry.ApiEvent;
import com.microsoft.identity.client.internal.telemetry.DefaultEvent;
import com.microsoft.identity.client.internal.telemetry.Event;
import com.microsoft.identity.client.internal.telemetry.EventDispatcher;
import com.microsoft.identity.client.internal.telemetry.OrphanedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Collects and publishes telemetry key/value pairs to subscribers.
 */
final class Telemetry {

    private static final String TAG = Telemetry.class.getSimpleName();

    private static final Telemetry INSTANCE = new Telemetry();

    private static boolean sDisableForTest;

    private static boolean sAllowPii = false;

    private final Map<String, List<Event.Builder>> mEvents;

    private EventDispatcher mPublisher;

    private boolean mTelemetryOnFailureOnly = false;

    private Telemetry() {
        mEvents = Collections.synchronizedMap(
                new LinkedHashMap<String, List<Event.Builder>>()
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

    /**
     * Sets the PII/OII allow flag. If set to true, PII/OII fields will not be explicitly blocked
     * in Telemetry data.
     *
     * @param allowFlag true, if PII/OII should be allowed in Telemetry data. False otherwise.
     */
    public static void setAllowPii(final boolean allowFlag) {
        sAllowPii = allowFlag;
    }

    /**
     * Gets the state of the PII/OII allow flag.
     *
     * @return the flag state.
     */
    public static boolean getAllowPii() {
        return sAllowPii;
    }

    /**
     * Register receiver instance and set dispatcher.
     *
     * @param receiver MSAL telemetry receiver instance
     */
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

        /**
         * set this dispatcher.
         */
        mPublisher = new EventDispatcher(receiver);
    }

    /**
     * Enables Telemetry only in case of failure.
     *
     * @param onFailure true if Telemetry is enabled only on failure. false by default
     */
    public void setTelemetryOnFailureOnly(final boolean onFailure) {
        mTelemetryOnFailureOnly = onFailure;
    }

    /**
     * Starts recording a new Event, based on requestId.
     *
     * @param requestId    the RequestId used to track this Event.
     * @param eventBuilder the Builder of the Event to start.
     */
    void startEvent(final String requestId, final Event.Builder eventBuilder) {
        if (null == mPublisher || sDisableForTest) {
            return;
        }

        eventBuilder.setStartTime(System.currentTimeMillis());

        synchronized (this) {
            if (null == mEvents.get(requestId)) {
                final List<Event.Builder> eventBuilders = new ArrayList<>();
                eventBuilders.add(eventBuilder);
                mEvents.put(requestId, eventBuilders);
            } else {
                mEvents.get(requestId).add(eventBuilder);
            }
        }
    }

    /**
     * Stops a previously started Event.
     *
     * @param requestId    the RequestId of the Event to stop.
     * @param eventBuilder the Event.Builder used to create the Event.
     */
    void stopEvent(final String requestId, final Event.Builder eventBuilder) {
        if (null == mPublisher || sDisableForTest) {
            return;
        }

        final List<Event.Builder> eventsForId;
        synchronized (this) {
            // Grab the List of Events associated to this requestId
            eventsForId = mEvents.get(requestId);
        }
        // Find the specific Builder we want to stop
        Event.Builder builderToStop = null;
        for (final Event.Builder builder : eventsForId) {
            if (builder.getEventName().equals(eventBuilder.getEventName()) && !builder.getIsCompleted()) {
                builderToStop = builder;
                break;
            }
        }

        // If we did not find the Builder to stop, log a warning and return
        if (null == builderToStop) {
            Logger.warning(TAG, null, "Could not stop Event: [" + eventBuilder.getEventName() + "] because no Event in progress was found.");
            return;
        }

        // Compute execution time
        final Long eventStartTime = builderToStop.getStartTime();

        // If we did not get anything back from the dictionary, most likely its a bug that stopEvent
        // was called without a corresponding startEvent
        if (null == eventStartTime) {
            Logger.warning(TAG, null, "Stop Event called without a corresponding start_event");
            return;
        }

        final long startTimeL = Long.parseLong(eventStartTime.toString());
        final long stopTimeL = System.currentTimeMillis();
        final long diffTime = stopTimeL - startTimeL;

        // Set execution time properties on the event
        builderToStop.setStopTime(stopTimeL);
        builderToStop.setElapsedTime(diffTime);

        // Mark the Event as complete...
        builderToStop.setIsCompleted(true);
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

        final List<Event.Builder> eventsToBuild;
        synchronized (this) {
            eventsToBuild = mEvents.remove(requestId);
        }

        if (null == eventsToBuild) {
            Logger.warning(TAG, null, "No completed Events returned for RequestId.");
            return;
        }

        final List<Event> eventsToDispatch = new ArrayList<>();
        for (final Event.Builder builder : eventsToBuild) {
            if (builder.getIsCompleted()) {
                eventsToDispatch.add(builder.build());
            } else {
                // This Event is orphaned
                final Event orphanedEvent = new OrphanedEvent.Builder(
                        builder.getEventName(),
                        builder.getStartTime()
                ).build();
                eventsToDispatch.add(orphanedEvent);
            }
        }

        if (mTelemetryOnFailureOnly) {
            // iterate over Events, if the ApiEvent was successful, don't dispatch
            boolean shouldRemoveEvents = false;

            for (final Event event : eventsToDispatch) {
                if (event instanceof ApiEvent) {
                    ApiEvent apiEvent = (ApiEvent) event;
                    shouldRemoveEvents = apiEvent.wasSuccessful();
                    break;
                }
            }

            if (shouldRemoveEvents) {
                eventsToDispatch.clear();
            }
        }

        if (!eventsToDispatch.isEmpty()) {
            eventsToDispatch.add(0, new DefaultEvent.Builder().build());
            mPublisher.dispatch(eventsToDispatch);
        }
    }
}
