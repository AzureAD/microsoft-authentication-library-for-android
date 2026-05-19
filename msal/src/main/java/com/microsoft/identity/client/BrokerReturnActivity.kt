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
package com.microsoft.identity.client

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.internal.broker.BrokerValidator
import com.microsoft.identity.common.logging.Logger

/**
 * POC: No-op activity invoked by the broker (Authenticator) after a Conditional
 * Access interrupt flow (e.g. device registration / MAM enrollment) completes,
 * to bring the calling app's task back to the foreground **without disturbing
 * the back stack**.
 *
 * Flow:
 *  1. App calls `acquireTokenInteractive`. MSAL opens `AuthorizationActivity`
 *     which renders the eSTS sign-in page in a WebView/CustomTab.
 *  2. eSTS shows a CA block page; user is handed off to Authenticator.
 *     The MSAL `AuthorizationActivity` is left **parked** in the caller's task.
 *  3. Authenticator finishes its CA flow and fires:
 *
 *     ```
 *     Intent(ACTION_VIEW, Uri.parse("msauth-return://<callerPackage>/resume"))
 *         .setPackage(callerPackage)
 *         .addFlags(FLAG_ACTIVITY_NEW_TASK)
 *     ```
 *  4. Android delivers the intent to this activity inside the calling app.
 *     Because [BrokerReturnActivity] uses a non-clearing launch mode and shares
 *     the caller's task affinity, the caller's existing task is brought to the
 *     foreground and this activity is pushed on top.
 *  5. [onCreate] calls [finish] immediately; the parked `AuthorizationActivity`
 *     beneath resumes with its WebView state intact.
 *
 * Why not just use `PackageManager.getLaunchIntentForPackage(...)` instead?
 * Apps commonly declare their launcher activity as `singleTask`, which would
 * clear everything above it in the task — killing the parked WebView. This
 * activity sidesteps that by claiming the return intent itself.
 */
class BrokerReturnActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val methodTag = "$TAG:onCreate"
        Logger.info(
            methodTag,
            "BrokerReturnActivity invoked; bouncing back to caller's parked task."
        )

        // Defense-in-depth: verify the launching app is the Microsoft Authenticator
        // (signed with Microsoft's broker signing key). The activity carries no data
        // and its only effect is to bring the caller's own task to the foreground
        // (UID-scoped via ActivityManager.getAppTasks), so this check exists to
        // gate the side-effect to legitimate broker invocations only and to flag
        // misuse early.
        //
        // Caller-resolution strategy:
        //   * API 34+ -> Activity.launchedFromPackage (set by AMS, not spoofable)
        //   * API <34 -> Activity.referrer host (best-effort; can be spoofed via
        //                Intent.EXTRA_REFERRER, but the signature check below is
        //                the real gate).
        val callerPackage: String? = resolveCallerPackage()
        val isAuthenticator = callerPackage != null &&
            AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME
                .equals(callerPackage, ignoreCase = true) &&
            BrokerValidator(this).verifySignature(callerPackage)
        if (!isAuthenticator) {
            Logger.warn(
                methodTag,
                "BrokerReturnActivity invoked by unexpected caller: $callerPackage. Ignoring."
            )
            finish()
            return
        }

        // FLAG_ACTIVITY_NEW_TASK + singleTop does NOT find an existing task by
        // affinity — it only matches the top of a task already rooted by this
        // activity. To bring the caller's parked task forward we ask
        // ActivityManager for our own AppTasks (no permission required) and
        // move the most recently active non-self task to the front.
        // getAppTasks() returns tasks in most-recent-first order.
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val ownTaskId = taskId
        val target = am.appTasks.firstOrNull { appTask ->
            runCatching { appTask.taskInfo.taskId != ownTaskId }.getOrDefault(false)
        }

        if (target != null) {
            val info = runCatching { target.taskInfo }.getOrNull()
            Logger.info(
                methodTag,
                "Moving caller task to front: taskId=" + info?.taskId +
                    " topActivity=" + info?.topActivity?.flattenToShortString()
            )
            target.moveToFront()
        } else {
            Logger.warn(
                methodTag,
                "No parked caller task found; nothing to bring forward."
            )
        }
        finish()
    }

    /**
     * Returns the launching app's package name, preferring the AMS-set
     * [Activity.getLaunchedFromPackage] (API 34+, not spoofable) and falling
     * back to the referrer host on older devices. Returns null if neither
     * source yields a value.
     */
    private fun resolveCallerPackage(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            launchedFromPackage?.let { return it }
        }
        return referrer?.host
    }

    companion object {
        private val TAG: String = BrokerReturnActivity::class.java.simpleName
    }
}
