// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.nativeauth.mfa

import com.google.gson.Gson
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.NEW_TOKEN
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.TOKEN
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.account
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.auth
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.registration
import com.microsoft.identity.nativeauth.mfa.MfaTestFixture.Companion.validation
import com.microsoft.identity.nativeauth.mfa.internal.MfaDurableFile
import com.microsoft.identity.nativeauth.mfa.internal.MfaFileOperations
import com.microsoft.identity.nativeauth.mfa.internal.MfaStore
import com.microsoft.identity.nativeauth.mfa.internal.MfaStoredState
import com.microsoft.identity.nativeauth.mfa.internal.MfaWriteHandle
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MfaDurableFileTest {
    private lateinit var directory: File

    @Before fun createFiles() {
        directory = File("build", "mfa-durability-${UUID.randomUUID()}")
        check(directory.mkdirs())
    }

    @After fun deleteFiles() { check(directory.deleteRecursively()) }

    @Test
    fun `real file replacement syncs and closes data before atomic rename and directory sync`() {
        val files = JvmFiles()
        val base = File(directory, "state")
        val storage = MfaDurableFile(base, files)
        assertNull(storage.read(100))
        storage.write("old-encrypted-bytes".toByteArray())
        files.events.clear()
        storage.write("new-encrypted-bytes".toByteArray())
        assertEquals(listOf(Stage.OPEN, Stage.WRITE, Stage.SYNC, Stage.CLOSE,
            Stage.RENAME, Stage.AFTER_RENAME, Stage.DIRECTORY_SYNC), files.events)
        assertEquals("new-encrypted-bytes", String(storage.read(100)!!))
        assertFalse(File(base.path + ".new").exists())
        assertTrue(runCatching { storage.read(1) }.exceptionOrNull() is IOException)
    }

    @Test
    fun `every failed commit stage throws and disables further access even when disk advanced`() {
        for (stage in Stage.values()) {
            val base = File(directory, stage.name)
            val files = JvmFiles()
            val storage = MfaDurableFile(base, files)
            storage.write("old".toByteArray())
            files.failStage = stage
            files.failWrite = files.writes + 1
            assertTrue(stage.name, runCatching { storage.write("new".toByteArray()) }.exceptionOrNull() is IOException)
            val advanced = stage in setOf(Stage.AFTER_RENAME, Stage.DIRECTORY_SYNC)
            assertEquals(stage.name, if (advanced) "new" else "old", base.readText())
            files.failStage = null
            assertTrue(stage.name, runCatching { storage.read(100) }.exceptionOrNull() is IOException)
            assertTrue(stage.name, runCatching { storage.write("retry".toByteArray()) }.exceptionOrNull() is IOException)
        }
    }

    @Test
    fun `interrupted staging never replaces base and ambiguous legacy backup fails closed`() {
        val base = File(directory, "state")
        val files = JvmFiles()
        MfaDurableFile(base, files).write("committed".toByteArray())
        File(base.path + ".new").writeText("incomplete")
        assertEquals("committed", String(MfaDurableFile(base, files).read(100)!!))
        File(base.path + ".bak").writeText("older")
        assertTrue(runCatching { MfaDurableFile(base, files).read(100) }.exceptionOrNull() is IOException)
        assertTrue(runCatching { MfaDurableFile(base, files).write("new".toByteArray()) }.exceptionOrNull() is IOException)
        assertEquals("committed", base.readText())
    }

    @Test
    fun `failed durable rotation guard prevents network at every commit stage`() = runBlocking {
        for (stage in Stage.values()) {
            val files = JvmFiles()
            val store = fileStore(stage.name, files)
            store.save(MfaStoredState(token = TOKEN, accounts = listOf(account())))
            val fixture = MfaTestFixture()
            val manager = fixture.manager(store)
            // First commit stores the latest token; the second reserves the remote mutation.
            files.failStage = stage
            files.failWrite = files.writes + 2
            assertGeneral { manager.updatePushRegistration(NEW_TOKEN) }
            assertTrue(stage.name, fixture.requests.isEmpty())
            files.failStage = null
            assertGeneral { manager.registeredAuthMethods() }
            assertGeneral { manager.updatePushRegistration(TOKEN) }
            assertTrue(stage.name, fixture.requests.isEmpty())
        }
    }

    @Test
    fun `failed directory sync after guard rename cannot continue from cached pre-guard state`() = runBlocking {
        val files = JvmFiles()
        val base = File(directory, "state")
        val store = FileStore(MfaDurableFile(base, files))
        store.save(MfaStoredState(token = TOKEN, accounts = listOf(account())))
        val fixture = MfaTestFixture()
        val manager = fixture.manager(store)
        val session = manager.handlePushNotification(auth()).single()
        files.failStage = Stage.DIRECTORY_SYNC
        // Respond refreshes metadata, then writes its replay guard before the decision POST.
        files.failWrite = files.writes + 2
        assertGeneral { manager.respond(session, MfaAction.Approve("42")) }
        assertTrue(fixture.requests.none { it.headers["x-ms-mac-action"] == "PhoneAppPinValidation" })
        val onDisk = FileStore(MfaDurableFile(base, JvmFiles())).load()
        assertTrue(onDisk.submitted.contains(session.guid))
        val count = fixture.requests.size
        files.failStage = null
        assertGeneral { manager.respond(session, MfaAction.Deny) }
        assertGeneral { manager.registeredAuthMethods() }
        assertEquals(count, fixture.requests.size)
    }

    @Test
    fun `registration never reports success when confirmed account directory sync fails`() = runBlocking {
        val files = JvmFiles()
        val base = File(directory, "state")
        val store = FileStore(MfaDurableFile(base, files))
        store.save(MfaStoredState(token = TOKEN))
        val fixture = MfaTestFixture()
        val manager = fixture.manager(store)
        files.failStage = Stage.DIRECTORY_SYNC
        files.failWrite = files.writes + 2 // Activation guard, then confirmed registration.
        val enrollment = async { runCatching { manager.registerAuthMethod(registration()) } }
        fixture.await("ActivateNew")
        manager.handlePushNotification(validation())
        val result = withTimeout(5_000) { enrollment.await() }
        assertTrue(result.exceptionOrNull() is MfaSdkException)
        assertTrue((result.exceptionOrNull() as MfaSdkException).isGeneralError())
        assertTrue(fixture.requests.any { it.headers["SOAPAction"]?.endsWith("/ConfirmActivation") == true })
        assertEquals(1, FileStore(MfaDurableFile(base, JvmFiles())).load().accounts.size)
        files.failStage = null
        assertGeneral { manager.registeredAuthMethods() }
    }

    private fun fileStore(name: String, files: JvmFiles) = FileStore(MfaDurableFile(File(directory, name), files))

    private suspend fun assertGeneral(block: suspend () -> Any?) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertTrue(failure is MfaSdkException)
        assertTrue((failure as MfaSdkException).isGeneralError())
        assertNull(failure.cause)
    }

    // Synthetic state exercises the same durability primitive and engine boundaries without Android
    // keystore dependencies. Production EncryptedMfaStore encrypts before invoking this primitive.
    private class FileStore(private val file: MfaDurableFile) : MfaStore {
        private val gson = Gson()
        override suspend fun load(): MfaStoredState = file.read(1_048_576)?.let {
            gson.fromJson(String(it, Charsets.UTF_8), MfaStoredState::class.java)
        } ?: MfaStoredState()
        override suspend fun save(state: MfaStoredState) {
            file.write(gson.toJson(state).toByteArray(Charsets.UTF_8))
        }
    }

    private enum class Stage { OPEN, WRITE, SYNC, CLOSE, RENAME, AFTER_RENAME, DIRECTORY_SYNC }

    /**
     * Real host file writes, descriptor sync, close and atomic replacement. Directory-fsync injection
     * models Android's checked Os boundary because host Windows does not expose POSIX directory fsync.
     */
    private class JvmFiles : MfaFileOperations {
        val events = mutableListOf<Stage>()
        var failStage: Stage? = null
        var failWrite = -1
        var writes = 0

        private fun stage(value: Stage) {
            events.add(value)
            if (writes == failWrite && failStage == value) throw IOException("Injected storage failure.")
        }

        override fun exists(file: File) = Files.exists(file.toPath())

        override fun openForWrite(file: File): MfaWriteHandle {
            writes++
            stage(Stage.OPEN)
            val stream = FileOutputStream(file)
            return object : MfaWriteHandle {
                override fun write(bytes: ByteArray) {
                    stream.write(bytes)
                    stage(Stage.WRITE)
                }
                override fun sync() {
                    stage(Stage.SYNC)
                    stream.fd.sync()
                }
                override fun close() {
                    stream.close()
                    stage(Stage.CLOSE)
                }
            }
        }

        override fun replace(source: File, target: File) {
            stage(Stage.RENAME)
            Files.move(source.toPath(), target.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            stage(Stage.AFTER_RENAME)
        }

        override fun syncDirectory(directory: File) {
            check(directory.isDirectory)
            stage(Stage.DIRECTORY_SYNC)
        }
    }
}
