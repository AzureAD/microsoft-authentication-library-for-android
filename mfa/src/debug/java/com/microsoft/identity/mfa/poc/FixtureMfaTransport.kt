// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.poc

import com.microsoft.identity.mfa.MfaErrorCode
import com.microsoft.identity.mfa.MfaSdkException
import com.microsoft.identity.mfa.internal.MfaProof
import com.microsoft.identity.mfa.internal.MfaServiceClient
import com.microsoft.identity.mfa.internal.MfaTransport
import com.microsoft.identity.mfa.internal.MfaWireRequest
import com.microsoft.identity.mfa.internal.MfaWireResponse
import com.microsoft.identity.mfa.internal.MfaXml
import com.microsoft.identity.mfa.internal.requireMfa
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A synthetic wire-level service. Uses the production XML codec in both directions; never injects
 * accounts, sessions, or a completed enrollment directly into the manager.
 */
internal class FixtureMfaTransport : MfaTransport {
    @Volatile var scenario = MfaPocScenario.HAPPY_PATH
    private val mutableOperations = MutableStateFlow<List<String>>(emptyList())
    val operations: StateFlow<List<String>> = mutableOperations.asStateFlow()
    private val challenges = LinkedHashMap<String, Boolean>()
    private val submitted = HashSet<String>()
    private var pullGuid: String? = null

    @Synchronized
    fun issue(numberMatching: Boolean = true): Map<String, String> {
        val guid = UUID.randomUUID().toString()
        challenges[guid] = numberMatching
        return mapOf("type" to "auth", "guid" to guid, "url" to HOST)
    }

    fun validationPayload(): Map<String, String> = mapOf(
        "type" to "validate", "guid" to UUID.randomUUID().toString(), "url" to HOST,
        "validationScenario" to "QrRegistration")

    @Synchronized
    fun reset() {
        challenges.clear()
        submitted.clear()
        pullGuid = null
        scenario = MfaPocScenario.HAPPY_PATH
        mutableOperations.value = emptyList()
    }

    override suspend fun send(request: MfaWireRequest): MfaWireResponse = process(request)

    @Synchronized
    private fun process(request: MfaWireRequest): MfaWireResponse {
        val root = MfaXml.parse(request.body)
        val soap = root.name == "Envelope"
        val operation: String
        val fields: MfaXml
        if (soap) {
            fields = root.child("Body").children.single()
            operation = fields.name
        } else {
            fields = root.child("request").children.single()
            operation = request.headers["x-ms-mac-action"] ?: throw MfaSdkException(MfaErrorCode.INVALID_RESPONSE)
        }
        requireMfa(request.endpoint.host == HOST && request.headers["AppName"]?.isNotEmpty() == true)
        append(operation)
        when (scenario) {
            MfaPocScenario.TRANSPORT_FAILURE -> throw MfaSdkException(MfaErrorCode.TRANSPORT_FAILURE)
            MfaPocScenario.MALFORMED_RESPONSE -> return MfaWireResponse(200, "<invalid>".toByteArray())
            else -> Unit
        }
        if (soap) {
            requireMfa(request.headers["SOAPAction"] == "${MfaServiceClient.NAMESPACE}/$operation")
            val responseFields = when (operation) {
                "ActivateNew" -> {
                    val params = fields.child("activationParams")
                    requireMfa(params.required("DeviceToken") == TOKEN)
                    params.required("DeviceName")
                    params.required("Version")
                    requireMfa(params.required("OathCounter").toLongOrNull() != null)
                    if (scenario == MfaPocScenario.REJECTED_ACTIVATION ||
                        params.value("ActivationCode") != CODE) {
                        listOf(MfaXml("ActivateNewResult", "false"),
                            MfaXml("error", children = listOf(MfaXml("Code", "17"))))
                    } else listOf(
                        MfaXml("ActivateNewResult", "true"),
                        MfaXml("activationInfo", children = listOf(
                            MfaXml("Username", USERNAME), MfaXml("OathTokenSecretKey", SECRET),
                            MfaXml("ConfirmationCode", "synthetic-confirmation"),
                            MfaXml("TenantId", TENANT), MfaXml("AzureObjectId", OBJECT)
                        ))
                    )
                }
                "ConfirmActivation" -> {
                    requireMfa(fields.required("confirmationCode") == "synthetic-confirmation")
                    listOf(MfaXml("ConfirmActivationResult", "true"), MfaXml("phoneAppDetailId", ACCOUNT))
                }
                else -> throw MfaSdkException(MfaErrorCode.INVALID_RESPONSE)
            }
            return wire(MfaXml("soap:Envelope", attributes = mapOf("xmlns:soap" to MfaServiceClient.SOAP),
                children = listOf(MfaXml("soap:Body", children = listOf(
                    MfaXml(operation + "Response", attributes = mapOf("xmlns" to MfaServiceClient.NAMESPACE),
                        children = responseFields))))))
        }
        requireMfa(fields.name == operation.replaceFirstChar { it.lowercaseChar() } + "Request")
        if (scenario == MfaPocScenario.EXPIRED_REQUEST && operation == "PhoneAppAuthentication") {
            return phone(operation, emptyList(), "failure")
        }
        val responseFields = when (operation) {
            "PhoneAppValidateDeviceToken" -> {
                requireMfa(fields.child("phoneAppContext").required("deviceToken") == TOKEN)
                requireMfa(fields.value("validationResult") == "yes")
                listOf(MfaXml("username", USERNAME), MfaXml("accountName", "Example work account"),
                    MfaXml("groupKey", "synthetic-group"), MfaXml("dosPreventer", "synthetic-dos"))
            }
            "PhoneAppCheckForAuthentication" -> {
                requireMfa(fields.child("deviceToken").attributes["notificationType"] == "gcm")
                requireMfa(fields.value("deviceToken") == TOKEN)
                pullGuid = issue()["guid"]
                listOf(MfaXml("checkForAuthenticationResult", "1"), MfaXml("padUrl", HOST),
                    MfaXml("username", USERNAME), MfaXml("oathCounter", MfaProof.counter().toString()))
            }
            "PhoneAppAuthentication" -> {
                val context = fields.child("phoneAppContext")
                requireMfa(context.required("deviceToken") == TOKEN)
                val guid = context.value("guid").ifEmpty {
                    requireMfa(context.required("oathCode").matches(Regex("[0-9A-F]{40}")))
                    pullGuid ?: throw MfaSdkException(MfaErrorCode.INVALID_RESPONSE)
                }
                requireMfa(challenges.containsKey(guid))
                listOf(
                    MfaXml("guid", guid), MfaXml("phoneAppDetailId", ACCOUNT),
                    MfaXml("tenantId", TENANT), MfaXml("azureObjectId", OBJECT),
                    MfaXml("replicationScope"), MfaXml("dosPreventer", "synthetic-dos"),
                    MfaXml("firstEntropyChallenge", if (challenges[guid] == true) EXPECTED_NUMBER else ""),
                    MfaXml("phoneAppInfo", attributes = mapOf("mode" to "standard")),
                    MfaXml("returnLocationData", "no"), MfaXml("isSilent", "no"),
                    MfaXml("isAppLockRequired", "no"), MfaXml("userCredentialPolicy")
                )
            }
            "PhoneAppPinValidation", "PhoneAppAuthenticationResult" -> {
                val guid = fields.child("phoneAppContext").required("guid")
                requireMfa(challenges.containsKey(guid) && submitted.add(guid))
                val expired = scenario == MfaPocScenario.EXPIRED_REQUEST
                if (operation == "PhoneAppPinValidation") {
                    requireMfa(fields.child("pin").attributes["stored"] == "no" &&
                        fields.value("authenticate") == "yes")
                    val valid = fields.value("selectedEntropyNumber") == EXPECTED_NUMBER
                    listOf(MfaXml("validationResult", if (expired) "8" else if (valid) "6" else "7"))
                } else {
                    requireMfa(fields.value("authenticationResult") in setOf("1", "2") &&
                        fields.child("newDeviceToken").attributes["notificationType"] == "gcm")
                    listOf(MfaXml("authenticationResultResult", if (expired) "8" else "1"))
                }
            }
            "PhoneAppGetEndpoints" -> listOf(
                MfaXml("version", "1"), MfaXml("endpoints", children = listOf(
                    MfaXml("endpoint", children = listOf(
                        MfaXml("url", "https://$HOST/pad"), MfaXml("replicationScopes", "synthetic-scope")))
                )))
            else -> throw MfaSdkException(MfaErrorCode.INVALID_RESPONSE)
        }
        return phone(operation, responseFields)
    }

    private fun phone(operation: String, fields: List<MfaXml>, disposition: String = "success") =
        wire(MfaXml("pfpMessage", attributes = mapOf("version" to "1.6"), children = listOf(
            MfaXml("response", children = listOf(
                MfaXml("status", attributes = mapOf("disposition" to disposition)),
                MfaXml(operation.replaceFirstChar { it.lowercaseChar() } + "Response", children = fields)
            ))
        )))

    private fun wire(xml: MfaXml) = MfaWireResponse(200, xml.encoded().toByteArray(Charsets.UTF_8))
    private fun append(operation: String) {
        mutableOperations.value = (mutableOperations.value + operation).takeLast(100)
    }

    companion object {
        const val HOST = "poc.mobileappcommunicator.auth.microsoft.com"
        const val CODE = "000000000"
        const val TOKEN = "synthetic-poc-android-token-not-a-real-registration"
        const val EXPECTED_NUMBER = "42"
        const val ACCOUNT = "11111111-1111-4111-8111-111111111111"
        const val TENANT = "22222222-2222-4222-8222-222222222222"
        const val OBJECT = "33333333-3333-4333-8333-333333333333"
        const val USERNAME = "alex@example.invalid"
        const val SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    }
}
