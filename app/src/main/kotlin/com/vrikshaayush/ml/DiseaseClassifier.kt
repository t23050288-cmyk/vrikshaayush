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
        const val CONFIDENCE_THRESHOLD = 0.35f

        // Map for human-friendly names
        val CROP_NAME_MAP = mapOf(
            "apple" to "Apple (Seb)",
            "bean" to "Bean",
            "bell_pepper" to "Bell Pepper (Shimla Mirch)",
            "cherry" to "Cherry",
            "corn" to "Maize (Makka)",
            "cotton" to "Cotton",
            "cucumber" to "Cucumber",
            "grape" to "Grape (Angoor)",
            "groundnut" to "Groundnut (Moongphali)",
            "guava" to "Guava (Amrood)",
            "lemon" to "Lemon (Nimbu)",
            "orange" to "Orange (Santra)",
            "peach" to "Peach (Aadoo)",
            "potato" to "Potato (Aloo)",
            "pumpkin" to "Pumpkin (Kaddu)",
            "rice" to "Rice (Chawal)",
            "strawberry" to "Strawberry",
            "sugarcane" to "Sugarcane (Ganna)",
            "tomato" to "Tomato (Tamatar)",
            "wheat" to "Wheat (Gehun)"
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

        if (confidence < CONFIDENCE_THRESHOLD) {
            return DiagnosisResult(
                diseaseName = "Cannot Identify Plant",
                cropType = "Unknown",
                confidence = confidence * 100,
                severity = "LOW",
                label = "uncertain",
                isUncertain = true
            )
        }

        val rawLabel = labels[maxIndex] // e.g., "apple_black_rot" or "healthy_wheat"
        
        // Parsing logic for new underscore format
        val parts = rawLabel.split("_")
        val cropTypeRaw: String
        val diseaseRaw: String

        if (rawLabel.startsWith("healthy_")) {
            cropTypeRaw = rawLabel.removePrefix("healthy_")
            diseaseRaw = "healthy"
        } else {
            cropTypeRaw = parts[0]
            diseaseRaw = parts.drop(1).joinToString(" ")
        }

        val cropType = CROP_NAME_MAP[cropTypeRaw] ?: cropTypeRaw.replaceFirstChar { it.uppercase() }
        val diseaseName = diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        val severity = when {
            diseaseName.lowercase() == "healthy" -> "LOW"
            confidence >= 0.80f -> "HIGH"
            confidence >= 0.50f -> "MEDIUM"
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

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
