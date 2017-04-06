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

/**
 * Internal telemetry data-container for the names of Events.
 */
class EventName extends Telemetry.ValueTypeDef {

    static final EventName DEFAULT_EVENT = new EventName(EventConstants.EVENT_PREFIX + "default");
    static final EventName API_EVENT = new EventName(EventConstants.EVENT_PREFIX + "api_event");
    static final EventName AUTHORITY_VALIDATION_EVENT = new EventName(EventConstants.EVENT_PREFIX + "authority_validation");
    static final EventName HTTP_EVENT = new EventName(EventConstants.EVENT_PREFIX + "http_event");
    static final EventName UI_EVENT = new EventName(EventConstants.EVENT_PREFIX + "ui_event");
    static final EventName TOKEN_CACHE_LOOKUP = new EventName(EventConstants.EVENT_PREFIX + "token_cache_lookup");
    static final EventName TOKEN_CACHE_WRITE = new EventName(EventConstants.EVENT_PREFIX + "token_cache_write");
    static final EventName TOKEN_CACHE_DELETE = new EventName(EventConstants.EVENT_PREFIX + "token_cache_delete");

    /**
     * Constructs a new EventName instance.
     *
     * @param value the name to use (as a String)
     */
    EventName(String value) {
        super(value);
    }
}
