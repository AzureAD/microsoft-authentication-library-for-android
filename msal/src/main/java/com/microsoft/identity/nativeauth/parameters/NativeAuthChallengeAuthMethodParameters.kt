package com.microsoft.identity.nativeauth.parameters

import com.microsoft.identity.nativeauth.AuthMethod

/**
 * Encapsulates the parameters passed to the challengeAuthMethod methods of RegisterStrongAuthState
 */
class NativeAuthChallengeAuthMethodParameters(
    /**
     * authentication method to challenge
     */
    val authMethod: AuthMethod
) {

    /**
     * email to contact to register a new strong authentication method
     */
    var verificationContact: String? = null
}