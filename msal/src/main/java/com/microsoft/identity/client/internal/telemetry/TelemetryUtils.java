// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.internal.telemetry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.identity.client.internal.telemetry.EventConstants.EventProperty.LOGIN_HINT;
import static com.microsoft.identity.client.internal.telemetry.EventConstants.EventProperty.TENANT_ID;
import static com.microsoft.identity.client.internal.telemetry.EventConstants.EventProperty.USER_ID;

public final class TelemetryUtils {

    public static final Set<String> GDPR_FILTERED_FIELDS = new HashSet<>();

    static {
        initializeGdprFilteredFields();
    }

    private TelemetryUtils() {
        // Intentionally left blank.
    }

    private static void initializeGdprFilteredFields() {
        GDPR_FILTERED_FIELDS.addAll(
                Arrays.asList(
                        LOGIN_HINT,
                        USER_ID,
                        TENANT_ID
                )
        );
    }

}
