// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

/** An opaque challenge issued by this manager, never the expected number-matching answer. */
class MfaAuthSession internal constructor(
    val guid: String,
    val accountName: String,
    val username: String,
    val requiresNumberMatching: Boolean
)
