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
package com.microsoft.identity.client.testapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.microsoft.identity.common.java.controllers.CommandDispatcher;

/**
 * Debug-only receiver used to probe {@link CommandDispatcher#isInteractiveInProgress()} from
 * outside the foreground Activity (so the AuthorizationActivity / WebView is not disturbed).
 * <p>
 * Trigger from a host shell:
 * <pre>
 *   adb shell am broadcast \
 *       -a com.microsoft.identity.client.testapp.CHECK_INTERACTIVE \
 *       -p com.msft.identity.client.sample.local
 * </pre>
 * Primary signal is logcat (tag {@code MSAL_INTERACTIVE_PROBE}); a Toast is also attempted
 * but Android may suppress Toasts when the app is not in the visible foreground.
 */
public class InteractiveStateReceiver extends BroadcastReceiver {

    /** Unique logcat tag — grep this from logcat to read the result. */
    private static final String TAG = "MSAL_INTERACTIVE_PROBE";
    public static final String ACTION_CHECK_INTERACTIVE =
            "com.microsoft.identity.client.testapp.CHECK_INTERACTIVE";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null || !ACTION_CHECK_INTERACTIVE.equals(intent.getAction())) {
            return;
        }
        final boolean inProgress = CommandDispatcher.isInteractiveInProgress();
        final String correlationId = CommandDispatcher.getActiveInteractiveCorrelationId();
        // High-visibility log line for `adb logcat -s MSAL_INTERACTIVE_PROBE:I`.
        Log.i(TAG, "=========================================");
        Log.i(TAG, "isInteractiveInProgress=" + inProgress);
        Log.i(TAG, "activeCorrelationId=" + correlationId);
        Log.i(TAG, "=========================================");
        // Best-effort Toast (may be suppressed when not in visible foreground).
        try {
            Toast.makeText(context.getApplicationContext(),
                    "interactive=" + inProgress + " cid=" + correlationId,
                    Toast.LENGTH_LONG).show();
        } catch (final Throwable ignored) { }
    }
}
