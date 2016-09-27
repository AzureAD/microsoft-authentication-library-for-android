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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Test activity for testing purpose.
 */
public class TestActivity extends Activity {
    private static final String ACTIVITY_TO_LAUNCH = "extraActivityIntentToLaunch";
    private static final int REQUEST_CODE = 1234;
    private static int sResultCode;
    private static Intent sResultData;

    public static Intent createIntent(final Context context, final Intent activityIntentToLaunch) {
        final Intent intent = new Intent(context, TestActivity.class);
        intent.putExtra(ACTIVITY_TO_LAUNCH, activityIntentToLaunch);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE) {
            return;
        }

        sResultCode = resultCode;
        sResultData = data;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            sResultCode = 0;
            sResultData = null;
            final Intent activityToLaunch = getIntent().getParcelableExtra(ACTIVITY_TO_LAUNCH);
            startActivityForResult(activityToLaunch, REQUEST_CODE);
        }
    }

    static int getResultCode() {
        return sResultCode;
    }

    static Intent getResultData() {
        return sResultData;
    }
}
