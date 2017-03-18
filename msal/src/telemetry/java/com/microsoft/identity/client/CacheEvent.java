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

class CacheEvent extends Event implements ICacheEvent {

    private CacheEvent(Builder builder) {
        super(builder);
        setProperty(EventProperty.EVENT_NAME, builder.mEventName.value);
        setProperty(EventProperty.TOKEN_TYPE, builder.mTokenType);
        setProperty(EventProperty.TOKEN_TYPE_IS_RT, String.valueOf(builder.mTokenTypeIsRT));
    }

    static class Builder extends Event.Builder<Builder> {

        private EventName mEventName;
        private String mTokenType;
        private boolean mTokenTypeIsRT;

        Builder eventName(final EventName eventName) {
            mEventName = eventName;
            return this;
        }

        Builder tokenType(final String tokenType) {
            mTokenType = tokenType;
            return this;
        }

        Builder tokenTypeIsRT(final boolean tokenTypeRT) {
            mTokenTypeIsRT = tokenTypeRT;
            return this;
        }

        ICacheEvent build() {
            // todo make sure that the event name is set?
            return new CacheEvent(this);
        }

    }
}
