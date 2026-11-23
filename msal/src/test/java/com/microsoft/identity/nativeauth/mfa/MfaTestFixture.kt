// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.nativeauth.mfa

import com.microsoft.identity.nativeauth.mfa.internal.*
import java.util.Collections
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

internal class MemoryMfaStore(var state: MfaStoredState = MfaStoredState()) : MfaStore {
    var beforeSave: (MfaStoredState) -> Unit = {}
    override suspend fun load(): MfaStoredState = state
    override suspend fun save(state: MfaStoredState) {
        beforeSave(state)
        this.state = state
    }
}

/** Adapted from the public PoC fixture; all successful outcomes pass through the real wire codec. */
internal class MfaTestFixture : MfaTransport {
    val requests: MutableList<MfaWireRequest> = Collections.synchronizedList(mutableListOf())
    val operations = Channel<String>(Channel.UNLIMITED)
    var intercept: suspend (String, MfaXml) -> MfaWireResponse? = { _, _ -> null }
    var numberMatching = true
    var credentialPolicy = ""
    var activationAccount = account()
    var fetchedAccount = account()
    var decisionCode = "6"
    var completionAccountResult = "Success"
    var responseFields: (String, List<MfaXml>) -> List<MfaXml> = { _, fields -> fields }

    fun manager(store: MfaStore = MemoryMfaStore(), timeout: Long = 5_000): MfaSdkManager =
        MfaSdkManager.create(MfaEngine(store,
            MfaServiceClient(this, PACKAGE, "1.0", "16", "Synthetic device"), timeout))

    suspend fun await(operation: String) = withTimeout(5_000) {
        while (operations.receive() != operation) { /* Skip earlier completed calls. */ }
    }

    override suspend fun send(request: MfaWireRequest): MfaWireResponse {
        requests.add(request)
        assertEquals(PACKAGE, request.headers["AppName"])
        val xml = MfaXml.parse(request.body)
        val soap = "SOAPAction" in request.headers
        val operation = if (soap) xml.child("Body").children.single().name
            else request.headers.getValue("x-ms-mac-action")
        val fields = if (soap) xml.child("Body").children.single() else xml.child("request").children.single()
        operations.send(operation)
        intercept(operation, fields)?.let { return it }
        val account = activationAccount
        if (soap) {
            assertEquals("${MfaServiceClient.NAMESPACE}/$operation", request.headers["SOAPAction"])
            return soap(operation, when (operation) {
                "ActivateNew" -> {
                    val params = fields.child("activationParams")
                    assertEquals(PACKAGE, params.required("PackageName"))
                    assertTrue(params.children.none { it.name == "BundleId" })
                    assertEquals(TOKEN, params.required("DeviceToken"))
                    params.required("DeviceName")
                    params.required("Version")
                    assertTrue(params.required("OathCounter").toLong() >= 0)
                    listOf(MfaXml("ActivateNewResult", "true"), MfaXml("activationInfo", children = listOf(
                        MfaXml("Username", account.username), MfaXml("OathTokenSecretKey", account.secret),
                        MfaXml("ConfirmationCode", "synthetic-confirmation"), MfaXml("TenantId", account.tenantId),
                        MfaXml("AzureObjectId", account.objectId)
                    )))
                }
                "ConfirmActivation" -> {
                    assertEquals("synthetic-confirmation", fields.value("confirmationCode"))
                    listOf(MfaXml("ConfirmActivationResult", "true"), MfaXml("phoneAppDetailId", account.id))
                }
                else -> error("Unexpected SOAP operation")
            })
        }
        val tag = operation.replaceFirstChar { it.lowercaseChar() }
        assertEquals(if (tag.endsWith("Request")) tag else tag + "Request", fields.name)
        return phone(operation, when (operation) {
            "PhoneAppValidateDeviceToken" -> {
                assertEquals(TOKEN, fields.child("phoneAppContext").required("deviceToken"))
                assertEquals("yes", fields.value("validationResult"))
                listOf(MfaXml("username", account.username), MfaXml("accountName", account.name),
                    MfaXml("groupKey", "synthetic-group"), MfaXml("dosPreventer", account.dosPreventer))
            }
            "PhoneAppCheckForAuthentication" -> {
                assertEquals("gcm", fields.child("deviceToken").attributes["notificationType"])
                listOf(MfaXml("checkForAuthenticationResult", "1"), MfaXml("padUrl", HOST),
                    MfaXml("username", fetchedAccount.username), MfaXml("oathCounter", "1234"))
            }
            "PhoneAppAuthentication" -> {
                val context = fields.child("phoneAppContext")
                if (context.value("guid").isEmpty()) {
                    assertEquals(MfaProof.validation(fetchedAccount.secret, 1234), context.value("oathCode"))
                }
                listOf(
                    MfaXml("guid", GUID), MfaXml("phoneAppDetailId", fetchedAccount.id),
                    MfaXml("tenantId", fetchedAccount.tenantId), MfaXml("azureObjectId", fetchedAccount.objectId),
                    MfaXml("firstEntropyChallenge", if (numberMatching) "42" else ""),
                    MfaXml("phoneAppInfo", attributes = mapOf("mode" to "standard")),
                    MfaXml("returnLocationData", "no"), MfaXml("isSilent", "no"),
                    MfaXml("isAppLockRequired", "no"), MfaXml("pinChangeRequired", "no"),
                    MfaXml("userCredentialPolicy", credentialPolicy)
                )
            }
            "PhoneAppPinValidation" -> {
                assertEquals("no", fields.child("pin").attributes["stored"])
                assertEquals("yes", fields.value("authenticate"))
                assertEquals("42", fields.value("selectedEntropyNumber"))
                listOf(MfaXml("validationResult", decisionCode))
            }
            "PhoneAppAuthenticationResult" -> {
                assertEquals("2", fields.value("authenticationResult"))
                assertEquals("gcm", fields.child("newDeviceToken").attributes["notificationType"])
                listOf(MfaXml("authenticationResultResult", "1"))
            }
            "phoneAppStartDeviceTokenChangeV2Request" -> {
                assertEquals(NEW_TOKEN, fields.required("newDeviceToken"))
                assertEquals("FCM", fields.value("notificationType"))
                assertEquals(PACKAGE, fields.required("appPackageName"))
                assertTrue(fields.children.none { it.name == "authenticatorFlavor" })
                listOf(MfaXml("deviceTokenChangeResult", "1"))
            }
            "phoneAppCompleteDeviceTokenChangeV2Request" -> {
                assertEquals(NEW_TOKEN, fields.required("newDeviceToken"))
                val item = fields.child("accounts").children.single()
                assertEquals(fetchedAccount.objectId, item.value("azureObjectId"))
                assertEquals(fetchedAccount.tenantId, item.value("azureTenantId"))
                assertEquals(fetchedAccount.id, item.value("phoneAppDetailId"))
                assertEquals(MfaProof.validation(fetchedAccount.secret, 1234), item.value("oathCode"))
                listOf(MfaXml("deviceTokenChangeResult", "1"),
                    MfaXml("accountValidationResults", children = listOf(
                        MfaXml("account", children = listOf(MfaXml("phoneAppDetailId", fetchedAccount.id),
                            MfaXml("validationResult", completionAccountResult))))))
            }
            else -> error("Unexpected PFP operation")
        })
    }

    fun phone(operation: String, fields: List<MfaXml>, disposition: String = "success") =
        wire(MfaXml("pfpMessage", children = listOf(MfaXml("response", children = listOf(
            MfaXml("status", attributes = mapOf("disposition" to disposition)),
            MfaXml(operation.replaceFirstChar { it.lowercaseChar() }.removeSuffix("Request") + "Response",
                children = responseFields(operation, fields))
        )))))

    fun soap(operation: String, fields: List<MfaXml>) =
        wire(MfaXml("soap:Envelope", attributes = mapOf("xmlns:soap" to MfaServiceClient.SOAP),
            children = listOf(MfaXml("soap:Body", children = listOf(
                MfaXml(operation + "Response", attributes = mapOf("xmlns" to MfaServiceClient.NAMESPACE),
                    children = fields))))))

    fun wire(xml: MfaXml) = MfaWireResponse(200, xml.encoded().toByteArray())

    companion object {
        const val HOST = "fixture.mobileappcommunicator.auth.microsoft.com"
        const val PACKAGE = "com.contoso.mfatest"
        const val TOKEN = "synthetic-original-token"
        const val NEW_TOKEN = "synthetic-replacement-token"
        const val GUID = "11111111-1111-4111-8111-111111111111"
        fun account(id: String = "method-1") = MfaStoredState.Account(
            id, "Example account", "user@example.invalid", "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            "22222222-2222-4222-8222-222222222222", "33333333-3333-4333-8333-333333333333",
            "https://$HOST/pad", TOKEN, "synthetic-dos")
        fun registration() = MfaRegistrationParameters.Activation("synthetic-code", "https://$HOST")
        fun validation(rotation: Boolean = false) = mapOf(
            "type" to "validate", "guid" to GUID, "url" to HOST,
            "validationScenario" to if (rotation) "DeviceTokenChangeV2" else "QrRegistration",
            "tenantId" to account().tenantId, "oathCounter" to "1234")
        fun auth() = mapOf("type" to "auth", "guid" to GUID, "url" to HOST)
    }
}
