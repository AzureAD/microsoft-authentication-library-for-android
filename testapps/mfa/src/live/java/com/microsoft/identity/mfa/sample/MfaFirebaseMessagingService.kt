// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Hands Firebase callbacks to the same application-owned dispatcher used by notification taps. */
class MfaFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        (application as MfaSampleApplication).forwardToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        (application as MfaSampleApplication).dispatchPush(message.data)
    }

    override fun onDeletedMessages() {
        (application as MfaSampleApplication).report("PUSH_MESSAGES_DELETED_REFRESH_REQUIRED")
    }
}
