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
package com.microsoft.identity.client.testapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * POC helper: fires the [FakeAuthenticatorActivity] deep-link intent from
 * within MsalTestApp's process so the system records
 * `launchedFromPackage = "com.msft.identity.client.sample.local"`. This is the
 * positive-test counterpart to firing the intent directly from `adb shell`
 * (which would always be `"com.android.shell"` and fail validation).
 *
 * Trigger via:
 * ```
 * adb shell am broadcast \
 *     -a com.microsoft.identity.client.testapp.FIRE_FAKE_AUTH \
 *     -p com.msft.identity.client.sample.local \
 *     --es "RETURN_URI" \
 *          "msauth-return://com.msft.identity.client.sample.local/resume"
 * ```
 */
class FireFakeAuthReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val returnUri = intent.getStringExtra("RETURN_URI")
        if (returnUri.isNullOrEmpty()) {
            Log.w(TAG, "Missing RETURN_URI extra; aborting.")
            return
        }

        val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse("msauth-fake://ca")).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_RETURN_URI, returnUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        Log.i(TAG, "Firing fake-auth deep link from app process: RETURN_URI=$returnUri")
        runCatching { context.startActivity(deepLink) }
            .onFailure { Log.e(TAG, "Failed to fire fake-auth deep link", it) }
    }

    companion object {
        private const val TAG = "FireFakeAuthReceiver"
        private const val EXTRA_RETURN_URI = "com.microsoft.identity.broker.RETURN_URI"
    }
}
