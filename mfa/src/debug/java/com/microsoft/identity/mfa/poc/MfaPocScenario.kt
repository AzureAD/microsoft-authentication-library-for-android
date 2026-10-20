// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.poc

/** Synthetic service behavior. None of these controls exist in the release SDK. */
enum class MfaPocScenario {
    HAPPY_PATH, REJECTED_ACTIVATION, WRONG_NUMBER, EXPIRED_REQUEST, TRANSPORT_FAILURE, MALFORMED_RESPONSE
}
