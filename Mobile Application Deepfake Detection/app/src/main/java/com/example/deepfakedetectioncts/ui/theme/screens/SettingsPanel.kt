package com.example.deepfakedetectioncts.ui.theme.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.example.deepfakedetectioncts.R
import com.example.deepfakedetectioncts.ScreenshotPermissionActivity
import com.example.deepfakedetectioncts.ui.theme.Green
import com.example.deepfakedetectioncts.ui.theme.bubble.BubbleService

@Composable
fun SettingsPanel(onCaptureToggled: (Boolean) -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val activity = context as? Activity
    
    var captureEnabled by remember { mutableStateOf(BubbleService.isRunning) }
    var detectionEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("detection_enabled", true)) }
    var explanationEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("explanation_enabled", false)) }
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Deepfake", style = MaterialTheme.typography.headlineSmall)
                Text("Detector", style = MaterialTheme.typography.headlineSmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))

        // Enable for Capture (Bubble)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable for capture", style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = captureEnabled,
                onCheckedChange = {
                    captureEnabled = it
                    onCaptureToggled(it)
                    if (it) {
                        if (activity != null && !Settings.canDrawOverlays(activity)) {
                            activity.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package:${activity.packageName}".toUri()
                                )
                            )
                        } else {
                            activity?.startActivity(Intent(activity, ScreenshotPermissionActivity::class.java))
                        }
                    } else {
                        activity?.stopService(Intent(activity, BubbleService::class.java))
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Green)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        // Detection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Detection", style = MaterialTheme.typography.titleMedium)
                Text("Send selected images to model", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = detectionEnabled,
                onCheckedChange = { 
                    detectionEnabled = it
                    sharedPrefs.edit().putBoolean("detection_enabled", it).apply()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Green)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))

        // Explanation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text("Explanation", style = MaterialTheme.typography.titleMedium)
                Text("Why image is deepfake", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = explanationEnabled,
                onCheckedChange = { 
                    explanationEnabled = it
                    sharedPrefs.edit().putBoolean("explanation_enabled", it).apply()
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Green)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // About Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAboutDialog = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Info, contentDescription = "About")
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text("About")
        }

        Text(
            text = "App Version 1.0",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hello user, this application has been developed by the project group led by:\n- Deepanshu Singh Shahi\n- Omanshu Bhatt\n- Devansh Gupta\nUnder the guidance of \nDr. Sunita Jalal Ma'am \nCollege of Technology \nGBPUAT Pantnagar .",
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}