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

import static com.microsoft.identity.client.EventConstants.EventProperty;

/**
 * Internal base-class for Event telemetry data.
 */
class Event extends ArrayList<Pair<String, String>> {

    /**
     * Constructs a new Event.
     *
     * @param builder the Builder instance for this Event.
     */
    Event(final Builder builder) {
        if (null == builder.mEventName) {
            throw new IllegalStateException("Event must have a name");
        }
        if (!EventConstants.EventName.DEFAULT_EVENT.equals(builder.mEventName)) {
            setProperty(EventProperty.EVENT_NAME, builder.mEventName);
        }
    }

    void setProperty(final String propertyName, final String propertyValue) {
        if (!MSALUtils.isEmpty(propertyName) && !MSALUtils.isEmpty(propertyValue)) {
            add(new Pair<>(propertyName, propertyValue));
        }
    }

    String getProperty(final String propertyName) {
        String propertyValue = null;
        for (final Pair<String, String> property : this) {
            if (property.first.equals(propertyName)) {
                propertyValue = property.second;
                break;
            }
        }
        return propertyValue;
    }

    int getPropertyCount() {
        return size();
    }

    String getEventName() {
        return getProperty(EventProperty.EVENT_NAME);
    }

    /**
     * Builder object used for Events.
     *
     * @param <T> generic type parameter for Builder subtypes.
     */
    abstract static class Builder<T extends Builder> {

        private final String mEventName;

        Builder(final String name) {
            mEventName = name;
        }

        /**
         * Gets the event name.
         *
         * @return the EventName to get.
         */
        final String getEventName() {
            return mEventName;
        }

        /**
         * Constructs a new Event.
         *
         * @return the newly constructed Event instance.
         */
        abstract Event build();
    }
}
