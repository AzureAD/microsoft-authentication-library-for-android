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
import android.content.Intent;
import android.os.Bundle;

/**
 * MSAL activity class(needs to be public to be discoverable by the os) to get the redirect with code from authorize
 * endpoint. This activity has to be exposed by "android:exported=true", and intent filter has to be declared in the
 * manifest for the activity. When chrome custom tab is launched, and we're redirected back with the redirect
 * uri(with this being said, the redirect has to be unique across apps), the os will fire an intent with the redirect,
 * and the CustomTabActivity will be launched.
 * <intent-filter>
 *     <action android:name="android.intent.action.VIEW" />
 *
 *     To receive implicit intents, have to put the activity in the category of default.
 *     <category android:name="android.intent.category.DEFAULT" />
 *
 *     The target activity allows itself to be started by a web browser to display data.
 *     <category android:name="android.intent.category.BROWSABLE" />
 *
 *     CustomTabActivity will be launched when matching the custom url scheme.
 *     <data android:scheme="msauth-clientid" android:host=="appPackageName"/>
 * </intent-filter>
 */
public final class CustomTabActivity extends Activity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = new Intent(this, AuthenticationActivity.class);
        intent.putExtra(Constants.CUSTOM_TAB_REDIRECT, getIntent().getDataString());

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}