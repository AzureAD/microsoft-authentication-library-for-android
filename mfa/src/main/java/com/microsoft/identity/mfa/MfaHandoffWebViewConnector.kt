// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import android.os.Looper
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.microsoft.identity.mfa.internal.MfaWebMessageReceiver
import com.microsoft.identity.mfa.internal.requireMfa

/**
 * Host-owned, nonvisual AndroidX WebKit listener. No navigation, WebView creation or site-specific JS.
 * [allowedHttpsOrigins] must be explicit HTTPS origins, without paths, wildcards or nonstandard ports.
 * Call [attach] before navigation and [close] before destroying the host's WebView, on its UI thread.
 * Detaching never cancels the app-owned handoff. There is no unsafe JavaScript-interface fallback.
 * Messages are strings: {"version":1,"type":"activation","activationLink":"<HTTPS link>"}.
 * Source validation uses WebKit's sourceOrigin and isMainFrame, never message-provided claims.
 */
class MfaHandoffWebViewConnector(
    handoff: MfaEnrollmentHandoff,
    allowedHttpsOrigins: Set<String>,
    private val onReceipt: (MfaHandoffReceipt) -> Unit = {}
) : AutoCloseable {
    private val receiver = MfaWebMessageReceiver(handoff, allowedHttpsOrigins)
    private var attached: WebView? = null

    /** Attaches once; throws WEBVIEW_UNSUPPORTED instead of weakening origin checks. */
    @MainThread
    fun attach(webView: WebView) {
        checkThread()
        requireMfa(attached == null, MfaErrorCode.INVALID_CONFIGURATION)
        requireMfa(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER),
            MfaErrorCode.WEBVIEW_UNSUPPORTED)
        WebViewCompat.addWebMessageListener(webView, JAVASCRIPT_OBJECT, receiver.allowedOrigins) {
                _, message, sourceOrigin, isMainFrame, _ ->
            if (attached === webView) {
                val data = if (message.type == WebMessageCompat.TYPE_STRING) message.data else null
                onReceipt(receiver.receive(data, sourceOrigin.toString(), isMainFrame))
            }
        }
        attached = webView
    }

    /** Removes only this connector's listener and releases its WebView reference. */
    @MainThread
    override fun close() {
        checkThread()
        attached?.let {
            WebViewCompat.removeWebMessageListener(it, JAVASCRIPT_OBJECT)
            attached = null
        }
    }

    private fun checkThread() {
        requireMfa(Looper.myLooper() == Looper.getMainLooper(), MfaErrorCode.INVALID_CONFIGURATION)
    }

    /** Stable JavaScript message endpoint. */
    companion object {
        /** Exposed only by WebKit to the configured origins. Call postMessage with the JSON string. */
        const val JAVASCRIPT_OBJECT = "mfaEnrollment"
    }
}
