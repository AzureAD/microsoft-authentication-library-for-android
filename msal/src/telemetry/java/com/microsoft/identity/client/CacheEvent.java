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

import static com.microsoft.identity.client.EventConstants.EventProperty;

/**
 * Internal class for CacheEvent telemetry data.
 */
final class CacheEvent extends Event implements ICacheEvent {

    private CacheEvent(Builder builder) {
        super(builder);
        setProperty(EventProperty.TOKEN_TYPE, builder.mTokenType);
    }

    @Override
    public String getTokenType() {
        return getProperty(EventProperty.TOKEN_TYPE);
    }

    @Override
    public Boolean tokenTypeisRT() {
        return getTokenType().equals(EventProperty.Value.TOKEN_TYPE_RT);
    }

    /**
     * Builder object for CacheEvents.
     */
    static class Builder extends Event.Builder<Builder> {

        private String mTokenType;

        Builder(final Telemetry.RequestId requestId, final EventName eventName) {
            super(requestId, eventName);
        }

        /**
         * Sets the tokenType.
         *
         * @param tokenType the tokenType to set.
         * @return the Builder instance.
         */
        Builder setTokenType(final String tokenType) {
            mTokenType = tokenType;
            return this;
        }

        /**
         * Constructs a new CacheEvent.
         *
         * @return the newly constructed CacheEvent instance.
         */
        @Override
        ICacheEvent build() {
            return new CacheEvent(this);
        }

    }
}
