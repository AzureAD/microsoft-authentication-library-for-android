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

import java.util.UUID;

/**
 * An Event that can be correlated by Id.
 */
abstract class AbstractCorrelatableEvent extends Event implements ICorrelatableEvent {
    /**
     * Constructs a new Event.
     *
     * @param builder the Builder instance for this Event.
     */
    AbstractCorrelatableEvent(Builder builder) {
        super(builder);
    }

    @Override
    public void setCorrelationId(UUID correlationId) {
        if (null != correlationId) {
            setProperty(EventConstants.EventProperty.CORRELATION_ID, correlationId.toString());
        }
    }

    @Override
    public UUID getCorrelationId() {
        UUID correlationId;
        try {
            correlationId = UUID.fromString(getProperty(EventConstants.EventProperty.CORRELATION_ID));
        } catch (NullPointerException | IllegalArgumentException e) {
            correlationId = null;
        }
        return correlationId;
    }

    void clearCorrelationId() {
        final UUID correlationId = getCorrelationId();
        if (null != correlationId) {
            remove(new Pair<>(EventConstants.EventProperty.CORRELATION_ID, correlationId.toString()));
        }
    }
}
