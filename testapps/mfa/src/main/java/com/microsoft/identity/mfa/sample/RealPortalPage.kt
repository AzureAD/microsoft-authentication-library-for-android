// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.annotation.SuppressLint
import android.os.Build
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.identity.mfa.MfaEnrollmentHandoff
import com.microsoft.identity.mfa.MfaHandoffDeepLinkAdapter
import com.microsoft.identity.mfa.MfaHandoffDisposition
import com.microsoft.identity.mfa.MfaHandoffReceipt
import org.json.JSONObject
import java.net.URI

/** Activity-owned real portal. Native enrollment and approval overlay this WebView without reloading it. */
@SuppressLint("SetJavaScriptEnabled")
internal class RealPortalPage(
    activity: AppCompatActivity,
    handoff: MfaEnrollmentHandoff,
    onReceipt: (MfaHandoffReceipt) -> Unit,
    onReviewSignIn: () -> Unit,
    onClose: () -> Unit
) {
    private var released = false
    private val script = activity.assets.open("portal-enrollment-demo.js").bufferedReader().use { it.readText() }
    private val navigation = MfaHandoffDeepLinkAdapter(
        handoff, "mfa-poc-live://activate", activity.packageName, setOf(REGISTRATION_ORIGIN))
    private val webView = WebView(activity).apply {
        id = R.id.portalWebView
        isSaveEnabled = false
        if (Build.VERSION.SDK_INT >= 26) importantForAutofill = WebView.IMPORTANT_FOR_AUTOFILL_NO
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            allowFileAccess = false
            allowContentAccess = false
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = false
            @Suppress("DEPRECATION")
            allowUniversalAccessFromFileURLs = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setGeolocationEnabled(false)
        }
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
        }
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (released) return true
                val receipt = navigation.receive(request, view.url)
                if (receipt.disposition != MfaHandoffDisposition.IGNORED) {
                    onReceipt(receipt)
                    return true
                }
                return !isHttps(request.url.toString())
            }

            @Deprecated("No request metadata: block deep links rather than bypass gesture/frame checks.")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !isHttps(url)

            override fun onPageFinished(view: WebView, url: String?) {
                if (!released && url == view.url && isRegistrationPage(url)) {
                    // This Microsoft-specific DOM/button integration is deliberately sample-only.
                    view.evaluateJavascript(
                        "if (window.top === window && location.origin === '$REGISTRATION_ORIGIN' && " +
                            "!window.__mfaPortalDemo) { ($script)({mode:'webview'," +
                            "packageName:${JSONObject.quote(activity.packageName)}}); }",
                        null
                    )
                }
            }
        }
    }
    private val dialog = AlertDialog.Builder(activity)
        .setTitle(R.string.portal_webview_title)
        .setView(webView)
        .setNeutralButton(R.string.portal_review_sign_in, null)
        .setNegativeButton(R.string.close_setup_page) { _, _ -> }
        .create()

    init {
        try {
            dialog.setOnDismissListener {
                release()
                onClose()
            }
            dialog.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                    if (event.action == KeyEvent.ACTION_UP) webView.goBack()
                    true
                } else false
            }
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).apply {
                id = R.id.portalReviewSignIn
                setOnClickListener { onReviewSignIn() }
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).id = R.id.portalClosePage
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            webView.loadUrl("https://entra.microsoft.com")
        } catch (error: RuntimeException) {
            dispose()
            throw error
        } catch (error: com.microsoft.identity.mfa.MfaSdkException) {
            dispose()
            throw error
        }
    }

    fun dispose() {
        dialog.setOnDismissListener(null)
        dialog.dismiss()
        release()
    }

    private fun release() {
        if (released) return
        released = true
        webView.stopLoading()
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    private fun isHttps(value: String): Boolean = try {
        val uri = URI(value)
        uri.scheme == "https" && uri.host != null && uri.rawUserInfo == null && uri.port in setOf(-1, 443)
    } catch (_: Exception) { false }

    private fun isRegistrationPage(value: String?): Boolean = try {
        val uri = URI(value ?: "")
        isHttps(value.orEmpty()) && uri.host == "mysignins.microsoft.com"
    } catch (_: Exception) { false }

    private companion object {
        const val REGISTRATION_ORIGIN = "https://mysignins.microsoft.com"
    }
}
