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

package com.microsoft.identity.client.internal.telemetry;

import android.util.Pair;

import com.microsoft.identity.client.internal.MsalUtils;

import java.util.ArrayList;

import static com.microsoft.identity.client.internal.telemetry.EventConstants.EventProperty;

/**
 * Internal base-class for Event telemetry data.
 */
public class Event extends ArrayList<Pair<String, String>> {

    /**
     * Constructs a new Event.
     *
     * @param builder the Builder instance for this Event.
     */
    public Event(final Builder builder) {
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

    public final void setProperty(final String propertyName, final String propertyValue) {
        if (!MsalUtils.isEmpty(propertyName) && !MsalUtils.isEmpty(propertyValue) && isPrivacyCompliant(propertyName)) {
            add(new Pair<>(propertyName, propertyValue));
        }
    }

    public final String getProperty(final String propertyName) {
        String propertyValue = null;
        for (final Pair<String, String> property : this) {
            if (property.first.equals(propertyName)) {
                propertyValue = property.second;
                break;
            }
        }
        return propertyValue;
    }

    public int getPropertyCount() {
        return size();
    }

    public String getEventName() {
        return getProperty(EventProperty.EVENT_NAME);
    }

    /**
     * Gets the Event startTime.
     *
     * @return the startTime to get.
     */
    public Long getStartTime() {
        return Long.valueOf(getProperty(EventProperty.START_TIME));
    }

    /**
     * Gets the Event stopTime.
     *
     * @return the stopTime to get.
     */
    public Long getStopTime() {
        return Long.valueOf(getProperty(EventProperty.STOP_TIME));
    }

    /**
     * Gets the Event elapsedTime.
     *
     * @return the elapsedTime to get.
     */
    public Long getElapsedTime() {
        return Long.valueOf(getProperty(EventProperty.ELAPSED_TIME));
    }

    /**
     * Tests supplied EventStrings for privacy compliance.
     *
     * @param fieldName The EventString to evaluate.
     * @return True, if the field can be reported. False otherwise.
     */
    public static boolean isPrivacyCompliant(final String fieldName) {
        return /*Telemetry.getAllowPii() ||*/ !TelemetryUtils.GDPR_FILTERED_FIELDS.contains(fieldName);
    }

    /**
     * Builder object used for Events.
     *
     * @param <T> generic type parameter for Builder subtypes.
     */
    public abstract static class Builder<T extends Builder> {

        private final String mEventName;
        private Long mEventStartTime;
        private Long mEventStopTime;
        private Long mEventElapsedTime;
        private boolean mIsCompleted;

        public Builder(final String name) {
            mEventName = name;
        }

        /**
         * Gets the event name.
         *
         * @return the EventName to get.
         */
        public final String getEventName() {
            return mEventName;
        }

        /**
         * Gets the startTime.
         *
         * @return the startTime to get.
         */
        public final Long getStartTime() {
            return mEventStartTime;
        }

        /**
         * Gets the completion status.
         *
         * @return the completion status to get.
         */
        public final boolean getIsCompleted() {
            return mIsCompleted;
        }

        /**
         * Sets the startTime of this Builder.
         *
         * @param startTime the startTime to set.
         * @return the Builder instance.
         */
        public final T setStartTime(final long startTime) {
            mEventStartTime = startTime;
            return (T) this;
        }

        /**
         * Sets the stopTime of this Builder.
         *
         * @param stopTime the stopTime to set.
         * @return the Builder instance.
         */
        public final T setStopTime(final long stopTime) {
            mEventStopTime = stopTime;
            return (T) this;
        }

        /**
         * Sets the elapsedTime of this Builder.
         *
         * @param elapsedTime the elapsedTime to set.
         * @return the Builder instance.
         */
        public final T setElapsedTime(final long elapsedTime) {
            mEventElapsedTime = elapsedTime;
            return (T) this;
        }

        /**
         * Sets the isCompleted flag of this Builder.
         *
         * @param isCompleted the isCompleted status to set.
         * @return the Builder instance.
         */
        public final T setIsCompleted(final boolean isCompleted) {
            mIsCompleted = isCompleted;
            return (T) this;
        }

        /**
         * Constructs a new Event.
         *
         * @return the newly constructed Event instance.
         */
        public abstract Event build();
    }
}
