package com.jay.nyxplay.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.nyxplay.data.ACCENT_OPTIONS
import com.jay.nyxplay.data.WALLPAPER_OPTIONS
import com.jay.nyxplay.data.AppFontFamily
import com.jay.nyxplay.data.HomeLayoutStyle
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.MediaType
import com.jay.nyxplay.data.MusicCardStyle
import com.jay.nyxplay.data.PlayerSkin

@Composable
fun SettingsScreen(
    videos: List<MediaEntity>,
    contextType: MediaType,
    onBack: () -> Unit,
    onOpenPlaylists: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
            Text("Configurações", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Gerais") })
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text(if (contextType == MediaType.VIDEO) "Vídeo" else "Música") }
            )
        }

        if (tab == 0) {
            GeneralSettingsTab(viewModel)
        } else if (contextType == MediaType.VIDEO) {
            VideoIndividualSettingsTab(videos, viewModel, onOpenPlaylists)
        } else {
            AudioIndividualSettingsTab(viewModel, onOpenPlaylists)
        }
    }
}

@Composable
private fun GeneralSettingsTab(viewModel: SettingsViewModel) {
    val accentHex by viewModel.accentColorHex.collectAsState()
    val wallpaperHex by viewModel.wallpaperColorHex.collectAsState()
    val font by viewModel.fontFamily.collectAsState()
    val homeLayout by viewModel.homeLayoutStyle.collectAsState()
    val rememberSection by viewModel.rememberLastSection.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SettingsSectionTitle("Cor de destaque") }
        item {
            ColorSwatchRow(ACCENT_OPTIONS, accentHex) { viewModel.setAccentColor(it) }
        }

        item { SettingsSectionTitle("Cor do papel de parede") }
        item {
            ColorSwatchRow(WALLPAPER_OPTIONS, wallpaperHex) { viewModel.setWallpaperColor(it) }
        }

        item { SettingsSectionTitle("Tipo de letra") }
        item {
            SettingsRadioRow("Padrão", font == AppFontFamily.DEFAULT) { viewModel.setFontFamily(AppFontFamily.DEFAULT) }
        }
        item {
            SettingsRadioRow("Serifada", font == AppFontFamily.SERIF) { viewModel.setFontFamily(AppFontFamily.SERIF) }
        }
        item {
            SettingsRadioRow("Monoespaçada", font == AppFontFamily.MONOSPACE) { viewModel.setFontFamily(AppFontFamily.MONOSPACE) }
        }

        item { SettingsSectionTitle("Layout do Início") }
        item {
            SettingsRadioRow("Lista horizontal", homeLayout == HomeLayoutStyle.LISTA) { viewModel.setHomeLayoutStyle(HomeLayoutStyle.LISTA) }
        }
        item {
            SettingsRadioRow("Grelha", homeLayout == HomeLayoutStyle.GRELHA) { viewModel.setHomeLayoutStyle(HomeLayoutStyle.GRELHA) }
        }

        item { SettingsSectionTitle("Comportamento") }
        item {
            SettingsSwitchRow(
                title = "Memorizar separador ao reabrir",
                subtitle = "Volta direto a Vídeo ou Música, em vez de começar sempre em Vídeo",
                checked = rememberSection,
                onCheckedChange = { viewModel.setRememberLastSection(it) }
            )
        }
    }
}

@Composable
private fun ColorSwatchRow(options: List<com.jay.nyxplay.data.AccentOption>, selectedHex: Long, onSelect: (Long) -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        options.forEach { option ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 16.dp).clickable { onSelect(option.hex) }
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(option.hex)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedHex == option.hex) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    }
                }
                Text(option.name, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun VideoIndividualSettingsTab(videos: List<MediaEntity>, viewModel: SettingsViewModel, onOpenPlaylists: () -> Unit) {
    val excludedBuckets by viewModel.excludedVideoBuckets.collectAsState()
    val bucketNames = videos.mapNotNull { it.bucketName?.takeIf { n -> n.isNotBlank() } }.distinct().sorted()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SettingsLinkRow("Gerir playlists de vídeo", onOpenPlaylists) }
        item { SettingsSectionTitle("Biblioteca de vídeo — origens incluídas") }
        items(bucketNames) { bucket ->
            SettingsCheckRow(
                label = bucket,
                checked = !excludedBuckets.contains(bucket),
                onCheckedChange = { checked -> viewModel.setVideoBucketExcluded(bucket, !checked) }
            )
        }
    }
}

@Composable
private fun AudioIndividualSettingsTab(viewModel: SettingsViewModel, onOpenPlaylists: () -> Unit) {
    val playerSkin by viewModel.playerSkin.collectAsState()
    val cardStyle by viewModel.musicCardStyle.collectAsState()
    val pauseVolumeZero by viewModel.pauseOnVolumeZero.collectAsState()
    val pauseBluetooth by viewModel.pauseOnBluetoothDisconnect.collectAsState()
    val historyEnabled by viewModel.historyEnabled.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { SettingsLinkRow("Gerir playlists de música", onOpenPlaylists) }

        item { SettingsSectionTitle("Estilo do player") }
        item {
            SettingsRadioRow("Card — capa grande, cor sólida de fundo", playerSkin == PlayerSkin.CARD) {
                viewModel.setPlayerSkin(PlayerSkin.CARD)
            }
        }
        item {
            SettingsRadioRow("Gradient — capa desfocada em ecrã inteiro", playerSkin == PlayerSkin.GRADIENT) {
                viewModel.setPlayerSkin(PlayerSkin.GRADIENT)
            }
        }

        item { SettingsSectionTitle("Tipo de cartão da lista de músicas") }
        item {
            SettingsRadioRow("Compacto — linha fina", cardStyle == MusicCardStyle.COMPACTO) {
                viewModel.setMusicCardStyle(MusicCardStyle.COMPACTO)
            }
        }
        item {
            SettingsRadioRow("Cartão — capa maior, mais espaçado", cardStyle == MusicCardStyle.CARTAO) {
                viewModel.setMusicCardStyle(MusicCardStyle.CARTAO)
            }
        }

        item { SettingsSectionTitle("Reprodução") }
        item {
            SettingsSwitchRow(
                "Pausar quando o volume chega a zero",
                "Retoma automaticamente se subires o volume de novo",
                pauseVolumeZero
            ) { viewModel.setPauseOnVolumeZero(it) }
        }
        item {
            SettingsSwitchRow(
                "Pausar ao perder Bluetooth/auscultadores",
                null,
                pauseBluetooth
            ) { viewModel.setPauseOnBluetoothDisconnect(it) }
        }
        item {
            SettingsSwitchRow(
                "Guardar histórico de reprodução",
                "Desligar impede novas entradas em Histórico e Mais tocadas",
                historyEnabled
            ) { viewModel.setHistoryEnabled(it) }
        }
    }
}

@Composable
private fun SettingsLinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp)
        Icon(Icons.Default.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 14.sp)
            subtitle?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 13.sp)
    }
}

@Composable
private fun SettingsCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, fontSize = 14.sp)
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}
