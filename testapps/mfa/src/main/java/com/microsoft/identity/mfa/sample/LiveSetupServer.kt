// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore

/** Bounded GET-only UI host, not an MFA endpoint. Construct on Dispatchers.IO. */
internal class LiveSetupServer(
    scope: CoroutineScope,
    private val session: LiveSetupSession,
    private val id: String,
    private val packageName: String
) : Closeable {
    private val listener = ServerSocket()
    private val sockets = Collections.synchronizedSet(mutableSetOf<Socket>())
    private val slots = Semaphore(4)
    private val job: Job
    val origin: String

    init {
        require(packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")))
        try {
            listener.bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 4)
            listener.soTimeout = 1_000
        } catch (error: Exception) {
            listener.close()
            throw error
        }
        origin = "http://127.0.0.1:${listener.localPort}"
        job = scope.launch(Dispatchers.IO) {
            while (isActive && !listener.isClosed) {
                val socket = try {
                    listener.accept()
                } catch (_: java.net.SocketTimeoutException) {
                    continue
                } catch (_: java.io.IOException) {
                    break
                }
                if (!slots.tryAcquire()) {
                    socket.close()
                    continue
                }
                sockets.add(socket)
                launch {
                    try {
                        socket.use {
                            it.soTimeout = 3_000
                            val path = SetupHttpRequest.read(it.getInputStream(), origin)
                            val current = session.current()?.takeIf { value -> value.id == id }
                            val response = when {
                                path == null -> SetupHttpResponse(400, "text/plain", "Invalid request.")
                                current == null -> SetupHttpResponse(410, "text/plain", "Page session expired or interrupted. Do not resubmit.")
                                path == current.path -> page(current)
                                path == "${current.path}/status" -> SetupPage.status(current)
                                else -> SetupHttpResponse(404, "text/plain", "Not found.")
                            }
                            val bytes = response.body.toByteArray(Charsets.UTF_8)
                            val headers = "HTTP/1.1 ${response.code} ${if (response.code == 200) "OK" else "Unavailable"}\r\n" +
                                "Content-Type: ${response.type}; charset=utf-8\r\nContent-Length: ${bytes.size}\r\n" +
                                "Connection: close\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\n" +
                                "X-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\n" +
                                "Content-Security-Policy: default-src 'none'; script-src 'nonce-${response.nonce}'; " +
                                "connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'\r\n\r\n"
                            it.getOutputStream().apply {
                                write(headers.toByteArray(Charsets.US_ASCII))
                                write(bytes)
                                flush()
                            }
                        }
                    } catch (_: java.io.IOException) {
                        // Disconnects and bounded read timeouts have no enrollment side effects.
                    }
                }.invokeOnCompletion {
                    try {
                        sockets.remove(socket)
                        socket.close()
                    } finally {
                        slots.release()
                    }
                }
            }
        }
    }

    private fun page(current: SetupSnapshot): SetupHttpResponse =
        SetupPage.render(current, packageName)

    override fun close() {
        listener.close()
        job.cancel()
        synchronized(sockets) { sockets.forEach { it.close() } }
    }
}

internal data class SetupHttpResponse(
    val code: Int,
    val type: String,
    val body: String,
    val nonce: String = LiveSetupSession.randomId()
)

/** Strict, bounded HTTP/1.x request head parser; no bodies, keepalive or proxy requests. */
internal object SetupHttpRequest {
    private const val MAX_HEADER_BYTES = 8_192

    fun read(input: InputStream, origin: String): String? {
        val bytes = ByteArrayOutputStream()
        var tail = 0
        val deadline = System.nanoTime() + 3_000_000_000L
        while (bytes.size() < MAX_HEADER_BYTES && System.nanoTime() < deadline) {
            val next = input.read()
            if (next < 0 || (next !in 32..126 && next != 13 && next != 10)) return null
            bytes.write(next)
            tail = (tail shl 8) or next
            if (tail == 0x0d0a0d0a) {
                return parse(bytes.toString("US-ASCII"), origin)
            }
        }
        return null
    }

    private fun parse(head: String, origin: String): String? {
        val lines = head.removeSuffix("\r\n\r\n").split("\r\n")
        val request = lines.first().split(' ')
        if (request.size != 3 || request[0] != "GET" ||
            request[2] !in listOf("HTTP/1.1", "HTTP/1.0")) return null
        val headers = mutableMapOf<String, String>()
        for (line in lines.drop(1)) {
            val separator = line.indexOf(':')
            if (separator <= 0) return null
            val name = line.substring(0, separator).lowercase(Locale.ROOT)
            if (!name.matches(Regex("[a-z0-9-]+")) || headers.containsKey(name)) return null
            headers[name] = line.substring(separator + 1).trim(' ')
        }
        val path = request[1].takeIf { it.matches(Regex("/setup/[0-9a-f]{64}(/status)?")) } ?: return null
        // A cross-site top-level navigation can open the page, but never fetch its status.
        val pageNavigation = !path.endsWith("/status") &&
            headers["sec-fetch-mode"] == "navigate" && headers["sec-fetch-dest"] == "document"
        if (headers["host"] != origin.removePrefix("http://") ||
            headers["origin"]?.let { it != origin } == true ||
            (headers["sec-fetch-site"] == "cross-site" && !pageNavigation) ||
            headers.containsKey("transfer-encoding") || headers.containsKey("expect") ||
            headers["content-length"]?.let { it != "0" } == true) return null
        return path
    }
}

/** Only fixed UI text and unguessable correlation identifiers are sent to the page. */
internal object SetupPage {
    fun status(current: SetupSnapshot) = SetupHttpResponse(
        200, "application/json",
        """{"phase":"${current.phase.name}","terminal":${current.phase.terminal},"message":"${current.phase.message}"}"""
    )

    fun render(current: SetupSnapshot, packageName: String): SetupHttpResponse {
        val nonce = LiveSetupSession.randomId()
        val link = if (current.source == SetupSource.EMBEDDED) current.launchUri else
            "intent://enroll/${current.id}#Intent;scheme=mfa-poc-live;package=$packageName;end"
        return SetupHttpResponse(200, "text/html", """
            <!doctype html>
            <html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="referrer" content="no-referrer"><title>Live MFA controlled demo</title></head>
            <body><h1>Live MFA controlled demo</h1>
            <p>A developer prepared fresh real activation input in the native app.
            This page never receives that input. Continue in the native app to enroll.</p>
            <p>This confirms native live enrollment only, not production portal continuation.</p>
            <p><a id="setup" href="$link" rel="noreferrer">Set up on this device</a></p>
            <p id="status" role="status" aria-live="polite">Checking this request...</p>
            <p>Keep the native app open while enrollment runs. Then resume this page to see its result.
            Do not reuse activation input. Sessions are temporary; process loss invalidates this page.</p>
            <script nonce="$nonce">
            const status = document.getElementById('status');
            const setup = document.getElementById('setup');
            let stopped = false;
            function stop(message) {
                stopped = true; status.textContent = message; setup.removeAttribute('href');
            }
            async function poll() {
                if (stopped) return;
                try {
                    const response = await fetch(location.pathname + '/status', {
                        cache: 'no-store', credentials: 'omit', referrerPolicy: 'no-referrer'
                    });
                    if (!response.ok) throw new Error();
                    const result = await response.json();
                    status.textContent = result.message;
                    if (result.phase !== 'PREPARED') setup.removeAttribute('href');
                    if (result.terminal) { stopped = true; return; }
                    setTimeout(poll, 1000);
                } catch (_) {
                    stop('Page unavailable, expired or process interrupted. Check the native app and service; do not resubmit.');
                }
            }
            setup.addEventListener('click', function () {
                status.textContent = 'Opening native confirmation. If nothing opens, return to the sample app.';
            });
            poll();
            </script></body></html>
        """.trimIndent(), nonce)
    }
}
