package com.jay.nyxplay.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.nyxplay.data.MediaEntity
import com.jay.nyxplay.data.PlayerSkin

@Composable
fun SettingsScreen(
    videos: List<MediaEntity>,
    onBack: () -> Unit,
    onOpenPlaylists: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val excludedBuckets by viewModel.excludedBuckets.collectAsState()
    val playerSkin by viewModel.playerSkin.collectAsState()

    val bucketNames = videos
        .mapNotNull { it.bucketName?.takeIf { name -> name.isNotBlank() } }
        .distinct()
        .sorted()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
            }
            Text("Configurações", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SettingsSectionTitle("Playlists") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPlaylists)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gerir playlists manuais", fontSize = 15.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            item { SettingsSectionTitle("Estilo do player de música") }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setPlayerSkin(PlayerSkin.CARD) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = playerSkin == PlayerSkin.CARD, onClick = { viewModel.setPlayerSkin(PlayerSkin.CARD) })
                    Text("Card — capa grande, cor sólida de fundo")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setPlayerSkin(PlayerSkin.GRADIENT) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = playerSkin == PlayerSkin.GRADIENT, onClick = { viewModel.setPlayerSkin(PlayerSkin.GRADIENT) })
                    Text("Gradient — capa desfocada em ecrã inteiro, gradiente dramático")
                }
            }

            item { SettingsSectionTitle("Biblioteca de vídeo — origens incluídas") }
            items(bucketNames) { bucket ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.setBucketExcluded(bucket, !excludedBuckets.contains(bucket))
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = !excludedBuckets.contains(bucket),
                        onCheckedChange = { checked -> viewModel.setBucketExcluded(bucket, !checked) }
                    )
                    Text(bucket, fontSize = 14.sp)
                }
            }
        }
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
