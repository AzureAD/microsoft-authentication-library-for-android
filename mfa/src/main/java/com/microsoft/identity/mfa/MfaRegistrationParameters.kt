// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

/** Activation input. Instances deliberately do not render their credential-bearing contents. */
sealed class MfaRegistrationParameters {
    /** QR contents supplied by the hosting application; camera capture is outside this SDK. */
    class QrCode(val text: String) : MfaRegistrationParameters()

    /** An activation code and its associated commercial-cloud service URL. */
    class Activation(val code: String, val url: String) : MfaRegistrationParameters()
}
