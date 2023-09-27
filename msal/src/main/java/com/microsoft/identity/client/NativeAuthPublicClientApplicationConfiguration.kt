//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client

import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import com.microsoft.identity.client.configuration.AccountMode
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.common.java.authorities.CIAMAuthority
import com.microsoft.identity.common.java.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthConstants
import lombok.Getter
import lombok.experimental.Accessors
import java.io.Serializable

/**
 * Extends [PublicClientApplicationConfiguration] to add a few extra fields specific to Native Auth
 * app implementation.
 */
@Accessors(prefix = ["m"])
@Getter
class NativeAuthPublicClientApplicationConfiguration :
    PublicClientApplicationConfiguration(),
    Serializable {
    companion object {
        private val TAG = NativeAuthPublicClientApplicationConfiguration::class.java.simpleName
        private val VALID_CHALLENGE_TYPES = listOf(NativeAuthConstants.GrantType.PASSWORD,
            NativeAuthConstants.GrantType.OOB, NativeAuthConstants.GrantType.REDIRECT)
    }

    private object NativeAuthSerializedNames {
        const val CHALLENGE_TYPES = "challenge_types"
        const val USE_REAL_AUTHORITY = "use_real_authority"
        const val DC = "dc"
    }

    //List of challenge types supported by the client.
    //For a complete list of challenge types see [NativeAuthConstants.GrantType]
    @SerializedName(NativeAuthSerializedNames.CHALLENGE_TYPES)
    private var challengeTypes: List<String>? = null

    //The mock API authority used for testing will be rejected by validation logic run on
    // instantiation. This flag is used to bypass those checks in various points in the application
    @SerializedName(NativeAuthSerializedNames.USE_REAL_AUTHORITY)
    var useRealAuthority: Boolean? = null

    // Appended to the URL constructed in NativeAuthOAuth2Configuration,
    // used for making calls to tenants on test slices
    @SerializedName(NativeAuthSerializedNames.DC)
    var dc: String? = null

    fun getChallengeTypes(): List<String>? {
        return challengeTypes
    }

    fun setChallengeTypes(challengeTypes: List<String>?) {
        this.challengeTypes = challengeTypes
    }

    fun mergeConfiguration(config: NativeAuthPublicClientApplicationConfiguration) {
        // Call super.mergeConfiguration to handle base configuration fields
        super.mergeConfiguration(config)

        // Native-auth specific account mode check. If dev passes MULTIPLE, we still want to set it so
        // we can throw an exception to clarify that MULTIPLE account mode can't be used with Native
        // Auth.
        accountMode = if (config.accountMode != null) config.accountMode else accountMode

        // Handle Native Auth specific fields
        challengeTypes = if (config.challengeTypes == null) challengeTypes else config.challengeTypes

        useRealAuthority = if (config.useRealAuthority == null) useRealAuthority else config.useRealAuthority

        dc = if (config.dc == null) dc else config.dc
    }

    /**
     * Override base validateConfiguration() method as native auth validation is different
     * Had to make this function public as companion object in
     * [NativeAuthPublicClientApplicationConfigurationFactory] cannot access it otherwise
     */
    public override fun validateConfiguration() {
        // Check that a client id was passed
        if (TextUtils.isEmpty(clientId)) {
            throw MsalClientException(
                MsalClientException.NATIVE_AUTH_USE_WITHOUT_CLIENT_ID_ERROR_CODE,
                MsalClientException.NATIVE_AUTH_USE_WITHOUT_CLIENT_ID_ERROR_MESSAGE
            )
        }

        // If we are given a redirectUri, we should run the superclass validation method to make sure
        // we can use web auth
        if (redirectUri != null) {
            super.validateConfiguration()
        } else {
            Logger.warn(TAG, "No redirect URI was passed.")
        }

        // Enforce that account mode must be "SINGLE"
        if (accountMode != AccountMode.SINGLE) {
            throw MsalClientException(
                MsalClientException.NATIVE_AUTH_INVALID_ACCOUNT_MODE_CONFIG_ERROR_CODE,
                MsalClientException.NATIVE_AUTH_INVALID_ACCOUNT_MODE_CONFIG_ERROR_MESSAGE
            )
        }

        // We only allow one authority being passed
        if (authorities == null || authorities.size == 0) {
            // Throw is no authority passed
            throw MsalClientException(
                MsalClientException.NATIVE_AUTH_USE_WITH_NO_AUTHORITY_ERROR_CODE,
                MsalClientException.NATIVE_AUTH_USE_WITH_NO_AUTHORITY_ERROR_MESSAGE
            )
        }
        else if (authorities.size > 1)
        {
            // This throws when more than one authority is passed
            throw MsalClientException(
                MsalClientException.NATIVE_AUTH_USE_WITH_MULTI_AUTHORITY_ERROR_CODE,
                MsalClientException.NATIVE_AUTH_USE_WITH_MULTI_AUTHORITY_ERROR_MESSAGE
            )
        }

        if (defaultAuthority !is NativeAuthCIAMAuthority) {
            // If the authority supplied is a CIAMAuthority, we can continue
            if (defaultAuthority is CIAMAuthority) {
                val nativeAuthAuthority = NativeAuthCIAMAuthority(
                    authorityUrl = defaultAuthority.authorityUri.toString(),
                    clientId = clientId
                )
                authorities.clear()
                authorities.add(nativeAuthAuthority)
            } else {
                // If not, throw exception
                throw MsalClientException(
                    MsalClientException.NATIVE_AUTH_INVALID_CIAM_AUTHORITY_ERROR_CODE,
                    MsalClientException.NATIVE_AUTH_INVALID_CIAM_AUTHORITY_ERROR_MESSAGE
                )
            }
        }

        // Enforce that shared device mode is disabled
        if (isSharedDevice) {
            throw MsalClientException(
                MsalClientException.NATIVE_AUTH_SHARED_DEVICE_MODE_ERROR_CODE,
                MsalClientException.NATIVE_AUTH_SHARED_DEVICE_MODE_ERROR_MESSAGE
            )
        }

        // Enforce that dev cannot set "broker_redirect_uri_registered = true"
        // This check is technically not necessary, since Native Auth configuration only loads with a
        // CIAM authority, which does not result in creating a broker controller
        // Adding it for clarity with devs
        if (useBroker) {
            throw MsalClientException(
                MsalClientException.NATIVE_AUTH_ATTEMPTING_TO_USE_BROKER_ERROR_CODE,
                MsalClientException.NATIVE_AUTH_ATTEMPTING_TO_USE_BROKER_ERROR_MESSAGE
            )
        }

        // Check that challenge types are all valid
        validateChallengeTypes()
    }

    /**
     * Validates that the challenge types passed are valid
     */
    private fun validateChallengeTypes() {
        // Make all challenge types lowercase for simplicity
        challengeTypes = challengeTypes?.map { it.lowercase() }

        challengeTypes?.forEach { challengeType ->
            // Make sure challenge types passed were valid
            if (challengeType !in VALID_CHALLENGE_TYPES) {
                throw MsalClientException(
                    MsalClientException.NATIVE_AUTH_INVALID_CHALLENGE_TYPE_ERROR_CODE,
                    MsalClientException.NATIVE_AUTH_INVALID_CHALLENGE_TYPE_ERROR_MESSAGE + " \"" + challengeType + "\""
                )
            }
        }
    }

    /**
     * Overriding this method to add a check for redirect uri. If no uri was passed, we don't need to check this.
     */
    @Throws(MsalClientException::class)
    override fun checkIntentFilterAddedToAppManifestForBrokerFlow() {
        if (redirectUri != null) {
            super.checkIntentFilterAddedToAppManifestForBrokerFlow()
        }
    }
}
