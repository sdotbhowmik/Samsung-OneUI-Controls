package com.virtualkeys.oneui

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.virtualkeys.oneui.audio.VolStream
import com.virtualkeys.oneui.media.MediaRepository
import com.virtualkeys.oneui.media.MediaState
import com.virtualkeys.oneui.notif.ShadeCardManager
import com.virtualkeys.oneui.service.ControlTileService
import com.virtualkeys.oneui.settings.AppSettings
import com.virtualkeys.oneui.settings.SettingsStore
import com.virtualkeys.oneui.ui.PanelActivity
import com.virtualkeys.oneui.ui.theme.Accents
import com.virtualkeys.oneui.ui.theme.LocalOneUi
import com.virtualkeys.oneui.ui.theme.OneUiTheme

class MainActivity : ComponentActivity() {

    private var tick by mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            tick++
            ShadeCardManager.refreshNow()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by SettingsStore.state.collectAsState()
            val dark = when (settings.themeMode) {
                1 -> true
                2 -> false
                else -> isSystemInDarkTheme()
            }
            OneUiTheme(dark = dark, accentIndex = settings.accentIndex) {
                LaunchedEffect(dark) {
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                MainScreen(
                    tick = tick,
                    dark = dark,
                    settings = settings,
                    onUpdate = { transform ->
                        SettingsStore.update(transform)
                        tick++
                    },
                    onAddTile = ::addTile,
                    onOpenPanel = { startActivity(Intent(this, PanelActivity::class.java)) },
                    onOpenNotificationAccess = {
                        openSystemSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    },
                    onOpenAccessibility = {
                        openSystemSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
        MediaRepository.refreshAccess()
        ShadeCardManager.refreshNow()
    }

    private fun addTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val statusBar = getSystemService(StatusBarManager::class.java)
                statusBar.requestAddTileService(
                    ComponentName(this, ControlTileService::class.java),
                    getString(R.string.tile_label),
                    Icon.createWithResource(this, R.drawable.ic_tile),
                    mainExecutor,
                ) { }
                Toast.makeText(this, "Confirm the dialog to add the tile", Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
                Toast.makeText(
                    this,
                    "Open the quick panel, tap edit, then drag in the Controls tile",
                    Toast.LENGTH_LONG,
                ).show()
            }
        } else {
            Toast.makeText(
                this,
                "Open the quick panel, tap edit, then drag in the Controls tile",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private fun openSystemSettings(action: String) {
        try {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Throwable) {
            Toast.makeText(this, "Settings screen not available", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun MainScreen(
    tick: Int,
    dark: Boolean,
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onAddTile: () -> Unit,
    onOpenPanel: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalOneUi.current
    val media by MediaRepository.state.collectAsState()

    val notificationAccess = remember(tick) { Permissions.notificationAccess(context) }
    val accessibility = remember(tick) { Permissions.accessibilityEnabled(context) }
    val notifications = remember(tick) { Permissions.canPostNotifications(context) }

    val background = Brush.verticalGradient(
        colors = listOf(
            palette.background,
            palette.accent.copy(alpha = if (dark) 0.10f else 0.06f),
            palette.background,
        ),
    )

    Box(Modifier.fillMaxSize().background(background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Brush.linearGradient(listOf(Accents[0], Accents[2]))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tile),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(27.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "One UI Controls",
                        color = palette.text,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Volume & media, One UI style",
                        color = palette.secondary,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            SettingsCard {
                Text(
                    text = "Quick panel",
                    color = palette.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Add the Controls tile, then tap it in the quick panel to open the panel.",
                    color = palette.secondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Row(Modifier.padding(top = 13.dp)) {
                    Button(
                        onClick = onAddTile,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.onAccent,
                        ),
                    ) {
                        Text("Add tile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(9.dp))
                    OutlinedButton(
                        onClick = onOpenPanel,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, palette.divider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.text),
                    ) {
                        Text("Open panel", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                if (media != null) {
                    NowPlaying(media!!)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Permissions")
            SettingsCard {
                PermissionRow(
                    iconRes = R.drawable.ic_music,
                    title = "Notification access",
                    subtitle = "Read and control media playback",
                    granted = notificationAccess,
                    onClick = onOpenNotificationAccess,
                )
                DividerLine()
                PermissionRow(
                    iconRes = R.drawable.ic_volume_up,
                    title = "Accessibility service",
                    subtitle = "Show this panel instead of the volume UI",
                    granted = accessibility,
                    onClick = onOpenAccessibility,
                )
                DividerLine()
                PermissionRow(
                    iconRes = R.drawable.ic_notifications,
                    title = "Notifications",
                    subtitle = "Show the media card in the shade",
                    granted = notifications,
                    onClick = onRequestNotifications,
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Controls")
            SettingsCard {
                ToggleRow(
                    title = "Volume key panel",
                    subtitle = "Replace Samsung's volume panel with this one",
                    checked = settings.interceptVolumeKeys,
                    onCheckedChange = { value ->
                        onUpdate { it.copy(interceptVolumeKeys = value) }
                    },
                )

                if (settings.interceptVolumeKeys && !accessibility) {
                    InlineWarning(
                        text = "Turn on the accessibility service to use the volume keys",
                        onClick = onOpenAccessibility,
                    )
                }

                DividerLine()
                ToggleRow(
                    title = "Shade media card",
                    subtitle = "Volume and playback controls in the notification shade",
                    checked = settings.showShadeCard,
                    onCheckedChange = { value ->
                        onUpdate { it.copy(showShadeCard = value) }
                        if (!value) ShadeCardManager.cancel() else ShadeCardManager.refreshNow()
                    },
                )

                if (settings.showShadeCard && !notifications) {
                    InlineWarning(
                        text = "Allow notifications to show the shade card",
                        onClick = onRequestNotifications,
                    )
                }

                DividerLine()
                ToggleRow(
                    title = "Haptics",
                    subtitle = "Vibrate when a volume step changes",
                    checked = settings.haptics,
                    onCheckedChange = { value -> onUpdate { it.copy(haptics = value) } },
                )

                DividerLine()
                ToggleRow(
                    title = "Ignore keys during calls",
                    subtitle = "Leave in-call volume to Samsung",
                    checked = settings.ignoreDuringCall,
                    onCheckedChange = { value -> onUpdate { it.copy(ignoreDuringCall = value) } },
                )

                DividerLine()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Auto-dismiss",
                            color = palette.text,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = "Close the panel after inactivity",
                            color = palette.secondary,
                            fontSize = 11.sp,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3, 5, 8, 10).forEach { seconds ->
                            Chip(
                                text = "${seconds}s",
                                selected = settings.autoDismissSeconds == seconds,
                                onClick = { onUpdate { it.copy(autoDismissSeconds = seconds) } },
                            )
                        }
                    }
                }

                DividerLine()
                ToggleRow(
                    title = "Always expand streams",
                    subtitle = "Show calls, system and alarm sliders by default",
                    checked = settings.showAllStreams,
                    onCheckedChange = { value -> onUpdate { it.copy(showAllStreams = value) } },
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Appearance")
            SettingsCard {
                Text(
                    text = "Theme",
                    color = palette.text,
                    fontSize = 14.sp,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    listOf("System" to 0, "Dark" to 1, "Light" to 2).forEach { (label, mode) ->
                        Chip(
                            text = label,
                            selected = settings.themeMode == mode,
                            modifier = Modifier.weight(1f),
                            onClick = { onUpdate { it.copy(themeMode = mode) } },
                        )
                    }
                }

                DividerLine()
                Text(
                    text = "Accent",
                    color = palette.text,
                    fontSize = 14.sp,
                )
                Row(
                    Modifier.padding(top = 10.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Accents.forEachIndexed { index, color ->
                        val selected = settings.accentIndex == index
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) palette.text else palette.divider,
                                    shape = CircleShape,
                                )
                                .clickable { onUpdate { it.copy(accentIndex = index) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning),
                        contentDescription = null,
                        tint = palette.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Good to know",
                        color = palette.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "Samsung does not allow other apps to add widgets inside its quick panel, so this app places a Controls tile there and opens the panel over your screen. " +
                        "Your original volume panel comes back as soon as the volume key toggle is switched off.",
                    color = palette.secondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = "Streams shown: " + VolStream.entries.joinToString { it.label } +
                        ". Nothing leaves your phone.",
                    color = palette.secondary.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Text(
                text = "Version ${BuildConfig.VERSION_NAME} · Not affiliated with Samsung",
                color = palette.secondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp),
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalOneUi.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.card)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
private fun SectionLabel(text: String) {
    val palette = LocalOneUi.current
    Text(
        text = text,
        color = palette.secondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 8.dp, bottom = 7.dp),
    )
}

@Composable
private fun DividerLine() {
    val palette = LocalOneUi.current
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .height(1.dp)
            .background(palette.divider),
    )
}

@Composable
private fun PermissionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalOneUi.current
    val green = Color(0xFF2FBF71)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(palette.track.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = palette.secondary,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = palette.secondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (granted) green.copy(alpha = 0.16f) else palette.accent.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = if (granted) "On" else "Set up",
                color = if (granted) green else palette.accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = LocalOneUi.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = palette.text,
                fontSize = 14.sp,
            )
            Text(
                text = subtitle,
                color = palette.secondary,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.88f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.accent,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = palette.secondary,
                uncheckedTrackColor = palette.track,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun InlineWarning(text: String, onClick: () -> Unit) {
    val palette = LocalOneUi.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(palette.accent.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = palette.accent,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron),
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun Chip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalOneUi.current
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) palette.accent else palette.track)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) palette.onAccent else palette.secondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun NowPlaying(media: MediaState) {
    val palette = LocalOneUi.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(palette.track.copy(alpha = 0.55f))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music),
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = media.title,
                color = palette.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (media.artist.isNotBlank()) "${media.artist} · ${media.appLabel}" else media.appLabel,
                color = palette.secondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = if (media.isPlaying) "Playing" else "Paused",
            color = palette.secondary,
            fontSize = 10.sp,
        )
    }
}
