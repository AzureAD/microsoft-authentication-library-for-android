package com.microsoft.identity.client;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class EventDispatcher {

    private final MsalEventReceiver mEventReceiver;

    EventDispatcher(MsalEventReceiver receiver) {
        mEventReceiver = receiver;
    }

    MsalEventReceiver getDispatcher() {
        return mEventReceiver;
    }

    void dispatch(final List<BaseEvent> eventsToPublish) {
        if (null == mEventReceiver) {
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

        mEventReceiver.onEventsReceived(eventsForPublication);
    }
}
