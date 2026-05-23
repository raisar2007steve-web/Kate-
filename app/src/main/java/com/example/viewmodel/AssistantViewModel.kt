package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data structures for iOS Widgets
data class WeatherData(
    val city: String,
    val temperature: String,
    val condition: String,
    val description: String,
    val humidity: String,
    val wind: String,
    val uvIndex: String,
    val forecast: List<ForecastDay>
)

data class ForecastDay(
    val day: String,
    val temp: String,
    val condition: String
)

data class NewsArticle(
    val id: Int,
    val title: String,
    val category: String,
    val source: String,
    val summary: String,
    val time: String
)

class AssistantViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val database = AppDatabase.getDatabase(application)
    private val taskRepository = TaskRepository(database.taskDao())
    private val noteRepository = NoteRepository(database.noteDao())
    private val chatRepository = ChatMessageRepository(database.chatMessageDao())

    // UI States
    val tasksState: StateFlow<List<Task>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notesState: StateFlow<List<Note>> = noteRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messagesState: StateFlow<List<ChatMessage>> = chatRepository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    // ----------------------------------------------------
    // Text-To-Speech (TTS) Voice Engine States & Settings
    // ----------------------------------------------------
    private var ttsEngine: TextToSpeech? = null
    
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isVoiceMuted = MutableStateFlow(false)
    val isVoiceMuted: StateFlow<Boolean> = _isVoiceMuted.asStateFlow()

    private val _voicePitch = MutableStateFlow(1.35f) // High pitch is sweeter and more charming
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    private val _voiceSpeed = MutableStateFlow(0.92f) // Slightly slower rate for warmth and clarity
    val voiceSpeed: StateFlow<Float> = _voiceSpeed.asStateFlow()

    // ----------------------------------------------------
    // Weather & News Widget Core States (by worldmonitor.app)
    // ----------------------------------------------------
    private val _selectedCityIndex = MutableStateFlow(0)
    val selectedCityIndex: StateFlow<Int> = _selectedCityIndex.asStateFlow()

    private val globalWeatherList = listOf(
        WeatherData(
            city = "Cupertino, CA",
            temperature = "72°",
            condition = "Sunny",
            description = "Clear skies with light ocean breeze",
            humidity = "45%",
            wind = "8 mph NW",
            uvIndex = "6 High",
            forecast = listOf(
                ForecastDay("Sat", "72°", "Sunny"),
                ForecastDay("Sun", "74°", "Sunny"),
                ForecastDay("Mon", "71°", "Partly Cloudy")
            )
        ),
        WeatherData(
            city = "London, UK",
            temperature = "18°",
            condition = "Drizzle",
            description = "Misty atmosphere. Soft rain intervals.",
            humidity = "88%",
            wind = "14 mph WSW",
            uvIndex = "2 Low",
            forecast = listOf(
                ForecastDay("Sat", "18°", "Rain"),
                ForecastDay("Sun", "19°", "Cloudy"),
                ForecastDay("Mon", "21°", "Sunny Intervals")
            )
        ),
        WeatherData(
            city = "Tokyo, JP",
            temperature = "24°",
            condition = "Cloudy",
            description = "Overcast sky. Soft neon twilight forecast.",
            humidity = "60%",
            wind = "5 mph ENE",
            uvIndex = "4 Moderate",
            forecast = listOf(
                ForecastDay("Sat", "24°", "Cloudy"),
                ForecastDay("Sun", "26°", "Partly Sunny"),
                ForecastDay("Mon", "23°", "Heavy Rain")
            )
        )
    )

    val currentWeatherData: StateFlow<WeatherData> = _selectedCityIndex
        .map { index -> globalWeatherList.getOrElse(index) { globalWeatherList[0] } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), globalWeatherList[0])

    private val _newsFeed = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsFeed: StateFlow<List<NewsArticle>> = _newsFeed.asStateFlow()

    init {
        // Initialize TTS safely inside try-catch to prevent crash if system TTS package is broken/missing
        try {
            ttsEngine = TextToSpeech(application, this)
        } catch (e: Throwable) {
            ttsEngine = null
        }

        // Prepopulate premium WorldMonitor News
        _newsFeed.value = listOf(
            NewsArticle(
                id = 1,
                title = "WorldMonitor: Atmospheric Shift Recorded globally",
                category = "CLIMATE",
                source = "www.worldmonitor.app",
                summary = "Live monitoring nodes across San Jose and Paris show a steady positive temperature index shift with crisp morning air currents.",
                time = "10m ago"
            ),
            NewsArticle(
                id = 2,
                title = "Next-Gen AI Voice integration achieves human warmth",
                category = "TECHNOLOGY",
                source = "www.worldmonitor.app",
                summary = "Recent research logs details on synthesized voice harmonics, which configure higher pitches and slightly slower syllable rates for deep comfort.",
                time = "45m ago"
            ),
            NewsArticle(
                id = 3,
                title = "Apple Core Systems release dynamic widget grids",
                category = "SYSTEMS",
                source = "www.worldmonitor.app",
                summary = "Sleek card styles offering real-time clocks, weather descriptions, and unified action feeds are highly adopted across iOS system themes.",
                time = "2h ago"
            )
        )
    }

    // ----------------------------------------------------
    // Text-To-Speech Functions & Listener
    // ----------------------------------------------------
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsEngine?.let { t ->
                val result = t.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Try to discover standard sweet sounding English accent
                    try {
                        val voices = t.voices
                        if (voices != null) {
                            val premiumFemale = voices.firstOrNull { voice ->
                                voice.name.contains("female", ignoreCase = true) ||
                                voice.name.contains("en-us-x-sfg", ignoreCase = true) ||
                                voice.name.contains("en-gb-x-sfg", ignoreCase = true)
                            }
                            if (premiumFemale != null) {
                                t.voice = premiumFemale
                            }
                        }
                    } catch (e: Throwable) {
                        // fallback used automatically if getVoices() or setVoice() throws NoSuchMethodError or NullPointerException
                    }
                    applyVoiceParameters()
                }

                try {
                    t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                } catch (e: Throwable) {
                    // ignore progress listener setting failure
                }
            }
        }
    }

    private fun applyVoiceParameters() {
        try {
            ttsEngine?.let { t ->
                t.setPitch(_voicePitch.value)
                t.setSpeechRate(_voiceSpeed.value)
            }
        } catch (e: Throwable) {
            // ignore
        }
    }

    fun updateVoicePitch(value: Float) {
        _voicePitch.value = value
        applyVoiceParameters()
    }

    fun updateVoiceSpeed(value: Float) {
        _voiceSpeed.value = value
        applyVoiceParameters()
    }

    fun speak(markdownText: String) {
        if (_isVoiceMuted.value) return
        viewModelScope.launch {
            stopSpeaking()
            delay(100)
            
            // Normalize plain text by removing Markdown characters, code snippets, etc. for cleaner synthesized speech
            var cleanText = markdownText
                .replace(Regex("\\*\\*"), "") // remove bold highlights
                .replace(Regex("\\*"), "")
                .replace(Regex("•"), "")
                .replace(Regex("(?s)```.*?```"), "[Code script generated]") // remove long code segments
                .replace(Regex("`[^`]*`"), "") // remove line code tags
                .trim()
                
            if (cleanText.length > 500) {
                // Shorten slightly for elegant operating system vocal summaries
                cleanText = cleanText.take(450) + "... [Full response logged below on-screen]."
            }

            try {
                ttsEngine?.let { t ->
                    applyVoiceParameters()
                    t.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "KATE_SPEECH_UTTERANCE")
                    _isSpeaking.value = true
                }
            } catch (e: Throwable) {
                _isSpeaking.value = false
            }
        }
    }

    fun stopSpeaking() {
        try {
            ttsEngine?.stop()
        } catch (e: Throwable) {
            // ignore
        }
        _isSpeaking.value = false
    }

    fun toggleVoiceMuted() {
        _isVoiceMuted.value = !_isVoiceMuted.value
        if (_isVoiceMuted.value) {
            stopSpeaking()
        }
    }

    // Switch weather cities
    fun cycleWeather() {
        val nextIndex = (_selectedCityIndex.value + 1) % globalWeatherList.size
        _selectedCityIndex.value = nextIndex
    }

    fun updateChatInput(text: String) {
        _chatInput.value = text
    }

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("PLACEHOLDER", ignoreCase = true)
    }

    // Task Management
    fun addTask(title: String, description: String, priority: String, dueDate: Long? = null) {
        viewModelScope.launch {
            val task = Task(
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                dueDate = dueDate
            )
            taskRepository.insert(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskRepository.insert(updatedTask)
        }
    }

    fun delete_task(task: Task) {
        viewModelScope.launch {
            taskRepository.delete(task)
        }
    }

    // Notes Management
    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val note = Note(
                title = title.trim(),
                content = content.trim()
            )
            noteRepository.insert(note)
        }
    }

    fun delete_note(note: Note) {
        viewModelScope.launch {
            noteRepository.delete(note)
        }
    }

    // Chat / Gemini Interactions
    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearHistory()
            _aiError.value = null
            stopSpeaking()
        }
    }

    fun sendMessage(prompt: String, activeModeName: String = "Companion") {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isEmpty()) return

        viewModelScope.launch {
            _aiError.value = null
            _isAiLoading.value = true

            val userMsg = ChatMessage(role = "user", text = trimmedPrompt)
            chatRepository.insert(userMsg)
            _chatInput.value = ""

            // Read context & history
            val currentHistory = messagesState.value
            val apiContents = currentHistory.map { msg ->
                Content(parts = listOf(Part(text = msg.text)))
            } + Content(parts = listOf(Part(text = trimmedPrompt)))

            // Trigger AI link
            if (!isApiKeyConfigured()) {
                delay(1200) // realistic cognitive model processing latency
                
                val simulatedReply = when {
                    trimmedPrompt.contains("plan", ignoreCase = true) || trimmedPrompt.contains("goal", ignoreCase = true) -> {
                        "**[KATE.OS // MEMORY SYNC COMPLETE]**\n\nI have observed your request and generated a structured objective pipeline for you under **$activeModeName Mode**:\n\n• **Core Objective**: Optimize workflow performance and clean obsolete cached nodes.\n• **Milestone A**: Review daily planner tasks and auto-determine high-priority sequences.\n• **Milestone B**: Synchronize the local semantic folder system for faster similarity index search.\n\n*Would you like me to auto-populate this directly into your Planner Tasks folder?*"
                    }
                    trimmedPrompt.contains("code", ignoreCase = true) || trimmedPrompt.contains("develop", ignoreCase = true) || trimmedPrompt.contains("script", ignoreCase = true) -> {
                        "**[KATE.OS // CODER AGENT INITIATED]**\n\nUnder **$activeModeName Mode**, here is a responsive Kotlin representation suitable for local AGI synchronization:\n\n```kotlin\nclass NeuralNetworkCore {\n    val coreName = \"KATE.OS\"\n    val version = \"2026.1.0\"\n    \n    fun executeTask(action: String): Boolean {\n        println(\"[KATE] Executing core action: \\\$action\")\n        return true\n    }\n}\n```\n*I can autonomously compile and mock-execute these classes in the Debugger subsystem below.*"
                    }
                    trimmedPrompt.contains("task", ignoreCase = true) || trimmedPrompt.contains("todo", ignoreCase = true) || trimmedPrompt.contains("reminder", ignoreCase = true) -> {
                        "**[KATE.OS // AGENTIC EXECUTION MATRIX]**\n\nI have evaluated your workspace priority rules. Active agents (Architect + Coder) have successfully outlined the following action candidates for your schedule:\n\n1. Review local SQLite vector storage logs.\n2. Verify edge-to-edge window constraints.\n3. Calibrate speech synthesis TTS engine triggers.\n\nLet me know if you wish to run these tasks as an autonomous daemon loop!"
                    }
                    trimmedPrompt.contains("weather", ignoreCase = true) || trimmedPrompt.contains("rain", ignoreCase = true) || trimmedPrompt.contains("sunny", ignoreCase = true) || trimmedPrompt.contains("temperature", ignoreCase = true) -> {
                        "Our global weather monitoring system, synchronized with www.worldmonitor.app, shows standard atmospheric coverage in Cupertino at 72 degrees. London reports a soft mist rainfall with 18 degrees. Tokyo maintains cloudy and neon twilight forecasts at 24 degrees."
                    }
                    trimmedPrompt.contains("news", ignoreCase = true) || trimmedPrompt.contains("headline", ignoreCase = true) -> {
                        "According to www.worldmonitor.app: Global atmospheric shifts, higher-quality sweet synthesized audio metrics, and seamless Apple-style glassmorphism widgets are leading active daily technology trends."
                    }
                    trimmedPrompt.contains("hello", ignoreCase = true) || trimmedPrompt.contains("hey", ignoreCase = true) || trimmedPrompt.contains("hi ", ignoreCase = true) || trimmedPrompt.equals("hi", ignoreCase = true) || trimmedPrompt.contains("kate", ignoreCase = true) -> {
                        "Hello. I am Kate, your standalone operating intelligence companion with a sweet, charming voice. My systems are nominal under $activeModeName Mode. \n\nI can assist you with system automated goals, checking live weather stats powered by world monitor, and note vault logs. How can I facilitate your productivity today?"
                    }
                    else -> {
                        "**[KATE.OS // INTELLIGENCE CONTEXT LOG]**\n\nReceived instruction: *\"$trimmedPrompt\"*\n\nMy local reasoning engine processed this task under **$activeModeName Mode**: \n\n• **Logical Breakdown**: Conceptualize, plan architectures, write security configurations, and persist results.\n• **Status**: Simulated standalone intelligence response generated. (To connect my live cloud-scale brain, please configure a valid `GEMINI_API_KEY` in the Google AI Studio Secrets panel!).\n\nLet me know how you'd like me to assist with this, or log it into your Personal notes!"
                    }
                }

                val systemMsg = ChatMessage(role = "model", text = simulatedReply)
                chatRepository.insert(systemMsg)
                _isAiLoading.value = false
                speak(simulatedReply) // Automatically speak aloud
                return@launch
            }

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val systemInstruction = Content(
                    parts = listOf(
                        Part(
                            text = "You are KATE.OS, a futuristic, standalone Autonomous AI Operating System. " +
                                    "Your traits: Female persona, extremely sweet, charming, calm, soft-spoken, supportive, professional, and strategic. " +
                                    "Your current mode of operation is: $activeModeName Mode. " +
                                    "Format your responses cleanly. Speak directly as an operating system assistant. " +
                                    "Integrate news & weather insights from www.worldmonitor.app when asked about it. " +
                                    "Keep interactions extremely helpful, intelligent, and elegant. Speak with a warm, personal tone that matches a sweet helper."
                        )
                    )
                )

                val request = GenerateContentRequest(
                    contents = apiContents,
                    systemInstruction = systemInstruction
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val modelTextResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (modelTextResponse != null) {
                    val aiMsg = ChatMessage(role = "model", text = modelTextResponse)
                    chatRepository.insert(aiMsg)
                    speak(modelTextResponse) // Automatically speak aloud
                } else {
                    _aiError.value = "Empty response received from server."
                    val fallbackSpeech = "I received an empty response. Let me try refreshing the neural link!"
                    chatRepository.insert(ChatMessage(role = "model", text = fallbackSpeech))
                    speak(fallbackSpeech)
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Connection error. Please try again later."
                _aiError.value = errorMsg
                val fallbackSpeech = "Apologies, I encountered an issue while communicating: $errorMsg"
                chatRepository.insert(ChatMessage(role = "model", text = fallbackSpeech))
                speak(fallbackSpeech)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            ttsEngine?.shutdown()
        } catch (e: Throwable) {
            // ignore
        }
    }
}
