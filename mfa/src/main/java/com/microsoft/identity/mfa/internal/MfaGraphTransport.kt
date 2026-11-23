// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request

internal fun interface MfaGraphTransport {
    suspend fun delete(endpoint: String, token: String): Int
}

internal class OkHttpMfaGraphTransport : MfaGraphTransport {
    private val client = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .cookieJar(CookieJar.NO_COOKIES).cache(null)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS).build()

    override suspend fun delete(endpoint: String, token: String): Int = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(endpoint).delete()
            .header("Authorization", "Bearer $token").build()
        client.newCall(request).execute().use { it.code }
    }
}
