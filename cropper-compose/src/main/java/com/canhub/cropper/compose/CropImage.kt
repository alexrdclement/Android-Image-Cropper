package com.canhub.cropper.compose

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class CropImageContent {
    data class Uri(val uri: android.net.Uri) : CropImageContent()
    data class Drawable(@DrawableRes val drawablesRes: Int) : CropImageContent()
}

@Stable
class CropImageState {
    var content: CropImageContent? by mutableStateOf(null)
    var scaleType: CropImageView.ScaleType by mutableStateOf(CropImageView.ScaleType.FIT_CENTER)
    var cropShape: CropImageView.CropShape by mutableStateOf(CropImageView.CropShape.RECTANGLE)
    var cornerShape: CropImageView.CropCornerShape by mutableStateOf(CropImageView.CropCornerShape.RECTANGLE)
    var guidelines: CropImageView.Guidelines by mutableStateOf(CropImageView.Guidelines.ON)
    var ratio: Pair<Int, Int>? by mutableStateOf(1 to 1)
    var maxZoomLvl: Int by mutableStateOf(2)
    var autoZoom: Boolean by mutableStateOf(true)
    var multiTouch: Boolean by mutableStateOf(true)
    var centerMove: Boolean by mutableStateOf(true)
    var showCropOverlay: Boolean by mutableStateOf(true)
    var showProgressBar: Boolean by mutableStateOf(true)
    var flipHorizontally: Boolean by mutableStateOf(false)
    var flipVertically: Boolean by mutableStateOf(false)
    var showCropLabel: Boolean by mutableStateOf(false)
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

/**
 * A basic composable wrapper for CropImageView.
 *
 * Inspired by Accompanist's WebView (https://github.com/google/accompanist/blob/main/web/src/main/java/com/google/accompanist/web/WebView.kt)
 */
@Composable
fun CropImage(
    state: CropImageState,
    modifier: Modifier = Modifier,
    controller: CropImageViewController = rememberCropImageViewController(),
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

/**
 * Creates and remembers a [CropImageViewController] using the default [CoroutineScope] or a provided
 * override.
 */
@Composable
fun rememberCropImageViewController(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): CropImageViewController = remember(coroutineScope) { CropImageViewController(coroutineScope) }

/**
 * Allows for interactions with CropImageView from outside the composable.
 *
 * @see [rememberCropImageViewController]
 */
@Stable
class CropImageViewController(private val coroutineScope: CoroutineScope) {

    private sealed class Event {
        data class CroppedImageAsync(
            val saveCompressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
            val saveCompressQuality: Int = 90,
            val reqWidth: Int = 0,
            val reqHeight: Int = 0,
            val options: CropImageView.RequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_INSIDE,
            val customOutputUri: Uri? = null,
        ) : Event()
    }

    private val events: MutableSharedFlow<Event> = MutableSharedFlow()

    internal suspend fun CropImageView.handleEvents(): Unit = withContext(Dispatchers.Main) {
        events.collect { event ->
            when (event) {
                is Event.CroppedImageAsync -> croppedImageAsync(
                    saveCompressFormat = event.saveCompressFormat,
                    saveCompressQuality = event.saveCompressQuality,
                    reqWidth = event.reqWidth,
                    reqHeight = event.reqHeight,
                    options = event.options,
                    customOutputUri = event.customOutputUri
                )
            }
        }
    }

    fun croppedImageAsync(
        saveCompressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        saveCompressQuality: Int = 90,
        reqWidth: Int = 0,
        reqHeight: Int = 0,
        options: CropImageView.RequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_INSIDE,
        customOutputUri: Uri? = null,
    ) {
        val event = Event.CroppedImageAsync(
            saveCompressFormat = saveCompressFormat,
            saveCompressQuality = saveCompressQuality,
            reqWidth = reqWidth,
            reqHeight = reqHeight,
            options = options,
            customOutputUri = customOutputUri
        )
        coroutineScope.launch { events.emit(event) }
    }
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
    maxZoom = state.maxZoomLvl
    isFlippedHorizontally = state.flipHorizontally
    isFlippedVertically = state.flipVertically
    isShowCropLabel = state.showCropLabel
}
