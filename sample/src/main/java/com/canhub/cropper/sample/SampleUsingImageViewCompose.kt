package com.canhub.cropper.sample

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
            CropImage(
                uri = cropImageUri,
                options = DefaultOptions,
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
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
                        // TODO
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
internal fun CropImage(
    uri: Uri?,
    options: SampleOptionsEntity,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            CropImageView(context)
        },
        update = { view ->
            view.setOptions(options)
            if (uri != null) {
                view.setImageUriAsync(uri)
            } else {
                view.imageResource = R.drawable.cat_small
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
