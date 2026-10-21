// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.microsoft.identity.mfa.poc.MfaPocDiagnostics
import com.microsoft.identity.mfa.poc.MfaPocDiagnostics.Event

/** Hands Firebase callbacks to the same application-owned dispatcher used by notification taps. */
class MfaFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        MfaPocDiagnostics.event(Event.FCM_TOKEN_RECEIVED)
        (application as MfaSampleApplication).forwardToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        MfaPocDiagnostics.event(Event.FCM_RECEIVED)
        if (message.notification != null) MfaPocDiagnostics.event(Event.FCM_NOTIFICATION_PRESENT)
        (application as MfaSampleApplication).dispatchPush(message.data)
    }

    override fun onDeletedMessages() {
        (application as MfaSampleApplication).report("PUSH_MESSAGES_DELETED_REFRESH_REQUIRED")
    }
}
