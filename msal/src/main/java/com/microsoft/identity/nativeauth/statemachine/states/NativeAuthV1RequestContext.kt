// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.statemachine.states

/**
 * Captured claims for an in-memory V1 sign-in operation. Null claims mean known-empty; a missing
 * context means unknown. This context is intentionally not persisted by the existing Parcel format.
 */
internal class NativeAuthV1RequestContext(val claimsRequestJson: String?) {
    companion object {
        const val RESTART_MESSAGE = "The original sign-in request is unavailable. Discard this state and restart sign-in."
    }
}
