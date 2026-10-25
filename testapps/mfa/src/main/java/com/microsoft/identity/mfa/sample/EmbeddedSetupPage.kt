// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream

/** Activity-owned, single-page WebView. Closing it never cancels a submitted SDK operation. */
@SuppressLint("SetJavaScriptEnabled")
internal class EmbeddedSetupPage(
    activity: AppCompatActivity,
    private val location: SetupLocation,
    private val requests: LiveSetupSession,
    onSetup: (String) -> Unit,
    onClose: () -> Unit
) {
    private var pageReady = false
    private val webView = WebView(activity).apply {
        isSaveEnabled = false
        if (Build.VERSION.SDK_INT >= 26) importantForAutofill = WebView.IMPORTANT_FOR_AUTOFILL_NO
        settings.apply {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            domStorageEnabled = false
            databaseEnabled = false
            setGeolocationEnabled(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                pageReady = false
            }

            override fun onPageFinished(view: WebView, url: String?) {
                pageReady = url == location.pageUrl && view.url == location.pageUrl
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val current = requests.current()
                if (pageReady && view.url == location.pageUrl &&
                    request.isForMainFrame && request.hasGesture() && request.method == "GET" &&
                    !request.isRedirect && current?.id == location.request.id &&
                    current.source == SetupSource.EMBEDDED &&
                    request.url.toString() == current.launchUri) {
                    onSetup(request.url.toString())
                }
                return true
            }

            @Deprecated("Legacy navigation lacks the required main-frame and gesture evidence.")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = true

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val expected = if (request.isForMainFrame) location.pageUrl else location.statusUrl
                if (request.method == "GET" && request.url.toString() == expected &&
                    requests.current()?.id == location.request.id) return null
                return WebResourceResponse(
                    "text/plain", "UTF-8", 403, "Blocked", emptyMap(),
                    ByteArrayInputStream("Navigation blocked.".toByteArray(Charsets.UTF_8))
                )
            }
        }
    }
    private val dialog = AlertDialog.Builder(activity)
        .setTitle(R.string.controlled_page_title)
        .setView(webView)
        .setNegativeButton(R.string.close_setup_page) { _, _ -> }
        .create()

    init {
        dialog.setOnDismissListener {
            release()
            onClose()
        }
        dialog.show()
        dialog.window?.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.loadUrl(location.pageUrl)
    }

    fun dispose() {
        dialog.setOnDismissListener(null)
        dialog.dismiss()
        release()
    }

    private fun release() {
        pageReady = false
        webView.stopLoading()
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.destroy()
    }
}
