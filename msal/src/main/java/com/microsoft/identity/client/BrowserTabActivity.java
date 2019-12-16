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
import android.os.Bundle;

import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.util.StringUtil;

/**
 * MSAL activity class (needs to be public in order to be discoverable by the os) to get the browser redirect with auth code from authorize
 * endpoint. This activity has to be exposed by "android:exported=true", and intent filter has to be declared in the
 * manifest for the activity.
 * <p>
 * When the AuthorizationAgent is launched, and we're redirected back with the redirect
 * uri (the redirect must be unique across apps on a device), the os will fire an intent with the redirect,
 * and the BrowserTabActivity will be launched.
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
public final class BrowserTabActivity extends Activity {
    //private static final String TAG = BrowserTabActivity.class.getSimpleName();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null
                && getIntent() != null
                && !StringUtil.isEmpty(getIntent().getDataString())) {
            startActivity(AuthorizationActivity.createCustomTabResponseIntent(this, getIntent().getDataString()));
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent() != null
                && getIntent().hasExtra(AuthorizationStrategy.RESULT_CODE)) {
            CommandDispatcher.completeInteractive(
                    getIntent().getIntExtra(AuthorizationStrategy.REQUEST_CODE, 0),
                    getIntent().getIntExtra(AuthorizationStrategy.RESULT_CODE, 0),
                    getIntent());
        }
        finish();
    }
}