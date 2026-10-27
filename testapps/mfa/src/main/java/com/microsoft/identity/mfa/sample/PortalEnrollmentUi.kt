// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.identity.mfa.MfaEnrollmentHandoff
import com.microsoft.identity.mfa.MfaHandoffPhase
import com.microsoft.identity.mfa.MfaHandoffState

/** Customer-owned UX only. Hiding it never cancels submitted coordinator work or its WebView. */
internal class PortalEnrollmentUi(
    private val activity: AppCompatActivity,
    private val handoff: MfaEnrollmentHandoff,
    private val ready: () -> Boolean,
    private val confirm: (String) -> Unit
) {
    private var dialog: AlertDialog? = null
    private var text: TextView? = null
    private var shownId: String? = null
    private var hiddenId: String? = null

    fun render(state: MfaHandoffState) {
        val id = state.requestId
        if (id == null || state.phase in setOf(MfaHandoffPhase.IDLE, MfaHandoffPhase.DISPOSED)) {
            dispose()
            return
        }
        if (hiddenId == id) return
        if (shownId != id) {
            dispose()
            shownId = id
            text = TextView(activity).apply {
                this.id = R.id.portalEnrollmentStatus
                setPadding(32, 24, 32, 24)
                isSaveEnabled = false
                accessibilityLiveRegion = android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
            dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.portal_confirmation_title)
                .setView(text)
                .setPositiveButton(R.string.continue_setup, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create().also { ui ->
                    ui.setOnCancelListener {
                        hiddenId = id
                        handoff.cancel(id)
                        dispose()
                    }
                    ui.setCanceledOnTouchOutside(false)
                    ui.show()
                    ui.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    ui.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                        this.id = R.id.portalConfirm
                        setOnClickListener {
                            val current = handoff.state.value
                            if (current.requestId != id) return@setOnClickListener
                            if (current.phase == MfaHandoffPhase.AWAITING_CONFIRMATION) confirm(id)
                            else if (current.phase != MfaHandoffPhase.SUBMITTING) handoff.acknowledge(id)
                        }
                    }
                    ui.getButton(AlertDialog.BUTTON_NEGATIVE).apply {
                        this.id = R.id.portalCancelOrHide
                        setOnClickListener {
                            if (handoff.state.value.phase == MfaHandoffPhase.AWAITING_CONFIRMATION) {
                                handoff.cancel(id)
                            } else {
                                hiddenId = id
                                dispose()
                            }
                        }
                    }
                }
        }
        val compactStatus = state.phase == MfaHandoffPhase.SUBMITTING ||
            state.phase == MfaHandoffPhase.SUCCEEDED
        dialog?.setTitle(if (compactStatus) statusText(state)
            else activity.getString(R.string.portal_confirmation_title))
        text?.apply {
            visibility = if (compactStatus) View.GONE else View.VISIBLE
            text = if (state.phase == MfaHandoffPhase.AWAITING_CONFIRMATION) {
                activity.getString(R.string.portal_confirmation_body)
            } else statusText(state)
        }
        updateButtons()
    }

    fun updateButtons() {
        val state = handoff.state.value
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
            setText(if (state.phase == MfaHandoffPhase.AWAITING_CONFIRMATION) R.string.continue_setup
                else android.R.string.ok)
            isEnabled = when (state.phase) {
                MfaHandoffPhase.AWAITING_CONFIRMATION -> ready()
                MfaHandoffPhase.SUBMITTING -> false
                else -> true
            }
        }
        dialog?.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
            setText(if (state.phase == MfaHandoffPhase.AWAITING_CONFIRMATION) android.R.string.cancel
                else R.string.portal_hide_status)
            visibility = if (state.phase == MfaHandoffPhase.SUCCEEDED) View.GONE else View.VISIBLE
        }
    }

    fun show() {
        hiddenId = null
        render(handoff.state.value)
    }

    fun statusText(state: MfaHandoffState): String = activity.getString(when (state.phase) {
        MfaHandoffPhase.IDLE -> R.string.portal_idle
        MfaHandoffPhase.AWAITING_CONFIRMATION -> R.string.portal_awaiting
        MfaHandoffPhase.SUBMITTING -> R.string.portal_submitting
        MfaHandoffPhase.SUCCEEDED -> R.string.portal_succeeded
        MfaHandoffPhase.FAILED -> R.string.portal_failed
        MfaHandoffPhase.CANCELLED -> R.string.portal_cancelled
        MfaHandoffPhase.EXPIRED -> R.string.portal_expired
        MfaHandoffPhase.OUTCOME_UNKNOWN -> R.string.portal_unknown
        MfaHandoffPhase.DISPOSED -> R.string.portal_disposed
    }) + (state.errorCode?.let { "\n${it.name}" } ?: "")

    fun dispose() {
        dialog?.setOnCancelListener(null)
        dialog?.dismiss()
        dialog = null
        text = null
        shownId = null
    }
}
