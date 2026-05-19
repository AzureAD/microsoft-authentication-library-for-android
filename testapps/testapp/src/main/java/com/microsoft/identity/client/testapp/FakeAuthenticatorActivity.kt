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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log

/**
 * POC stand-in for Authenticator's CA-flow deep-link receiver. Simulates what
 * Authenticator would do on receipt of a CA deep link carrying an
 * [EXTRA_RETURN_URI] from a calling app's WebView:
 *
 *  1. Read the return URI from the intent extra.
 *  2. Validate that the URI's authority (the package to bounce back to) equals
 *     the package that fired this intent — derived from
 *     [Activity.getLaunchedFromPackage] on API 34+. On older APIs the check is
 *     skipped (no trustworthy source).
 *  3. If validation passes (or is skipped), simulate "CA flow completion" by
 *     firing the return intent so [com.microsoft.identity.client.BrokerReturnActivity]
 *     can raise the caller's parked task.
 *
 * This activity has no UI; it logs the outcome and finishes immediately.
 *
 * Test commands (POC):
 *
 *  Negative case (ADB-fired, launchedFromPackage = "com.android.shell"):
 *  ```
 *  adb shell am start \
 *      -a android.intent.action.VIEW \
 *      -d "msauth-fake://ca" \
 *      -p com.msft.identity.client.sample.local \
 *      --es "com.microsoft.identity.broker.RETURN_URI" \
 *           "msauth-return://com.msft.identity.client.sample.local/resume"
 *  ```
 *  Expected: validation FAILS — host (com.msft.identity.client.sample.local)
 *  does not equal launchedFromPackage (com.android.shell). Return intent is
 *  NOT fired.
 *
 *  Positive case (fired from MainActivity within MsalTestApp):
 *  Same extra, but launchedFromPackage = com.msft.identity.client.sample.local
 *  → matches host → return intent IS fired → BrokerReturnActivity raises task.
 */
class FakeAuthenticatorActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val returnUriString = intent.getStringExtra(EXTRA_RETURN_URI)
        if (returnUriString.isNullOrEmpty()) {
            Log.w(TAG, "No EXTRA_RETURN_URI in intent; nothing to do.")
            finish()
            return
        }

        val returnUri = runCatching { Uri.parse(returnUriString) }.getOrNull()
        if (returnUri == null || returnUri.scheme != EXPECTED_SCHEME) {
            Log.w(TAG, "Return URI rejected (bad shape): $returnUriString")
            finish()
            return
        }

        val expectedPkg = returnUri.host.orEmpty()

        // API 34+ exposes the kernel-set firer package for VIEW intents.
        // On older APIs there is no trustworthy answer, so we accept.
        val onSecureApi = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        val actualPkg: String? = if (onSecureApi) launchedFromPackage else null

        // On API 34+, null means the system could not resolve the caller
        // (typically due to package-visibility rules) — treat as REJECT.
        // On pre-34, null just means "no trustworthy source" — accept.
        val validationPassed = if (onSecureApi) {
            actualPkg != null && expectedPkg == actualPkg
        } else {
            true
        }

        Log.i(
            TAG,
            "Validation: expectedPkg=$expectedPkg actualPkg=$actualPkg " +
                "onSecureApi=$onSecureApi passed=$validationPassed"
        )

        if (!validationPassed) {
            Log.w(
                TAG,
                "Spoof check FAILED. Dropping return URI; not firing intent. " +
                    "(host=$expectedPkg, launchedFromPackage=$actualPkg)"
            )
            finish()
            return
        }

        Log.i(TAG, "Firing return intent: $returnUri")
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, returnUri)
                    .setPackage(expectedPkg)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { Log.e(TAG, "Failed to fire return intent", it) }

        finish()
    }

    companion object {
        private const val TAG = "FakeAuthenticator"
        private const val EXTRA_RETURN_URI = "com.microsoft.identity.broker.RETURN_URI"
        private const val EXPECTED_SCHEME = "msauth-return"
    }
}
