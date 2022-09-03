package com.canhub.cropper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageView

/**
 * A basic composable wrapper for CropImageView.
 *
 * Inspired by Accompanist's WebView (https://github.com/google/accompanist/blob/main/web/src/main/java/com/google/accompanist/web/WebView.kt)
 */
@Composable
fun CropImage(
    state: CropImageState,
    modifier: Modifier = Modifier,
    controller: CropImageViewController = rememberCropImageController(),
    onCropImageComplete: (CropImageView.CropResult) -> Unit = {}
) {
    var cropView by remember { mutableStateOf<CropImageView?>(null) }

    LaunchedEffect(cropView, controller) {
        with(controller) { cropView?.handleEvents() }
    }

    AndroidView(
        factory = { context ->
            CropImageView(context).apply {
                setOnCropImageCompleteListener { _, result -> onCropImageComplete(result) }
            }.also {
                cropView = it
            }
        },
        update = { view ->
            view.updateState(state)
        },
        modifier = modifier
    )
}

private fun CropImageView.updateState(state: CropImageState) {
    when (val content = state.content) {
        is CropImageContent.Drawable -> {
            imageResource = content.drawablesRes
        }
        is CropImageContent.Uri -> {
            imageResource = 0
            setImageUriAsync(content.uri)
        }
        null -> {
            setImageUriAsync(null)
            imageResource = 0
        }
    }

    cornerShape = state.cornerShape
    scaleType = state.scaleType
    cropShape = state.cropShape
    guidelines = state.guidelines
    val ratio = state.ratio
    if (ratio == null) {
        setFixedAspectRatio(false)
    } else {
        setFixedAspectRatio(true)
        setAspectRatio(ratio.first, ratio.second)
    }
    setMultiTouchEnabled(state.multiTouch)
    setCenterMoveEnabled(state.centerMove)
    isShowCropOverlay = state.showCropOverlay
    isShowProgressBar = state.showProgressBar
    isAutoZoomEnabled = state.autoZoom
    maxZoom = state.maxZoomLevel
    isFlippedHorizontally = state.flipHorizontally
    isFlippedVertically = state.flipVertically
    isShowCropLabel = state.showCropLabel
}
