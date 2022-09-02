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
import androidx.fragment.app.Fragment
import com.canhub.cropper.CropImageView
import com.canhub.cropper.compose.CropImage
import com.canhub.cropper.compose.CropViewOptions
import com.canhub.cropper.compose.rememberCropImageViewController

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

private val DefaultOptions = CropViewOptions(
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
            val controller = rememberCropImageViewController()
            CropImage(
                uri = cropImageUri,
                options = DefaultOptions,
                modifier = Modifier.fillMaxWidth(),
                controller = controller,
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
                        controller.croppedImageAsync()
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
