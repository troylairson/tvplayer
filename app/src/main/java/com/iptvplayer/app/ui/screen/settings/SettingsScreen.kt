package com.iptvplayer.app.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.tv.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.iptvplayer.app.ui.theme.Navy950
import com.iptvplayer.app.ui.theme.Red400

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onConnected: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val connectState by viewModel.connectState.collectAsState()
    val buttonFocus = remember { FocusRequester() }

    var serverUrl by remember { mutableStateOf("") }
    var username  by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }

    // Sole navigation trigger on success
    LaunchedEffect(connectState) {
        if (connectState is ConnectState.Success) onConnected()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(480.dp),
        ) {
            Text(
                text = "IPTV Player",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
            )
            Text(
                text = "Enter your Xtream Codes credentials",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it; viewModel.resetState() },
                label = { Text("Server URL") },
                placeholder = { Text("http://your-provider.com:8080") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; viewModel.resetState() },
                label = { Text("Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; viewModel.resetState() },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (connectState is ConnectState.Error) {
                Text(
                    text = (connectState as ConnectState.Error).message,
                    color = Red400,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(8.dp))

            LaunchedEffect(Unit) { buttonFocus.requestFocus() }

            Button(
                onClick = { viewModel.connect(serverUrl, username, password) },
                enabled = connectState !is ConnectState.Loading,
                modifier = Modifier
                    .focusRequester(buttonFocus)
                    .width(200.dp)
                    .height(52.dp),
            ) {
                if (connectState is ConnectState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text("Connect")
                }
            }
        }
    }
}
