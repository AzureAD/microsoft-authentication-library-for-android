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

import static com.microsoft.identity.client.internal.telemetry.EventConstants.EventProperty;

/**
 * Internal class for CacheEvent telemetry data.
 */
public final class CacheEvent extends Event {

    private CacheEvent(Builder builder) {
        super(builder);
        setProperty(EventProperty.TOKEN_TYPE, builder.mTokenType);
        setProperty(EventProperty.IS_AT, String.valueOf(builder.mIsAT));
        setProperty(EventProperty.IS_RT, String.valueOf(builder.mIsRT));
    }

    public String getTokenType() {
        return getProperty(EventProperty.TOKEN_TYPE);
    }


    public boolean isRT() {
        return Boolean.valueOf(getProperty(EventProperty.IS_RT));
    }

    public boolean isAT() {
        return Boolean.valueOf(getProperty(EventProperty.IS_AT));
    }

    /**
     * Builder object for CacheEvents.
     */
    public static class Builder extends Event.Builder<Builder> {

        private String mTokenType;
        private boolean mIsAT;
        private boolean mIsRT;

        public Builder(final String eventName) {
            super(eventName);
        }

        /**
         * Sets the tokenType.
         *
         * @param tokenType the tokenType to set.
         * @return the Builder instance.
         */
        public Builder setTokenType(final String tokenType) {
            mTokenType = tokenType;
            return this;
        }

        public Builder setIsAT(boolean isAT) {
            mIsAT = isAT;
            return this;
        }

        public Builder setIsRT(boolean isRT) {
            mIsRT = isRT;
            return this;
        }

        /**
         * Constructs a new CacheEvent.
         *
         * @return the newly constructed CacheEvent instance.
         */
        @Override
        public CacheEvent build() {
            return new CacheEvent(this);
        }
    }
}
