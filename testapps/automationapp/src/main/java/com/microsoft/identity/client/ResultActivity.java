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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

/**
 * Activity that is used to display the result sent back, could be success case or error case.
 */
public class ResultActivity extends AppCompatActivity {

    private TextView mTextView;
    private TextView mLogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mTextView = (TextView) findViewById(R.id.resultInfo);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        String resultText;
        try {
            resultText = convertIntentDataToJsonString();
        } catch (final JSONException e) {
            resultText = "{\"error \" : \"Unable to convert to JSON\"}";
        }
        mTextView.setText(resultText);

        mLogView = (TextView) findViewById(R.id.resultLogs);
        mLogView.setText(((AndroidAutomationApp)this.getApplication()).getMsalLogs());
        
        final Button doneButton = (Button) findViewById(R.id.resultDone);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResultActivity.this.finish();
            }
        });
    }

    private String convertIntentDataToJsonString() throws JSONException {
        final Intent intent = getIntent();

        final JSONObject jsonObject = new JSONObject();

        if (!TextUtils.isEmpty(intent.getStringExtra(AutomationAppConstants.ACCESS_TOKEN))) {
            jsonObject.put(AutomationAppConstants.ACCESS_TOKEN, intent.getStringExtra(AutomationAppConstants.ACCESS_TOKEN));
            jsonObject.put(AutomationAppConstants.ACCESS_TOKEN_TYPE, intent.getStringExtra(AutomationAppConstants.ACCESS_TOKEN_TYPE));
            jsonObject.put(AutomationAppConstants.REFRESH_TOKEN, intent.getStringExtra(AutomationAppConstants.REFRESH_TOKEN));
            jsonObject.put(AutomationAppConstants.EXPIRES_ON, new Date(intent.getLongExtra(AutomationAppConstants.EXPIRES_ON, 0)));
            jsonObject.put(AutomationAppConstants.TENANT_ID, intent.getStringExtra(AutomationAppConstants.TENANT_ID));
            jsonObject.put(AutomationAppConstants.UNIQUE_ID, intent.getStringExtra(AutomationAppConstants.UNIQUE_ID));
            jsonObject.put(AutomationAppConstants.DISPLAYABLE_ID, intent.getStringExtra(AutomationAppConstants.DISPLAYABLE_ID));
            jsonObject.put(AutomationAppConstants.NAME, intent.getStringExtra(AutomationAppConstants.NAME));
            jsonObject.put(AutomationAppConstants.UNIQUE_USER_IDENTIFIER, intent.getStringArrayExtra(AutomationAppConstants.UNIQUE_USER_IDENTIFIER));
            jsonObject.put(AutomationAppConstants.IDENTITY_PROVIDER, intent.getStringExtra(AutomationAppConstants.IDENTITY_PROVIDER));
            jsonObject.put(AutomationAppConstants.ID_TOKEN, intent.getStringExtra(AutomationAppConstants.ID_TOKEN));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(AutomationAppConstants.ERROR))) {
            jsonObject.put(AutomationAppConstants.ERROR, intent.getStringExtra(AutomationAppConstants.ERROR));
            jsonObject.put(AutomationAppConstants.ERROR_DESCRIPTION, intent.getStringExtra(AutomationAppConstants.ERROR_DESCRIPTION));
            jsonObject.put(AutomationAppConstants.ERROR_CAUSE, intent.getSerializableExtra(AutomationAppConstants.ERROR_CAUSE));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(AutomationAppConstants.EXPIRED_ACCESS_TOKEN_COUNT))) {
            jsonObject.put(AutomationAppConstants.EXPIRED_ACCESS_TOKEN_COUNT, intent.getStringExtra(AutomationAppConstants.EXPIRED_ACCESS_TOKEN_COUNT));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(AutomationAppConstants.INVALIDATED_REFRESH_TOKEN_COUNT))) {
            jsonObject.put(AutomationAppConstants.INVALIDATED_REFRESH_TOKEN_COUNT, intent.getStringExtra(AutomationAppConstants.INVALIDATED_REFRESH_TOKEN_COUNT));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(AutomationAppConstants.INVALIDATED_FAMILY_REFRESH_TOKEN_COUNT))) {
            jsonObject.put(AutomationAppConstants.INVALIDATED_FAMILY_REFRESH_TOKEN_COUNT, intent.getStringExtra(AutomationAppConstants.INVALIDATED_FAMILY_REFRESH_TOKEN_COUNT));
        } else if (!TextUtils.isEmpty(intent.getStringExtra(AutomationAppConstants.CLEARED_ACCESS_TOKEN_COUNT))) {
            jsonObject.put(AutomationAppConstants.CLEARED_ACCESS_TOKEN_COUNT, intent.getStringExtra(AutomationAppConstants.CLEARED_ACCESS_TOKEN_COUNT));
        } else if (intent.getStringArrayListExtra(AutomationAppConstants.READ_CACHE) != null) {
            final ArrayList<String> items = intent.getStringArrayListExtra(AutomationAppConstants.READ_CACHE);
            jsonObject.put(AutomationAppConstants.ITEM_COUNT, items.size());

            final ArrayList<String> itemsWithCount = new ArrayList<>();
            itemsWithCount.addAll(items);
            final JSONArray arrayItems = new JSONArray(itemsWithCount);
            jsonObject.put("items", arrayItems);
        }
        
        return jsonObject.toString();
    }
}
