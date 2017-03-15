package com.microsoft.identity.client;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EventPublisher {

    private final IDispatcher mDispatcher;

    EventPublisher(IDispatcher dispatcher) {
        mDispatcher = dispatcher;
    }

    IDispatcher getDispatcher() {
        return mDispatcher;
    }

    void publish(final List<BaseEvent> eventsToPublish) {
        if (null == mDispatcher) {
            return;
        }

        List<Map<String, String>> eventsForPublication = new ArrayList<>();

        for (BaseEvent event : eventsToPublish) {
            Map<String, String> eventProperties = new HashMap<>();
            for (Pair<String, String> property : event) {
                eventProperties.put(property.first, property.second);
            }
            eventsForPublication.add(eventProperties);
        }

        mDispatcher.onEventsReceived(eventsForPublication);
    }
}
