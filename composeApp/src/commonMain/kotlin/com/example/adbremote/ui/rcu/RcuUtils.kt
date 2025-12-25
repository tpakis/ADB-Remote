package com.example.adbremote.ui.rcu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.adbremote.ui.icons.BootstrapYoutube
import com.example.adbremote.ui.icons.TablerBrandAmazon
import com.example.adbremote.ui.icons.TablerBrandNetflix

// Icon sizes
val iconSmall: Dp = 16.dp
val iconMedium: Dp = 24.dp
val iconBig: Dp = 32.dp

// Background color for buttons
@Composable
fun backgroundColor(): Color = Color(0xFF3A3A3A)

// Main control keys
val backKey = RcuKeyInfo(
    tag = Tag.BACK,
    icon = Icons.Default.ArrowBack,
    title = "Back"
)

val homeKey = RcuKeyInfo(
    tag = Tag.HOME,
    icon = Icons.Default.Home,
    title = "Home"
)

val settingsKey = RcuKeyInfo(
    tag = Tag.SETTINGS,
    icon = Icons.Default.Settings,
    title = "Settings"
)

// Deeplinks control keys
val appsKey = RcuKeyInfo(
    tag = Tag.APPS,
    icon = Icons.Default.Apps,
    title = "Apps"
)

val recordingsKey = RcuKeyInfo(
    tag = Tag.RECORDINGS,
    icon = Icons.Default.VideoLibrary,
    title = "Recordings",
)

val searchKey = RcuKeyInfo(
    tag = Tag.SEARCH,
    icon = Icons.Default.Search,
    title = "Search"
)

val infoKey = RcuKeyInfo(
    tag = Tag.INFO,
    icon = Icons.Default.Info,
    title = "Info"
)

// Guide row keys
val guideKey = RcuKeyInfo(
    tag = Tag.GUIDE,
    icon = Icons.Default.List,
    title = "Guide"
)

val menuKey = RcuKeyInfo(
    tag = Tag.MENU,
    icon = Icons.Default.MoreVert,
    title = "Menu"
)

val assistantKey = RcuKeyInfo(
    tag = Tag.ASSISTANT,
    icon = Icons.Filled.Mic,
    title = "Assistant"
)

val powerKey = RcuKeyInfo(
    tag = Tag.POWER,
    icon = Icons.Default.PowerSettingsNew,
    title = "Power",
)

// Numpad special keys
val teletextKey = RcuKeyInfo(
    tag = Tag.TELETEXT,
    icon = Icons.Default.TextFields,
    title = "Teletext"
)

val previousKey = RcuKeyInfo(
    tag = Tag.PREVIOUS,
    icon = Icons.Default.Refresh,
    title = "Previous"
)

// HbbTV color keys
val hbbtvKeys = listOf(
    RcuKeyInfo(tag = Tag.RED, icon = Icons.Filled.HorizontalRule, title = "Red", tintColor = Color.Red),
    RcuKeyInfo(tag = Tag.GREEN, icon = Icons.Filled.HorizontalRule, title = "Green", tintColor = Color.Green),
    RcuKeyInfo(tag = Tag.YELLOW, icon = Icons.Filled.HorizontalRule, title = "Yellow", tintColor = Color.Yellow),
    RcuKeyInfo(tag = Tag.BLUE, icon = Icons.Filled.HorizontalRule, title = "Blue", tintColor = Color.Blue)
)

// Playback control keys
val playbackKeys = listOf(
    RcuKeyInfo(tag = Tag.REWIND, icon = Icons.Filled.FastRewind, title = "Rewind"),
    RcuKeyInfo(tag = Tag.PLAY_PAUSE, icon = Icons.Default.PlayArrow, title = "Play/Pause"),
    RcuKeyInfo(tag = Tag.FAST_FORWARD, icon = Icons.Filled.FastForward, title = "Fast Forward"),
    RcuKeyInfo(tag = Tag.STOP, icon = Icons.Filled.Stop, title = "Stop"),
    RcuKeyInfo(tag = Tag.PREVIOUS, icon = Icons.Filled.SkipPrevious, title = "Previous"),
    RcuKeyInfo(tag = Tag.NEXT, icon = Icons.Filled.SkipNext, title = "Next"),
    RcuKeyInfo(tag = Tag.RECORD, icon = Icons.Filled.Circle, title = "Record", tintColor = Color.Red),
    RcuKeyInfo(tag = Tag.CAPTIONS, icon = Icons.Default.ClosedCaption, title = "Captions")
)

// External app keys
val externalAppsKeys = listOf(
    RcuKeyInfo(tag = Tag.NETFLIX, icon = TablerBrandNetflix, title = "Netflix", tintColor = Color.Red),
    RcuKeyInfo(tag = Tag.YOUTUBE, icon = BootstrapYoutube, title = "YouTube", tintColor = Color.Red),
    RcuKeyInfo(tag = Tag.PRIME_VIDEO, icon = TablerBrandAmazon, title = "Prime", tintColor = Color.Cyan)
)
