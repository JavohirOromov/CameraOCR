package com.javohir.cameraocr

import android.Manifest
import android.R.attr.bitmap
import android.R.attr.text
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.javohir.cameraocr.ui.theme.CameraOCRTheme
import kotlinx.coroutines.launch

enum class CaptureMode { NONE, RASM_OLISH, BITMAP_OLISH }
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CameraOCRTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    var showCamera by remember { mutableStateOf(false) }
                    var captureMode by remember { mutableStateOf(CaptureMode.NONE) }
                    var captureBitmap by remember { mutableStateOf<Bitmap?>(null) }
                    val coroutineScope = rememberCoroutineScope()
                    var showOcrDialog by remember { mutableStateOf(false) }
                    var ocrText by remember { mutableStateOf("") }



                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) showCamera = true
                    }

                    fun openCamera(mode: CaptureMode){
                        captureMode = mode

                        when{
                            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED -> showCamera = true
                            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }

                    val showCaptureButton = captureMode == CaptureMode.RASM_OLISH || captureMode == CaptureMode.BITMAP_OLISH


                    if (showCamera){
                        CameraScreen(
                            onBack = {showCamera = false},
                            showCaptureButton = showCaptureButton,
                            captureMode = captureMode,
                            onBitmapCaptured = {bitmap -> captureBitmap = bitmap},
                            modifier = Modifier.padding(innerPadding)
                        )
                    }else{
                      HomeScreen(
                          paddingValues = innerPadding,
                          onOpenCamera = {openCamera(CaptureMode.NONE)},
                          onTakePhoto = {openCamera(CaptureMode.RASM_OLISH)},
                          onGetBitmap = {openCamera(CaptureMode.BITMAP_OLISH)},
                          onMLKit = {
                              val bitmap = captureBitmap
                              if (bitmap == null){
                                  Toast.makeText(this, "Avval bitmap oling", Toast.LENGTH_SHORT).show()
                              }else{
                                  coroutineScope.launch {
                                      recognizeText(bitmap)
                                          .onSuccess { text ->
                                              runOnUiThread {
                                                  ocrText = text.ifEmpty { "Matn topilmadi" }
                                                  showOcrDialog = true
                                              }
                                          }
                                          .onFailure {
                                              ocrText = "Xatolik: ${it.message}"
                                              showOcrDialog = true
                                          }
                                  }
                              }
                          }
                      )
                    }

                    if (showOcrDialog){
                        TextDialog(
                            text = ocrText,
                            onDismiss = {showOcrDialog = false}
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    onTakePhoto: () -> Unit,
    onOpenCamera: () -> Unit,
    onGetBitmap: () -> Unit,
    onMLKit: () -> Unit = {}
){
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = {onOpenCamera()},
            modifier = Modifier.fillMaxWidth().padding(paddingValues = paddingValues)
        ) {
            Text("Kamera ochish", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        }
        Button(
            onClick = {onTakePhoto()},
            modifier = Modifier.fillMaxWidth().padding(paddingValues = paddingValues).padding(vertical = 16.dp)
        ) {
            Text("Rasm olish", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        }
        Button(
            onClick = {onGetBitmap()},
            modifier = Modifier.fillMaxWidth().padding(paddingValues = paddingValues).padding(bottom = 16.dp)
        ) {
            Text("bitmap olish", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        }
        Button(
            onClick = {onMLKit()},
            modifier = Modifier.fillMaxWidth().padding(paddingValues = paddingValues)
        ) {
            Text("ML Kit ga yuborish", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        }

    }
}
@Composable
@Preview
fun HomePreview(){
    HomeScreen(
        paddingValues = PaddingValues(all = 12.dp),
        onOpenCamera = {},
        onTakePhoto = {},
        onGetBitmap = {}
    )
}

@Composable
fun TextDialog(
    text: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OCR natijasi") },
        text = {
            Column {
                Text(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Yopish")
            }
        }
    )
}