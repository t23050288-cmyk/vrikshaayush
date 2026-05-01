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
    val label: String,
    val isUncertain: Boolean = false
)

class DiseaseClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    companion object {
        const val MODEL_FILE = "plant_disease_model.tflite"
        const val LABELS_FILE = "labels.txt"
        const val INPUT_SIZE = 224
        const val PIXEL_SIZE = 3
        const val IMAGE_STD = 255.0f
        const val CONFIDENCE_THRESHOLD = 0.45f  // below this = uncertain

        // Map model crop names to friendly Indian crop names
        val CROP_NAME_MAP = mapOf(
            "Corn_(maize)" to "Maize (Makka)",
            "Tomato" to "Tomato (Tamatar)",
            "Potato" to "Potato (Aloo)",
            "Rice" to "Rice (Chawal)",
            "Pepper,_bell" to "Bell Pepper (Shimla Mirch)",
            "Apple" to "Apple (Seb)",
            "Grape" to "Grape (Angoor)",
            "Strawberry" to "Strawberry",
            "Peach" to "Peach (Aadoo)",
            "Orange" to "Orange (Santra)",
            "Soybean" to "Soybean (Soya)",
            "Squash" to "Squash (Kaddu)",
            "Blueberry" to "Blueberry",
            "Cherry_(including_sour)" to "Cherry (Cheery)",
            "Raspberry" to "Raspberry"
        )
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

        // LOW CONFIDENCE - return uncertain result
        if (confidence < CONFIDENCE_THRESHOLD) {
            return DiagnosisResult(
                diseaseName = "Uncertain - Retake Photo",
                cropType = "Unknown Plant",
                confidence = confidence * 100,
                severity = "LOW",
                label = "uncertain",
                isUncertain = true
            )
        }

        val rawLabel = labels[maxIndex]
        val parts = rawLabel.split("___")
        val rawCrop = parts[0]
        val rawDisease = if (parts.size > 1) parts[1] else "Unknown"

        // Use friendly crop name
        val cropType = CROP_NAME_MAP[rawCrop] ?: rawCrop.replace("_", " ")
        val diseaseName = rawDisease.replace("_", " ")

        val severity = when {
            confidence >= 0.80f -> "HIGH"
            confidence >= 0.55f -> "MEDIUM"
            else -> "LOW"
        }

        return DiagnosisResult(
            diseaseName = diseaseName,
            cropType = cropType,
            confidence = confidence * 100,
            severity = severity,
            label = rawLabel,
            isUncertain = false
        )
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val `val` = intValues[pixel++]
                byteBuffer.putFloat(((`val` shr 16) and 0xFF) / IMAGE_STD)
                byteBuffer.putFloat(((`val` shr 8) and 0xFF) / IMAGE_STD)
                byteBuffer.putFloat((`val` and 0xFF) / IMAGE_STD)
            }
        }
        return byteBuffer
    }
}

