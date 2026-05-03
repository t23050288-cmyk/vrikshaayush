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
        const val CONFIDENCE_THRESHOLD = 0.25f

        // Map label prefix -> friendly crop name
        val CROP_MAP = mapOf(
            "apple" to "Apple (Seb)",
            "bean" to "Bean (Rajma)",
            "bell_pepper" to "Bell Pepper (Shimla Mirch)",
            "cherry" to "Cherry",
            "corn" to "Maize (Makka)",
            "cotton" to "Cotton (Kapas)",
            "cucumber" to "Cucumber (Kheera)",
            "grape" to "Grape (Angoor)",
            "groundnut" to "Groundnut (Moongphali)",
            "guava" to "Guava (Amrood)",
            "lemon" to "Lemon (Nimbu)",
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

        val rawLabel = labels[maxIndex] // e.g. "tomato_early_blight" or "healthy_rice"

        // Parse label: format is "crop_disease" or "healthy_crop" or "diseased_crop"
        val parts = rawLabel.split("_")
        
        val cropType: String
        val diseaseName: String
        
        when {
            // healthy_cropname
            parts[0] == "healthy" -> {
                val crop = parts.drop(1).joinToString("_")
                cropType = CROP_MAP[crop] ?: crop.replaceFirstChar { it.uppercase() }
                diseaseName = "Healthy ✅"
            }
            // diseased_cropname
            parts[0] == "diseased" -> {
                val crop = parts.drop(1).joinToString("_")
                cropType = CROP_MAP[crop] ?: crop.replaceFirstChar { it.uppercase() }
                diseaseName = "Disease Detected"
            }
            // Try 2-word crop prefix: e.g. bell_pepper_bacterial_spot
            parts.size >= 3 && CROP_MAP.containsKey("${parts[0]}_${parts[1]}") -> {
                val cropKey = "${parts[0]}_${parts[1]}"
                cropType = CROP_MAP[cropKey]!!
                diseaseName = parts.drop(2).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            // Single word crop prefix: e.g. tomato_early_blight
            CROP_MAP.containsKey(parts[0]) -> {
                cropType = CROP_MAP[parts[0]]!!
                diseaseName = parts.drop(1).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            else -> {
                cropType = parts[0].replaceFirstChar { it.uppercase() }
                diseaseName = parts.drop(1).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }

        val severity = when {
            diseaseName.contains("Healthy", ignoreCase = true) -> "HEALTHY"
            confidence >= 0.80f -> "HIGH"
            confidence >= 0.50f -> "MEDIUM"
            else -> "LOW"
        }

        return DiagnosisResult(
            diseaseName = if (diseaseName.isBlank()) "Unknown" else diseaseName,
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
            byteBuffer.putFloat(((pixel shr 16 and 0xFF) / 255.0f))
            byteBuffer.putFloat(((pixel shr 8 and 0xFF) / 255.0f))
            byteBuffer.putFloat(((pixel and 0xFF) / 255.0f))
        }
        return byteBuffer
    }

    fun close() {
        interpreter?.close()
    }
}
