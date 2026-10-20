// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import android.content.Context
import android.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.identity.common.crypto.AndroidAuthSdkStorageEncryptionManager
import com.microsoft.identity.mfa.MfaErrorCode
import com.microsoft.identity.mfa.MfaSdkException
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface MfaStore {
    suspend fun load(): MfaStoredState
    suspend fun save(state: MfaStoredState)
}

internal class EncryptedMfaStore(context: Context, name: String) : MfaStore {
    private val appContext = context.applicationContext
    private val atomic = AtomicFile(File(appContext.noBackupFilesDir, name))
    private val crypto by lazy { AndroidAuthSdkStorageEncryptionManager(appContext, true) }
    private val gson = Gson()

    override suspend fun load(): MfaStoredState = io {
        // AtomicFile.openRead also recovers its own backup after an interrupted write.
        if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) {
            MfaStoredState()
        } else {
            val encrypted = atomic.openRead().use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    requireMfa(output.size() + count <= STORE_LIMIT, MfaErrorCode.STORAGE_FAILURE)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val plain = crypto.decrypt(encrypted)
            try {
                val json = JsonParser.parseString(String(plain, Charsets.UTF_8))
                validateShape(json)
                gson.fromJson(json, MfaStoredState::class.java).also { it.validate() }
            } finally {
                plain.fill(0)
            }
        }
    }

    override suspend fun save(state: MfaStoredState) = io {
        state.validate()
        val plain = gson.toJson(state).toByteArray(Charsets.UTF_8)
        val encrypted = try { crypto.encrypt(plain) } finally { plain.fill(0) }
        requireMfa(encrypted.size <= STORE_LIMIT, MfaErrorCode.STORAGE_FAILURE)
        var stream: FileOutputStream? = null
        try {
            stream = atomic.startWrite()
            stream.write(encrypted)
            atomic.finishWrite(stream)
        } catch (e: Exception) {
            if (stream != null) atomic.failWrite(stream)
            throw e
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) {
        try { block() } catch (e: CancellationException) { throw e }
        catch (_: Exception) { throw MfaSdkException(MfaErrorCode.STORAGE_FAILURE) }
    }

    private fun validateShape(json: JsonElement) {
        requireMfa(json.isJsonObject, MfaErrorCode.STORAGE_FAILURE)
        val root = json.asJsonObject
        val version = root.get("version")
        requireMfa(version != null && version.isJsonPrimitive && version.asJsonPrimitive.isNumber &&
            version.asString == "1", MfaErrorCode.STORAGE_FAILURE)
        requiredBoolean(root, "tokenChangeBlocked")
        val accounts = root.get("accounts")
        val submitted = root.get("submitted")
        requireMfa(accounts != null && accounts.isJsonArray && submitted != null && submitted.isJsonArray,
            MfaErrorCode.STORAGE_FAILURE)
        root.get("token")?.let { requiredString(root, "token") }
        for (account in accounts.asJsonArray) {
            requireMfa(account.isJsonObject, MfaErrorCode.STORAGE_FAILURE)
            for (field in listOf("id", "name", "username", "secret", "tenantId", "objectId",
                "endpoint", "token", "dosPreventer", "groupKey", "replicationScope", "routingHint", "countryCode")) {
                requiredString(account.asJsonObject, field)
            }
        }
        for (id in submitted.asJsonArray) {
            requireMfa(id.isJsonPrimitive && id.asJsonPrimitive.isString &&
                id.asString.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")),
                MfaErrorCode.STORAGE_FAILURE)
        }
        root.get("pending")?.let {
            requireMfa(it.isJsonObject, MfaErrorCode.STORAGE_FAILURE)
            val p = it.asJsonObject
            for (field in listOf("id", "digest", "token", "endpoint")) requiredString(p, field)
            requiredBoolean(p, "validationAttempt")
            requiredBoolean(p, "confirming")
            for (field in listOf("activation", "validation", "validationEndpoint")) {
                if (p.has(field)) requiredString(p, field)
            }
        }
    }

    private fun requiredString(json: JsonObject, field: String) {
        val value = json.get(field)
        requireMfa(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString,
            MfaErrorCode.STORAGE_FAILURE)
    }

    private fun requiredBoolean(json: JsonObject, field: String) {
        val value = json.get(field)
        requireMfa(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isBoolean,
            MfaErrorCode.STORAGE_FAILURE)
    }

    companion object { private const val STORE_LIMIT = 8 * 1_048_576 }
}
