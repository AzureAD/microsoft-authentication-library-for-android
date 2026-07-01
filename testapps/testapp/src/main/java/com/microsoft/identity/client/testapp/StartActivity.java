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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.microsoft.identity.common.internal.providers.BrokerInstallResumeActivity;

public class StartActivity extends AppCompatActivity {

    //private static final String TAG = StartActivity.class.getSimpleName();
    private Button mStartTaskButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Broker-install resume: the common-provided BrokerInstallResumeActivity forwards the
        // single-use correlation id to this launcher (MainActivity is not exported). Relay it to
        // MainActivity, which performs the consumer-side resume (load store + adapt + re-call).
        if (getIntent() != null
                && getIntent().getStringExtra(BrokerInstallResumeActivity.EXTRA_RESUME_CORRELATION_ID) != null) {
            final Intent resume = new Intent(getApplicationContext(), MainActivity.class);
            resume.putExtras(getIntent());
            resume.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(resume);
            finish();
            return;
        }

        // POC DEBUG-ONLY: allow seeding a broker-install resume request via the exported launcher,
        // forwarding the seed extras to MainActivity (which is not exported). Remove before production.
        if (getIntent() != null && getIntent().getStringExtra("seed_resume_cid") != null) {
            final Intent seed = new Intent(getApplicationContext(), MainActivity.class);
            seed.putExtras(getIntent());
            seed.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(seed);
            finish();
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            int leftInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).left;
            int rightInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).right;

            view.setPadding(leftInset, topInset, rightInset, bottomInset);
            return insets;
        });
        mStartTaskButton = findViewById(R.id.btnStartTask);

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

    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {


        return super.onCreateView(parent, name, context, attrs);
    }
}
