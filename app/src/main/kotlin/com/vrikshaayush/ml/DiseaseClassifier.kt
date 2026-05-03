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
        // Lowered threshold — 30% to allow maize and other crops to be detected
        const val CONFIDENCE_THRESHOLD = 0.30f

        // Friendly crop name map
        val CROP_NAME_MAP = mapOf(
            "corn" to "Maize (Makka)",
            "maize" to "Maize (Makka)",
            "tomato" to "Tomato (Tamatar)",
            "potato" to "Potato (Aloo)",
            "pepper" to "Bell Pepper (Shimla Mirch)",
            "apple" to "Apple (Seb)",
            "grape" to "Grape (Angoor)",
            "strawberry" to "Strawberry",
            "peach" to "Peach (Aadoo)",
            "orange" to "Orange (Santra)",
            "soybean" to "Soybean (Soya)",
            "squash" to "Squash (Kaddu)",
            "blueberry" to "Blueberry",
            "cherry" to "Cherry (Cheery)",
            "raspberry" to "Raspberry"
        )

        // Disease display name map
        val DISEASE_NAME_MAP = mapOf(
            "northern leaf blight" to "Northern Leaf Blight",
            "cercospora leaf spot gray leaf spot" to "Gray Leaf Spot",
            "common rust" to "Common Rust",
            "early blight" to "Early Blight",
            "late blight" to "Late Blight",
            "leaf mold" to "Leaf Mold",
            "septoria leaf spot" to "Septoria Leaf Spot",
            "spider mites two-spotted spider mite" to "Spider Mites",
            "target spot" to "Target Spot",
            "tomato yellow leaf curl virus" to "Yellow Leaf Curl Virus",
            "tomato mosaic virus" to "Mosaic Virus",
            "bacterial spot" to "Bacterial Spot",
            "black rot" to "Black Rot",
            "cedar apple rust" to "Cedar Apple Rust",
            "apple scab" to "Apple Scab",
            "esca black measles" to "Esca (Black Measles)",
            "leaf blight isariopsis leaf spot" to "Leaf Blight",
            "haunglongbing citrus greening" to "Citrus Greening (HLB)",
            "powdery mildew" to "Powdery Mildew",
            "leaf scorch" to "Leaf Scorch",
            "healthy" to "Healthy"
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
                diseaseName = "Cannot Identify Plant",
                cropType = "Unknown",
                confidence = confidence * 100,
                severity = "LOW",
                label = "uncertain",
                isUncertain = true
            )
        }

        val rawLabel = labels[maxIndex]

        // Parse the label — format: "crop disease" (space separated, no ___)
        // e.g. "corn maize northern leaf blight" or "tomato early blight"
        val cropType: String
        val diseaseName: String

        when {
            rawLabel.startsWith("corn maize") || rawLabel.startsWith("corn") -> {
                cropType = "Maize (Makka)"
                val diseaseRaw = rawLabel.removePrefix("corn maize").removePrefix("corn").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("tomato") -> {
                cropType = "Tomato (Tamatar)"
                val diseaseRaw = rawLabel.removePrefix("tomato").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("potato") -> {
                cropType = "Potato (Aloo)"
                val diseaseRaw = rawLabel.removePrefix("potato").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("apple") -> {
                cropType = "Apple (Seb)"
                val diseaseRaw = rawLabel.removePrefix("apple").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("grape") -> {
                cropType = "Grape (Angoor)"
                val diseaseRaw = rawLabel.removePrefix("grape").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("orange") -> {
                cropType = "Orange (Santra)"
                val diseaseRaw = rawLabel.removePrefix("orange").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("pepper bell") -> {
                cropType = "Bell Pepper (Shimla Mirch)"
                val diseaseRaw = rawLabel.removePrefix("pepper bell").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("peach") -> {
                cropType = "Peach (Aadoo)"
                val diseaseRaw = rawLabel.removePrefix("peach").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("strawberry") -> {
                cropType = "Strawberry"
                val diseaseRaw = rawLabel.removePrefix("strawberry").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("cherry") -> {
                cropType = "Cherry"
                val diseaseRaw = rawLabel.removePrefix("cherry including sour").removePrefix("cherry").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            rawLabel.startsWith("soybean") -> {
                cropType = "Soybean (Soya)"
                diseaseName = "Healthy"
            }
            rawLabel.startsWith("blueberry") -> {
                cropType = "Blueberry"
                diseaseName = "Healthy"
            }
            rawLabel.startsWith("raspberry") -> {
                cropType = "Raspberry"
                diseaseName = "Healthy"
            }
            rawLabel.startsWith("squash") -> {
                cropType = "Squash (Kaddu)"
                val diseaseRaw = rawLabel.removePrefix("squash").trim()
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
            else -> {
                // Fallback: first word is crop, rest is disease
                val words = rawLabel.split(" ")
                val rawCrop = words.firstOrNull() ?: "Unknown"
                cropType = CROP_NAME_MAP[rawCrop] ?: rawCrop.replaceFirstChar { it.uppercase() }
                val diseaseRaw = words.drop(1).joinToString(" ")
                diseaseName = DISEASE_NAME_MAP[diseaseRaw] ?: diseaseRaw.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }

        val severity = when {
            confidence >= 0.80f -> "HIGH"
            confidence >= 0.50f -> "MEDIUM"
            else -> "LOW"
        }

        return DiagnosisResult(
            diseaseName = if (diseaseName.isBlank()) "Unknown Disease" else diseaseName,
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
    }
}
