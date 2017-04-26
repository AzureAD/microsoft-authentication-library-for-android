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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String FLOW_CODE = "FlowCode";
    public static final int ACQUIRE_TOKEN = 1001;
    public static final int ACQUIRE_TOKEN_SILENT = 1002;
    public static final int INVALIDATE_ACCESS_TOKEN = 1003;
    public static final int INVALIDATE_REFRESH_TOKEN = 1004;
    public static final int INVALIDATE_FAMILY_REFRESH_TOKEN = 1006;
    public static final int READ_CACHE = 1005;
    
    private Context mContext;    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        
        // Button for acquireToken call
        final Button acquireTokenButton = (Button) findViewById(R.id.acquireToken);
        acquireTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchAuthenticationInfoActivity(ACQUIRE_TOKEN);
            }
        });

        // Button for acquireTokenSilent call
        final Button acquireTokenSilentButton = (Button) findViewById(R.id.acquireTokenSilent);
        acquireTokenSilentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAuthenticationInfoActivity(ACQUIRE_TOKEN_SILENT);
            }
        });

        final Button invalidateAccessTokenButton = (Button) findViewById(R.id.expireAccessToken);
        invalidateAccessTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAuthenticationInfoActivity(INVALIDATE_ACCESS_TOKEN);
            }
        });

        final Button invalidateRefreshToken = (Button) findViewById(R.id.invalidateRefreshToken);
        invalidateRefreshToken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchAuthenticationInfoActivity(INVALIDATE_REFRESH_TOKEN);
            }
        });

        final Button readCacheButton = (Button) findViewById(R.id.readCache);
        readCacheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processReadCacheRequest();
            }
        });

        final Button emptyCacheButton = (Button) findViewById(R.id.clearCache);
        emptyCacheButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processEmptyCacheRequest();
            }
        });
    }

    private void launchAuthenticationInfoActivity(int flowCode) {
        final Intent intent = new Intent();
        intent.setClass(mContext, SignInActivity.class);
        intent.putExtra(FLOW_CODE, flowCode);
        this.startActivity(intent);
    }

    private void processEmptyCacheRequest() {
        final Intent intent = new Intent();

        final TokenCacheAccessor tokenCacheAccessor = new TokenCacheAccessor(getApplicationContext());
        int clearedAccessTokenCount = tokenCacheAccessor.getAllAccessTokens("").size();
        intent.putExtra(AutomationAppConstants.CLEARED_ACCESS_TOKEN_COUNT, String.valueOf(clearedAccessTokenCount));
        final SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences("com.microsoft.identity.client.token", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        int clearedRefreshTokenCount = tokenCacheAccessor.getAllAccessTokens("").size();
        intent.putExtra(AutomationAppConstants.CLEARED_REFRESH_TOKEN_COUNT, String.valueOf(clearedRefreshTokenCount));
        final SharedPreferences.Editor rtEditor = getApplicationContext().getSharedPreferences("com.microsoft.identity.client.refreshToken", MODE_PRIVATE).edit();
        rtEditor.clear();
        editor.apply();
        
        launchResultActivity(intent);
    }

    private void processReadCacheRequest() {
        final TokenCacheAccessor tokenCacheAccessor = new TokenCacheAccessor(getApplicationContext());
        Intent intent = new Intent();
        final ArrayList<String> allItems = new ArrayList<>(tokenCacheAccessor.getAllAccessTokens(""));
        intent.putStringArrayListExtra(AutomationAppConstants.READ_CACHE, allItems);
        
        launchResultActivity(intent);
    }

    private void launchResultActivity(final Intent intent) {
        intent.setClass(this.getApplicationContext(), ResultActivity.class);
        this.startActivity(intent);
    }
}