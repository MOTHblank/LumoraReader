package com.mothblank.lumorareader.data

import android.content.Context
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files

/**
 * Process-safe, independently owned EPUB workspaces.
 *
 * The root lock serializes directory creation and recovery across processes.
 * Every open reader holds a separate OS file lock OUTSIDE its extracted content,
 * so ZIP entries cannot overwrite the lease. A crashed process releases its
 * locks automatically, allowing the next open to discard abandoned extractions.
 */
internal object EpubExtractionStorage {
    private const val ROOT_LOCK_NAME = ".storage.lock"
    private const val DIRECTORY_PREFIX = "epub_instance_"
    private const val READER_LOCK_SUFFIX = ".reader.lock"
    private val monitor = Any()

    fun acquire(context: Context): Lease = synchronized(monitor) {
        val root = File(context.cacheDir, "epub_cache")
        if (!root.isDirectory && !root.mkdirs()) {
            throw IOException("Could not create EPUB extraction cache")
        }

        RandomAccessFile(File(root, ROOT_LOCK_NAME), "rw").use { registry ->
            registry.channel.lock().use {
                cleanupAbandoned(root)
                val directory = Files.createTempDirectory(root.toPath(), DIRECTORY_PREFIX).toFile()
                val lockPath = File(root, directory.name + READER_LOCK_SUFFIX)
                var handle: RandomAccessFile? = null
                var lock: FileLock? = null
                try {
                    handle = RandomAccessFile(lockPath, "rw")
                    lock = handle.channel.lock()
                    Lease(directory, lockPath, handle, lock)
                } catch (failure: Throwable) {
                    try { lock?.release() } catch (closeError: Exception) {
                        failure.addSuppressed(closeError)
                    }
                    try { handle?.close() } catch (closeError: Exception) {
                        failure.addSuppressed(closeError)
                    }
                    directory.deleteRecursively()
                    lockPath.delete()
                    throw failure
                }
            }
        }
    }

    private fun cleanupAbandoned(root: File) {
        root.listFiles()?.forEach { directory ->
            // Includes legacy epub_<uri-hash> directories, which had no lease files.
            if (!directory.isDirectory || !directory.name.startsWith("epub_")) return@forEach
            val lockPath = File(root, directory.name + READER_LOCK_SUFFIX)
            if (!lockPath.exists()) {
                directory.deleteRecursively()
                return@forEach
            }

            try {
                val abandoned = RandomAccessFile(lockPath, "rw").use { file ->
                    val lock = try {
                        file.channel.tryLock()
                    } catch (_: OverlappingFileLockException) {
                        null // Another reader in this process is using it.
                    }
                    if (lock == null) false else {
                        lock.release()
                        true
                    }
                }
                if (abandoned) {
                    directory.deleteRecursively()
                    lockPath.delete()
                }
            } catch (_: IOException) {
                // A live reader in another process may still own this directory.
                // Keep it intact; a later open can retry recovery.
            }
        }
    }

    class Lease internal constructor(
        val directory: File,
        private val lockPath: File,
        private val handle: RandomAccessFile,
        private val lock: FileLock
    ) : Closeable {
        private var closed = false

        override fun close() {
            synchronized(monitor) {
                if (closed) return
                closed = true
                try {
                    lock.release()
                } finally {
                    try {
                        handle.close()
                    } finally {
                        directory.deleteRecursively()
                        lockPath.delete()
                    }
                }
            }
        }
    }
}
