package com.gamebox.os.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CatalogProvider {
    suspend fun load(): CatalogSnapshot
}

class AssetCatalogProvider(
    private val context: Context,
    private val parser: CatalogParser = CatalogParser(),
    private val assetPath: String = "catalog/authorized-fixture.json"
) : CatalogProvider {
    override suspend fun load(): CatalogSnapshot = withContext(Dispatchers.IO) {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        parser.parse(text)
    }
}
