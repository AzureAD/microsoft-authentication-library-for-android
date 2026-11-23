// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.nativeauth.mfa.internal

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

internal interface MfaWriteHandle : Closeable {
    fun write(bytes: ByteArray)
    fun sync()
}

internal interface MfaFileOperations {
    fun exists(file: File): Boolean
    fun openForWrite(file: File): MfaWriteHandle
    fun replace(source: File, target: File)
    fun syncDirectory(directory: File)
}

/** Checked Android filesystem operations, available since API 21 (the library minimum is 24). */
internal object AndroidMfaFileOperations : MfaFileOperations {
    override fun exists(file: File): Boolean = try {
        Os.stat(file.path)
        true
    } catch (error: ErrnoException) {
        if (error.errno != OsConstants.ENOENT) throw error
        false
    }

    override fun openForWrite(file: File): MfaWriteHandle {
        val stream = FileOutputStream(file)
        return object : MfaWriteHandle {
            override fun write(bytes: ByteArray) { stream.write(bytes) }
            override fun sync() { stream.fd.sync() }
            override fun close() { stream.close() }
        }
    }

    override fun replace(source: File, target: File) { Os.rename(source.path, target.path) }

    override fun syncDirectory(directory: File) {
        val fd = Os.open(directory.path, OsConstants.O_RDONLY, 0)
        try {
            if (!OsConstants.S_ISDIR(Os.fstat(fd).st_mode)) {
                throw IOException("MFA storage directory is invalid.")
            }
            Os.fsync(fd)
        } finally {
            Os.close(fd)
        }
    }
}

/**
 * Same-directory atomic replacement with checked data, close, rename and directory durability.
 * The owning engine serializes access. Failed commits disable this instance: rename may already
 * have advanced the destination. Unsupported directory sync is a failure, never best-effort success.
 */
internal class MfaDurableFile(
    private val base: File,
    private val files: MfaFileOperations = AndroidMfaFileOperations
) {
    private val staging = File(base.path + ".new")
    private var failed = false

    fun read(limit: Int): ByteArray? {
        checkState()
        if (!files.exists(base)) return null
        return FileInputStream(base).use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (output.size() + count > limit) throw IOException("MFA storage size limit.")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    }

    fun write(bytes: ByteArray) {
        checkState()
        try {
            files.openForWrite(staging).use {
                it.write(bytes)
                it.sync()
            }
            files.replace(staging, base)
            files.syncDirectory(checkNotNull(base.parentFile))
        } catch (error: Exception) {
            failed = true
            throw error
        }
    }

    private fun checkState() {
        if (failed) throw IOException("MFA storage commit outcome is uncertain.")
        // A legacy AtomicFile backup cannot prove which replay guards reached durable storage.
        // Never restore an older snapshot over possibly committed security state.
        if (files.exists(File(base.path + ".bak"))) {
            throw IOException("MFA legacy storage outcome is uncertain.")
        }
    }
}
