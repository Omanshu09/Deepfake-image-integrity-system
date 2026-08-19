package com.example.deepfakedetectioncts.ui.theme.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepfakedetectioncts.ui.theme.Green
import com.example.deepfakedetectioncts.ui.theme.Red
import com.example.deepfakedetectioncts.ui.theme.Blue

@Composable
fun HistoryScreen(captureEnabled: Boolean, viewModel: HistoryViewModel = viewModel()) {

    val screenshots by viewModel.screenshots.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val expandedScreenshot by viewModel.expandedScreenshot.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadScreenshots()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!captureEnabled) {
                Text(
                    text = "Enable the capture bubble to capture",
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (screenshots.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 70.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No Recent",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                HistoryGrid(screenshots = screenshots, viewModel = viewModel)
            }
        }

        AnimatedVisibility(
            visible = expandedScreenshot != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { viewModel.dismissDetailedView() },
                contentAlignment = Alignment.Center
            ) {
                expandedScreenshot?.let {
                    DetailedScreenshotCard(screenshotItem = it)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryGrid(screenshots: List<ScreenshotItem>, viewModel: HistoryViewModel) {
    val selectedScreenshots by viewModel.selectedScreenshots.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(screenshots) { screenshotItem ->
            val isSelected = selectedScreenshots.contains(screenshotItem)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { viewModel.onScreenshotClicked(screenshotItem) },
                        onLongClick = { viewModel.toggleSelection(screenshotItem) }
                    )
            ) {
                ScreenshotCard(screenshotItem = screenshotItem)
                if (isSelected) {
                    Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f)))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = Green,
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenshotCard(screenshotItem: ScreenshotItem, modifier: Modifier = Modifier) {
    val resultText: String
    val resultColor: Color

    when {
        screenshotItem.result.contains("Deepfake", ignoreCase = true) -> {
            resultText = "DEEPFAKE"
            resultColor = Red
        }
        screenshotItem.result.contains("Synthetic", ignoreCase = true) || screenshotItem.result.contains("AI Generated", ignoreCase = true) -> {
            resultText = "SYNTHETIC"
            resultColor = Blue
        }
        else -> {
            resultText = "AUTHENTIC"
            resultColor = Green
        }
    }

    Card(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = screenshotItem.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = resultColor,
                modifier = Modifier.padding(vertical = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DetailedScreenshotCard(screenshotItem: ScreenshotItem) {
    val resultColor = when {
        screenshotItem.result.contains("Deepfake", ignoreCase = true) -> Red
        screenshotItem.result.contains("Synthetic", ignoreCase = true) || screenshotItem.result.contains("AI Generated", ignoreCase = true) -> Blue
        else -> Green
    }

    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.8f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = screenshotItem.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = screenshotItem.result,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = resultColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan History Entry",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (screenshotItem.explanation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AI Explanation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = screenshotItem.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
