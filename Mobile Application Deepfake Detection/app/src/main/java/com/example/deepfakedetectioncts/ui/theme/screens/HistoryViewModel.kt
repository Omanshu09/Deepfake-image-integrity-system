package com.example.deepfakedetectioncts.ui.theme.screens

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ScreenshotItem(
    val file: File, 
    val bitmap: Bitmap, 
    val result: String = "Authentic",
    val explanation: String? = null
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val _screenshots = MutableStateFlow<List<ScreenshotItem>>(emptyList())
    val screenshots = _screenshots.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _selectedScreenshots = MutableStateFlow<Set<ScreenshotItem>>(emptySet())
    val selectedScreenshots = _selectedScreenshots.asStateFlow()

    private val _expandedScreenshot = MutableStateFlow<ScreenshotItem?>(null)
    val expandedScreenshot = _expandedScreenshot.asStateFlow()

    fun onScreenshotClicked(item: ScreenshotItem) {
        if (_selectedScreenshots.value.isNotEmpty()) {
            toggleSelection(item)
        } else {
            _expandedScreenshot.value = item
        }
    }

    fun dismissDetailedView() {
        _expandedScreenshot.value = null
    }

    fun loadScreenshots() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val imageList = mutableListOf<ScreenshotItem>()
            val app = getApplication<Application>()
            val dir = app.getDir("screenshots", Context.MODE_PRIVATE)
            if (dir.exists()) {
                val files = dir.listFiles { file -> file.extension == "png" }
                if (files != null) {
                    files.sortByDescending { it.lastModified() }
                    for (file in files) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (bitmap != null) {
                                // Load result
                                val resultFile = File(file.parent, file.nameWithoutExtension + ".txt")
                                val result = if (resultFile.exists()) resultFile.readText() else "Authentic"
                                
                                // Load explanation
                                val explanationFile = File(file.parent, file.nameWithoutExtension + ".explanation")
                                val explanation = if (explanationFile.exists()) explanationFile.readText() else null
                                
                                imageList.add(ScreenshotItem(file, bitmap, result, explanation))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            _screenshots.value = imageList
            _isLoading.value = false
        }
    }

    fun toggleSelection(item: ScreenshotItem) {
        val currentSelection = _selectedScreenshots.value.toMutableSet()
        if (currentSelection.contains(item)) {
            currentSelection.remove(item)
        } else {
            currentSelection.add(item)
        }
        _selectedScreenshots.value = currentSelection
    }

    fun deleteSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedScreenshots.value.forEach { 
                it.file.delete()
                File(it.file.parent, it.file.nameWithoutExtension + ".txt").delete()
                File(it.file.parent, it.file.nameWithoutExtension + ".explanation").delete()
            }
            _selectedScreenshots.value = emptySet()
            loadScreenshots()
        }
    }

    fun clearSelection() {
        _selectedScreenshots.value = emptySet()
    }

    fun clearRecents() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val dir = app.getDir("screenshots", Context.MODE_PRIVATE)
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
            loadScreenshots()
        }
    }
}
