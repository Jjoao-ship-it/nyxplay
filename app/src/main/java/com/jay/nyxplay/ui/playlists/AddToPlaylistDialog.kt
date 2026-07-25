package com.jay.nyxplay.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jay.nyxplay.data.MediaType

@Composable
fun AddToPlaylistDialog(
    type: MediaType,
    mediaUid: String,
    onDismiss: () -> Unit,
    viewModel: PlaylistViewModel = viewModel()
) {
    val playlists by viewModel.observePlaylists(type).collectAsState(initial = emptyList())
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar a playlist") },
        text = {
            Column {
                playlists.forEach { playlist ->
                    Text(
                        playlist.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addMediaToPlaylist(playlist.id, mediaUid)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp)
                    )
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Ou cria uma nova…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newName.isNotBlank()) {
                    viewModel.createPlaylist(newName, type) { newId ->
                        viewModel.addMediaToPlaylist(newId, mediaUid)
                    }
                }
                onDismiss()
            }) { Text(if (newName.isNotBlank()) "Criar e adicionar" else "Fechar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
