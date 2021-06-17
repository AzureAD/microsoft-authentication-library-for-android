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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.microsoft.identity.common.internal.providers.oauth2.CurrentTaskBrowserAuthorizationFragment;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.DESTROY_REDIRECT_RECEIVING_ACTIVITY_ACTION;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.REDIRECT_RETURNED_ACTION;


/**
 * MSAL activity class (needs to be public in order to be discoverable by the os) to get the browser redirect with auth code from authorize
 * endpoint. This activity has to be exposed by "android:exported=true", and intent filter has to be declared in the
 * manifest for the activity.
 * <p>
 * When the AuthorizationAgent is launched, and we're redirected back with the redirect
 * uri (the redirect must be unique across apps on a device), the os will fire an intent with the redirect,
 * and the CurrentTaskBrowserTabActivity will be launched.
 * <p>
 * Only use this if you've configured MSAL to use authorization_in_current_task
 * <pre>
 * &lt;intent-filter&gt;
 *     &lt;action android:name="android.intent.action.VIEW" /&gt;
 *
 *     To receive implicit intents, have to put the activity in the category of default.
 *     &lt;category android:name="android.intent.category.DEFAULT" /&gt;
 *
 *     The target activity allows itself to be started by a web browser to display data.
 *     &lt;category android:name="android.intent.category.BROWSABLE" /&gt;
 *
 *     BrowserTabActivity will be launched when matching the custom url scheme.
 *     &lt;data android:scheme="msalclientid" android:host="auth" /&gt;
 * &lt;/intent-filter&gt;
 * </pre>
 */
public final class CurrentTaskBrowserTabActivity extends Activity {
    private static final String TAG = CurrentTaskBrowserTabActivity.class.getSimpleName();
    private static final int REDIRECT_RECEIVED_CODE = 2;
    private BroadcastReceiver mCloseBroadcastReceiver;
    //private int mTaskIdResponseFor;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String response = getIntent().getDataString();

        if (savedInstanceState == null
                && getIntent() != null
                && !StringUtil.isEmpty(getIntent().getDataString())) {
            //Leaving this for now to set static response URI value
            final Intent responseIntent = CurrentTaskBrowserAuthorizationFragment.createCustomTabResponseIntent(this, response);

            if (responseIntent != null) {
                startActivityForResult(responseIntent, REDIRECT_RECEIVED_CODE);
            } else {
                Logger.warn(TAG, "Received NULL response intent. Unable to complete authorization.");
                Toast.makeText(getApplicationContext(), "Unable to complete authorization as there is no interactive call in progress. This can be due to closing the app while the authorization was in process.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_CANCELED) {
            // We weren't able to open CurrentTaskAuthorizationActivity from the back stack. Send a broadcast
            // instead.
            Intent broadcast = new Intent(REDIRECT_RETURNED_ACTION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);




            // Wait for the custom tab to be removed from the back stack before finishing.
            mCloseBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    boolean hasNullTaskAffinity = false;
                    final PackageManager packageManager = CurrentTaskBrowserTabActivity.this.getApplicationContext().getPackageManager();
                    try {
                        final ActivityInfo activityInfo = CurrentTaskBrowserTabActivity.this.getComponentName() != null ? packageManager.getActivityInfo(CurrentTaskBrowserTabActivity.this.getComponentName(), 0) : null;
                        if(activityInfo.taskAffinity == null){
                            hasNullTaskAffinity = true;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                    finishActivity(REDIRECT_RECEIVED_CODE);
                    if(Build.VERSION.SDK_INT > 21 && hasNullTaskAffinity) {
                        finishAndRemoveTask();
                    }else{
                        finish();
                    }
                }
            };
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    mCloseBroadcastReceiver,
                    new IntentFilter(DESTROY_REDIRECT_RECEIVING_ACTIVITY_ACTION)
            );
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCloseBroadcastReceiver);
        super.onDestroy();
    }

}