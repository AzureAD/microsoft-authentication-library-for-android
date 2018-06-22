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
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MsalChromeCustomTabManager {

    private static final String TAG = MsalChromeCustomTabManager.class.getSimpleName();
    private MsalCustomTabsServiceConnection mCustomTabsServiceConnection;
    private CustomTabsIntent mCustomTabsIntent;
    private String mChromePackageWithCustomTabSupport;
    private Activity mParentActivity;
    private static final long CUSTOM_TABS_MAX_CONNECTION_TIMEOUT = 1L;

    /**
     * Constructor of MsalChromeCustomTabManager.
     *
     * @param activity Instance of calling activity.
     */
    public MsalChromeCustomTabManager(final Activity activity) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity parameter cannot be null");
        }
        mParentActivity = activity;
        //TODO: Can move MsalUtils chrome specific util method to common when refactoring
        mChromePackageWithCustomTabSupport = MsalUtils.getChromePackageWithCustomTabSupport(mParentActivity.getApplicationContext());
    }

    /**
     * Method to bind Chrome {@link android.support.customtabs.CustomTabsService}.
     * Waits until the {@link MsalCustomTabsServiceConnection} is connected or the
     * {@link MsalChromeCustomTabManager#CUSTOM_TABS_MAX_CONNECTION_TIMEOUT} is timed out.
     */
    public void bindCustomTabsService() {
        if (mChromePackageWithCustomTabSupport != null) {

            final CountDownLatch latch = new CountDownLatch(1);
            mCustomTabsServiceConnection = new MsalCustomTabsServiceConnection(latch);

            // Initiate the service-bind action
            CustomTabsClient.bindCustomTabsService(mParentActivity, mChromePackageWithCustomTabSupport, mCustomTabsServiceConnection);

            boolean customTabsServiceConnected = waitForServiceConnectionToEstablish(latch);

            final CustomTabsIntent.Builder builder = customTabsServiceConnected
                    ? new CustomTabsIntent.Builder(mCustomTabsServiceConnection.getCustomTabsSession()) : new CustomTabsIntent.Builder();

            // Create the Intent used to launch the Url
            mCustomTabsIntent = builder.setShowTitle(true).build();
            mCustomTabsIntent.intent.setPackage(mChromePackageWithCustomTabSupport);
        }
    }

    /**
     * Helper method to wait for MsalCustomTabsServiceConnection to establish.
     */
    private boolean waitForServiceConnectionToEstablish(CountDownLatch latch) {
        boolean connectionEstablished = true;
        try {
            // await returns true if count is 0, false if action times out
            // invert this boolean to indicate if we should skip warming up
            boolean timedOut = !latch.await(CUSTOM_TABS_MAX_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
            if (timedOut) {
                // if the request timed out, we don't actually know whether or not the service connected.
                // to be safe, we'll skip warmup and rely on mCustomTabsServiceIsBound
                // to unbind the Service when onStop() is called.
                connectionEstablished = false;
                Logger.warning(TAG, null, "Connection to CustomTabs timed out. Skipping warmup.");
            }
        } catch (InterruptedException e) {
            Logger.error(TAG, null, "Failed to connect to CustomTabs. Skipping warmup.", e);
            connectionEstablished = false;
        }
        return connectionEstablished;
    }

    /**
     * Method to unbind Chrome {@link android.support.customtabs.CustomTabsService}.
     */
    public void unbindCustomTabsService() {
        if (null != mCustomTabsServiceConnection && mCustomTabsServiceConnection.getCustomTabsServiceIsBound()) {
            mParentActivity.unbindService(mCustomTabsServiceConnection);
        }
    }

    /**
     * Launches a Chrome Custom tab if available else Chrome Browser for the URL.
     * CustomTabService needs to be bound using {@link MsalChromeCustomTabManager#bindCustomTabsService()}
     * before calling this method.
     *
     * @param requestUrl URL to be loaded.
     */
    public void launchChromeTabOrBrowserForUrl(String requestUrl) {
        if (mChromePackageWithCustomTabSupport != null && mCustomTabsIntent != null) {
            Logger.info(TAG, null, "ChromeCustomTab support is available, launching chrome tab.");
            mCustomTabsIntent.launchUrl(mParentActivity, Uri.parse(requestUrl));
        } else {
            Logger.info(TAG, null, "Chrome tab support is not available, launching chrome browser.");
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl));
            ////TODO: Can move MsalUtils chrome specific util method to common when refactoring.
            browserIntent.setPackage(MsalUtils.getChromePackage(mParentActivity.getApplicationContext()));
            browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
            mParentActivity.startActivity(browserIntent);
        }
    }

    /**
     * Sub class of CustomTabsServiceConnection to handle lifetime of the
     * CustomTabService connection.
     */
    private static class MsalCustomTabsServiceConnection extends CustomTabsServiceConnection {

        private final WeakReference<CountDownLatch> mLatchWeakReference;
        private CustomTabsClient mCustomTabsClient;
        private CustomTabsSession mCustomTabsSession;
        private boolean mCustomTabsServiceIsBound;

        MsalCustomTabsServiceConnection(final CountDownLatch latch) {
            mLatchWeakReference = new WeakReference<>(latch);
        }

        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            final CountDownLatch latch = mLatchWeakReference.get();

            mCustomTabsServiceIsBound = true;
            mCustomTabsClient = client;
            mCustomTabsClient.warmup(0L);
            mCustomTabsSession = mCustomTabsClient.newSession(null);

            if (null != latch) {
                latch.countDown();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mCustomTabsServiceIsBound = false;
        }

        /**
         * Gets the {@link CustomTabsSession} associated to this CustomTabs connection.
         *
         * @return the session.
         */
        CustomTabsSession getCustomTabsSession() {
            return mCustomTabsSession;
        }

        /**
         * Boolean to indicate if the {@link android.support.customtabs.CustomTabsService} is bound or not.
         *
         * @return true if the CustomTabsService is bound else false.
         */
        boolean getCustomTabsServiceIsBound() {
            return mCustomTabsServiceIsBound;
        }
    }
}
