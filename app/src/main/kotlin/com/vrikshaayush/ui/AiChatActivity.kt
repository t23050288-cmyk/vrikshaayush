package com.vrikshaayush.ui

import android.content.Context
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
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false))

    override fun getItemCount() = msgs.size

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = msgs[pos]
        holder.tvMessage.text = item.text

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f
        }

        if (item.isUser) {
            holder.tvSender.text = "You"
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

class AiChatActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLocale()
        super.onCreate(savedInstanceState)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(messages)
        val lm = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvChat.layoutManager = lm
        binding.rvChat.adapter = adapter
        binding.rvChat.itemAnimator = null

        binding.btnBack.setOnClickListener { finish() }

        scanContext = intent.getStringExtra("SCAN_CONTEXT")

        val welcome = if (scanContext != null)
            "\uD83C\uDF3F I can see you scanned: $scanContext\n\nAsk me anything — symptoms, causes, treatment, prevention. I answer based on Indian farming conditions."
        else
            "\uD83C\uDF3F Hello! I am your AI Plant Expert.\n\nAsk me:\n\u2022 How to treat Tomato Late Blight?\n\u2022 Which pesticide is safe for Wheat?\n\u2022 How to prevent fungal disease in monsoon?\n\nI only answer farming and agriculture questions."

        addMessage(ChatMessage(welcome, false))

        if (scanContext != null) {
            binding.etMessage.setText("Explain $scanContext and tell me the best treatment for an Indian farmer.")
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

    private fun applyLocale() {
        val lang = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("language", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun isOnline(): Boolean {
        val cm  = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork)
        return cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun updateConnectivityUI() {
        val online = isOnline()
        binding.layoutOfflineNotice.visibility = if (online) View.GONE else View.VISIBLE
        binding.tvStatus.text = if (online) "Online \u2014 Ask me about plant diseases" else "Offline \u2014 Need internet for AI"
    }

    private fun sendMessage(userText: String) {
        updateConnectivityUI()
        if (!isOnline()) {
            Toast.makeText(this, "No internet \u2014 please connect and try again", Toast.LENGTH_SHORT).show()
            return
        }
        addMessage(ChatMessage(userText, true))
        binding.tvTyping.visibility = View.VISIBLE
        binding.btnSend.isEnabled   = false

        scope.launch {
            val reply = withContext(Dispatchers.IO) { callAi(userText) }
            binding.tvTyping.visibility = View.GONE
            binding.btnSend.isEnabled   = true
            addMessage(ChatMessage(reply, false))
        }
    }

    private fun callAi(userText: String): String {
        val system = buildString {
            append("You are an expert agricultural assistant for Indian farmers. ")
            append("You ONLY answer questions about farming, plant diseases, crops, pesticides, fertilizers, and agriculture. ")
            append("If asked anything unrelated to farming/agriculture, say: 'I only answer farming and plant questions.' ")
            append("Keep answers short and practical for a rural Indian farmer. ")
            append("Always mention safety precautions for chemical treatments. ")
            if (scanContext != null) append("The farmer scanned a plant diagnosed as: $scanContext. ")
        }

        val sarvam = callEndpoint(SARVAM_URL, SARVAM_KEY, SARVAM_MODEL, system, userText)
        if (!sarvam.startsWith("ERR:")) return sarvam

        val nvidia = callEndpoint(NVIDIA_URL, NVIDIA_KEY, NVIDIA_MODEL, system, userText)
        if (!nvidia.startsWith("ERR:")) return nvidia

        return "\u26A0\uFE0F AI service unavailable right now.\nPlease check your internet and try again.\n\nDetails: ${sarvam.removePrefix("ERR:")}"
    }

    private fun callEndpoint(url: String, key: String, model: String, system: String, user: String): String {
        return try {
            val bodyJson = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", system) })
                    put(JSONObject().apply { put("role", "user");   put("content", user)   })
                })
                put("temperature", 0.5)
                put("max_tokens", 600)
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("api-subscription-key", key)
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

            val messageObj = JSONObject(respBody)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")

            var reply = messageObj.optString("content", "").trim()
            
            if (reply == "null" || reply.isEmpty()) {
                return "ERR: API returned null content. Raw response: $respBody"
            }

            reply = reply.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()
            if (reply.isEmpty()) "ERR: Empty reply from model" else reply

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
