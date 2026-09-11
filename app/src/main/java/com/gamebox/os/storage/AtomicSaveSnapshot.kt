package com.gamebox.os.storage

import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** One bounded file contains both payload and digest; publication requires atomic rename. */
internal object AtomicSaveSnapshot {
    private const val MAGIC = 0x4742534E41503031L
    private const val HEADER_BYTES = 48L

    fun hasHeader(source: File): Boolean = runCatching {
        source.isFile && source.length() >= 8L && RandomAccessFile(source, "r").use { it.readLong() == MAGIC }
    }.getOrDefault(false)

    fun write(destination: File, source: InputStream, limit: Long) {
        require(limit > 0)
        require(!Files.isSymbolicLink(destination.toPath()))
        require(!destination.exists() || destination.isFile)
        val parent = requireNotNull(destination.parentFile)
        check(parent.mkdirs() || parent.isDirectory)
        val staged = Files.createTempFile(parent.toPath(), ".snapshot-", ".part").toFile()
        try {
            RandomAccessFile(staged, "rw").use { output ->
                output.writeLong(MAGIC)
                output.writeLong(0L)
                output.write(ByteArray(32))
                val digest = MessageDigest.getInstance("SHA-256")
                var size = 0L
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    require(count.toLong() <= limit - size) { "Snapshot exceeds size limit" }
                    output.write(buffer, 0, count)
                    digest.update(buffer, 0, count)
                    size += count
                }
                require(size > 0) { "Snapshot is empty" }
                output.seek(8L)
                output.writeLong(size)
                output.write(digest.digest())
                output.fd.sync()
            }
            // Never degrade to a non-atomic replacement of the previous snapshot.
            Files.move(staged.toPath(), destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            staged.delete()
        }
    }

    /** Validate fully before returning any bytes to a restore/export caller. */
    fun read(source: File, limit: Long): ByteArray {
        require(limit in 1..Int.MAX_VALUE.toLong())
        require(!Files.isSymbolicLink(source.toPath()))
        require(source.isFile && source.length() in (HEADER_BYTES + 1)..(HEADER_BYTES + limit))
        return DataInputStream(source.inputStream()).use { input ->
            require(input.readLong() == MAGIC) { "Invalid snapshot header" }
            val size = input.readLong()
            require(size in 1..limit) { "Invalid snapshot length" }
            val expected = ByteArray(32).also(input::readFully)
            val payload = ByteArray(size.toInt()).also(input::readFully)
            require(input.read() == -1) { "Trailing snapshot bytes" }
            require(MessageDigest.isEqual(expected, MessageDigest.getInstance("SHA-256").digest(payload))) {
                "Snapshot checksum mismatch"
            }
            payload
        }
    }
}
