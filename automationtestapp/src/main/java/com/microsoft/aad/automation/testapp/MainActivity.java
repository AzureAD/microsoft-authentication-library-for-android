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

package com.microsoft.aad.automation.testapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.microsoft.testapp.R;

import static com.microsoft.testapp.R.id.acquireToken;
import static com.microsoft.testapp.R.id.acquireTokenSilent;
import static com.microsoft.testapp.R.id.clearCache;
import static com.microsoft.testapp.R.id.et_userHomeId;
import static com.microsoft.testapp.R.id.expireAccessToken;
import static com.microsoft.testapp.R.id.getUsers;
import static com.microsoft.testapp.R.id.invalidateFamilyRefreshToken;
import static com.microsoft.testapp.R.id.invalidateRefreshToken;
import static com.microsoft.testapp.R.id.readCache;
import static com.microsoft.testapp.R.id.signout;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int[] sButtonIds = {
            acquireToken,
            acquireTokenSilent,
            expireAccessToken,
            invalidateRefreshToken,
            invalidateFamilyRefreshToken,
            readCache,
            clearCache,
            getUsers,
            signout
    };

    private EditText mUserHomeObjectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUserHomeObjectId = (EditText) findViewById(et_userHomeId);

        for (int viewId : sButtonIds) {
            findViewById(viewId).setOnClickListener(mClickListener);
        }
    }

    View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view instanceof Button) {
                Button clicked = (Button) view;
                Log.v(LOG_TAG, "Clicked: [" + clicked.getText() + "]");
            }

            switch (view.getId()) {
                case acquireToken:
                    onAcquireTokenClicked();
                    break;
                case acquireTokenSilent:
                    onAcquireTokenSilentClicked();
                    break;
                case expireAccessToken:
                    onExpireAccessTokenClicked();
                    break;
                case invalidateRefreshToken:
                    onInvalidateRefreshTokenClicked();
                    break;
                case invalidateFamilyRefreshToken:
                    onInvalidateFamilyRefreshTokenClicked();
                    break;
                case readCache:
                    onReadCacheClicked();
                    break;
                case clearCache:
                    onClearCacheClicked();
                    break;
                case getUsers:
                    onGetUsersClicked();
                    break;
                case signout:
                    onSignOutClicked();
                    break;
                default:
                    throw new IllegalStateException("Click event not matched to an action");
            }
        }
    };

    private void onAcquireTokenClicked() {
        // TODO
    }

    private void onAcquireTokenSilentClicked() {
        // TODO
    }

    private void onExpireAccessTokenClicked() {
        // TODO
    }

    private void onInvalidateRefreshTokenClicked() {
        // TODO
    }

    private void onInvalidateFamilyRefreshTokenClicked() {
        // TODO
    }

    private void onReadCacheClicked() {
        // TODO
    }

    private void onClearCacheClicked() {
        // TODO
    }

    private void onGetUsersClicked() {
        // TODO
    }

    private void onSignOutClicked() {
        final String userHomeObjectId = mUserHomeObjectId.getText().toString();
        // TODO
    }
}
