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
package com.microsoft.identity.client.testapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.microsoft.identity.common.internal.ui.ReturnToCallerActivity;

import java.util.UUID;

public class StartActivity extends AppCompatActivity {

    //private static final String TAG = StartActivity.class.getSimpleName();
    private static final String TAG = "StartActivity";
    private static final String AUTHENTICATOR_PACKAGE = "com.azure.authenticator";
    private static final String RETURN_PENDING_INTENT_EXTRA = "return_pending_intent";
    private static final String REQUEST_STATE_EXTRA = "request_state";

    private Button mStartTaskButton;
    private Button mTriggerVidButton;
    private EditText mVidUriEditText;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int leftInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            int rightInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;

            view.setPadding(leftInset, topInset, rightInset, bottomInset);
            return insets;
        });
        mStartTaskButton = findViewById(R.id.btnStartTask);
        mTriggerVidButton = findViewById(R.id.btnTriggerVid);
        mVidUriEditText = findViewById(R.id.etVidUri);

        mStartTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startMainIntent = new Intent(getApplicationContext(), MainActivity.class);

                /*
                https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_MULTIPLE_TASK

                Using the Multiple_Task flag skips a search for a matching activity in existing tasks and always
                creates a new task.
                 */
                startMainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                startActivity(startMainIntent);

            }
        });

        mTriggerVidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerVid();
            }
        });

    }

    /**
     * Shortcut that mimics an MSAL-consuming app starting the VID flow: it creates a one-time,
     * immutable return PendingIntent (targeting our own {@link ReturnToCallerActivity}) and hands
     * it to Authenticator alongside the openid-vc deep link. After VID completes, Authenticator
     * invokes the PendingIntent to bring this app's task back to the foreground.
     */
    private void triggerVid() {
        final String vidUri = mVidUriEditText.getText().toString().trim();
        if (vidUri.isEmpty()) {
            Toast.makeText(this, "Enter an openid-vc URI", Toast.LENGTH_SHORT).show();
            return;
        }

        final String requestState = UUID.randomUUID().toString();
        final PendingIntent returnPendingIntent = createReturnToCallerPendingIntent(requestState);

        final Intent vidIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(vidUri));
        vidIntent.setPackage(AUTHENTICATOR_PACKAGE);
        vidIntent.putExtra(RETURN_PENDING_INTENT_EXTRA, returnPendingIntent);
        vidIntent.putExtra(REQUEST_STATE_EXTRA, requestState);

        try {
            startActivity(vidIntent);
            Log.i(TAG, "Launched VID flow with request_state=" + requestState);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to launch Authenticator for VID", e);
            Toast.makeText(this, "Authenticator not available: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private PendingIntent createReturnToCallerPendingIntent(final String requestState) {
        final Intent returnIntent = new Intent(this, ReturnToCallerActivity.class);
        returnIntent.setAction("com.microsoft.identity.RETURN_FROM_VID");
        returnIntent.putExtra(REQUEST_STATE_EXTRA, requestState);
        returnIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        returnIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(
                this,
                requestState.hashCode(),
                returnIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {


        return super.onCreateView(parent, name, context, attrs);
    }
}
