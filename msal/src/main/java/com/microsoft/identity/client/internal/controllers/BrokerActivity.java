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
package com.microsoft.identity.client.internal.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.controllers.CommandDispatcher;
import com.microsoft.identity.common.internal.logging.Logger;

public final class BrokerActivity extends Activity {

    public static final String BROKER_INTENT = "broker_intent";
    static final String BROKER_INTENT_STARTED = "broker_intent_started";
    static final int BROKER_INTENT_REQUEST_CODE = 1001;

    private static final String TAG = BrokerActivity.class.getSimpleName();

    private Intent mBrokerInteractiveRequestIntent;
    private Boolean mBrokerIntentStarted = false;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            mBrokerInteractiveRequestIntent = getIntent().getExtras().getParcelable(BROKER_INTENT);
        } else {
            mBrokerInteractiveRequestIntent = savedInstanceState.getParcelable(BROKER_INTENT);
            mBrokerIntentStarted = savedInstanceState.getBoolean(BROKER_INTENT_STARTED);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBrokerIntentStarted) {
            mBrokerIntentStarted = true;
            startActivityForResult(mBrokerInteractiveRequestIntent, BROKER_INTENT_REQUEST_CODE);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BROKER_INTENT, mBrokerInteractiveRequestIntent);
        outState.putBoolean(BROKER_INTENT_STARTED, mBrokerIntentStarted);
    }

    /**
     * Receive result from broker intent
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        final String methodName = ":onActivityResult";
        Logger.info(TAG + methodName,
                "Result received from Broker "
                        + "Request code: " + requestCode
                        +  " Result code: " + requestCode
        );

        if (resultCode == AuthenticationConstants.UIResponse.TOKEN_BROKER_RESPONSE ||
                resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_CANCEL
                || resultCode == AuthenticationConstants.UIResponse.BROWSER_CODE_ERROR) {

            Logger.verbose(TAG + methodName, "Completing interactive request ");
            CommandDispatcher.completeInteractive(requestCode, resultCode, data);
        }
        finish();
    }


}
