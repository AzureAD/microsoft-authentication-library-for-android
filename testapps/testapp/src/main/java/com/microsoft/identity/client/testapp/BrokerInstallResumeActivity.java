// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.testapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.logging.Logger;

/**
 * POC deep-link receiver for broker-install request resume. The broker (Company Portal stand-in),
 * on first launch after install, deep-links back to {@code msauth://<pkg>/resume?resume=<cid>}.
 * This activity forwards the single-use correlation id to {@link MainActivity}, which resumes the
 * original interactive request by reading the **full** persisted request from the encrypted store
 * (see {@code EncryptedBrokerInstallResumeStore}) — no request parameters are reconstructed here.
 *
 * <p>Intentionally no-history and finishes immediately so it never becomes a dead-end.
 */
public class BrokerInstallResumeActivity extends Activity {

    private static final String TAG = BrokerInstallResumeActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri data = getIntent() != null ? getIntent().getData() : null;
        final String correlationId = data == null ? null : data.getQueryParameter("resume");

        if (correlationId == null) {
            Logger.warn(TAG, "Resume deep-link missing correlation id; nothing to resume.");
            finish();
            return;
        }

        Logger.info(TAG, "Forwarding broker-install resume for correlation id.");

        final Intent resume = new Intent(this, MainActivity.class);
        resume.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        resume.putExtra(MainActivity.EXTRA_RESUME_CORRELATION_ID, correlationId);
        startActivity(resume);
        finish();
    }
}
