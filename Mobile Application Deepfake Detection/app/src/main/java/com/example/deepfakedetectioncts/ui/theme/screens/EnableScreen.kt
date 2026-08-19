package com.example.deepfakedetectioncts.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EnableScreen() {

    var enabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Enable Bubble Mode")

        Spacer(modifier = Modifier.height(20.dp))

        Switch(
            checked = enabled,
            onCheckedChange = { enabled = it }
        )
    }
}
