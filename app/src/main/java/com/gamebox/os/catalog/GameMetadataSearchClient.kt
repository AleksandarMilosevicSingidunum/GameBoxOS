package com.gamebox.os.catalog

import com.gamebox.os.domain.Game

data class MetadataMatchCandidate(
    val externalId: String,
    val title: String,
    val platform: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val description: String? = null,
    val artworkUrl: String? = null,
    val provider: String,
) {
    init {
        require(externalId.isNotBlank() && externalId.length <= 80)
        require(title.isNotBlank() && title.length <= 240)
        require(year == null || year in 1900..2100)
        require(description == null || description.length <= 4_000)
    }
}

fun interface GameMetadataCandidateProvider {
    suspend fun findCandidates(game: Game): List<MetadataMatchCandidate>
}

/** Shows independent exact-match sources so a user can choose text or artwork. */
class GameMetadataSearchClient(private val providers: List<GameMetadataCandidateProvider>) {
    suspend fun findCandidates(game: Game): List<MetadataMatchCandidate> =
        providers.flatMap { it.findCandidates(game) }
            .distinctBy { it.provider to it.externalId }
}
