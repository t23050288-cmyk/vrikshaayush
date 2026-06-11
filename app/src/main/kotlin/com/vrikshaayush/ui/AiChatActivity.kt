package com.vrikshaayush.ui

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vrikshaayush.BuildConfig
import com.vrikshaayush.R
import com.vrikshaayush.databinding.ActivityAiChatBinding
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatAdapter(private val msgs: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvSender: TextView  = view.findViewById(R.id.tvSender)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false))

    override fun getItemCount() = msgs.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = msgs[pos]
        holder.tvMessage.text = item.text
        val bg = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = 18f }
        if (item.isUser) {
            holder.tvSender.text = "\uD83D\uDC68\u200D\uD83C\uDF3E You"
            holder.tvSender.setTextColor(Color.parseColor("#1B7A3E"))
            bg.setColor(Color.parseColor("#E8F5E9"))
            holder.tvMessage.background = bg
            holder.tvMessage.setPadding(24, 16, 24, 16)
            (holder.tvSender.layoutParams  as LinearLayout.LayoutParams).gravity = Gravity.END
            (holder.tvMessage.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
            (holder.itemView as LinearLayout).gravity = Gravity.END
        } else {
            holder.tvSender.text = "\uD83E\uDD16 AI Expert"
            holder.tvSender.setTextColor(Color.parseColor("#145C2E"))
            bg.setColor(Color.WHITE)
            holder.tvMessage.background = bg
            holder.tvMessage.setPadding(24, 16, 24, 16)
            (holder.tvSender.layoutParams  as LinearLayout.LayoutParams).gravity = Gravity.START
            (holder.tvMessage.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
            (holder.itemView as LinearLayout).gravity = Gravity.START
        }
    }
}

class AiChatActivity : BaseActivity() {

    private lateinit var binding: ActivityAiChatBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val SARVAM_KEY   = BuildConfig.SARVAM_API_KEY
    private val SARVAM_URL   = "https://api.sarvam.ai/v1/chat/completions"
    private val SARVAM_MODEL = "sarvam-30b"

    private val NVIDIA_KEY   = BuildConfig.NVIDIA_API_KEY
    private val NVIDIA_URL   = "https://integrate.api.nvidia.com/v1/chat/completions"
    private val NVIDIA_MODEL = "meta/llama-3.1-8b-instruct"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private var scanContext: String? = null
    private var saveSuggestionKey: String? = null
    private var lastAiReply: String = ""
    private var currentLang: String = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentLang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"

        adapter = ChatAdapter(messages)
        val lm = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvChat.layoutManager = lm
        binding.rvChat.adapter = adapter
        binding.rvChat.itemAnimator = null

        binding.btnBack.setOnClickListener {
            // Save last AI reply to SharedPrefs so ResultActivity can pick it up
            if (saveSuggestionKey != null && lastAiReply.isNotEmpty()) {
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                    .putString(saveSuggestionKey, lastAiReply)
                    .apply()
            }
            finish()
        }

        scanContext = intent.getStringExtra("SCAN_CONTEXT")
        saveSuggestionKey = intent.getStringExtra("SAVE_SUGGESTION_KEY")

        val welcome = buildWelcomeMessage()
        addMessage(ChatMessage(welcome, false))

        if (scanContext != null) {
            binding.etMessage.setText(getAutoQuestion(scanContext!!))
        }

        updateConnectivityUI()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                binding.etMessage.setText("")
                sendMessage(text)
            }
        }
    }


    private fun buildWelcomeMessage(): String {
        return when (currentLang) {
            "hi" -> if (scanContext != null)
                "\uD83C\uDF3F मैं देख सकता हूँ आपने स्कैन किया: $scanContext\n\nइस रोग के बारे में कुछ भी पूछें — लक्षण, कारण, उपचार। मैं भारतीय किसानों की मदद के लिए हूँ।"
            else
                "\uD83C\uDF3F नमस्ते! मैं आपका AI पौध विशेषज्ञ हूँ।\n\nआप पूछ सकते हैं:\n• टमाटर की अगेती झुलसा कैसे ठीक करें?\n• गेहूँ के लिए कौन सी दवा सुरक्षित है?\n• मानसून में फफूंद रोग कैसे रोकें?\n\nमैं खेती और पौध रोग से जुड़े सभी सवालों का जवाब दूँगा।"
            "kn" -> if (scanContext != null)
                "\uD83C\uDF3F ನೀವು ಸ್ಕ್ಯಾನ್ ಮಾಡಿದ್ದೀರಿ: $scanContext\n\nಈ ರೋಗದ ಬಗ್ಗೆ ಏನಾದರೂ ಕೇಳಿ — ಲಕ್ಷಣಗಳು, ಕಾರಣಗಳು, ಚಿಕಿತ್ಸೆ।"
            else
                "\uD83C\uDF3F ನಮಸ್ಕಾರ! ನಾನು ನಿಮ್ಮ AI ಸಸ್ಯ ತಜ್ಞ.\n\nಕೇಳಿ:\n• ಟೊಮೇಟೋ ಬ್ಲೈಟ್ ಹೇಗೆ ಗುಣಪಡಿಸುವುದು?\n• ಮಳೆಗಾಲದಲ್ಲಿ ಶಿಲೀಂಧ್ರ ರೋಗ ತಡೆಯುವುದು ಹೇಗೆ?"
            "ta" -> "\uD83C\uDF3F வணக்கம்! நான் உங்கள் AI தாவர நிபுணன்.\n\nகேளுங்கள்:\n• தக்காளி நோய்களை எப்படி குணப்படுத்துவது?\n• பருவமழையில் பூஞ்சை நோய்களை எப்படி தடுப்பது?"
            "te" -> "\uD83C\uDF3F నమస్కారం! నేను మీ AI మొక్కల నిపుణుడిని.\n\nఅడగండి:\n• టమాటో వ్యాధులను ఎలా నివారించాలి?\n• వర్షాకాలంలో శిలీంధ్ర వ్యాధులు ఎలా నిరోధించాలి?"
            "ml" -> "\uD83C\uDF3F നമസ്കാരം! ഞാൻ നിങ്ങളുടെ AI സസ്യ വിദഗ്ദ്ധനാണ്.\n\nചോദിക്കൂ:\n• തക്കാളി രോഗങ്ങൾ എങ്ങനെ ചികിത്സിക്കാം?\n• മൺസൂണിൽ ഫംഗൽ രോഗങ്ങൾ എങ്ങനെ തടയാം?"
            "mr" -> "\uD83C\uDF3F नमस्कार! मी तुमचा AI वनस्पती तज्ञ आहे.\n\nविचारा:\n• टोमॅटोच्या रोगांवर काय उपाय आहे?\n• पावसाळ्यात बुरशीजन्य रोग कसे रोखायचे?"
            else -> if (scanContext != null)
                "\uD83C\uDF3F I can see you scanned: $scanContext\n\nAsk me anything — symptoms, causes, treatment, prevention. I answer based on Indian farming conditions."
            else
                "\uD83C\uDF3F Hello! I am your AI Plant Expert.\n\nAsk me:\n• How to treat Tomato Late Blight?\n• Which pesticide is safe for Wheat?\n• How to prevent fungal disease in monsoon?\n\nI answer all farming and agriculture questions!"
        }
    }

    private fun getAutoQuestion(context: String): String {
        return when (currentLang) {
            "hi" -> "$context के बारे में बताएं और भारतीय किसान के लिए सबसे अच्छा उपचार क्या है?"
            "kn" -> "$context ಬಗ್ಗೆ ವಿವರಿಸಿ ಮತ್ತು ಭಾರತೀಯ ರೈತರಿಗೆ ಉತ್ತಮ ಚಿಕಿತ್ಸೆ ಏನು?"
            else  -> "Explain $context and tell me the best treatment for an Indian farmer."
        }
    }

    private fun isOnline(): Boolean {
        val cm  = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork)
        return cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun updateConnectivityUI() {
        val online = isOnline()
        binding.layoutOfflineNotice.visibility = if (online) View.GONE else View.VISIBLE
        binding.tvStatus.text = if (online)
            getString(R.string.ai_online_status)
        else
            getString(R.string.ai_offline_status)
    }

    private fun sendMessage(userText: String) {
        updateConnectivityUI()
        if (!isOnline()) {
            Toast.makeText(this, getString(R.string.no_internet_msg), Toast.LENGTH_SHORT).show()
            return
        }
        addMessage(ChatMessage(userText, true))
        binding.tvTyping.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false

        scope.launch {
            val reply = withContext(Dispatchers.IO) { callAi(userText) }
            binding.tvTyping.visibility = View.GONE
            binding.btnSend.isEnabled = true
            lastAiReply = reply
            // Auto-save to SharedPrefs so ResultActivity always has it
            if (saveSuggestionKey != null) {
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                    .putString(saveSuggestionKey, reply)
                    .apply()
            }
            addMessage(ChatMessage(reply, false))
        }
    }

    private fun callAi(userText: String): String {
        // Detect greeting/small talk
        val isGreeting = userText.trim().lowercase() in listOf(
            "hi", "hello", "hey", "namaste", "hii", "helo", "hai",
            "नमस्ते", "हेलो", "हाय", "ನಮಸ್ಕಾರ", "வணக்கம்", "నమస్కారం"
        )

        val langInstruction = when (currentLang) {
            "hi" -> "Always reply in Hindi (हिंदी). "
            "kn" -> "Always reply in Kannada (ಕನ್ನಡ). "
            "ta" -> "Always reply in Tamil (தமிழ்). "
            "te" -> "Always reply in Telugu (తెలుగు). "
            "ml" -> "Always reply in Malayalam (മലയാളം). "
            "mr" -> "Always reply in Marathi (मराठी). "
            else -> "Reply in English. "
        }

        val system = if (isGreeting) {
            // Friendly greeting — no farming restriction for hello
            langInstruction + "You are a friendly AI plant expert. Respond warmly to greetings and ask how you can help with their plants or farming questions. Keep it short and welcoming."
        } else {
            buildString {
                append(langInstruction)
                append("You are an expert agricultural assistant for Indian farmers. ")
                append("Answer questions about farming, plant diseases, crops, pesticides, fertilizers, and agriculture. ")
                append("For casual conversation (how are you, what's your name, etc.) respond naturally and briefly, then steer toward plants/farming. ")
                append("If asked something completely unrelated (movies, politics, etc.) say: 'I specialise in plants and farming — let me know if you have any crop or disease questions!' ")
                append("Keep answers short and practical for a rural Indian farmer. ")
                append("Always mention safety precautions for chemical treatments. ")
                if (scanContext != null) append("The farmer scanned a plant diagnosed as: $scanContext. ")
            }
        }

        val sarvam = callEndpoint(SARVAM_URL, SARVAM_KEY, SARVAM_MODEL, system, userText)
        if (!sarvam.startsWith("ERR:")) return sarvam

        val nvidia = callEndpoint(NVIDIA_URL, NVIDIA_KEY, NVIDIA_MODEL, system, userText)
        if (!nvidia.startsWith("ERR:")) return nvidia

        return "\u26A0\uFE0F AI service unavailable.\nPlease check your internet and try again.\n\nDetails: ${sarvam.removePrefix("ERR:")}"
    }

    private fun callEndpoint(url: String, key: String, model: String, system: String, user: String): String {
        return try {
            val bodyJson = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", system) })
                    put(JSONObject().apply { put("role", "user");   put("content", user)   })
                })
                put("temperature", 0.6)
                put("max_tokens", 600)
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val resp     = httpClient.newCall(req).execute()
            val respBody = resp.body?.string() ?: return "ERR: Empty response"

            if (!resp.isSuccessful) {
                return "ERR: " + try {
                    JSONObject(respBody).optJSONObject("error")?.optString("message") ?: "HTTP ${resp.code}"
                } catch (e: Exception) { "HTTP ${resp.code}" }
            }

            val jsonResp = JSONObject(respBody)
            val choices = jsonResp.optJSONArray("choices")
            if (choices == null || choices.length() == 0) return "ERR: No choices in response"
            
            val message = choices.getJSONObject(0).optJSONObject("message")
            val rawContent = message?.opt("content")
            
            // Handle null content (some models return JSON null)
            if (rawContent == null || rawContent == JSONObject.NULL || rawContent.toString() == "null") {
                return "ERR: Empty reply from AI — please try again"
            }
            
            var reply = rawContent.toString().trim()
            reply = reply.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()
            if (reply.isEmpty()) "ERR: Empty reply" else reply

        } catch (e: java.net.SocketTimeoutException) {
            "ERR: Timed out — AI is busy, try again"
        } catch (e: java.io.IOException) {
            "ERR: Network error — ${e.message}"
        } catch (e: Exception) {
            "ERR: ${e.message}"
        }
    }

    private fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        adapter.notifyItemInserted(messages.size - 1)
        binding.rvChat.scrollToPosition(messages.size - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
