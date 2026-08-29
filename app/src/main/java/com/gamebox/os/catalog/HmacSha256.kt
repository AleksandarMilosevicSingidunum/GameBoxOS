package com.gamebox.os.catalog

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal object HmacSha256 {
    fun digest(key: ByteArray, value: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(value.toByteArray(Charsets.UTF_8))
    }
    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}