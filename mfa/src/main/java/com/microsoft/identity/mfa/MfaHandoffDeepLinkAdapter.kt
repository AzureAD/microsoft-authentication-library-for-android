// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import android.content.Intent
import android.os.Build
import android.webkit.WebResourceRequest
import com.microsoft.identity.mfa.internal.MfaHandoffIntentContract
import com.microsoft.identity.mfa.internal.fail
import com.microsoft.identity.mfa.internal.requireMfa
import java.net.URI
import java.util.Locale

/**
 * Receives package-targeted intent-link navigations from a host-owned WebView without launching them.
 *
 * [callbackUri] is the exact callback without query/fragment; [hostPackageName] must be the actual
 * application's package. [allowedHttpsOrigins] contains 1..16 exact HTTPS origins (no paths).
 * The current page restricts host context only: it is NOT authenticated initiating-origin evidence.
 * Activation validation and explicit native confirmation remain mandatory.
 * This adapter retains neither a WebView nor an Activity and never sends results to the page.
 */
class MfaHandoffDeepLinkAdapter(
    handoff: MfaEnrollmentHandoff,
    callbackUri: String,
    private val hostPackageName: String,
    allowedHttpsOrigins: Set<String>
) {
    private val contract = MfaHandoffIntentContract(handoff, callbackUri, MfaHandoffSource.WEBVIEW)
    private val callbackScheme = URI(callbackUri).scheme
    private val allowedOrigins = allowedHttpsOrigins.map { value ->
        val uri = httpsPage(value) ?: fail(MfaErrorCode.INVALID_CONFIGURATION)
        requireMfa(value.length <= 512 && uri.rawPath.orEmpty().isEmpty() &&
            uri.rawQuery == null && uri.rawFragment == null, MfaErrorCode.INVALID_CONFIGURATION)
        origin(uri)
    }.toSet()

    init {
        requireMfa(hostPackageName.length <= 255 &&
            hostPackageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) &&
            allowedOrigins.size in 1..16, MfaErrorCode.INVALID_CONFIGURATION)
    }

    /**
     * Call from the request-based shouldOverrideUrlLoading with the WebView's current URL.
     * Consume every non-IGNORED receipt, including rejection, by returning true to WebView.
     * Requires API 24+, main frame, a user gesture, GET, no redirect and an allowed loaded HTTPS page.
     * Unknown navigation metadata (including the legacy String callback) must never be substituted.
     * Ordinary schemes are IGNORED; intent/callback-scheme failures are REJECTED without staging.
     */
    fun receive(request: WebResourceRequest, currentPageUrl: String?): MfaHandoffReceipt {
        return try {
            val scheme = request.url.scheme
            if (!scheme.equals("intent", ignoreCase = true) &&
                !scheme.equals(callbackScheme, ignoreCase = true)) {
                return MfaHandoffReceipt(MfaHandoffDisposition.IGNORED)
            }
            val value = request.url.toString()
            if (Build.VERSION.SDK_INT < 24 || !value.startsWith("intent:") ||
                value.length > MAX_NAVIGATION_BYTES ||
                value.toByteArray(Charsets.UTF_8).size > MAX_NAVIGATION_BYTES ||
                value.any { it.isISOControl() || it.isWhitespace() } ||
                !request.isForMainFrame || !request.hasGesture() || request.method != "GET" ||
                request.isRedirect || !isAllowedPage(currentPageUrl)) return rejected()
            // Android can discard a selector when a package is also present. Reject its wire form too.
            if (value.contains(";SEL;") || value.contains(";component=")) return rejected()
            val intent = Intent.parseUri(value, Intent.URI_INTENT_SCHEME)
            if (intent.type != null || !intent.categories.isNullOrEmpty() || intent.flags != 0 ||
                intent.sourceBounds != null ||
                intent.extras?.keySet() != setOf(MfaHandoffIntentAdapter.EXTRA_ACTIVATION_LINK)) {
                return rejected()
            }
            @Suppress("DEPRECATION")
            receiveDecoded(intent.action, intent.selector != null, intent.component != null,
                intent.`package`, intent.dataString, intent.extras?.get(MfaHandoffIntentAdapter.EXTRA_ACTIVATION_LINK),
                currentPageUrl, request.isForMainFrame, request.hasGesture(), request.method, request.isRedirect)
        } catch (_: Exception) {
            rejected()
        }
    }

    internal fun receiveDecoded(
        action: String?, hasSelector: Boolean, hasComponent: Boolean, packageName: String?,
        uri: String?, payload: Any?, currentPageUrl: String?, isMainFrame: Boolean,
        hasGesture: Boolean, method: String?, isRedirect: Boolean
    ): MfaHandoffReceipt {
        if (!isMainFrame || !hasGesture || method != "GET" || isRedirect ||
            !isAllowedPage(currentPageUrl) || packageName != hostPackageName || hasComponent) return rejected()
        val receipt = contract.receive(action, hasSelector, uri, payload)
        return if (receipt.disposition == MfaHandoffDisposition.IGNORED) rejected() else receipt
    }

    private fun isAllowedPage(value: String?): Boolean =
        httpsPage(value)?.let { origin(it) in allowedOrigins } == true

    private fun httpsPage(value: String?): URI? = try {
        value?.takeIf { it.length <= 8192 && !it.contains("\\") && !it.contains("*") }
            ?.let(::URI)?.takeIf {
                it.scheme == "https" && it.host != null && !it.host.endsWith(".") &&
                    it.rawUserInfo == null && it.port in setOf(-1, 443) && !it.isOpaque
            }
    } catch (_: Exception) { null }

    private fun origin(uri: URI) = "https://${uri.host.lowercase(Locale.ROOT)}"

    private fun rejected() =
        MfaHandoffReceipt(MfaHandoffDisposition.REJECTED, errorCode = MfaErrorCode.INVALID_ACTIVATION)

    private companion object {
        const val MAX_NAVIGATION_BYTES = MfaEnrollmentHandoff.MAX_LINK_BYTES * 3 + 1024
    }
}
