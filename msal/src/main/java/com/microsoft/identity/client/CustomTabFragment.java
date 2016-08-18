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
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;

/**
 * Fragment embedded in {@link AuthenticationActivity} to launch the chrome custom tab.
 */
public final class CustomTabFragment extends Fragment {
    private static final String TAG = CustomTabFragment.class.getSimpleName();
    private boolean mRestarted;
    private String mRequestUrl;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRestarted = savedInstanceState != null;
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
    }

    @Override
    public final void onResume() {
        super.onResume();

        if (mRestarted) {
            final Activity activity = getActivity();
            if (activity instanceof AuthenticationActivity)  {
                ((AuthenticationActivity) activity).cancelRequest();
                return;
            }
        }

        mRestarted = true;

        final String chromePackageWithCustomTabSupport = MSALUtils.getChromePackageWithCustomTabSupport(
                this.getActivity().getApplicationContext());
        final boolean isCustomTabDisabled = getActivity().getIntent().getBooleanExtra(InteractiveRequest.DISABLE_CHROMETAB, false);
        mRequestUrl =  getActivity().getIntent().getStringExtra(Constants.REQUEST_URL_KEY);

        if (chromePackageWithCustomTabSupport != null && !isCustomTabDisabled) {
            final CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
            customTabsIntent.intent.setPackage(MSALUtils.getChromePackageWithCustomTabSupport(this.getActivity()));
            customTabsIntent.launchUrl(getActivity(), Uri.parse(mRequestUrl));
        } else {
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mRequestUrl));
            browserIntent.setPackage(MSALUtils.getChromePackage(this.getActivity().getApplicationContext()));
            browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
//            browserIntent.putExtra(Browser.EXTRA_APPLICATION_ID, "com.microsoft.identity.client");
            this.getActivity().startActivity(browserIntent);
        }
    }

    @Override
    public final void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(Constants.REQUEST_URL_KEY, mRequestUrl);
    }
}
