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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import static com.microsoft.aad.automation.testapp.R.id.acquireToken;
import static com.microsoft.aad.automation.testapp.R.id.acquireTokenSilent;
import static com.microsoft.aad.automation.testapp.R.id.clearCache;
import static com.microsoft.aad.automation.testapp.R.id.expireAccessToken;
import static com.microsoft.aad.automation.testapp.R.id.getUsers;
import static com.microsoft.aad.automation.testapp.R.id.invalidateRefreshToken;
import static com.microsoft.aad.automation.testapp.R.id.readCache;
import static com.microsoft.aad.automation.testapp.R.id.signOut;
import static com.microsoft.aad.automation.testapp.R.layout.activity_main;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int[] sButtonIds = {
            acquireToken,
            acquireTokenSilent,
            expireAccessToken,
            invalidateRefreshToken,
            readCache,
            clearCache,
            getUsers,
            signOut
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_main);

        for (int viewId : sButtonIds) {
            findViewById(viewId).setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        if (view instanceof Button) {
            Button clicked = (Button) view;
            Log.v(LOG_TAG, "Clicked: [" + clicked.getText() + "]");
        }

        switch (view.getId()) {
            case acquireToken:
            case acquireTokenSilent:
            case expireAccessToken:
            case invalidateRefreshToken:
            case signOut:
                launchAuthenticationInfoActivity(view.getId());
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
            default:
                throw new IllegalStateException("Click event not matched to an action");
        }
    }

    private void launchAuthenticationInfoActivity(int flowCode) {
        final Intent intent = new Intent(this, SignInActivity.class);
        intent.putExtra(SignInActivity.FLOW_CODE, flowCode);
        startActivity(intent);
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
}
