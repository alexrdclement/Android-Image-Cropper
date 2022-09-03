package com.canhub.cropper.compose

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Creates and remembers a [CropImageViewController] using the default [CoroutineScope] or a provided
 * override.
 */
@Composable
fun rememberCropImageController(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): CropImageViewController = remember(coroutineScope) { CropImageViewController(coroutineScope) }

/**
 * Allows for interactions with CropImageView from outside the composable.
 *
 * @see [rememberCropImageController]
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
