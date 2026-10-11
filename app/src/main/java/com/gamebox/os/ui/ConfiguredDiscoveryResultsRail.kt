package com.gamebox.os.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.gamebox.os.catalog.LibretroThumbnailClient
import com.gamebox.os.source.vimmLairCorrectedQuery
import com.gamebox.os.data.DiscoveryGame
import com.gamebox.os.source.DiscoverySourceGame

@Composable
internal fun ConfiguredDiscoveryResultsRail(
    results: List<DiscoverySourceGame>,
    allDiscoveryGames: List<DiscoveryGame>,
    onOpenResult: (DiscoverySourceGame) -> Unit,
    cardWidth: Dp,
    cardHeight: Dp,
    modifier: Modifier = Modifier,
    firstResultFocusRequester: FocusRequester? = null,
    returnFocusRequester: FocusRequester? = null,
    returnFocusKey: String? = null,
    query: String? = null,
    onUseSearchSuggestion: (String) -> Unit = {},
    headingFontSize: Int = 15,
) {
    if (results.isEmpty()) return
    val thumbnailClient = remember { LibretroThumbnailClient() }
    val resultsByConsole = remember(results) { configuredDiscoveryResultConsoleRows(results) }
    val firstResultKey = resultsByConsole.firstOrNull()?.games?.firstOrNull()
        ?.let { it.sourceId + ":" + it.externalId }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Results by console", fontSize = headingFontSize.sp, fontWeight = FontWeight.Bold)
            Text(
                "${results.size} titles",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
            )
        }
        query?.let { typedQuery ->
            val suggestion = remember(typedQuery, results) {
                vimmLairCorrectedQuery(typedQuery, results)
            }
            suggestion?.let { correctedQuery ->
                BlueprintTextButton(
                    onClick = { onUseSearchSuggestion(correctedQuery) },
                ) {
                    Text("Did you mean “$correctedQuery”?", fontSize = 11.sp)
                }
            }
        }
        resultsByConsole.forEach { consoleResults ->
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(consoleResults.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${consoleResults.games.size} ${if (consoleResults.games.size == 1) "game" else "games"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 5.dp),
            ) {
                items(consoleResults.games, key = { it.sourceId + ":" + it.externalId }) { result ->
                    val resultKey = result.sourceId + ":" + result.externalId
                    ConfiguredSourceResultCard(
                        game = result,
                        presentation = configuredSourceResultToDiscoveryGame(result, allDiscoveryGames),
                        thumbnailClient = thumbnailClient,
                        modifier = Modifier.width(cardWidth).height(cardHeight)
                            .then(
                                if (resultKey == firstResultKey) {
                                    firstResultFocusRequester?.let(Modifier::focusRequester) ?: Modifier
                                } else Modifier
                            )
                            .then(
                                if (resultKey == returnFocusKey) {
                                    returnFocusRequester?.let(Modifier::focusRequester) ?: Modifier
                                } else Modifier
                            ),
                        onClick = { onOpenResult(result) },
                    )
                }
            }
        }
    }
}
