// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.microsoft.identity.mfa.MfaAuthSession
import com.microsoft.identity.mfa.MfaEnrollmentStage
import com.microsoft.identity.mfa.MfaRegistrationParameters
import com.microsoft.identity.mfa.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Single-screen, foreground-only enrollment and explicit user decision sample. */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var model: MfaSampleViewModel
    private val decisionButtons = mutableListOf<Button>()
    private var lastSessions: List<MfaAuthSession>? = null

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
        binding.refresh.setOnClickListener { model.refresh() }
        binding.retrySetup.setOnClickListener { model.retrySetup() }
        configureMode()
        observeState()
        consumePushIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumePushIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::model.isInitialized && model.environment.mock == null) showPermissionStatus()
    }

    override fun onStop() {
        clearActivationInputs()
        clearNumberInputs(binding.challenges)
        super.onStop()
    }

    private fun configureMode() {
        val mock = model.environment.mock
        binding.mockControls.visibility = if (mock == null) View.GONE else View.VISIBLE
        binding.liveControls.visibility = if (mock == null) View.VISIBLE else View.GONE
        if (mock == null) {
            binding.requestPermission.setOnClickListener {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
                }
            }
            return
        }
        binding.scenario.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, mock.scenarios
        )
        binding.scenario.setSelection(model.selectedScenario)
        binding.scenario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                model.selectedScenario = position
                mock.setScenario(mock.scenarios[position])
                binding.browserNumber.text = getString(R.string.mock_number, mock.signInNumber)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.browserNumber.text = getString(R.string.mock_number, mock.signInNumber)
        binding.loadQr.setOnClickListener {
            clearActivationInputs()
            binding.qrInput.setText(mock.qrCode)
        }
        binding.loadManual.setOnClickListener {
            clearActivationInputs()
            binding.codeInput.setText(mock.activationCode)
            binding.urlInput.setText(mock.activationUrl)
        }
        binding.injectValidation.setOnClickListener { model.injectValidation() }
        binding.injectChallenge.setOnClickListener { model.injectChallenge() }
        binding.resetMock.setOnClickListener {
            clearActivationInputs()
            model.resetMock()
        }
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
                launch { model.injecting.collect { updateButtons() } }
                model.environment.mock?.let { mock ->
                    launch {
                        mock.operations.collect { operations ->
                            // The debug SDK contract guarantees these are sanitized operation summaries.
                            binding.operations.text = operations.takeLast(20)
                                .joinToString("\n") { it.take(120) }
                            binding.browserNumber.text =
                                getString(R.string.mock_number, mock.signInNumber)
                        }
                    }
                }
            }
        }
    }

    private fun updateButtons() {
        val available = !model.busy.value
        val status = model.status.value
        binding.registerQr.isEnabled = available && status.tokenReady && !status.tokenChangeBlocked
        binding.registerManual.isEnabled = binding.registerQr.isEnabled
        binding.refresh.isEnabled = available
        binding.scenario.isEnabled = available
        binding.loadQr.isEnabled = available
        binding.loadManual.isEnabled = available
        binding.injectChallenge.isEnabled = available &&
            status.enrollmentStage == MfaEnrollmentStage.REGISTERED
        binding.resetMock.isEnabled = available && !model.injecting.value
        binding.injectValidation.isEnabled = !model.injecting.value &&
            status.enrollmentStage == MfaEnrollmentStage.AWAITING_VALIDATION
        decisionButtons.forEach { it.isEnabled = available }
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
            val number = EditText(this).apply {
                setHint(R.string.number_hint)
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                filters = arrayOf(InputFilter.LengthFilter(8))
                isSaveEnabled = false
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
                decisionButtons.add(button)
                row.addView(button)
            }
            binding.challenges.addView(row)
        }
        updateButtons()
    }

    private fun consumePushIntent(source: Intent?) {
        if (model.environment.mock != null) return
        val extras = source?.extras ?: return
        val payload = extras.keySet().filterNot {
            it.startsWith("google.") || it.startsWith("gcm.")
        }.mapNotNull { key -> (extras.get(key) as? String)?.let { key to it } }.toMap()
        source.replaceExtras(null as Bundle?)
        (application as MfaSampleApplication).dispatchPush(payload)
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
