package com.gamebox.os.save

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CloudSaveEnvelopeCodecTest {
    @Test fun roundTripsPayloadTimestampAndChecksum() {
        val payload = "SAVE-DATA".toByteArray()

        val decoded = CloudSaveEnvelopeCodec.decode(
            "galaxy-patrol",
            CloudSaveEnvelopeCodec.encode("galaxy-patrol", 1234L, payload),
        )

        assertEquals("galaxy-patrol", decoded.gameId)
        assertEquals(1234L, decoded.updatedAtMillis)
        assertArrayEquals(payload, decoded.payload)
        assertEquals(CloudSaveEnvelopeCodec.sha256(payload), decoded.payloadSha256)
    }

    @Test fun rejectsCrossGameTamperingAndTrailingBytes() {
        val encoded = CloudSaveEnvelopeCodec.encode("galaxy-patrol", 1L, byteArrayOf(1, 2, 3))
        assertThrows(IllegalArgumentException::class.java) {
            CloudSaveEnvelopeCodec.decode("other-game", encoded)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CloudSaveEnvelopeCodec.decode("galaxy-patrol", encoded + byteArrayOf(0))
        }
        val corrupted = encoded.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
        assertThrows(IllegalArgumentException::class.java) {
            CloudSaveEnvelopeCodec.decode("galaxy-patrol", corrupted)
        }
    }

    @Test fun enforcesRawPayloadLimitBeforeAllocation() {
        assertThrows(IllegalArgumentException::class.java) {
            CloudSaveEnvelopeCodec.encode(
                "galaxy-patrol",
                1L,
                ByteArray(CloudSaveEnvelopeCodec.MAX_RAW_PAYLOAD_BYTES + 1),
            )
        }
    }
}
