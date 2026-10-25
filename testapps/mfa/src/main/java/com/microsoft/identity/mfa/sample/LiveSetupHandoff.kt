// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.os.SystemClock
import com.microsoft.identity.mfa.MfaRegistrationParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class SetupLocation(val request: SetupSnapshot, val origin: String) {
    val pageUrl: String get() = origin + request.path
    val statusUrl: String get() = "$pageUrl/status"
}

/** Application-owned page lifetime. Call from Main; never retains Activity or WebView references. */
internal class LiveSetupHandoff(
    private val scope: CoroutineScope,
    private val packageName: String
) {
    val requests = LiveSetupSession(SystemClock::elapsedRealtime)
    private val mutableLocation = MutableStateFlow<SetupLocation?>(null)
    val location: StateFlow<SetupLocation?> = mutableLocation
    private var server: LiveSetupServer? = null
    private var expiry: Job? = null

    suspend fun prepare(input: MfaRegistrationParameters, source: SetupSource): SetupLocation? {
        val request = requests.prepare(input, source) ?: return null
        closePage()
        try {
            // Use the application scope, not a page's lifecycle, for socket ownership.
            val started = withContext(Dispatchers.IO) {
                LiveSetupServer(scope, requests, request.id, packageName).also { server = it }
            }
            if (requests.current()?.id != request.id) {
                started.close()
                return null
            }
            return SetupLocation(request, started.origin).also {
                mutableLocation.value = it
                expiry = scope.launch {
                    while (isActive && requests.current()?.id == request.id) delay(1_000)
                    closePage()
                }
            }
        } catch (error: Exception) {
            clear()
            throw error
        }
    }

    fun clear() {
        requests.clear()
        closePage()
    }

    private fun closePage() {
        expiry?.cancel()
        expiry = null
        server?.close()
        server = null
        mutableLocation.value = null
    }
}
