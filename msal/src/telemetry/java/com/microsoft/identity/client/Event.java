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
        if (null != builder.mEventStartTime) {
            setProperty(EventProperty.START_TIME, String.valueOf(builder.mEventStartTime));
        }
        if (null != builder.mEventStartTime) {
            setProperty(EventProperty.STOP_TIME, String.valueOf(builder.mEventStopTime));
        }
        if (null != builder.mEventElapsedTime) {
            setProperty(EventProperty.ELAPSED_TIME, String.valueOf(builder.mEventElapsedTime));
        }
    }

    final void setProperty(final String propertyName, final String propertyValue) {
        if (!MsalUtils.isEmpty(propertyName) && !MsalUtils.isEmpty(propertyValue)) {
            add(new Pair<>(propertyName, propertyValue));
        }
    }

    final String getProperty(final String propertyName) {
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
     * Gets the Event startTime.
     *
     * @return the startTime to get.
     */
    Long getStartTime() {
        return Long.valueOf(getProperty(EventProperty.START_TIME));
    }

    /**
     * Gets the Event stopTime.
     *
     * @return the stopTime to get.
     */
    Long getStopTime() {
        return Long.valueOf(getProperty(EventProperty.STOP_TIME));
    }

    /**
     * Gets the Event elapsedTime.
     *
     * @return the elapsedTime to get.
     */
    Long getElapsedTime() {
        return Long.valueOf(getProperty(EventProperty.ELAPSED_TIME));
    }

    /**
     * Builder object used for Events.
     *
     * @param <T> generic type parameter for Builder subtypes.
     */
    abstract static class Builder<T extends Builder> {

        private final String mEventName;
        private Long mEventStartTime;
        private Long mEventStopTime;
        private Long mEventElapsedTime;
        private boolean mIsCompleted;

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
         * Gets the startTime.
         *
         * @return the startTime to get.
         */
        final Long getStartTime() {
            return mEventStartTime;
        }

        /**
         * Gets the completion status.
         *
         * @return the completion status to get.
         */
        final boolean getIsCompleted() {
            return mIsCompleted;
        }

        /**
         * Sets the startTime of this Builder.
         *
         * @param startTime the startTime to set.
         * @return the Builder instance.
         */
        final T setStartTime(final long startTime) {
            mEventStartTime = startTime;
            return (T) this;
        }

        /**
         * Sets the stopTime of this Builder.
         *
         * @param stopTime the stopTime to set.
         * @return the Builder instance.
         */
        final T setStopTime(final long stopTime) {
            mEventStopTime = stopTime;
            return (T) this;
        }

        /**
         * Sets the elapsedTime of this Builder.
         *
         * @param elapsedTime the elapsedTime to set.
         * @return the Builder instance.
         */
        final T setElapsedTime(final long elapsedTime) {
            mEventElapsedTime = elapsedTime;
            return (T) this;
        }

        /**
         * Sets the isCompleted flag of this Builder.
         *
         * @param isCompleted the isCompleted status to set.
         * @return the Builder instance.
         */
        final T setIsCompleted(final boolean isCompleted) {
            mIsCompleted = isCompleted;
            return (T) this;
        }

        /**
         * Constructs a new Event.
         *
         * @return the newly constructed Event instance.
         */
        abstract Event build();
    }
}
