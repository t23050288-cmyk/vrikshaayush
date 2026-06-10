package com.vrikshaayush.ui

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.layoutBubble)
        val sender: TextView     = view.findViewById(R.id.tvSender)
        val msg: TextView        = view.findViewById(R.id.tvMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false))

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = messages[position]
        holder.msg.text = item.text
        if (item.isUser) {
            holder.sender.text = holder.itemView.context.getString(R.string.you_label)
            holder.sender.setTextColor(holder.itemView.context.getColor(R.color.primary_green))
            holder.layout.setBackgroundColor(holder.itemView.context.getColor(R.color.primary_green_light))
            (holder.itemView as LinearLayout).gravity = android.view.Gravity.END
            (holder.layout.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 60; marginEnd = 0
            }
        } else {
            holder.sender.text = holder.itemView.context.getString(R.string.ai_label)
            holder.sender.setTextColor(holder.itemView.context.getColor(R.color.primary_green_dark))
            holder.layout.setBackgroundColor(holder.itemView.context.getColor(R.color.card_white))
            (holder.itemView as LinearLayout).gravity = android.view.Gravity.START
            (holder.layout.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 0; marginEnd = 60
            }
        }
    }
}

class AiChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiChatBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── API CONFIG ────────────────────────────────────────────
    // Primary: Sarvam AI  (set SARVAM_API_KEY to your key)
    private val SARVAM_API_KEY = "sk_1blcusaf_CBHJwlBsnXClMJC0vlsqCetf"
    private val SARVAM_URL     = "https://api.sarvam.ai/v1/chat/completions"
    private val SARVAM_MODEL   = "sarvam-30b"

    // Fallback: NVIDIA (set to your new key when available)
    private val NVIDIA_API_KEY = "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"
    private val NVIDIA_URL     = "https://integrate.api.nvidia.com/v1/chat/completions"
    private val NVIDIA_MODEL   = "meta/llama-3.1-8b-instruct"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // Context from scan result (set when launched from ResultActivity)
    private var scanContext: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLocale()
        super.onCreate(savedInstanceState)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvChat.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        // Check if launched from scan result with leaf context
        scanContext = intent.getStringExtra("SCAN_CONTEXT")

        val welcomeMsg = if (scanContext != null) {
            "🌿 I can see the scan result for **${scanContext}**.\n\nYou can ask me:\n• What causes this disease?\n• How to treat it organically?\n• Which pesticide is safe to use?\n• How to prevent it next season?\n\nI need internet to answer."
        } else {
            "Hello! 🌿 I am your AI Plant Expert.\n\nAsk me:\n• How to treat tomato early blight?\n• What causes apple black rot?\n• Which pesticide is safe for potatoes?\n• How to identify leaf diseases?\n\nI need internet to answer. If offline, use the Disease Library."
        }
        addMessage(ChatMessage(welcomeMsg, false))

        // If scan context provided, auto-ask first question
        if (scanContext != null) {
            val autoQuestion = "I just scanned a plant leaf and it was diagnosed as: $scanContext. Please explain this disease simply and tell me the best treatment for an Indian farmer."
            binding.etMessage.setText(autoQuestion)
        }

        checkConnectivity()

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

    private fun checkConnectivity(): Boolean {
        val cm  = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork)
        val online = cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        binding.layoutOfflineNotice.visibility = if (online) View.GONE else View.VISIBLE
        binding.tvStatus.text = if (online)
            getString(R.string.ai_online_status)
        else
            getString(R.string.ai_offline_status)
        return online
    }

    private fun sendMessage(text: String) {
        if (!checkConnectivity()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        addMessage(ChatMessage(text, true))
        binding.tvTyping.visibility = View.VISIBLE
        binding.btnSend.isEnabled  = false

        scope.launch {
            val response = withContext(Dispatchers.IO) { callApi(text) }
            binding.tvTyping.visibility = View.GONE
            binding.btnSend.isEnabled   = true
            addMessage(ChatMessage(response, false))
        }
    }

    private fun callApi(userMessage: String): String {
        val systemPrompt = buildString {
            append("You are an expert agricultural assistant for Indian farmers, specializing in plant diseases and crop health. ")
            append("Answer clearly and simply in short paragraphs. Give practical advice. ")
            append("Always mention safety precautions for any chemical treatments. ")
            if (scanContext != null) {
                append("The farmer's plant was diagnosed as: $scanContext. Keep this context in mind when answering. ")
            }
        }

        // Try Sarvam first, fall back to NVIDIA
        val useSarvam = SARVAM_API_KEY != "sk_1blcusaf_CBHJwlBsnXClMJC0vlsqCetf"
        val useNvidia = NVIDIA_API_KEY != "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"

        if (!useSarvam && !useNvidia) {
            return "⚠️ No API key configured. Please contact the app developer to set up the AI service."
        }

        return if (useSarvam) {
            callEndpoint(SARVAM_URL, SARVAM_API_KEY, SARVAM_MODEL, systemPrompt, userMessage)
                .takeIf { !it.startsWith("ERROR:") }
                ?: if (useNvidia) callEndpoint(NVIDIA_URL, NVIDIA_API_KEY, NVIDIA_MODEL, systemPrompt, userMessage)
                   else "Sorry, could not reach AI service. Please try again."
        } else {
            callEndpoint(NVIDIA_URL, NVIDIA_API_KEY, NVIDIA_MODEL, systemPrompt, userMessage)
        }
    }

    private fun callEndpoint(url: String, key: String, model: String, system: String, user: String): String {
        return try {
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", system) })
                    put(JSONObject().apply { put("role", "user");   put("content", user)   })
                })
                put("temperature", 0.6)
                put("max_tokens", 512)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val resp = client.newCall(request).execute()
            val respBody = resp.body?.string() ?: return "ERROR: Empty response from server."

            if (!resp.isSuccessful) {
                // Parse error message if possible
                return try {
                    val errJson = JSONObject(respBody)
                    val errMsg = errJson.optJSONObject("error")?.optString("message")
                        ?: errJson.optString("detail")
                        ?: "HTTP ${resp.code}"
                    "ERROR: $errMsg"
                } catch (e: Exception) { "ERROR: HTTP ${resp.code}" }
            }

            val json   = JSONObject(respBody)
            var reply  = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
            // Remove <think> blocks from reasoning models
            reply = reply.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()
            if (reply.isEmpty()) return "I processed your question but got an empty response. Please try again."
            reply

        } catch (e: java.net.SocketTimeoutException) {
            "ERROR: Request timed out. The AI is taking too long — please try again."
        } catch (e: java.io.IOException) {
            "ERROR: Network error — ${e.message}"
        } catch (e: Exception) {
            "ERROR: ${e.message}"
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
        client.dispatcher.executorService.shutdown()
    }
}
