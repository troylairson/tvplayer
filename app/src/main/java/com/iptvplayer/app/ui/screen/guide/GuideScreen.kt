package com.iptvplayer.app.ui.screen.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.tv.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.iptvplayer.app.ui.theme.Navy950

// Phase 3 — full guide implementation.
// Phase 1 stub: fetches channel list and shows count.

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GuideScreen(
    onChannelSelected: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: GuideViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState) {
            is GuideUiState.Loading -> CircularProgressIndicator(color = Color.White)

            is GuideUiState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Failed to load channels",
                    color = Color.Red,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = state.message,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            is GuideUiState.Ready -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.channels.size} channels loaded",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                )
                Text(
                    text = "Full guide coming in Phase 3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp),
                )
                // Tap first channel to test playback
                if (state.channels.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    androidx.tv.material3.Button(
                        onClick = { onChannelSelected(state.channels.first().id) }
                    ) {
                        Text("Play: ${state.channels.first().name}")
                    }
                }
            }
        }
    }
}
