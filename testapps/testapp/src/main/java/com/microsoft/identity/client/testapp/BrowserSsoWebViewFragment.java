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
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.EditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Fragment for E2E testing of Browser SSO via a WebView.
 *
 * <p>Flow:
 * <ol>
 *   <li>User enters a URL (e.g. https://www.office.com) and taps Go</li>
 *   <li>WebView loads the page normally, following redirects</li>
 *   <li>When WebView navigates to {@code login.microsoftonline.com}, the navigation is
 *       intercepted via {@code shouldOverrideUrlLoading}</li>
 *   <li>The fragment calls {@link AccountManager#getAuthToken} asynchronously to fetch
 *       SSO headers from the broker</li>
 *   <li>On callback, the URL is reloaded via {@code WebView.loadUrl(url, headers)} with
 *       the SSO headers injected</li>
 *   <li>If eSTS accepts the PRT cookie, the user is signed in without a password prompt</li>
 * </ol>
 *
 * <p>This uses WebView's native request handling so cookies, redirects, and JavaScript
 * all work correctly. The broker's debug allow-list must include this test app's package
 * ({@code com.msft.identity.client.sample.local}).
 */
public class BrowserSsoWebViewFragment extends Fragment {

    private static final String TAG = BrowserSsoWebViewFragment.class.getSimpleName();

    private static final String ACCOUNT_TYPE_ENTRA = "com.microsoft.entra";
    private static final String AUTH_TOKEN_TYPE_SSO_HEADER = "sso_header";
    private static final String KEY_URL = "url";
    private static final String KEY_SSO_HEADER_RESULT = "sso_header_result";
    private static final String SSO_HOST = "login.microsoftonline.com";

    private EditText mEditUrl;
    private Button mBtnGo;
    private ProgressBar mProgressBar;
    private TextView mTxtStatus;
    private WebView mWebView;
    private TextView mTxtLog;
    private ScrollView mScrollLog;
    private Button mBtnToggleLog;
    private Button mBtnClearLog;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Gson mGson = new Gson();

    /**
     * Tracks URLs that have already been reloaded with SSO headers so we don't
     * intercept the same navigation twice (which would cause an infinite loop).
     */
    private String mUrlLoadedWithSsoHeaders = null;

    public BrowserSsoWebViewFragment() {}

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_browser_sso_webview, container, false);

        mEditUrl = view.findViewById(R.id.edit_text_url);
        mBtnGo = view.findViewById(R.id.button_go);
        mProgressBar = view.findViewById(R.id.progress_bar);
        mTxtStatus = view.findViewById(R.id.text_status);
        mWebView = view.findViewById(R.id.webview_browser_sso);
        mTxtLog = view.findViewById(R.id.text_log);
        mScrollLog = view.findViewById(R.id.scroll_log);
        mBtnToggleLog = view.findViewById(R.id.button_toggle_log);
        mBtnClearLog = view.findViewById(R.id.button_clear_log);

        // WebView configuration
        final WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        // Enable cookies (needed for SSO to persist across page loads)
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);

        mWebView.setWebViewClient(new SsoWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());

        // Button listeners
        mBtnGo.setOnClickListener(v -> onGoClicked());
        mBtnToggleLog.setOnClickListener(v -> toggleLogPanel());
        mBtnClearLog.setOnClickListener(v -> {
            mTxtLog.setText("");
            appendLog("Log cleared.");
        });

        // Allow "Go" from keyboard
        mEditUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                onGoClicked();
                return true;
            }
            return false;
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        if (mWebView != null) {
            mWebView.stopLoading();
            mWebView.destroy();
        }
        super.onDestroyView();
    }

    private void onGoClicked() {
        String url = getText(mEditUrl);
        if (url.isEmpty()) {
            setStatus("Enter a URL.");
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
            mEditUrl.setText(url);
        }
        mUrlLoadedWithSsoHeaders = null;
        appendLog("Navigating to: " + url);
        mWebView.loadUrl(url);
    }

    private void toggleLogPanel() {
        if (mScrollLog.getVisibility() == View.VISIBLE) {
            mScrollLog.setVisibility(View.GONE);
            mBtnToggleLog.setText("Log");
        } else {
            mScrollLog.setVisibility(View.VISIBLE);
            mBtnToggleLog.setText("Hide");
            mScrollLog.post(() -> mScrollLog.fullScroll(View.FOCUS_DOWN));
        }
    }

    // ── WebViewClient ──────────────────────────────────────────────────

    private class SsoWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mProgressBar.setVisibility(View.VISIBLE);
            setStatus("Loading…");
        }

        @Override
        public void onPageFinished(final WebView view, final String url) {
            super.onPageFinished(view, url);
            mProgressBar.setVisibility(View.GONE);
            mEditUrl.setText(url);
            setStatus("Loaded");
        }

        /**
         * Intercepts navigations (not sub-resource loads). When the navigation target
         * is login.microsoftonline.com and we haven't already injected headers for this
         * URL, we cancel the navigation, fetch SSO headers asynchronously, and re-load
         * with headers.
         */
        @Override
        public boolean shouldOverrideUrlLoading(final WebView view,
                                                final android.webkit.WebResourceRequest request) {
            final Uri uri = request.getUrl();
            final String host = uri != null ? uri.getHost() : null;
            final String url = uri != null ? uri.toString() : "";

            if (!SSO_HOST.equalsIgnoreCase(host)) {
                return false; // Let WebView handle it.
            }

            // If we already reloaded this exact URL with SSO headers, don't intercept again.
            if (url.equals(mUrlLoadedWithSsoHeaders)) {
                mUrlLoadedWithSsoHeaders = null; // Reset for future navigations.
                appendLog("Proceeding with SSO headers for: " + truncateUrl(url));
                return false;
            }

            appendLog("Intercepted navigation to: " + truncateUrl(url));

            // Log correlation / client-request-id if present in query params
            final String clientRequestId = uri.getQueryParameter("client-request-id");
            final String correlationId = uri.getQueryParameter("correlation_id");
            if (clientRequestId != null) {
                appendLog("  client-request-id: " + clientRequestId);
            }
            if (correlationId != null) {
                appendLog("  correlation_id: " + correlationId);
            }

            setStatus("Fetching SSO headers…");
            mProgressBar.setVisibility(View.VISIBLE);

            fetchSsoHeadersAndReload(url);
            return true; // Cancel the current navigation — we'll reload with headers.
        }
    }

    // ── SSO header fetching ─────────────────────────────────────────────

    /**
     * Calls AccountManager.getAuthToken asynchronously, then reloads the URL
     * with SSO headers injected via {@code WebView.loadUrl(url, headers)}.
     */
    private void fetchSsoHeadersAndReload(@NonNull final String url) {
        final AccountManager am = AccountManager.get(requireContext());
        final Account account = new Account("placeholder", ACCOUNT_TYPE_ENTRA);

        final Bundle options = new Bundle();
        options.putString(KEY_URL, url);

        appendLog("Calling AccountManager.getAuthToken…");

        am.getAuthToken(
                account,
                AUTH_TOKEN_TYPE_SSO_HEADER,
                options,
                false,
                new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(final AccountManagerFuture<Bundle> future) {
                        mMainHandler.post(() -> onSsoHeaderResult(future, url));
                    }
                },
                mMainHandler
        );
    }

    private void onSsoHeaderResult(@NonNull final AccountManagerFuture<Bundle> future,
                                   @NonNull final String originalUrl) {
        mProgressBar.setVisibility(View.GONE);

        try {
            final Bundle result = future.getResult();

            if (result == null) {
                appendLog("Broker returned null — loading without SSO.");
                mWebView.loadUrl(originalUrl);
                return;
            }

            // Check for error
            final String errorCode = result.getString("error_code");
            if (errorCode != null) {
                appendLog("Broker error: " + errorCode
                        + " — " + result.getString("error_message"));
                setStatus("SSO error: " + errorCode);
                // Load anyway without SSO so the user can still see the login page.
                mWebView.loadUrl(originalUrl);
                return;
            }

            // Parse SSO headers
            final Map<String, String> ssoHeaders = extractSsoHeaders(result);

            if (ssoHeaders == null || ssoHeaders.isEmpty()) {
                appendLog("No SSO headers returned — loading without SSO.");
                mWebView.loadUrl(originalUrl);
                return;
            }

            appendLog("Injecting " + ssoHeaders.size() + " SSO header(s):");
            for (final Map.Entry<String, String> entry : ssoHeaders.entrySet()) {
                appendLog("  " + entry.getKey() + ": " + truncateValue(entry.getValue()));
            }

            // Mark this URL so we don't intercept the reload.
            mUrlLoadedWithSsoHeaders = originalUrl;
            setStatus("Loading with SSO headers…");

            // Merge SSO headers into the extra-headers map and reload.
            final Map<String, String> allHeaders = new HashMap<>(ssoHeaders);
            mWebView.loadUrl(originalUrl, allHeaders);

        } catch (final Exception e) {
            appendLog("Exception: " + e.getClass().getSimpleName() + " — " + e.getMessage());
            setStatus("Error fetching SSO headers");
            mWebView.loadUrl(originalUrl);
        }
    }

    /**
     * Extracts the SSO header map from the broker's response bundle.
     * Tries {@code sso_header_result} (JSON) first, then falls back to {@code authtoken}.
     */
    @Nullable
    private Map<String, String> extractSsoHeaders(@NonNull final Bundle result) {
        String json = result.getString(KEY_SSO_HEADER_RESULT);
        if (json == null) {
            json = result.getString(AccountManager.KEY_AUTHTOKEN);
        }
        if (json == null) {
            appendLog("Unexpected response keys: " + result.keySet());
            return null;
        }
        return parseHeaderResultJson(json);
    }

    @Nullable
    private Map<String, String> parseHeaderResultJson(@NonNull final String json) {
        try {
            final Map<String, Object> envelope = mGson.fromJson(json,
                    new TypeToken<Map<String, Object>>() {}.getType());
            if (envelope == null) return null;

            final Object headersObj = envelope.get("headers");
            if (headersObj instanceof Map) {
                @SuppressWarnings("unchecked")
                final Map<String, String> headers = (Map<String, String>) headersObj;
                return headers;
            }
            return null;
        } catch (final Exception e) {
            appendLog("JSON parse error: " + e.getMessage());
            return null;
        }
    }

    // ── UI helpers ──────────────────────────────────────────────────────

    private void setStatus(@NonNull final String message) {
        mTxtStatus.setText(message);
    }

    private void appendLog(@NonNull final String message) {
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String ts = sdf.format(new Date());
        mTxtLog.append("[" + ts + " UTC] " + message + "\n");
        mScrollLog.post(() -> mScrollLog.fullScroll(View.FOCUS_DOWN));
    }

    @NonNull
    private static String getText(@Nullable final EditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    @NonNull
    private static String truncateUrl(@NonNull final String url) {
        return url.length() > 80 ? url.substring(0, 80) + "…" : url;
    }

    @NonNull
    private static String truncateValue(@NonNull final String value) {
        return value.length() > 50
                ? value.substring(0, 50) + "…(" + value.length() + " chars)"
                : value;
    }
}
