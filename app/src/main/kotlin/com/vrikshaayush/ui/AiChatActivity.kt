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
import java.io.IOException
import java.util.Locale

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatAdapter(private val messages: MutableList<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val layout: LinearLayout = view.findViewById(R.id.layoutBubble)
        val sender: TextView = view.findViewById(R.id.tvSender)
        val msg: TextView = view.findViewById(R.id.tvMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return VH(v)
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = messages[position]
        holder.msg.text = item.text
        if (item.isUser) {
            holder.sender.text = holder.itemView.context.getString(R.string.you_label)
            holder.sender.setTextColor(holder.itemView.context.getColor(R.color.primary_green))
            holder.layout.setBackgroundColor(holder.itemView.context.getColor(R.color.primary_green_light))
            (holder.itemView as LinearLayout).gravity = android.view.Gravity.END
            holder.layout.layoutParams = (holder.layout.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 60
                marginEnd = 0
            }
        } else {
            holder.sender.text = holder.itemView.context.getString(R.string.ai_label)
            holder.sender.setTextColor(holder.itemView.context.getColor(R.color.primary_green_dark))
            holder.layout.setBackgroundColor(holder.itemView.context.getColor(R.color.card_white))
            (holder.itemView as LinearLayout).gravity = android.view.Gravity.START
            holder.layout.layoutParams = (holder.layout.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 0
                marginEnd = 60
            }
        }
    }
}

class AiChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiChatBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ── NVIDIA API KEY ──────────────────────────────────────
    private val NVIDIA_API_KEY = "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"
    private val API_URL = "https://integrate.api.nvidia.com/v1/chat/completions"
    private val MODEL = "meta/llama-3.1-8b-instruct"

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLocale()
        super.onCreate(savedInstanceState)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.rvChat.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        // Welcome message
        addMessage(ChatMessage(
            "Hello! 🌿 I am your AI Plant Expert.\n\nYou can ask me:\n• How to treat tomato early blight?\n• What causes apple black rot?\n• Which pesticide is safe for potatoes?\n• How to identify leaf diseases?\n\nI need internet to answer. If offline, use the Disease Library instead.",
            false
        ))

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
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cap = cm.getNetworkCapabilities(cm.activeNetwork)
        val online = cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        binding.layoutOfflineNotice.visibility = if (online) View.GONE else View.VISIBLE
        if (online) {
            binding.tvStatus.text = getString(R.string.ai_online_status)
        } else {
            binding.tvStatus.text = getString(R.string.ai_offline_status)
        }
        return online
    }

    private fun sendMessage(text: String) {
        if (!checkConnectivity()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        addMessage(ChatMessage(text, true))
        binding.tvTyping.visibility = View.VISIBLE

        scope.launch {
            val response = withContext(Dispatchers.IO) {
                callNvidiaApi(text)
            }
            binding.tvTyping.visibility = View.GONE
            addMessage(ChatMessage(response, false))
        }
    }

    private fun callNvidiaApi(userMessage: String): String {
        return try {
            val systemPrompt = "You are an expert agricultural assistant specializing in plant diseases, crop health, and farming in India. Answer questions about plant diseases, symptoms, causes, organic and chemical treatments clearly and simply. Keep answers concise and practical for Indian farmers. Always mention safety precautions when recommending chemicals."

            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 512)
            }

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $NVIDIA_API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return "Sorry, no response received."

            if (!response.isSuccessful) {
                return "Error: Could not get response. Please try again."
            }

            val json = JSONObject(responseBody)
            var reply = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
            // Remove any <think>...</think> reasoning blocks (for thinking models)
            reply = reply.replace(Regex("<think>[\s\S]*?</think>"), "").trim()
            if (reply.isEmpty()) reply = "I processed your question but got an empty response. Please try again."
            reply

        } catch (e: IOException) {
            "Network error. Please check your connection and try again."
        } catch (e: Exception) {
            "Sorry, something went wrong. Please try again."
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
