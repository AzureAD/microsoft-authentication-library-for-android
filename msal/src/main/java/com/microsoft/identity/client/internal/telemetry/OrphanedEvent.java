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


/**
 * OrphanedEvents are Events which were started but never finished before
 * Telemetry#flush(String) was called.
 */
public final class OrphanedEvent extends Event {

    /**
     * Constructs a new Event.
     *
     * @param builder the Builder instance for this Event.
     */
    private OrphanedEvent(Builder builder) {
        super(builder);
        // Set execution time properties on the event
        setProperty(EventConstants.EventProperty.START_TIME, String.valueOf(builder.mStartTime));
        setProperty(EventConstants.EventProperty.STOP_TIME, String.valueOf(Builder.EVENT_END_TIME));
    }

    public static class Builder extends Event.Builder<Builder> {

        /**
         * OrphanedEvents have negative endTime to indicate incompleteness.
         */
        public static final Long EVENT_END_TIME = -1L;

        /**
         * The startTime of this OrphanedEvent.
         */
        final Long mStartTime;


        public Builder(final String name, final Long startTime) {
            super(name);
            mStartTime = startTime;
        }

        @Override
        public Event build() {
            return new OrphanedEvent(this);
        }
    }
}
