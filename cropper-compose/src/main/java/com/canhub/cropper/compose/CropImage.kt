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
    interactor: CropImageViewInteractor = rememberCropImageViewInteractor(),
    onCropImageComplete: (CropImageView.CropResult) -> Unit = {}
) {
    var cropView by remember { mutableStateOf<CropImageView?>(null) }

    LaunchedEffect(cropView, interactor) {
        with(interactor) { cropView?.handleInteractions() }
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
 * Creates and remembers a [CropImageViewInteractor] using the default [CoroutineScope] or a provided
 * override.
 */
@Composable
fun rememberCropImageViewInteractor(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): CropImageViewInteractor = remember(coroutineScope) { CropImageViewInteractor(coroutineScope) }

/**
 * Allows for interactions with CropImageView from outside the composable.
 *
 * @see [rememberCropImageViewInteractor]
 */
@Stable
class CropImageViewInteractor(private val coroutineScope: CoroutineScope) {

    private sealed class Interaction {
        data class CroppedImageAsync(
            val saveCompressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
            val saveCompressQuality: Int = 90,
            val reqWidth: Int = 0,
            val reqHeight: Int = 0,
            val options: CropImageView.RequestSizeOptions = CropImageView.RequestSizeOptions.RESIZE_INSIDE,
            val customOutputUri: Uri? = null,
        ) : Interaction()
    }

    private val interactions: MutableSharedFlow<Interaction> = MutableSharedFlow()

    internal suspend fun CropImageView.handleInteractions(): Unit = withContext(Dispatchers.Main) {
        interactions.collect { interaction ->
            when (interaction) {
                is Interaction.CroppedImageAsync -> croppedImageAsync(
                    saveCompressFormat = interaction.saveCompressFormat,
                    saveCompressQuality = interaction.saveCompressQuality,
                    reqWidth = interaction.reqWidth,
                    reqHeight = interaction.reqHeight,
                    options = interaction.options,
                    customOutputUri = interaction.customOutputUri
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
        val interaction = Interaction.CroppedImageAsync(
            saveCompressFormat = saveCompressFormat,
            saveCompressQuality = saveCompressQuality,
            reqWidth = reqWidth,
            reqHeight = reqHeight,
            options = options,
            customOutputUri = customOutputUri
        )
        coroutineScope.launch { interactions.emit(interaction) }
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
