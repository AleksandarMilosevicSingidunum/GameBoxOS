package com.gamebox.os.storage

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.io.InputStream

class AtomicSaveSnapshotTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun replacementIsReadableByANewReader() {
        val target = temp.root.resolve("save.snapshot")
        AtomicSaveSnapshot.write(target, "OLD".byteInputStream(), 8)
        AtomicSaveSnapshot.write(target, "NEW".byteInputStream(), 8)
        assertEquals("NEW", AtomicSaveSnapshot.read(target, 8).toString(Charsets.UTF_8))
        assertEquals(listOf("save.snapshot"), temp.root.list()!!.toList())
    }

    @Test fun emptyOversizedAndInterruptedWritesKeepPreviousSnapshot() {
        val target = temp.root.resolve("save.snapshot")
        AtomicSaveSnapshot.write(target, "GOOD".byteInputStream(), 8)
        val before = target.readBytes()
        for (source in listOf("".byteInputStream(), "TOO LARGE".byteInputStream(), object : InputStream() {
            var count = 0
            override fun read(): Int = if (count++ == 0) 42 else throw IOException("disconnected")
        })) {
            assertThrows(Exception::class.java) { AtomicSaveSnapshot.write(target, source, 8) }
            assertArrayEquals(before, target.readBytes())
            assertEquals("GOOD", AtomicSaveSnapshot.read(target, 8).toString(Charsets.UTF_8))
            assertEquals(listOf("save.snapshot"), temp.root.list()!!.toList())
        }
    }

    @Test fun corruptedAndTruncatedSnapshotsNeverReturnPayload() {
        val target = temp.root.resolve("save.snapshot")
        AtomicSaveSnapshot.write(target, "GOOD".byteInputStream(), 8)
        val valid = target.readBytes()
        val corrupt = valid.copyOf().apply { this[lastIndex] = 0 }
        for (bytes in listOf(corrupt, valid.copyOf(20), valid + byteArrayOf(1))) {
            target.writeBytes(bytes)
            assertThrows(Exception::class.java) { AtomicSaveSnapshot.read(target, 8) }
        }
    }
}
