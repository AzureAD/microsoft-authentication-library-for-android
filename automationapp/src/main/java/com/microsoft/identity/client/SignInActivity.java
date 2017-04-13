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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Handle the coming request, will gather request info (JSON format of the data contains the authority, resource, clientId,
 * redirect, ect).
 */
public class SignInActivity extends AppCompatActivity {

    public static final String AUTHORITY = "authority";
    public static final String CLIENT_ID = "client_id";
    public static final String PROMPT_BEHAVIOR = "ui_behavior";
    public static final String EXTRA_QUERY_PARAM = "extra_qp";
    public static final String VALIDATE_AUTHORITY = "validate_authority";
    public static final String LOGIN_HINT = "login_hint";
    public static final String USER_IDENTIFIER = "user_identifier";
    public static final String SCOPE = "scope";
    public static final String ADDITIONAL_SCOPE = "additional_scope";
    public static final String FORCE_REFRESH = "force_refresh";

    static final String INVALID_REFRESH_TOKEN = "some invalid refresh token";

    private TextView mTextView;
    private String mAuthority;
    private String[] mScopes;
    private String[] mAdditionalScopes;
    private String mClientId;
    private UiBehavior mUiBehavior;
    private String mLoginHint;
    private String mExtraQueryParam;
    private PublicClientApplication mPublicClientApplication;
    private boolean mValidateAuthority;
    private String mUserIdentifier;
    private boolean mForceRefresh;
    private User mUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        mTextView = (EditText) findViewById(R.id.requestInfo);
        
        final Button goButton = (Button) findViewById(R.id.requestGo);
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performAuthentication();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mTextView.setText("");
        mPublicClientApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    private void performAuthentication() {
        final Intent receivedIntent = getIntent();
        
        int flowCode = receivedIntent.getIntExtra(MainActivity.FLOW_CODE, 0);

        final Map<String, String> inputItems;
        try {
            inputItems = readAuthenticationInfo();
        } catch (final JSONException e) {
            sendErrorToResultActivity(AutomationAppConstants.JSON_ERROR, "Unable to read the input JSON info " + e.getMessage());
            return;
        }

        if (inputItems.isEmpty()) {
            return;
        }

        validateUserInput(inputItems, flowCode);

        setAuthenticationData(inputItems);

        mPublicClientApplication = new PublicClientApplication(getApplicationContext(), mClientId);
        mPublicClientApplication.setValidateAuthority(mValidateAuthority);

        if (!TextUtils.isEmpty(mUserIdentifier)) {
            try {
                mUser = mPublicClientApplication.getUser(mUserIdentifier);
            } catch (final MsalClientException e) {
                sendErrorToResultActivity(e.getErrorCode(), e.getMessage());
                return;
            }

            if (mUser == null) {
                sendErrorToResultActivity("null_user", "No user matching the user identifier exists.");
                return;
            }
        }

        switch (flowCode) {
            case MainActivity.ACQUIRE_TOKEN:
                acquireToken();
                break;
            case MainActivity.ACQUIRE_TOKEN_SILENT:
                acquireTokenSilent();
                break;
            case MainActivity.INVALIDATE_ACCESS_TOKEN:
                processExpireAccessTokenRequest();
                break;
            case MainActivity.INVALIDATE_REFRESH_TOKEN:
                processInvalidateRefreshTokenRequest();
                break;
            default:
                sendErrorToResultActivity("unknown_request", "Unknown request is received");
                break;
        }
    }

    static Intent getErrorIntentForResultActivity(final String error, final String errorDescription) {
        final Intent intent = new Intent();
        intent.putExtra(AutomationAppConstants.ERROR, error);
        intent.putExtra(AutomationAppConstants.ERROR_DESCRIPTION, errorDescription);

        return intent;
    }

    private void sendErrorToResultActivity(final String error, final String errorDescription) {
        launchResultActivity(getErrorIntentForResultActivity(error, errorDescription));
    }

    private void processExpireAccessTokenRequest() {
        final Intent intent = new Intent();
        launchResultActivity(intent);
    }

    private void processInvalidateRefreshTokenRequest() {
        final Intent intent = new Intent();
        launchResultActivity(intent);
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

    private static void extractJsonObjects(Map<String, String> inputItems, String jsonStr)
            throws JSONException {
        final JSONObject jsonObject = new JSONObject(jsonStr);
        final Iterator<?> iterator = jsonObject.keys();

        while (iterator.hasNext()) {
            final String key = (String) iterator.next();
            inputItems.put(key, jsonObject.getString(key));
        }
    }

    private void validateUserInput(final Map<String, String> inputItems, int flowCode) {
        if (inputItems.isEmpty()) {
            throw new IllegalArgumentException("No sign-in data typed in the textBox");
        }


        if (TextUtils.isEmpty(inputItems.get(AUTHORITY))) {
            throw new IllegalArgumentException("authority");
        }

        if (TextUtils.isEmpty(inputItems.get(CLIENT_ID))) {
            throw new IllegalArgumentException("clientId");
        }
    }

    private void setAuthenticationData(final Map<String, String> inputItems) {
        mAuthority = inputItems.get(AUTHORITY);

        final String scopes = inputItems.get(SCOPE);
        if (TextUtils.isEmpty(scopes)) {
            mScopes = null;
        } else {
            mScopes = scopes.toLowerCase().split(" ");
        }

        final String additionalScopes = inputItems.get(ADDITIONAL_SCOPE);
        if (TextUtils.isEmpty(additionalScopes)) {
            mAdditionalScopes = null;
        } else {
            mAdditionalScopes = additionalScopes.toLowerCase().split(" ");
        }

        mClientId = inputItems.get(CLIENT_ID);
        mUiBehavior = getUiBehavior(inputItems.get(PROMPT_BEHAVIOR));
        mExtraQueryParam = inputItems.get(EXTRA_QUERY_PARAM);
        mValidateAuthority = inputItems.get(VALIDATE_AUTHORITY) == null ? true : Boolean.valueOf(
                inputItems.get(VALIDATE_AUTHORITY));
        mLoginHint = inputItems.get(LOGIN_HINT);
        mUserIdentifier = inputItems.get(USER_IDENTIFIER);
        mForceRefresh = inputItems.get(FORCE_REFRESH) == null ? false : Boolean.valueOf(inputItems.get(FORCE_REFRESH));
    }

    UiBehavior getUiBehavior(final String inputPromptBehaviorString) {
        if (TextUtils.isEmpty(inputPromptBehaviorString)) {
            return null;
        }

        if (inputPromptBehaviorString.equalsIgnoreCase(UiBehavior.SELECT_ACCOUNT.toString())) {
            return UiBehavior.SELECT_ACCOUNT;
        } else if (inputPromptBehaviorString.equalsIgnoreCase(UiBehavior.FORCE_LOGIN.toString())) {
            return UiBehavior.FORCE_LOGIN;
        } else if (inputPromptBehaviorString.equalsIgnoreCase(UiBehavior.CONSENT.toString())) {
            return UiBehavior.CONSENT;
        }

        return null;
    }

    private void acquireToken() {
        if (mUser != null) {
            mPublicClientApplication.acquireToken(SignInActivity.this, mScopes, mUser, mUiBehavior, mExtraQueryParam, mAdditionalScopes,
                    mAuthority, getMsalCallback());
        } else {
            mPublicClientApplication.acquireToken(SignInActivity.this, mScopes, mLoginHint, mUiBehavior, mExtraQueryParam, mAdditionalScopes,
                    mAuthority, getMsalCallback());
        }
    }

    private void acquireTokenSilent() {
        mPublicClientApplication.acquireTokenSilentAsync(mScopes, mUser, mAuthority, mForceRefresh, getMsalCallback());
    }

    private void expireAccessToken() throws MsalClientException {
        // Not doing anything for now.
        return;
    }

    private AuthenticationCallback getMsalCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                final Intent intent = createIntentFromAuthenticationResult(authenticationResult);
                launchResultActivity(intent);
            }

            @Override
            public void onError(MsalException e) {
                final Intent intent = createIntentFromReturnedException(e);
                launchResultActivity(intent);
            }

            @Override
            public void onCancel() {

            }
        };
    }

    private Intent createIntentFromAuthenticationResult(final AuthenticationResult result) {
        final Intent intent = new Intent();
        intent.putExtra(AutomationAppConstants.ACCESS_TOKEN, result.getAccessToken());
        intent.putExtra(AutomationAppConstants.EXPIRES_ON, result.getExpiresOn().getTime());
        intent.putExtra(AutomationAppConstants.TENANT_ID, result.getTenantId());
        intent.putExtra(AutomationAppConstants.ID_TOKEN, result.getIdToken());
        intent.putExtra(AutomationAppConstants.UNIQUE_ID, result.getUniqueId());

        if (result.getUser() != null) {
            final User user = result.getUser();
            intent.putExtra(AutomationAppConstants.DISPLAYABLE_ID, user.getDisplayableId());
            intent.putExtra(AutomationAppConstants.NAME, user.getName());
            intent.putExtra(AutomationAppConstants.UNIQUE_USER_IDENTIFIER, user.getUserIdentifier());
            intent.putExtra(AutomationAppConstants.IDENTITY_PROVIDER, user.getIdentityProvider());
        }

        return intent;
    }

    private Intent createIntentFromReturnedException(final MsalException e) {
        final Intent intent = new Intent();

        intent.putExtra(AutomationAppConstants.ERROR, e.getErrorCode());
        intent.putExtra(AutomationAppConstants.ERROR_DESCRIPTION, e.getMessage());
        intent.putExtra(AutomationAppConstants.ERROR_CAUSE, e.getCause());

        if (e instanceof MsalServiceException) {
            final MsalServiceException serviceException = (MsalServiceException) e;
            intent.putExtra(AutomationAppConstants.ERROR_HTTP_CODE, serviceException.getHttpStatusCode());
        }

        return intent;
    }

    private void launchResultActivity(final Intent intent) {
        intent.putExtra(AutomationAppConstants.READ_LOGS, ((AndroidAutomationApp)this.getApplication()).getMsalLogs());
        intent.setClass(this.getApplicationContext(), ResultActivity.class);
        this.startActivity(intent);
        this.finish();
    }
}
