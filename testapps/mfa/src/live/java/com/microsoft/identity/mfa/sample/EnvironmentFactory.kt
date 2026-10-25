// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.microsoft.identity.mfa.MfaSdkManager
import com.microsoft.identity.mfa.MfaPolicyProfile
import com.microsoft.identity.mfa.poc.MfaPocDiagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object EnvironmentFactory {
    fun create(context: Context): SampleMfaEnvironment = LiveEnvironment(context.applicationContext)
}

private class LiveEnvironment(private val context: Context) : SampleMfaEnvironment {
    override val manager = MfaSdkManager.create(context, enableExperimentalService = true,
        policyProfile = MfaPolicyProfile.valueOf(BuildConfig.MFA_POLICY_PROFILE))
    override val modeLabel = "LIVE — experimental service; authorized test setup only\n" +
        "Policy profile: ${BuildConfig.MFA_POLICY_PROFILE}"
    override val setup = MutableStateFlow(SampleSetup.STARTING)
    private val initialization = Mutex()

    override suspend fun initialize() = initialization.withLock {
        if (FirebaseApp.initializeApp(context) == null) {
            setup.value = SampleSetup.FIREBASE_SETUP_REQUIRED
            return@withLock
        }
        setup.value = SampleSetup.ACQUIRING_TOKEN
        val token = try {
            FcmTokenProvider.currentToken()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            setup.value = SampleSetup.TOKEN_ACQUISITION_FAILED
            throw TokenAcquisitionFailure()
        }
        setup.value = SampleSetup.TOKEN_ACQUIRED
        MfaPocDiagnostics.event(MfaPocDiagnostics.Event.TOKEN_ACQUIRED)
        // A real Firebase token is not proof of SAS routing or permitted enrollment.
        manager.updatePushRegistration(token)
    }

    override fun prepareNotifications() {
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL, context.getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                    description = "Open the sample to review a sign-in request."
                }
            )
        }
    }

    override fun notifyPending() {
        val notifications = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (!notifications.areNotificationsEnabled()) return
        val launch = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tap = PendingIntent.getActivity(
            context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, CHANNEL)
        else Notification.Builder(context)
        notifications.notify(
            100, builder.setSmallIcon(R.drawable.ic_mfa_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text))
                .setContentIntent(tap)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .build()
        )
    }

    private companion object {
        const val CHANNEL = "mfa_requests"
    }
}
