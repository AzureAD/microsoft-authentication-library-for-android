// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.microsoft.identity.mfa.MfaAuthSession
import com.microsoft.identity.mfa.MfaApprovalBlockReason
import com.microsoft.identity.mfa.MfaRegistrationParameters
import com.microsoft.identity.mfa.MfaHandoffDisposition
import com.microsoft.identity.mfa.MfaSdkException
import com.microsoft.identity.mfa.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Single-screen, foreground-only enrollment and explicit user decision sample. */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var model: MfaSampleViewModel
    private val decisionButtons = mutableListOf<Pair<Button, Boolean>>()
    private var lastSessions: List<MfaAuthSession>? = null
    private var embeddedPage: EmbeddedSetupPage? = null
    private var confirmation: AlertDialog? = null
    private var confirmationId: String? = null
    private var portalPage: RealPortalPage? = null
    private lateinit var portalUi: PortalEnrollmentUi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom
            )
            insets
        }
        model = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MfaSampleViewModel::class.java]
        portalUi = PortalEnrollmentUi(this, model.portalHandoff,
            model::portalRegistrationReady, model::confirmPortal)
        binding.modeLabel.text = model.environment.modeLabel
        binding.registerQr.setOnClickListener {
            val input = MfaRegistrationParameters.QrCode(binding.qrInput.text.toString())
            clearActivationInputs()
            model.register(input)
        }
        binding.registerManual.setOnClickListener {
            val input = MfaRegistrationParameters.Activation(
                binding.codeInput.text.toString(), binding.urlInput.text.toString()
            )
            clearActivationInputs()
            model.register(input)
        }
        binding.openEmbeddedSetup.setOnClickListener { prepareSetup(SetupSource.EMBEDDED) }
        binding.openBrowserSetup.setOnClickListener { prepareSetup(SetupSource.BROWSER) }
        binding.openEntraWebView.setOnClickListener { openPortal() }
        binding.showPortalStatus.setOnClickListener { portalUi.show() }
        binding.returnSetup.setOnClickListener {
            model.handoff.location.value?.let(::openSetup)
        }
        binding.clearSetup.setOnClickListener {
            closeEmbeddedPage()
            model.handoff.clear()
        }
        binding.refresh.setOnClickListener { model.refresh() }
        binding.retrySetup.setOnClickListener { model.retrySetup() }
        binding.requestPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
        observeState()
        consumeIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::model.isInitialized) {
            showPermissionStatus()
            if (model.embeddedPageOpen && embeddedPage == null) {
                model.handoff.location.value?.let(::openSetup)
            }
        }
    }

    override fun onStop() {
        clearActivationInputs()
        clearNumberInputs(binding.challenges)
        super.onStop()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.status.collect { status ->
                        binding.sdkStatus.text = "Enrollment: ${status.enrollmentStage.name}\n" +
                            "Token ready: ${status.tokenReady}"
                        binding.rotationStatus.visibility =
                            if (status.tokenChangeBlocked) View.VISIBLE else View.GONE
                        updateButtons()
                    }
                }
                launch {
                    model.environment.setup.collect { setup ->
                        binding.setupStatus.text = if (setup == SampleSetup.FIREBASE_SETUP_REQUIRED)
                            getString(R.string.setup_required) else "Push setup: ${setup.name}"
                        updateButtons()
                    }
                }
                launch { model.notice.collect { binding.notice.text = it } }
                launch {
                    model.accounts.collect { accounts ->
                        binding.accounts.text = if (accounts.isEmpty()) getString(R.string.no_accounts)
                        else accounts.joinToString("\n") { it.accountName.take(160) }
                    }
                }
                launch { model.sessions.collect { renderSessions(it) } }
                launch { model.busy.collect { updateButtons() } }
                launch {
                    model.pageToOpen.collect { location ->
                        if (location != null) {
                            model.pageOpened()
                            openSetup(location)
                        }
                    }
                }
                launch {
                    model.handoff.requests.state.collect { request ->
                        binding.pageStatus.text = request?.phase?.message ?: getString(R.string.no_setup_page)
                        renderConfirmation(request)
                        if (request == null) closeEmbeddedPage()
                        updateButtons()
                    }
                }
                launch { model.handoff.location.collect { updateButtons() } }
                launch {
                    model.portalHandoff.state.collect {
                        binding.portalStatus.text = portalUi.statusText(it)
                        portalUi.render(it)
                        updateButtons()
                    }
                }
            }
        }
    }

    private fun updateButtons() {
        val available = !model.busy.value
        binding.registerQr.isEnabled = model.registrationReady() && model.handoff.requests.canPrepare()
        binding.registerManual.isEnabled = binding.registerQr.isEnabled
        binding.openEmbeddedSetup.isEnabled = model.pageRegistrationReady() && model.handoff.requests.canPrepare()
        binding.openBrowserSetup.isEnabled = binding.openEmbeddedSetup.isEnabled
        binding.returnSetup.isEnabled = model.handoff.location.value != null
        binding.clearSetup.isEnabled = model.handoff.location.value != null
        confirmation?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = model.pageRegistrationReady()
        portalUi.updateButtons()
        binding.openEntraWebView.isEnabled = portalPage == null && embeddedPage == null
        binding.showPortalStatus.isEnabled = model.portalHandoff.state.value.requestId != null
        binding.refresh.isEnabled = available
        decisionButtons.forEach { (button, supported) -> button.isEnabled = available && supported }
    }

    private fun renderSessions(sessions: List<MfaAuthSession>) {
        if (lastSessions == sessions) return
        lastSessions = sessions
        binding.challenges.removeAllViews()
        decisionButtons.clear()
        if (sessions.isEmpty()) {
            binding.challenges.addView(TextView(this).apply { setText(R.string.no_sessions) })
        }
        sessions.forEach { session ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 16, 0, 16)
                isSaveEnabled = false
            }
            row.addView(TextView(this).apply {
                text = "${session.accountName.take(160)}\n${session.username.take(160)}"
                isSaveEnabled = false
            })
            if (!session.canApprove) {
                row.addView(TextView(this).apply {
                    text = getString(R.string.approval_blocked,
                        session.approvalBlockedReasons.joinToString("\n") { getString(blockReason(it)) })
                })
            }
            val number = EditText(this).apply {
                setHint(R.string.number_hint)
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                filters = arrayOf(InputFilter.LengthFilter(8))
                isSaveEnabled = false
                isEnabled = session.canApprove
                if (Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                visibility = if (session.requiresNumberMatching) View.VISIBLE else View.GONE
            }
            row.addView(number)
            if (!session.requiresNumberMatching) {
                row.addView(TextView(this).apply { setText(R.string.number_not_required) })
            }
            listOf(true, false).forEach { approve ->
                val button = Button(this).apply {
                    setText(if (approve) R.string.approve else R.string.deny)
                    setOnClickListener {
                        val answer = if (approve) number.text.toString() else ""
                        number.text.clear()
                        model.decide(session, approve, answer)
                    }
                }
                decisionButtons.add(button to (!approve || session.canApprove))
                row.addView(button)
            }
            binding.challenges.addView(row)
        }
        updateButtons()
    }

    private fun blockReason(reason: MfaApprovalBlockReason): Int = when (reason) {
        MfaApprovalBlockReason.UNSUPPORTED_MODE -> R.string.policy_mode
        MfaApprovalBlockReason.MFA_DISABLED -> R.string.policy_disabled
        MfaApprovalBlockReason.APP_LOCK_REQUIRED -> R.string.policy_app_lock
        MfaApprovalBlockReason.APPROVAL_PIN_REQUIRED -> R.string.policy_approval_pin
        MfaApprovalBlockReason.PIN_CHANGE_REQUIRED -> R.string.policy_pin_change
        MfaApprovalBlockReason.APP_CONTEXT_REQUIRED -> R.string.policy_app_context
        MfaApprovalBlockReason.LOCATION_CONTEXT_REQUIRED -> R.string.policy_location_context
        MfaApprovalBlockReason.LOCATION_REQUIRED -> R.string.policy_location
        MfaApprovalBlockReason.SILENT_REQUEST -> R.string.policy_silent
        MfaApprovalBlockReason.NUMBER_MATCHING_DATA_MISSING -> R.string.policy_missing_number
        MfaApprovalBlockReason.ADMIN_POLICY -> R.string.policy_admin
        MfaApprovalBlockReason.UNKNOWN_POLICY -> R.string.policy_unknown
        MfaApprovalBlockReason.MALFORMED_POLICY -> R.string.policy_malformed
    }

    private fun consumePushIntent(source: Intent?) {
        val extras = source?.extras ?: return
        val payload = extras.keySet().filterNot {
            it.startsWith("google.") || it.startsWith("gcm.")
        }.mapNotNull { key -> (extras.get(key) as? String)?.let { key to it } }.toMap()
        source.replaceExtras(null as Bundle?)
        (application as MfaSampleApplication).dispatchPush(payload)
    }

    private fun consumeIncomingIntent(source: Intent?) {
        if (source == null) return
        val receipt = model.portalIntentAdapter.receive(source)
        if (receipt.disposition != MfaHandoffDisposition.IGNORED) {
            model.portalReceipt(receipt)
            source.data = null
            source.replaceExtras(null as Bundle?)
            source.action = null
            setIntent(Intent())
            return
        }
        if (source.action == Intent.ACTION_VIEW || source.data != null) {
            if (source.action == Intent.ACTION_VIEW && source.selector == null) {
                model.receiveSetup(source.dataString.orEmpty(), SetupSource.BROWSER)
            } else {
                model.reportInvalidPage()
            }
            // Page intents never enter the push dispatcher, including untrusted extras.
            source.data = null
            source.replaceExtras(null as Bundle?)
            source.action = null
        } else {
            consumePushIntent(source)
        }
    }

    private fun openPortal() {
        if (portalPage != null || embeddedPage != null) return
        try {
            portalPage = RealPortalPage(this, model.portalHandoff,
                onReceipt = {
                    model.portalReceipt(it)
                    if (it.disposition != MfaHandoffDisposition.STAGED) {
                        Toast.makeText(this, "Portal handoff: ${it.disposition.name}", Toast.LENGTH_LONG).show()
                    }
                },
                onClose = { portalPage = null; updateButtons() })
            // Bring native state back above the page without replacing the WebView/document.
            portalUi.dispose()
            portalUi.show()
        } catch (error: Exception) {
            model.portalUnavailable(error)
            Toast.makeText(this, if (error is MfaSdkException) error.code.name
                else "PORTAL_WEBVIEW_UNAVAILABLE", Toast.LENGTH_LONG).show()
        }
        updateButtons()
    }

    private fun prepareSetup(source: SetupSource) {
        val qr = binding.qrInput.text.toString()
        val code = binding.codeInput.text.toString()
        val url = binding.urlInput.text.toString()
        clearActivationInputs()
        val input = when {
            qr.isNotBlank() && code.isBlank() && url.isBlank() -> MfaRegistrationParameters.QrCode(qr)
            qr.isBlank() && code.isNotBlank() && url.isNotBlank() -> MfaRegistrationParameters.Activation(code, url)
            else -> {
                model.reportInvalidInput()
                return
            }
        }
        model.preparePage(input, source)
    }

    private fun openSetup(location: SetupLocation) {
        if (model.handoff.requests.current()?.id != location.request.id) {
            model.reportInvalidPage()
            return
        }
        if (location.request.source == SetupSource.BROWSER) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(location.pageUrl))
                    .addCategory(Intent.CATEGORY_BROWSABLE))
            } catch (_: ActivityNotFoundException) {
                model.pageUnavailable(location.request.id)
            } catch (_: SecurityException) {
                model.pageUnavailable(location.request.id)
            }
        } else if (embeddedPage == null) {
            if (Build.VERSION.SDK_INT < 24) {
                model.pageUnavailable(location.request.id)
                return
            }
            try {
                model.embeddedPageOpen = true
                embeddedPage = EmbeddedSetupPage(
                    this, location, model.handoff.requests,
                    onSetup = { model.receiveSetup(it, SetupSource.EMBEDDED) },
                    onClose = {
                        embeddedPage = null
                        model.embeddedPageOpen = false
                        model.handoff.requests.cancel(location.request.id)
                    }
                )
                confirmation?.dismiss()
                confirmation = null
                confirmationId = null
                renderConfirmation(model.handoff.requests.current())
            } catch (_: RuntimeException) {
                model.embeddedPageOpen = false
                model.pageUnavailable(location.request.id)
            }
        }
    }

    private fun renderConfirmation(request: SetupSnapshot?) {
        if (request?.phase != SetupPhase.CONFIRMATION) {
            confirmation?.dismiss()
            confirmation = null
            confirmationId = null
            return
        }
        if (confirmationId == request.id) return
        confirmation?.dismiss()
        confirmationId = request.id
        confirmation = AlertDialog.Builder(this)
            .setTitle(R.string.native_setup_confirmation)
            .setMessage(if (request.source == SetupSource.EMBEDDED)
                R.string.confirm_embedded_setup else R.string.confirm_browser_setup)
            .setPositiveButton(R.string.continue_setup) { _, _ -> model.confirmSetup(request.id) }
            .setNegativeButton(android.R.string.cancel) { _, _ -> model.handoff.requests.cancel(request.id) }
            .setOnCancelListener { model.handoff.requests.cancel(request.id) }
            .create().also {
                it.show()
                it.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        updateButtons()
    }

    private fun closeEmbeddedPage() {
        embeddedPage?.dispose()
        embeddedPage = null
        model.embeddedPageOpen = false
    }

    override fun onDestroy() {
        portalUi.dispose()
        portalPage?.dispose()
        portalPage = null
        confirmation?.dismiss()
        confirmation = null
        // Recreation disposes only UI; native session/operation lifetime is application-owned.
        embeddedPage?.dispose()
        embeddedPage = null
        super.onDestroy()
    }

    private fun showPermissionStatus() {
        val manager = getSystemService(NotificationManager::class.java)
        val permitted = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val channelEnabled = Build.VERSION.SDK_INT < 26 ||
            manager.getNotificationChannel("mfa_requests")?.importance != NotificationManager.IMPORTANCE_NONE
        val visible = permitted && manager.areNotificationsEnabled() && channelEnabled
        binding.permissionStatus.text = if (visible)
            "Visible notifications enabled. Delivery is not guaranteed."
        else "Visible notifications disabled. This does not mean token acquisition or all data delivery failed."
        binding.requestPermission.visibility =
            if (Build.VERSION.SDK_INT >= 33 && !permitted) View.VISIBLE else View.GONE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) showPermissionStatus()
    }

    private fun clearActivationInputs() {
        binding.qrInput.text.clear()
        binding.codeInput.text.clear()
        binding.urlInput.text.clear()
    }

    private fun clearNumberInputs(group: LinearLayout) {
        for (index in 0 until group.childCount) {
            when (val child = group.getChildAt(index)) {
                is EditText -> child.text.clear()
                is LinearLayout -> clearNumberInputs(child)
            }
        }
    }
}
