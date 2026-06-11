package com.vrikshaayush.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
    val isUncertain: Boolean = false,
    val isNotLeaf: Boolean = false
)

class DiseaseClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    companion object {
        const val MODEL_FILE = "plant_disease_model.tflite"
        const val LABELS_FILE = "labels.txt"
        const val INPUT_SIZE = 224
        const val CONFIDENCE_THRESHOLD = 0.25f

        val CROP_MAP = mapOf(
            "apple"        to "Apple (Seb)",
            "bean"         to "Bean (Rajma)",
            "bell_pepper"  to "Bell Pepper (Shimla Mirch)",
            "blueberry"    to "Blueberry",
            "cherry"       to "Cherry",
            "corn"         to "Maize (Makka)",
            "cotton"       to "Cotton (Kapas)",
            "cucumber"     to "Cucumber (Kheera)",
            "grape"        to "Grape (Angoor)",
            "groundnut"    to "Groundnut (Moongphali)",
            "guava"        to "Guava (Amrood)",
            "lemon"        to "Lemon (Nimbu)",
            "orange"       to "Orange (Santra)",
            "peach"        to "Peach (Aadoo)",
            "pepper_bell"  to "Bell Pepper (Shimla Mirch)",
            "potato"       to "Potato (Aloo)",
            "pumpkin"      to "Pumpkin (Kaddu)",
            "raspberry"    to "Raspberry",
            "rice"         to "Rice (Chawal)",
            "soybean"      to "Soybean (Soya)",
            "squash"       to "Squash / Pumpkin",
            "strawberry"   to "Strawberry",
            "sugarcane"    to "Sugarcane (Ganna)",
            "tomato"       to "Tomato (Tamatar)",
            "wheat"        to "Wheat (Gehun)"
        )
    }

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        val afd = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(afd.fileDescriptor)
        val model = inputStream.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        interpreter = Interpreter(model)
    }

    private fun loadLabels() {
        val reader = InputStreamReader(context.assets.open(LABELS_FILE))
        labels = reader.readLines().filter { it.isNotEmpty() }
        reader.close()
    }

    /**
     * Check if image is likely a plant/leaf by measuring green pixel ratio.
     * Samples 500 pixels and checks if enough of them are "greenish".
     */
    private fun isLikelyLeaf(bitmap: Bitmap): Boolean {
        val sample = Bitmap.createScaledBitmap(bitmap, 50, 50, false)
        var greenCount = 0
        val total = 50 * 50
        for (x in 0 until 50) {
            for (y in 0 until 50) {
                val pixel = sample.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Green dominant = plant-like pixel
                // Also allow brown/yellow diseased leaf tones
                val isGreenish = g > r + 20 && g > b + 10
                val isBrownish = r > 80 && g > 50 && b < 100 && r > b
                val isYellowish = r > 150 && g > 150 && b < 100
                if (isGreenish || isBrownish || isYellowish) greenCount++
            }
        }
        val ratio = greenCount.toFloat() / total
        // At least 18% of pixels should be plant-like colours
        return ratio >= 0.18f
    }

    fun classify(bitmap: Bitmap): DiagnosisResult {
        // ── Non-leaf detection ────────────────────────────────
        if (!isLikelyLeaf(bitmap)) {
            return DiagnosisResult(
                diseaseName = "No Leaf Detected",
                cropType = "Please take a clear photo of a plant leaf",
                confidence = 0f,
                severity = "LOW",
                label = "no_leaf",
                isUncertain = false,
                isNotLeaf = true
            )
        }

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedBitmap)

        val output = Array(1) { FloatArray(labels.size) }
        interpreter?.run(byteBuffer, output)

        val scores = output[0]
        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: 0
        val confidence = scores[maxIndex]

        val rawLabelCheck = labels[maxIndex]
        val isHealthyPrediction = rawLabelCheck.endsWith("healthy")
        
        // For disease predictions, require at least 40% confidence to avoid false positives
        // For healthy predictions, use lower threshold (25%) - better to show healthy than wrong disease
        val effectiveThreshold = if (isHealthyPrediction) CONFIDENCE_THRESHOLD else 0.40f
        
        if (confidence < effectiveThreshold) {
            return DiagnosisResult(
                diseaseName = "Cannot Identify Plant",
                cropType = "Try a clearer photo with better lighting",
                confidence = confidence * 100,
                severity = "LOW",
                label = "uncertain",
                isUncertain = true
            )
        }

        val rawLabel = labels[maxIndex]
        val parts = rawLabel.split("_")

        val cropType: String
        val diseaseName: String

        when {
            parts.size >= 2 && CROP_MAP.containsKey("${parts[0]}_${parts[1]}") -> {
                val cropKey = "${parts[0]}_${parts[1]}"
                cropType = CROP_MAP[cropKey]!!
                val rest = parts.drop(2)
                diseaseName = if (rest.isEmpty() || rest == listOf("healthy")) "Healthy ✅"
                else rest.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            parts.size >= 3 && parts[0] == parts[1] && CROP_MAP.containsKey(parts[0]) -> {
                cropType = CROP_MAP[parts[0]]!!
                val rest = parts.drop(2)
                diseaseName = rest.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.ifEmpty { "Unknown" }
            }
            parts.size == 2 && parts[1] == "healthy" && CROP_MAP.containsKey(parts[0]) -> {
                cropType = CROP_MAP[parts[0]]!!
                diseaseName = "Healthy ✅"
            }
            parts.size >= 2 && CROP_MAP.containsKey(parts[0]) -> {
                cropType = CROP_MAP[parts[0]]!!
                val rest = parts.drop(1)
                diseaseName = if (rest == listOf("healthy")) "Healthy ✅"
                else rest.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            else -> {
                cropType = parts[0].replaceFirstChar { it.uppercase() }
                diseaseName = parts.drop(1).joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }.ifEmpty { "Unknown Disease" }
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
            isUncertain = false,
            isNotLeaf = false
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
        interpreter = null
    }
}
