package com.canhub.cropper.sample

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.canhub.cropper.CropImageView
import com.canhub.cropper.sample.options_dialog.SampleOptionsEntity
import com.example.croppersample.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SampleUsingImageViewCompose : Fragment() {

    companion object {
        fun newInstance() = SampleUsingImageViewCompose()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    SampleUsingImageViewContent()
                }
            }
        }
    }
}

private val DefaultOptions = SampleOptionsEntity(
    scaleType = CropImageView.ScaleType.FIT_CENTER,
    cropShape = CropImageView.CropShape.RECTANGLE,
    cornerShape = CropImageView.CropCornerShape.RECTANGLE,
    guidelines = CropImageView.Guidelines.ON,
    ratio = Pair(1, 1),
    autoZoom = true,
    maxZoomLvl = 2,
    multiTouch = true,
    centerMove = true,
    showCropOverlay = true,
    showProgressBar = true,
    flipHorizontally = false,
    flipVertically = false,
    showCropLabel = false
)

@Composable
fun SampleUsingImageViewContent() {
    Surface {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            var cropImageUri: Uri? by remember { mutableStateOf(null) }
            val openPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                cropImageUri = uri
            }
            val interactor = rememberCropImageViewInteractor()
            CropImage(
                uri = cropImageUri,
                options = DefaultOptions,
                modifier = Modifier.fillMaxWidth(),
                interactor = interactor,
                onCropImageComplete = {
                    cropImageUri = it.uriContent
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectImageButton(
                    onClick = {
                        openPicker.launch("image/*")
                    }
                )
                CropButton(
                    onClick = {
                        interactor.croppedImageAsync()
                    }
                )
            }
        }
    }
}

@Composable
fun SelectImageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text("Select Image")
    }
}

@Composable
fun CropButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text("Crop")
    }
}

@Composable
fun rememberCropImageViewInteractor(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): CropImageViewInteractor = remember(coroutineScope) { CropImageViewInteractor(coroutineScope) }

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

    internal suspend fun CropImageView.handleInteractionEvents(): Unit = withContext(Dispatchers.Main) {
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

@Composable
internal fun CropImage(
    uri: Uri?,
    options: SampleOptionsEntity,
    modifier: Modifier = Modifier,
    interactor: CropImageViewInteractor = rememberCropImageViewInteractor(),
    onCropImageComplete: (CropImageView.CropResult) -> Unit = {}
) {
    var cropView by remember { mutableStateOf<CropImageView?>(null) }

    LaunchedEffect(cropView, interactor) {
        with(interactor) { cropView?.handleInteractionEvents() }
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
            } else {
                view.imageResource = R.drawable.cat
            }
        },
        modifier = modifier
    )
}

private fun CropImageView.setOptions(options: SampleOptionsEntity) {
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
