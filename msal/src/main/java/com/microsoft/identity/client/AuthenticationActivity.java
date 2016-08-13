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
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;

import java.util.List;

/**
 * Custom tab requires the device to have a browser with custom tab support, chrome with version >= 45 comes with the
 * support and is available on all devices with API version >= 16 . The sdk use chrome custom tab, and before launching
 * chrome custom tab, we need to check if chrome package is in the device, if it is, it's safe to launch the chrome
 * custom tab; Otherwise the sdk will fall back to the embedded webview(TODO: what is the fallback solution, default browser or webview).
 * AuthenticationActivity will be responsible for checking if it's safe to launch chrome custom tab, if not, will
 * create webview and launch the embedded webview.
 */
public final class AuthenticationActivity extends Activity {

    private static final String TAG = AuthenticationActivity.class.getSimpleName();

    private String mRequestUrl;

    private String mRequestId;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: when AuthenticationActivity is created, request url will be passed in. The url will be constructed in
        // the request object itself.
        // read the url if savedInstanceState is null
        if (savedInstanceState != null) {
            return;
        }

        final Intent data = getIntent();
        if (data == null) {
            // TODO: return to caller with error case.
            return;
        }

        mRequestUrl = data.getStringExtra(Constants.REQUEST_URL_KEY);
        mRequestId = data.getStringExtra(Constants.REQUEST_ID);
        if (MSALUtils.isEmpty(mRequestUrl)) {
            // TODO: return to caller with error case
            return;
        }

        if (MSALUtils.getChromePackages(this.getApplicationContext()) != null) {
            if (!hasCustomTabRedirectActivity()) {
                // TODO: error case.
                // Every app should opt in to use custom tab. We'll check if they have the app itself has the url
                // scheme, if developers does not set it and chrome custom tab is supported, we'll fail.
                return;
            }

            final CustomTabFragment fragment = new CustomTabFragment();
            fragment. setRetainInstance(true);

            final FragmentManager fragmentManager = this.getFragmentManager();
            fragmentManager.beginTransaction().add(fragment, TAG).commit();
        } else {
            // TODO: provide a fallback. We haven't concluded on how we should proceed with fallback, with webview
            // or default browser or just chrome.
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        final String url = intent.getStringExtra(Constants.CUSTOM_TAB_REDIRECT);

        // TODO: figure out how the request object interacts with the AuthenticationActivity.
        final Intent resultIntent = new Intent();
//        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_FINAL_URL, url);
//        resultIntent.putExtra(AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
//                mAuthRequest);
//        returnToCaller(AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE,
//                resultIntent);
    }

    void cancelRequest() {
        returnToCaller(Constants.UIResponse.CANCEL, new Intent());
    }

    void returnToCaller(final int resultCode, final Intent data) {
        data.putExtra(Constants.REQUEST_ID, mRequestId);

        setResult(resultCode, data);
        this.finish();
    }

    private boolean hasCustomTabRedirectActivity() {
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ResolveInfo> infos = null;
        if (pm != null) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setData(Uri.parse("x-msauth-adaltestapp-210://com.microsoft.adal.2.1.0.TestApp"));
            infos = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
        }
        boolean hasActivity = false;
        if (infos != null) {
            for (ResolveInfo info : infos) {
                ActivityInfo activityInfo = info.activityInfo;
                if (activityInfo.name.equals(CustomTabActivity.class.getName())) {
                    hasActivity = true;
                } else {
                    // another application is listening for this url scheme, don't open
                    // Custom Tab for security reasons
                    return false;
                }
            }
        }
        return hasActivity;
    }
}
