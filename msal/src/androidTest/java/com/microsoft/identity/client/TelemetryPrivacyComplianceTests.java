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

import androidx.test.runner.AndroidJUnit4;
import android.util.Pair;

import com.microsoft.identity.client.internal.telemetry.ApiEvent;
import com.microsoft.identity.client.internal.telemetry.CacheEvent;
import com.microsoft.identity.client.internal.telemetry.Event;
import com.microsoft.identity.client.internal.telemetry.EventConstants;
import com.microsoft.identity.client.internal.telemetry.HttpEvent;
import com.microsoft.identity.client.internal.telemetry.OrphanedEvent;
import com.microsoft.identity.client.internal.telemetry.TelemetryUtils;
import com.microsoft.identity.client.internal.telemetry.UiEvent;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import static com.microsoft.identity.client.ApiEventTest.TEST_AUTHORITY_COMMON;
import static com.microsoft.identity.client.ApiEventTest.getTestApiEvent;
import static com.microsoft.identity.client.CacheEventTest.TEST_TOKEN_TYPE;
import static com.microsoft.identity.client.CacheEventTest.getTestCacheEvent;
import static com.microsoft.identity.client.HttpEventTest.getTestHttpEvent;
import static com.microsoft.identity.client.UiEventTest.getTestUiEvent;

@RunWith(AndroidJUnit4.class)
public class TelemetryPrivacyComplianceTests {

    @Test
    public void testApiEventPrivacyCompliance() {
        final ApiEvent apiEvent = getTestApiEvent(Telemetry.generateNewRequestId(), TEST_AUTHORITY_COMMON);
        populateWithPii(apiEvent);
        verifyDoesntContainPii(apiEvent);
    }

    @Test
    public void testCacheEventPrivacyCompliance() {
        final String eventName = EventConstants.EventName.TOKEN_CACHE_DELETE;
        final CacheEvent cacheEvent = getTestCacheEvent(eventName, TEST_TOKEN_TYPE);
        populateWithPii(cacheEvent);
        verifyDoesntContainPii(cacheEvent);
    }

    @Test
    public void testHttpEventPrivacyCompliance() {
        final HttpEvent httpEvent = getTestHttpEvent();
        populateWithPii(httpEvent);
        verifyDoesntContainPii(httpEvent);
    }

    @Test
    public void testOrphanedEventPrivacyCompliance() {
        final Event orphanedEvent =
                new OrphanedEvent.Builder(
                        EventConstants.EventName.API_EVENT,
                        1L
                ).build();
        populateWithPii(orphanedEvent);
        verifyDoesntContainPii(orphanedEvent);
    }

    @Test
    public void testUiEventPrivacyCompliance() {
        final UiEvent uiEvent = getTestUiEvent();
        populateWithPii(uiEvent);
        verifyDoesntContainPii(uiEvent);
    }

    @Test
    public void testApiEventWithPiiCompliance() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Telemetry.setAllowPii(true);
        final ApiEvent apiEvent = new ApiEvent.Builder(Telemetry.generateNewRequestId()).build();
        populateWithPii(apiEvent);
        for (final Pair<String, String> eventKeyPair : apiEvent) {
            if (TelemetryUtils.GDPR_FILTERED_FIELDS.contains(eventKeyPair.first)) {
                Assert.assertEquals("sample_value", eventKeyPair.second);
            }
        }
        Telemetry.setAllowPii(false);
    }

    private void populateWithPii(final Event event) {
        for (final String key : TelemetryUtils.GDPR_FILTERED_FIELDS) {
            event.setProperty(key, "sample_value");
        }
    }

    private void verifyDoesntContainPii(final Event event) {
        for (final Pair<String, String> eventKeyPair : event) {
            if (TelemetryUtils.GDPR_FILTERED_FIELDS.contains(eventKeyPair.first)) {
                throw new AssertionError(
                        "Event contains PII/OII protected pair: "
                                + eventKeyPair.first
                                + " : "
                                + eventKeyPair.second
                );
            }
        }
    }
}
