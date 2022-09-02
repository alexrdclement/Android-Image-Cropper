package com.canhub.cropper.compose

import android.graphics.Bitmap
import android.net.Uri
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

data class CropViewOptions(
    val scaleType: CropImageView.ScaleType,
    val cropShape: CropImageView.CropShape,
    val cornerShape: CropImageView.CropCornerShape,
    val guidelines: CropImageView.Guidelines,
    val ratio: Pair<Int, Int>?,
    val maxZoomLvl: Int,
    val autoZoom: Boolean,
    val multiTouch: Boolean,
    val centerMove: Boolean,
    val showCropOverlay: Boolean,
    val showProgressBar: Boolean,
    val flipHorizontally: Boolean,
    val flipVertically: Boolean,
    val showCropLabel: Boolean
)

/**
 * A basic composable wrapper for CropImageView.
 *
 * Inspired by Accompanist's WebView (https://github.com/google/accompanist/blob/main/web/src/main/java/com/google/accompanist/web/WebView.kt)
 */
@Composable
fun CropImage(
    uri: Uri?,
    options: CropViewOptions,
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
            view.setOptions(options)
            if (uri != null) {
                view.setImageUriAsync(uri)
            }
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

private fun CropImageView.setOptions(options: CropViewOptions) {
    cornerShape = options.cornerShape
    scaleType = options.scaleType
    cropShape = options.cropShape
    guidelines = options.guidelines
    if (options.ratio == null) {
        setFixedAspectRatio(false)
    } else {
        setFixedAspectRatio(true)
        setAspectRatio(options.ratio.first, options.ratio.second)
    }
    setMultiTouchEnabled(options.multiTouch)
    setCenterMoveEnabled(options.centerMove)
    isShowCropOverlay = options.showCropOverlay
    isShowProgressBar = options.showProgressBar
    isAutoZoomEnabled = options.autoZoom
    maxZoom = options.maxZoomLvl
    isFlippedHorizontally = options.flipHorizontally
    isFlippedVertically = options.flipVertically
    isShowCropLabel = options.showCropLabel
}
