// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import com.microsoft.identity.mfa.*
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class MfaDeletionController(
    private val stateMutex: Mutex,
    private val read: suspend () -> MfaStoredState,
    private val save: suspend (MfaStoredState) -> Unit,
    private val invalidate: (String) -> Unit,
    private val graph: MfaGraphTransport = OkHttpMfaGraphTransport()
) {
    private val busy = ConcurrentHashMap.newKeySet<String>()
    private val remoteDeleted = ConcurrentHashMap.newKeySet<String>()
    private val responseGates = ConcurrentHashMap<String, Mutex>()

    fun isRemotelyDeleted(id: String): Boolean = id in remoteDeleted

    suspend fun <T> respond(id: String, block: suspend () -> T): T =
        gate(id).withLock {
            if (isRemotelyDeleted(id)) throw MfaDeletionException(MfaDeletionError.UNKNOWN_REGISTRATION)
            block()
        }

    suspend fun delete(
        id: String,
        provider: MfaDeletionTokenProvider,
        allowsInteraction: Boolean,
        progress: (MfaDeletionStage) -> Unit
    ) {
        if (!busy.add(id)) throw MfaDeletionException(MfaDeletionError.OPERATION_IN_PROGRESS)
        fun stage(value: MfaDeletionStage) {
            // Observers cannot prevent a confirmed remote deletion from being persisted.
            try { progress(value) } catch (_: Exception) { }
        }
        try {
            val account = stateMutex.withLock {
                read().accounts.singleOrNull { it.id == id }
                    ?: throw MfaDeletionException(MfaDeletionError.UNKNOWN_REGISTRATION)
            }
            if (isRemotelyDeleted(id)) {
                gate(id).withLock { cleanup(id, ::stage) }
                return
            }
            if (!listOf(id, account.tenantId, account.objectId).all { GUID.matches(it) }) {
                throw MfaDeletionException(MfaDeletionError.INVALID_REGISTRATION)
            }
            val request = MfaDeletionTokenRequest(account.tenantId, account.objectId,
                account.username, allowsInteraction)
            stage(MfaDeletionStage.ACQUIRING_TOKEN)
            val token = try {
                provider.acquireToken(request).let { it.copy(grantedScopes = it.grantedScopes.toList()) }
            } catch (e: CancellationException) { throw e }
            catch (e: MfaDeletionException) { throw e }
            catch (_: Exception) { throw MfaDeletionException(MfaDeletionError.PROVIDER_FAILED) }
            validateToken(request, token)
            stage(MfaDeletionStage.WAITING_FOR_APPROVAL)
            gate(id).withLock {
                stateMutex.withLock {
                    if (read().accounts.none { it.id == account.id &&
                            it.tenantId == account.tenantId && it.objectId == account.objectId &&
                            it.secret == account.secret }) {
                        throw MfaDeletionException(MfaDeletionError.UNKNOWN_REGISTRATION)
                    }
                }
                currentCoroutineContext().ensureActive()
                validateToken(request, token)
                stage(MfaDeletionStage.DELETING)
                currentCoroutineContext().ensureActive()
                // Once dispatched, wait for the bounded HTTP result. Abandoning a delivered 204
                // at a cancellable dispatcher boundary would lose the committed remote outcome.
                withContext(NonCancellable) {
                    val status = try {
                        graph.delete("https://graph.microsoft.com/v1.0/users/${account.objectId}" +
                            "/authentication/microsoftAuthenticatorMethods/$id", token.accessToken)
                    } catch (_: Exception) {
                        throw MfaDeletionException(MfaDeletionError.OUTCOME_UNKNOWN)
                    }
                    if (status != 204 && status != 404) {
                        throw MfaDeletionException(MfaDeletionError.GRAPH_REJECTED, status)
                    }
                    remoteDeleted.add(id)
                    cleanup(id, ::stage)
                }
            }
        } finally {
            busy.remove(id)
        }
    }

    private suspend fun cleanup(id: String, progress: (MfaDeletionStage) -> Unit) =
        withContext(NonCancellable) {
            try {
                stateMutex.withLock {
                    invalidate(id)
                    val state = read()
                    save(state.copy(accounts = state.accounts.filterNot { it.id == id }))
                }
            } catch (_: Exception) {
                progress(MfaDeletionStage.LOCAL_CLEANUP_REQUIRED)
                throw MfaDeletionException(MfaDeletionError.LOCAL_CLEANUP_REQUIRED)
            }
            progress(MfaDeletionStage.SUCCEEDED)
        }

    private fun gate(id: String): Mutex = responseGates.getOrPut(id) { Mutex() }

    internal companion object {
        private val GUID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

        fun validateToken(request: MfaDeletionTokenRequest, token: MfaDeletionToken) {
            val authority = try { URI(token.authority) } catch (_: Exception) { null }
            val validAuthority = authority != null && authority.scheme == "https" &&
                authority.host.equals("login.microsoftonline.com", true) &&
                authority.port == -1 && authority.userInfo == null &&
                authority.query == null && authority.fragment == null &&
                authority.rawPath.trimEnd('/').equals("/${request.tenantId}", true)
            if (!validAuthority || !token.tenantId.equals(request.tenantId, true) ||
                !token.objectId.equals(request.objectId, true) ||
                token.accessToken.length !in 1..65536 || token.accessToken.any { it !in '!'..'~' } ||
                token.expiresOnMillis <= System.currentTimeMillis() + 30_000 ||
                !token.authenticationScheme.equals("Bearer", true) ||
                token.grantedScopes.none { it.equals("UserAuthenticationMethod.ReadWrite", true) ||
                    it.equals(request.scopes.single(), true) }) {
                throw MfaDeletionException(MfaDeletionError.INVALID_TOKEN)
            }
        }
    }
}
