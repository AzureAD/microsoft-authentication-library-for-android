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
 * Internal class for UiEvent telemetry data.
 */
public final class UiEvent extends Event {

    private UiEvent(Builder builder) {
        super(builder);
        setProperty(EventProperty.HTTP_USER_AGENT, builder.mUserAgent);
        setProperty(EventProperty.USER_CANCEL, builder.mUserDidCancel);
    }

    public String getUserAgent() {
        return getProperty(EventProperty.HTTP_USER_AGENT);
    }

    public Boolean userCancelled() {
        return Boolean.valueOf(getProperty(EventProperty.USER_CANCEL));
    }

    /**
     * Builder object for UiEvents.
     */
    public static class Builder extends Event.Builder<Builder> {

        private String mUserAgent;
        private String mUserDidCancel = "false";

        public Builder() {
            super(EventConstants.EventName.UI_EVENT);
        }

        /**
         * Sets the userAgent.
         *
         * @param userAgent the userAgent to set.
         * @return the Builder instance.
         */
        public Builder setUserAgent(final String userAgent) {
            mUserAgent = userAgent;
            return this;
        }

        /**
         * Sets userDidCancel to 'true'.
         *
         * @return the Builder instance.
         */
        public Builder setUserDidCancel() {
            mUserDidCancel = "true";
            return this;
        }

        /**
         * Constructs a new IUiEvent.
         *
         * @return the newly created IUiEvent instance.
         */
        @Override
        public UiEvent build() {
            return new UiEvent(this);
        }
    }
}
