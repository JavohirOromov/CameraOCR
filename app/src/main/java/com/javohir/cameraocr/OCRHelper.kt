package com.javohir.cameraocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Created by: Javohir Oromov macos
 * Project: CameraOCR
 * Package: com.javohir.cameraocr
 * Description: OCR funksiyasi 
 */

suspend fun recognizeText(bitmap: Bitmap): Result<String> = runCatching {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromBitmap(bitmap, 0)
    val result = recognizer.process(image).await()
    result.text ?: ""
}