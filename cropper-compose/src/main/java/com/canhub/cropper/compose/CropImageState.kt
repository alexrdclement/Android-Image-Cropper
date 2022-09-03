package com.canhub.cropper.compose

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.canhub.cropper.CropImageView

@Stable
class CropImageState {
    var content: CropImageContent? by mutableStateOf(null)
    var scaleType: CropImageView.ScaleType by mutableStateOf(CropImageView.ScaleType.FIT_CENTER)
    var cropShape: CropImageView.CropShape by mutableStateOf(CropImageView.CropShape.RECTANGLE)
    var cornerShape: CropImageView.CropCornerShape by mutableStateOf(CropImageView.CropCornerShape.RECTANGLE)
    var guidelines: CropImageView.Guidelines by mutableStateOf(CropImageView.Guidelines.ON)
    var ratio: Pair<Int, Int>? by mutableStateOf(1 to 1)
    var maxZoomLevel: Int by mutableStateOf(2)
    var autoZoom: Boolean by mutableStateOf(true)
    var multiTouch: Boolean by mutableStateOf(true)
    var centerMove: Boolean by mutableStateOf(true)
    var showCropOverlay: Boolean by mutableStateOf(true)
    var showProgressBar: Boolean by mutableStateOf(true)
    var flipHorizontally: Boolean by mutableStateOf(false)
    var flipVertically: Boolean by mutableStateOf(false)
    var showCropLabel: Boolean by mutableStateOf(false)
}

sealed class CropImageContent {
    data class Uri(val uri: android.net.Uri) : CropImageContent()
    data class Drawable(@DrawableRes val drawablesRes: Int) : CropImageContent()
}

@Composable
fun rememberCropImageState(uri: Uri): CropImageState = remember(uri) {
    CropImageState().apply {
        content = CropImageContent.Uri(uri)
    }
}

@Composable
fun rememberCropImageState(@DrawableRes drawableRes: Int): CropImageState = remember(drawableRes) {
    CropImageState().apply {
        content = CropImageContent.Drawable(drawableRes)
    }
}
