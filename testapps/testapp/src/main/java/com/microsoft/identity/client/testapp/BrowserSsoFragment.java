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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment for E2E testing of the Browser SSO flow.
 *
 * <p>Simulates a browser calling {@link AccountManager#getAuthToken} with:
 * <ul>
 *   <li>accountType = "com.microsoft.entra"</li>
 *   <li>authTokenType = "sso_header"</li>
 * </ul>
 *
 * <p>Per the design spec, callers pass a hardcoded placeholder {@link Account} — the
 * broker ignores the account name and always returns SSO headers for all signed-in
 * accounts on the device. No account enumeration is needed.
 *
 * <p>The broker's debug allow-list must include this test app's package name
 * ({@code com.msft.identity.client.sample.local}) for the request to succeed.
 */
public class BrowserSsoFragment extends Fragment {

    private static final String TAG = BrowserSsoFragment.class.getSimpleName();

    /** Account type registered by the broker for Entra accounts. */
    private static final String ACCOUNT_TYPE_ENTRA = "com.microsoft.entra";

    /** Auth token type that triggers the Browser SSO flow. */
    private static final String AUTH_TOKEN_TYPE_SSO_HEADER = "sso_header";

    /** Bundle key for the SSO URL. */
    private static final String KEY_SSO_URL = "url";

    /** Bundle key for the optional correlation ID. */
    private static final String KEY_CORRELATION_ID = "correlation_id";

    // UI elements
    private TextInputEditText mEditSsoUrl;
    private TextInputEditText mEditCorrelationId;
    private Button mBtnGetSsoHeaders;
    private ProgressBar mProgressBar;
    private TextView mTxtStatus;
    private TextView mTxtResult;
    private Button mBtnCopyResult;

    public BrowserSsoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_browser_sso, container, false);

        mEditSsoUrl = view.findViewById(R.id.edit_sso_url);
        mEditCorrelationId = view.findViewById(R.id.edit_correlation_id);
        mBtnGetSsoHeaders = view.findViewById(R.id.btn_get_sso_headers);
        mProgressBar = view.findViewById(R.id.progress_bar);
        mTxtStatus = view.findViewById(R.id.txt_status);
        mTxtResult = view.findViewById(R.id.txt_result);
        mBtnCopyResult = view.findViewById(R.id.btn_copy_result);

        mBtnGetSsoHeaders.setOnClickListener(v -> onGetSsoHeadersClicked());
        mBtnCopyResult.setOnClickListener(v -> copyResultToClipboard());

        return view;
    }

    /**
     * Triggered when the "Get SSO Headers" button is clicked.
     *
     * <p>Per the design spec, a hardcoded placeholder Account is passed to
     * {@link AccountManager#getAuthToken}. The broker ignores the account name
     * and returns SSO headers for all accounts on the device.
     */
    private void onGetSsoHeadersClicked() {
        final String ssoUrl = getText(mEditSsoUrl);
        final String correlationId = getText(mEditCorrelationId);

        if (ssoUrl.isEmpty()) {
            setStatus("SSO URL is required.");
            return;
        }

        // Build the options bundle.
        final Bundle options = new Bundle();
        options.putString(KEY_SSO_URL, ssoUrl);
        if (!correlationId.isEmpty()) {
            options.putString(KEY_CORRELATION_ID, correlationId);
        }

        // Per the design spec, callers instantiate a placeholder Account with a
        // dummy name. The broker doesn't care about the account name — it always
        // returns headers for all signed-in accounts on the device.
        final Account account = new Account("placeholder", ACCOUNT_TYPE_ENTRA);

        setStatus("Requesting SSO headers …");
        mProgressBar.setVisibility(View.VISIBLE);
        mBtnGetSsoHeaders.setEnabled(false);

        // Call getAuthToken — this is exactly what a real browser would do.
        AccountManager.get(requireContext()).getAuthToken(
                account,
                AUTH_TOKEN_TYPE_SSO_HEADER,
                options,
                false, // notifyAuthFailure
                new SsoHeaderCallback(),
                new Handler(Looper.getMainLooper())
        );
    }

    /**
     * Callback that receives the result from the broker's
     * {@code EntraAccountAuthenticator.getAuthToken()}.
     */
    private class SsoHeaderCallback implements AccountManagerCallback<Bundle> {
        @Override
        public void run(final AccountManagerFuture<Bundle> future) {
            // Always dispatch UI updates to main thread.
            requireActivity().runOnUiThread(() -> {
                mProgressBar.setVisibility(View.GONE);
                mBtnGetSsoHeaders.setEnabled(true);

                try {
                    final Bundle result = future.getResult();
                    displayResult(result);
                } catch (final Exception e) {
                    final String error = "Exception: " + e.getClass().getSimpleName()
                            + "\n" + e.getMessage();
                    setStatus(error);
                    mTxtResult.setText(error);
                }
            });
        }
    }

    /**
     * Formats and displays the result bundle returned by the broker.
     */
    private void displayResult(@NonNull final Bundle result) {
        final StringBuilder sb = new StringBuilder();
        final String timestamp = new SimpleDateFormat(
                "HH:mm:ss.SSS", Locale.US).format(new Date());
        sb.append("── Response at ").append(timestamp).append(" ──\n\n");

        // Iterate over all keys in the bundle for full visibility.
        for (final String key : result.keySet()) {
            final Object value = result.get(key);
            sb.append(key).append(" = ").append(value).append("\n\n");
        }

        final String output = sb.toString().trim();
        mTxtResult.setText(output);

        // Check for error vs success.
        if (result.containsKey("error_code")) {
            setStatus("❌ Error: " + result.getString("error_code")
                    + " — " + result.getString("error_message"));
        } else if (result.containsKey("authtoken")) {
            setStatus("✅ SSO headers received successfully.");
        } else if (result.containsKey("sso_header_result")) {
            setStatus("✅ SSO header result received.");
        } else {
            setStatus("⚠️ Response received — check result for details.");
        }
    }

    private void setStatus(@NonNull final String message) {
        mTxtStatus.setText(message);
    }

    @NonNull
    private static String getText(@Nullable final TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private void copyResultToClipboard() {
        final CharSequence text = mTxtResult.getText();
        if (text == null || text.length() == 0) {
            return;
        }
        final ClipboardManager clipboard = (ClipboardManager)
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Browser SSO Result", text));
            Toast.makeText(requireContext(), "Result copied", Toast.LENGTH_SHORT).show();
        }
    }
}
