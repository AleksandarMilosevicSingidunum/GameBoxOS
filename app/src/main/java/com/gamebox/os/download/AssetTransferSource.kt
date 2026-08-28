package com.gamebox.os.download

import android.content.res.AssetManager
import java.io.InputStream

class AssetTransferSource(
    private val assets: AssetManager,
    private val assetPath: String,
    override val totalBytes: Long?,
    override val expectedSha256: String
) : TransferSource {
    init {
        val segments = assetPath.split('/')
        require(assetPath.isNotBlank() && !assetPath.startsWith('/')) { "Asset path must be relative" }
        require('\\' !in assetPath) { "Asset path must use forward slashes" }
        require(segments.none { it.isBlank() || it == "." || it == ".." }) {
            "Asset path contains an unsafe segment"
        }
    }

    override fun openInput(): InputStream = assets.open(assetPath, AssetManager.ACCESS_STREAMING)
}
