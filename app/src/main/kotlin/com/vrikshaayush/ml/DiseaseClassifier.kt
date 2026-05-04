package com.vrikshaayush.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
        const val CONFIDENCE_THRESHOLD = 0.25f

        // 38-class PlantVillage MobileNetV2 model crop map
        val CROP_MAP = mapOf(
            "apple"        to "Apple (Seb)",
            "blueberry"    to "Blueberry",
            "cherry"       to "Cherry",
            "corn"         to "Maize (Makka)",
            "grape"        to "Grape (Angoor)",
            "orange"       to "Orange (Santra)",
            "peach"        to "Peach (Aadoo)",
            "pepper_bell"  to "Bell Pepper (Shimla Mirch)",
            "potato"       to "Potato (Aalu)",
            "raspberry"    to "Raspberry",
            "soybean"      to "Soybean (Soya)",
            "squash"       to "Squash / Pumpkin",
            "strawberry"   to "Strawberry",
            "tomato"       to "Tomato (Tamatar)"
        )
    }

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(model)
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

        // Below 25% threshold → cannot identify
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

        val rawLabel = labels[maxIndex]
        // 38-class labels format:
        // apple_apple_scab, apple_healthy, corn_cercospora_leaf_spot,
        // pepper_bell_bacterial_spot, tomato_early_blight, potato_healthy, etc.

        val parts = rawLabel.split("_")

        val cropType: String
        val diseaseName: String

        when {
            // pepper_bell prefix (2-word crop key)
            parts.size >= 2 && CROP_MAP.containsKey("${parts[0]}_${parts[1]}") -> {
                val cropKey = "${parts[0]}_${parts[1]}"
                cropType = CROP_MAP[cropKey]!!
                val rest = parts.drop(2)
                diseaseName = if (rest.isEmpty() || rest == listOf("healthy")) "Healthy ✅"
                else rest.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            // apple_apple_scab pattern (crop repeated)
            parts.size >= 3 && parts[0] == parts[1] && CROP_MAP.containsKey(parts[0]) -> {
                cropType = CROP_MAP[parts[0]]!!
                val rest = parts.drop(2)
                diseaseName = if (rest.isEmpty()) "Unknown" 
                else rest.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            // crop_healthy pattern
            parts.size == 2 && parts[1] == "healthy" && CROP_MAP.containsKey(parts[0]) -> {
                cropType = CROP_MAP[parts[0]]!!
                diseaseName = "Healthy ✅"
            }
            // Normal: crop_disease (tomato_early_blight, potato_late_blight, etc.)
            parts.size >= 2 && CROP_MAP.containsKey(parts[0]) -> {
                cropType = CROP_MAP[parts[0]]!!
                val rest = parts.drop(1)
                diseaseName = if (rest == listOf("healthy")) "Healthy ✅"
                else rest.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            // Fallback
            else -> {
                cropType = parts[0].replaceFirstChar { it.uppercase() }
                diseaseName = parts.drop(1).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    .ifEmpty { "Unknown Disease" }
            }
        }

        val severity = when {
            diseaseName.contains("Healthy", ignoreCase = true) -> "HEALTHY"
            confidence >= 0.80f -> "HIGH"
            confidence >= 0.50f -> "MEDIUM"
            else -> "LOW"
        }

        return DiagnosisResult(
            diseaseName = diseaseName.ifBlank { "Unknown Disease" },
            cropType = cropType,
            confidence = confidence * 100,
            severity = severity,
            label = rawLabel,
            isUncertain = false
        )
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        intValues.forEach { pixel ->
            byteBuffer.putFloat((pixel shr 16 and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixel shr 8 and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }
        return byteBuffer
    }

    fun close() {
        interpreter?.close()
    }
}
