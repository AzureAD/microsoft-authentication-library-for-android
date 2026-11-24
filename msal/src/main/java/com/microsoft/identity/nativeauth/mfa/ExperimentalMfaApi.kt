// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

/** Marks MFA APIs that are not ready for production use. */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "Warning: This API is experimental. It may be changed in the future without notice. " +
        "Do not use in production applications."
)
annotation class ExperimentalMfaApi
