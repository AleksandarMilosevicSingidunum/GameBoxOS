package com.gamebox.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class InstallState {
    NOT_INSTALLED, QUEUED, DOWNLOADING, PAUSED, VERIFYING,
    INSTALLING, INSTALLED, UPDATE_AVAILABLE, MISSING_FILES, FAILED
}

data class Game(
    val id: String,
    val title: String,
    val platform: String,
    val state: InstallState,
    val lastPlayed: String? = null,
    val minutesPlayed: Int = 0
)

private val fixtures = listOf(
    Game("celeste", "Celeste Classic", "Homebrew", InstallState.INSTALLED, "Today", 180),
    Game("cave-story", "Cave Story", "Retro", InstallState.INSTALLED, "Yesterday", 95),
    Game("openarena", "OpenArena", "Android", InstallState.INSTALLED),
    Game("supertuxkart", "SuperTuxKart", "Android", InstallState.UPDATE_AVAILABLE),
    Game("freedoom", "Freedoom", "Homebrew", InstallState.NOT_INSTALLED),
    Game("luanti", "Luanti", "Android", InstallState.QUEUED),
    Game("openmw", "OpenMW", "Source Port", InstallState.PAUSED),
    Game("retro-test", "Retro Test Suite", "Retro", InstallState.VERIFYING)
)

private val colors = darkColorScheme(
    primary = Color(0xFF5B8CFF),
    secondary = Color(0xFF9A6BFF),
    background = Color(0xFF080B12),
    surface = Color(0xFF141927),
    onBackground = Color(0xFFF4F7FF),
    onSurface = Color(0xFFF4F7FF)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = colors) { GameBoxApp() } }
    }
}

private enum class Destination(val title: String) { HOME("Home"), LIBRARY("Library"), STORE("Store") }

@Composable
fun GameBoxApp() {
    var destination by remember { mutableStateOf(Destination.HOME) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("GAMEBOX", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black,
                fontSize = 28.sp, modifier = Modifier.padding(end = 28.dp, top = 8.dp))
            Destination.entries.forEach { item ->
                Button(
                    onClick = { destination = item },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item == destination) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                ) { Text(item.title) }
            }
        }
        Spacer(Modifier.height(30.dp))
        when (destination) {
            Destination.HOME -> HomeScreen()
            Destination.LIBRARY -> CollectionScreen(
                "Your Library", "Installed and ready offline",
                fixtures.filter { it.state == InstallState.INSTALLED || it.state == InstallState.UPDATE_AVAILABLE }
            )
            Destination.STORE -> CollectionScreen(
                "Authorized Catalog", "Homebrew, freeware, and configured personal sources", fixtures
            )
        }
        Spacer(Modifier.weight(1f))
        Text("A Select    B Back    LB/RB Tabs    Menu Options",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 14.sp)
    }
}

@Composable
private fun HomeScreen() {
    val hero = fixtures.first()
    Column {
        Text("Good evening", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Text("Ready to play?", fontSize = 42.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(26.dp))
        GameCard(hero, Modifier.fillMaxWidth().height(190.dp), hero = true)
        Spacer(Modifier.height(26.dp))
        Text("Recently played", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        GameRow(fixtures.filter { it.lastPlayed != null })
    }
}

@Composable
private fun CollectionScreen(title: String, subtitle: String, games: List<Game>) {
    Column {
        Text(title, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        Spacer(Modifier.height(24.dp))
        GameRow(games)
    }
}

@Composable
private fun GameRow(games: List<Game>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(games, key = { it.id }) { GameCard(it, Modifier.width(230.dp).height(170.dp)) }
    }
}

@Composable
private fun GameCard(game: Game, modifier: Modifier, hero: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) MaterialTheme.colorScheme.primary else Color.Transparent)
    Surface(
        modifier.onFocusChanged { focused = it.isFocused }.clickable { }.focusable(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (focused) 3.dp else 1.dp, border),
        tonalElevation = if (focused) 8.dp else 2.dp
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(if (hero) "CONTINUE PLAYING" else game.platform.uppercase(),
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text(game.title, fontSize = if (hero) 32.sp else 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if (hero) "Press A to resume" else stateLabel(game.state), fontSize = 13.sp)
        }
    }
}

private fun stateLabel(state: InstallState) = state.name.lowercase().replace('_', ' ')
