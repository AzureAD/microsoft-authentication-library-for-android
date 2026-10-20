// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

/** Display metadata for a service-confirmed registration; contains no proof material. */
data class MfaRegistrationResult(val registrationId: String, val accountName: String)
