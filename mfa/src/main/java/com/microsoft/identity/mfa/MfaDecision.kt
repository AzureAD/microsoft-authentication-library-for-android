// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa

/**
 * Service acknowledgment of the action passed to [MfaSdkManager.respond].
 *
 * Neither value means sign-in completed or a token was issued. Failure and cancellation throw
 * instead of returning a decision; a rejected approval must never become [DENIED].
 */
enum class MfaDecision {
    /** The service acknowledged the requested approval. */
    APPROVED,

    /** The service acknowledged the requested denial. */
    DENIED
}
