package com.microsoft.identity.client;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.microsoft.identity.client.BaseEvent.Properties;

public class Telemetry implements ITelemetry {

    private static final Telemetry INSTANCE = new Telemetry();

    private final Map<Pair<RequestId, EventName>, EventStartTime> mEventsInProgress;

    private final Map<RequestId, List<BaseEvent>> mCompletedEvents;

    private EventDispatcher mPublisher;

    private boolean mTelemetryOnFailureOnly = false;

    private Telemetry() {
        mEventsInProgress = new ConcurrentHashMap<>();
        mCompletedEvents = new ConcurrentHashMap<>();
    }

    static Telemetry getInstance() {
        return INSTANCE;
    }

    static RequestId generateNewRequestId() {
        return new RequestId(UUID.randomUUID().toString());
    }

    @Override
    public synchronized void registerDispatcher(MsalEventReceiver dispatcher) {
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

    void stopEvent(final RequestId requestId, final EventName eventName, final BaseEvent event) {
        final Pair<RequestId, EventName> eventKey = new Pair<>(requestId, eventName);

        // Compute execution time
        final EventStartTime eventStartTime = mEventsInProgress.get(eventKey);
        final long startTimeL = Long.parseLong(eventStartTime.value);
        final long stopTimeL = System.currentTimeMillis();
        final long diffTime = stopTimeL - startTimeL;
        final String stopTime = Long.toString(stopTimeL);

        // Set execution time properties on the event
        event.setProperty(Properties.START_TIME, eventStartTime.value);
        event.setProperty(Properties.STOP_TIME, stopTime);
        event.setProperty(Properties.RESPONSE_TIME, Long.toString(diffTime));

        if (null == mCompletedEvents.get(requestId)) {
            // if this is the first event associated to this
            // RequestId we need to initialize a new List to hold
            // all of sibling events
            mCompletedEvents.put(
                    requestId,
                    new ArrayList<BaseEvent>() {{
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
        final List<BaseEvent> eventsToFlush = mCompletedEvents.remove(requestId);
        mPublisher.dispatch(eventsToFlush);
    }

    private static abstract class ValueTypeDef {

        final String value;

        private ValueTypeDef(final String value) {
            this.value = value;
        }
    }

    static final class RequestId extends ValueTypeDef {

        RequestId(final String value) {
            super(value);
        }

    }

    static final class EventName extends ValueTypeDef {

        EventName(final String value) {
            super(value);
        }

    }

    private static final class EventStartTime extends ValueTypeDef {

        private EventStartTime(final String value) {
            super(value);
        }

    }
}
