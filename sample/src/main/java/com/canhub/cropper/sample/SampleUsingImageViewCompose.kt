package com.canhub.cropper.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.canhub.cropper.compose.CropImage
import com.canhub.cropper.compose.CropImageContent
import com.canhub.cropper.compose.rememberCropImageState
import com.canhub.cropper.compose.rememberCropImageController
import com.example.croppersample.R
import com.google.android.material.composethemeadapter.MdcTheme

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
                MdcTheme {
                    SampleUsingImageViewContent()
                }
            }
        }
    }
}

@Composable
fun SampleUsingImageViewContent() {
    Surface {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val state = rememberCropImageState(drawableRes = R.drawable.cat)
            val openPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                state.content = uri?.let { CropImageContent.Uri(uri) }
            }
            val controller = rememberCropImageController()
            CropImage(
                state = state,
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                onCropImageComplete = { result ->
                    state.content = result.uriContent?.let { CropImageContent.Uri(it) }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
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
