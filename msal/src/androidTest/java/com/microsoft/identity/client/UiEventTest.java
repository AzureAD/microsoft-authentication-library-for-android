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

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UiEventTest {

    static final EventName sTestExpectedEventName = EventName.UI_EVENT;
    static final Integer sTestRedirectCount = 2;
    static final boolean sTestUserDidCancel = true;

    static IUiEvent getTestUiEvent(final Telemetry.RequestId requestId) {
        return new UiEvent.Builder()
                .requestId(requestId)
                .redirectCount(2)
                .setUserDidCancel()
                .build();
    }

    @Test
    public void testUiEventInitializes() {
        final Telemetry.RequestId requestId = Telemetry.generateNewRequestId();
        IUiEvent uiEvent = getTestUiEvent(requestId);
        Assert.assertEquals(sTestExpectedEventName, uiEvent.getEventName());
        Assert.assertEquals(sTestRedirectCount, uiEvent.getRedirectCount());
        Assert.assertEquals(Boolean.valueOf(sTestUserDidCancel), uiEvent.userCancelled());
    }

}
