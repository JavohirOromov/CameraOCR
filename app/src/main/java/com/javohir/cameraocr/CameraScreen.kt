package com.javohir.cameraocr

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/**
 * Created by: Javohir Oromov macos
 * Project: CameraOCR
 * Package: com.javohir.cameraocr.ui.theme
 * Description: Camera Preview
 */

@Composable
fun CameraScreen(
    onBack: () -> Unit,
    showCaptureButton: Boolean = false,
    captureMode: CaptureMode = CaptureMode.NONE,
    onBitmapCaptured: (Bitmap) -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
){

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var useFrontCamera by remember { mutableStateOf(false) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraSelector = if (useFrontCamera){
        CameraSelector.DEFAULT_FRONT_CAMERA
    }else{
        CameraSelector.DEFAULT_BACK_CAMERA
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                val cameraProviderFeature = ProcessCameraProvider.getInstance(context)
                cameraProviderFeature.addListener({
                    val cameraProvider = cameraProviderFeature.get()
                    val preview = Preview.Builder().build()
                    preview.surfaceProvider = previewView.surfaceProvider
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                }, ContextCompat.getMainExecutor(context))
            }
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack, contentDescription = "orqaga"
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {useFrontCamera = !useFrontCamera},
                modifier = modifier.padding(horizontal = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,contentDescription = "Kamera aylantirish"
                )
            }

            if (showCaptureButton){
                IconButton(
                    onClick = {
                        when(captureMode){
                            CaptureMode.RASM_OLISH ->
                                takePhoto(context,imageCapture, onSaved = { uri ->
                                    Toast.makeText(context, uri, Toast.LENGTH_SHORT).show()
                                }, onFailure = { failure ->
                                    Toast.makeText(context, failure, Toast.LENGTH_SHORT).show()

                                })
                            CaptureMode.BITMAP_OLISH -> {
                                takeBitmap(
                                    context = context,
                                    imageCapture = imageCapture,
                                    onBitmapCaptured = { bitmap ->
                                        onBitmapCaptured(bitmap)
                                        Toast.makeText(context, "Bitmap olindi", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    },
                                    onFailure = { failure ->
                                        Toast.makeText(context, failure, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            CaptureMode.NONE -> {
                                Toast.makeText(context,"Khotam Sadirov", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Rasmga olish"
                    )
                }
            }
        }
    }
}


private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSaved: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val uri = output.savedUri
                onSaved(uri.toString())
            }

            override fun onError(exception: ImageCaptureException) {
                onFailure("Saqlashda xatolik: ${exception.message}")
            }
        }
    )
}

private fun takeBitmap(
    context: Context,
    imageCapture: ImageCapture,
    onBitmapCaptured: (Bitmap) -> Unit,
    onFailure: (String) -> Unit
){
    val photoFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback{
            override fun onImageSaved(p0: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                photoFile.delete()
                bitmap?.let {
                    onBitmapCaptured(it)
                } ?: onFailure("Bitmap olishda xatolik")

            }

            override fun onError(p0: ImageCaptureException) {
                photoFile.delete()
                onFailure("Xatolik ${p0.message}")
            }


        }
    )

}