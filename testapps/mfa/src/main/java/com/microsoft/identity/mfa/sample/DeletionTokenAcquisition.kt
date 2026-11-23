// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.mfa.MfaDeletionError
import com.microsoft.identity.mfa.MfaDeletionException
import com.microsoft.identity.mfa.MfaDeletionToken

internal suspend fun acquireDeletionToken(
    allowsInteraction: Boolean,
    silent: suspend () -> MfaDeletionToken,
    interactive: suspend () -> MfaDeletionToken
): MfaDeletionToken = try {
    silent()
} catch (error: MfaDeletionException) {
    if (!allowsInteraction || error.code != MfaDeletionError.INTERACTION_REQUIRED) throw error
    interactive()
}
