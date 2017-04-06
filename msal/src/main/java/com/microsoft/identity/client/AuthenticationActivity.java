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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.webkit.WebSettings;

/**
 * Custom tab requires the device to have a browser with custom tab support, chrome with version >= 45 comes with the
 * support and is available on all devices with API version >= 16 . The sdk use chrome custom tab, and before launching
 * chrome custom tab, we need to check if chrome package is in the device, if it is, it's safe to launch the chrome
 * custom tab; Otherwise the sdk will launch chrome(TODO: what is the fallback solution, default browser or webview).
 * AuthenticationActivity will be responsible for checking if it's safe to launch chrome custom tab, if not, will
 * go with chrome browser, if chrome is not installed, we throw error back.
 */
public final class AuthenticationActivity extends Activity {

    private static final String TAG = AuthenticationActivity.class.getSimpleName(); //NOPMD
    private String mRequestUrl;
    private int mRequestId;
    private boolean mRestarted;
    private String mChromePackageWithCustomTabSupport;
    private CustomTabsClient mCustomTabsClient;
    private CustomTabsSession mCustomTabsSession;
    private CustomTabsIntent mCustomTabsIntent;
    private CustomTabsServiceConnection mCustomTabsServiceConnection;
    private boolean mCustomTabsServiceIsBound;
    private UiEvent.Builder mUiEventBuilder;
    private Telemetry.RequestId mTelemetryRequestId;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If activity is killed by the os, savedInstance will be the saved bundle.
        if (savedInstanceState != null) {
            Logger.verbose(TAG, null, "AuthenticationActivity is re-created after killed by the os.");
            mRestarted = true;
            return;
        }

        final Intent data = getIntent();
        if (data == null) {
            sendError(MSALError.UNRESOLVABLE_INTENT, "Received null data intent from caller");
            return;
        }

        mRequestUrl = data.getStringExtra(Constants.REQUEST_URL_KEY);
        mRequestId = data.getIntExtra(Constants.REQUEST_ID, 0);
        if (MSALUtils.isEmpty(mRequestUrl)) {
            sendError(MSALError.UNRESOLVABLE_INTENT, "Request url is not set on the intent");
            return;
        }

        // We'll use custom tab if the chrome installed on the device comes with custom tab support(on 45 and above it
        // does). If the chrome package doesn't contain the support, we'll use chrome to launch the UI.
        if (MSALUtils.getChromePackage(this.getApplicationContext()) == null) {
            Logger.info(TAG, null, "Chrome is not installed on the device, cannot continue with auth.");
            sendError(MSALError.CHROME_NOT_INSTALLED, "Chrome is not installed on the device, cannot proceed with auth");
            return;
        }

        mChromePackageWithCustomTabSupport = MSALUtils.getChromePackageWithCustomTabSupport(getApplicationContext());

        mTelemetryRequestId = new Telemetry.RequestId(data.getStringExtra(Constants.TELEMETRY_REQUEST_ID));
        mUiEventBuilder = new UiEvent.Builder();
        mUiEventBuilder.setUserAgent(WebSettings.getDefaultUserAgent(this));
        Telemetry.getInstance().startEvent(mTelemetryRequestId, mUiEventBuilder.getEventName());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mChromePackageWithCustomTabSupport != null) {
            warmUpCustomTabs();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCustomTabsServiceIsBound) {
            unbindService(mCustomTabsServiceConnection);
            mCustomTabsServiceIsBound = false;
        }
    }

    private void warmUpCustomTabs() {
        mCustomTabsServiceConnection = createCustomTabsServiceConnection();

        // Initiate the service-bind action
        CustomTabsClient.bindCustomTabsService(
                this,
                mChromePackageWithCustomTabSupport,
                mCustomTabsServiceConnection
        );

        // Create the Intent used to launch the Url
        mCustomTabsIntent = new CustomTabsIntent.Builder(mCustomTabsSession)
                .setShowTitle(true)
                .build();
        mCustomTabsIntent.intent.setPackage(mChromePackageWithCustomTabSupport);
    }

    @NonNull
    private CustomTabsServiceConnection createCustomTabsServiceConnection() {
        return new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                mCustomTabsServiceIsBound = true;
                mCustomTabsClient = client;
                mCustomTabsClient.warmup(0L);
                mCustomTabsSession = mCustomTabsClient.newSession(null);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mCustomTabsClient = null;
            }
        };
    }

    /**
     * OnNewIntent will be called before onResume.
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Logger.info(TAG, null, "onNewIntent is called, received redirect from system webview.");
        final String url = intent.getStringExtra(Constants.CUSTOM_TAB_REDIRECT);

        final Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, url);
        returnToCaller(Constants.UIResponse.AUTH_CODE_COMPLETE,
                resultIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mRestarted) {
            cancelRequest();
            return;
        }

        mRestarted = true;

        final String chromePackageWithCustomTabSupport = MSALUtils.getChromePackageWithCustomTabSupport(
                this.getApplicationContext());
        mRequestUrl =  this.getIntent().getStringExtra(Constants.REQUEST_URL_KEY);

        Logger.infoPII(TAG, null, "Request to launch is: " + mRequestUrl);
        if (chromePackageWithCustomTabSupport != null) {
            Logger.info(TAG, null, "ChromeCustomTab support is available, launching chrome tab.");
            final CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder().build();
            customTabsIntent.intent.setPackage(MSALUtils.getChromePackageWithCustomTabSupport(this));
            customTabsIntent.launchUrl(this, Uri.parse(mRequestUrl));
        } else {
            Logger.info(TAG, null, "Chrome tab support is not available, launching chrome browser.");
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mRequestUrl));
            browserIntent.setPackage(MSALUtils.getChromePackage(this.getApplicationContext()));
            browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
            this.startActivity(browserIntent);
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(Constants.REQUEST_URL_KEY, mRequestUrl);
    }

    /**
     * Cancels the auth request.
     */
    void cancelRequest() {
        Logger.verbose(TAG, null, "Cancel the authentication request.");
        mUiEventBuilder.setUserDidCancel();
        returnToCaller(Constants.UIResponse.CANCEL, new Intent());
    }

    /**
     * Return the error back to caller.
     *
     * @param resultCode The result code to return back.
     * @param data       {@link Intent} contains the detailed result.
     */
    private void returnToCaller(final int resultCode, final Intent data) {
        Logger.info(TAG, null, "Return to caller with resultCode: " + resultCode + "; requestId: " + mRequestId);
        data.putExtra(Constants.REQUEST_ID, mRequestId);

        if (null != mUiEventBuilder) {
            Telemetry.getInstance().stopEvent(mTelemetryRequestId, mUiEventBuilder);
        }

        setResult(resultCode, data);
        this.finish();
    }

    /**
     * Send error back to caller with the error description.
     *
     * @param errorCode        The error code to send back.
     * @param errorDescription The error description to send back.
     */
    private void sendError(final String errorCode, final String errorDescription) {
        Logger.info(TAG, null, "Sending error back to the caller, errorCode: " + errorCode + "; errorDescription"
                + errorDescription);
        final Intent errorIntent = new Intent();
        errorIntent.putExtra(Constants.UIResponse.ERROR_CODE, errorCode);
        errorIntent.putExtra(Constants.UIResponse.ERROR_DESCRIPTION, errorDescription);
        returnToCaller(Constants.UIResponse.AUTH_CODE_ERROR, errorIntent);
    }
}
