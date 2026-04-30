package com.vrikshaayush.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class DiagnosisResult(
    val diseaseName: String,
    val cropType: String,
    val confidence: Float,
    val severity: String,
    val label: String
)

class DiseaseClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    companion object {
        const val MODEL_FILE = "plant_disease_model.tflite"
        const val LABELS_FILE = "labels.txt"
        const val INPUT_SIZE = 224
        const val PIXEL_SIZE = 3
        const val IMAGE_MEAN = 0f
        const val IMAGE_STD = 255.0f
    }

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(model)
        return model
    }

    private fun loadLabels() {
        val reader = InputStreamReader(context.assets.open(LABELS_FILE))
        labels = reader.readLines().filter { it.isNotEmpty() }
        reader.close()
    }

    fun classify(bitmap: Bitmap): DiagnosisResult {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        val output = Array(1) { FloatArray(labels.size) }
        interpreter?.run(byteBuffer, output)

        val scores = output[0]
        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
        val confidence = scores[maxIndex]

        val rawLabel = labels[maxIndex] // e.g. "Tomato___Early_blight"
        val parts = rawLabel.split("___")
        val cropType = parts[0].replace("_", " ")
        val diseaseName = if (parts.size > 1) parts[1].replace("_", " ") else "Unknown"

        val severity = when {
            confidence >= 0.85f -> "HIGH"
            confidence >= 0.60f -> "MEDIUM"
            else -> "LOW"
        }

        return DiagnosisResult(
            diseaseName = diseaseName,
            cropType = cropType,
            confidence = confidence * 100,
            severity = severity,
            label = rawLabel
        )
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(
            4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)

            byteBuffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
            byteBuffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
            byteBuffer.putFloat((b - IMAGE_MEAN) / IMAGE_STD)
        }

        return byteBuffer
    }

    fun close() {
        interpreter?.close()
    }
}
