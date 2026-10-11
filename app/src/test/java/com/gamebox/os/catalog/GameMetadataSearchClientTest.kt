package com.gamebox.os.catalog

import com.gamebox.os.domain.Game
import com.gamebox.os.domain.GameId
import com.gamebox.os.domain.InstallState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GameMetadataSearchClientTest {
    private val game = Game(
        id = GameId("local-1"), title = "Galaxy Patrol", platform = "PSP",
        year = 0, genre = "", sizeMb = 1, state = InstallState.INSTALLED,
    )

    @Test
    fun fallsBackWhenPrimaryProviderHasNoMatch() = runBlocking {
        val requests = mutableListOf<String>()
        val client = GameMetadataSearchClient(
            listOf(
                GameMetadataCandidateProvider { requests += "primary"; emptyList() },
                GameMetadataCandidateProvider {
                    requests += "fallback"
                    listOf(MetadataMatchCandidate("Q123", "Galaxy Patrol", provider = "WIKIDATA"))
                },
            ),
        )

        val matches = client.findCandidates(game)

        assertEquals(listOf("primary", "fallback"), requests)
        assertEquals("WIKIDATA", matches.single().provider)
    }

    @Test
    fun includesCandidatesFromIndependentProviders() = runBlocking {
        val requests = mutableListOf<String>()
        val client = GameMetadataSearchClient(
            listOf(
                GameMetadataCandidateProvider {
                    requests += "primary"
                    listOf(MetadataMatchCandidate("42", "Galaxy Patrol", provider = "TEST"))
                },
                GameMetadataCandidateProvider {
                    requests += "fallback"
                    listOf(MetadataMatchCandidate("Q123", "Galaxy Patrol", provider = "WIKIDATA"))
                },
            ),
        )

        assertEquals(listOf("TEST", "WIKIDATA"), client.findCandidates(game).map { it.provider })
        assertEquals(listOf("primary", "fallback"), requests)
    }
}
