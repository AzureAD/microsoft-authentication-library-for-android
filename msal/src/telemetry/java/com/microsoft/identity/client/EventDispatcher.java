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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dispatcher for telemetry event data.
 * Turns MSAL internal Events into an externally consumable format before publishing.
 */
class EventDispatcher {

    private final MsalEventReceiver mEventReceiver;

    /**
     * Constructs a new EventDispatcher.
     *
     * @param receiver the {@link MsalEventReceiver} to receive {@link Event} data.
     */
    EventDispatcher(final MsalEventReceiver receiver) {
        mEventReceiver = receiver;
    }

    /**
     * Returns the {@link MsalEventReceiver} to which telemetry data is dispatched.
     *
     * @return the event receiver.
     */
    MsalEventReceiver getReceiver() {
        return mEventReceiver;
    }

    /**
     * Dispatches the {@link Event} instances associated to receiver.
     *
     * @param eventsToPublish the Events to publish.
     */
    void dispatch(final List<IEvent> eventsToPublish) {
        if (null == mEventReceiver) {
            return;
        }

        List<Map<String, String>> eventsForPublication = new ArrayList<>();

        for (final IEvent event : eventsToPublish) {
            Map<String, String> eventProperties = new LinkedHashMap<>();
            for (Pair<String, String> property : event) {
                eventProperties.put(property.first, property.second);
            }
            eventsForPublication.add(eventProperties);
        }

        mEventReceiver.onEventsReceived(eventsForPublication);
    }

}
