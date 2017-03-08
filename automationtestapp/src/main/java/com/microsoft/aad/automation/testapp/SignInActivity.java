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
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.microsoft.identity.client.UIOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.aad.automation.testapp.R.id.acquireToken;
import static com.microsoft.aad.automation.testapp.R.id.acquireTokenSilent;
import static com.microsoft.aad.automation.testapp.R.id.expireAccessToken;
import static com.microsoft.aad.automation.testapp.R.id.invalidateRefreshToken;
import static com.microsoft.aad.automation.testapp.R.id.requestGo;
import static com.microsoft.aad.automation.testapp.R.id.requestInfo;
import static com.microsoft.aad.automation.testapp.R.id.signout;
import static com.microsoft.aad.automation.testapp.R.layout.activity_sign_in;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    static final String FLOW_CODE = "FlowCode";

    private static final String LOG_TAG = SignInActivity.class.getSimpleName();

    private static final String RESOURCE = "resource";
    private static final String AUTHORITY = "authority";
    private static final String CLIENT_ID = "client_id";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String USER_IDENTIFIER = "user_identifier";
    private static final String CORRELATION_ID = "correlation_id";
    private static final String UI_OPTION = "ui_option";
    private static final String EXTRA_QUERY_PARAM = "extra_qp";
    private static final String VALIDATE_AUTHORITY = "validate_authority";


    private TextView mTextView;
    private String mAuthority;
    private String mResource;
    private String mClientId;
    private String mRedirectUri;
    private UIOptions mUiOption;
    private String mLoginHint;
    private String mUserId;
    private String mExtraQueryParam;
    private boolean mValidateAuthority;
    private UUID mCorrelationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_sign_in);
        mTextView = (EditText) findViewById(requestInfo);
        findViewById(requestGo).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        final int flowCode = getIntent().getIntExtra(FLOW_CODE, -1);

        final Map<String, String> inputItems;
        try {
            inputItems = readAuthenticationInfo();
        } catch (final JSONException e) {
            sendErrorToResultActivity(Constants.JSON_ERROR, "Unable to read the input JSON info " + e.getMessage());
            return;
        }

        if (inputItems.isEmpty()) {
            return;
        }

        validateUserInput(inputItems, flowCode);

        setAuthenticationData(inputItems);

        switch (flowCode) {
            case acquireToken:
                break;
            case acquireTokenSilent:
                break;
            case expireAccessToken:
                break;
            case invalidateRefreshToken:
                break;
            case signout:
                break;
            default:
                throw new IllegalStateException("Undefined flow");
        }
    }

    private Map<String, String> readAuthenticationInfo() throws JSONException {
        final String userInputText = mTextView.getText().toString();
        if (TextUtils.isEmpty(userInputText)) {
            // TODO: return error
            sendErrorToResultActivity("empty_requestInfo", "No user input for the request.");
            return Collections.emptyMap();
        }

        // parse Json response
        final Map<String, String> inputItems = new HashMap<>();
        extractJsonObjects(inputItems, userInputText);

        return inputItems;
    }

    private void validateUserInput(final Map<String, String> inputItems, int flowCode) {
        if (inputItems.isEmpty()) {
            throw new IllegalArgumentException("No sign-in data typed in the textBox");
        }

        if (TextUtils.isEmpty(inputItems.get(RESOURCE))) {
            throw new IllegalArgumentException("resource");
        }

        if (TextUtils.isEmpty(inputItems.get(AUTHORITY))) {
            throw new IllegalArgumentException("authority");
        }

        if (TextUtils.isEmpty(inputItems.get(CLIENT_ID))) {
            throw new IllegalArgumentException("clientId");
        }

        if (acquireToken == flowCode && TextUtils.isEmpty(inputItems.get(REDIRECT_URI))) {
            throw new IllegalArgumentException("redirect_uri");
        }

        if (expireAccessToken == flowCode && TextUtils.isEmpty(inputItems.get(USER_IDENTIFIER))) {
            throw new IllegalArgumentException("user identifier");
        }
    }

    private void setAuthenticationData(final Map<String, String> inputItems) {
        mAuthority = inputItems.get(AUTHORITY);
        mResource = inputItems.get(RESOURCE);
        mRedirectUri = inputItems.get(REDIRECT_URI);
        mClientId = inputItems.get(CLIENT_ID);
        mUiOption = getUiOption(inputItems.get(UI_OPTION));
        mExtraQueryParam = inputItems.get(EXTRA_QUERY_PARAM);
        mValidateAuthority = inputItems.get(VALIDATE_AUTHORITY) == null ? true : Boolean.valueOf(
                inputItems.get(VALIDATE_AUTHORITY));

        if (!TextUtils.isEmpty(inputItems.get("unique_id"))) {
            mUserId = inputItems.get("unique_id");
        }

        if (!TextUtils.isEmpty(inputItems.get("displayable_id")) || !TextUtils.isEmpty(inputItems.get("user_identifier"))) {
            mLoginHint = inputItems.get("displayable_id") == null ? inputItems.get("user_identifier") : inputItems.get("displayable_id");
        }

        final String correlationId = inputItems.get(CORRELATION_ID);
        if (!TextUtils.isEmpty(correlationId)) {
            mCorrelationId = UUID.fromString(correlationId);
        }
    }

    private static void extractJsonObjects(Map<String, String> inputItems, String jsonStr)
            throws JSONException {
        final JSONObject jsonObject = new JSONObject(jsonStr);
        final Iterator<?> iterator = jsonObject.keys();

        while (iterator.hasNext()) {
            final String key = (String) iterator.next();
            inputItems.put(key, jsonObject.getString(key));
        }
    }

    UIOptions getUiOption(final String inputPromptBehaviorString) {
        if (TextUtils.isEmpty(inputPromptBehaviorString)) {
            return null;
        }

        if (inputPromptBehaviorString.equalsIgnoreCase(UIOptions.SELECT_ACCOUNT.toString())) {
            return UIOptions.SELECT_ACCOUNT;
        } else if (inputPromptBehaviorString.equalsIgnoreCase(UIOptions.FORCE_LOGIN.toString())) {
            return UIOptions.FORCE_LOGIN;
        } else if (inputPromptBehaviorString.equalsIgnoreCase(UIOptions.ACT_AS_CURRENT_USER.toString())) {
            return UIOptions.ACT_AS_CURRENT_USER;
        }

        return null;
    }

    private void sendErrorToResultActivity(final String error, final String errorDescription) {
        launchResultActivity(getErrorIntentForResultActivity(error, errorDescription));
    }

    static Intent getErrorIntentForResultActivity(final String error, final String errorDescription) {
        final Intent intent = new Intent();
        intent.putExtra(Constants.ERROR, error);
        intent.putExtra(Constants.ERROR_DESCRIPTION, errorDescription);

        return intent;
    }

    private void launchResultActivity(final Intent intent) {
        intent.putExtra(
                Constants.READ_LOGS,
                ((AndroidAutomationApp) getApplication())
                        .getMsalLogs()
        );
        intent.setClass(getApplicationContext(), ResultActivity.class);
        this.startActivity(intent);
        this.finish();
    }
}
