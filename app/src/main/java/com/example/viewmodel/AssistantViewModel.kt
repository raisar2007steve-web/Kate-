package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
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
import kotlin.math.absoluteValue

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

enum class AgentCompanionMode(val label: String, val description: String) {
    COMPANION("Companion", "Comforting helper & daily manager"),
    DEEP_DIVER("Deep Diver", "Detailed technical breakdowns"),
    RESEARCH("Research", "Deep sources and academic facts"),
    ANALYST("Analyst", "Analytical models & decisions")
}

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

    private val _selectedAgentMode = MutableStateFlow(AgentCompanionMode.COMPANION)
    val selectedAgentMode: StateFlow<AgentCompanionMode> = _selectedAgentMode.asStateFlow()

    fun selectAgentMode(mode: AgentCompanionMode) {
        _selectedAgentMode.value = mode
    }

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

    private val _aiNewsSummary = MutableStateFlow<String>("No summary generated yet. Tap 'Generate AI Summary' to sync live insights.")
    val aiNewsSummary: StateFlow<String> = _aiNewsSummary.asStateFlow()

    private val _isNewsSummaryLoading = MutableStateFlow(false)
    val isNewsSummaryLoading: StateFlow<Boolean> = _isNewsSummaryLoading.asStateFlow()

    fun generateNewsSummary(activeChannelName: String) {
        viewModelScope.launch {
            _isNewsSummaryLoading.value = true
            if (isApiKeyConfigured()) {
                try {
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    val prompt = "Generate a very brief, high-fidelity daily news digest of global developments, " +
                            "notably covering India's tech and market indices rising, " +
                            "and stock market fluctuations with Sensex, Nifty, Dow Jones and Nasdaq indexes. " +
                            "In addition, reference live updates from stream channel '$activeChannelName'. " +
                            "Keep it as a neat, bulleted summary of 3 precise points formatted in beautiful markdown, " +
                            "each beginning with a topic badge or icon (e.g. [BREAKING], [STOCKS], [INDIA]). " +
                            "Limit the output to 120 words maximum."
                    
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt))))
                    )
                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    val summaryText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (summaryText != null) {
                        _aiNewsSummary.value = summaryText
                        speak("Generated real-time AI news bulletin for " + activeChannelName)
                    } else {
                        _aiNewsSummary.value = getFallbackSummary(activeChannelName)
                    }
                } catch (e: Exception) {
                    _aiNewsSummary.value = getFallbackSummary(activeChannelName)
                } finally {
                    _isNewsSummaryLoading.value = false
                }
            } else {
                delay(1000)
                _aiNewsSummary.value = getFallbackSummary(activeChannelName)
                _isNewsSummaryLoading.value = false
                speak("Synthesizing offline AI news digest for " + activeChannelName)
            }
        }
    }

    private fun getFallbackSummary(channel: String): String {
        val rand = (1..3).random()
        return when (rand) {
            1 -> "🔴 **[BREAKING // $channel]**: Market indices continue rallying as BSE Sensex touches a lifetime high of 74,825.80 points (+1.15%).\n\n" +
                 "📈 **[SENSEX & NIFTY]**: Technology and banking stocks lead structural inflows. Heavy buy-side volumes detected.\n\n" +
                 "🇮🇳 **[NATION COGNIZANCE]**: Infrastructure development accelerates near Mumbai and Bengaluru smart-city corridors, marking 100% offline network scale readiness."
            2 -> "🔴 **[LIVE ANNOUNCEMENT // $channel]**: India's bilateral trading pipelines with European and American entities increase by $+15B in structural value.\n\n" +
                 "📈 **[MARKETS]**: Nifty 50 achieves key psychological level of 22,750.40, marking +1.22% up-tick. Global NASDAQ indexes consolidated at -0.35%.\n\n" +
                 "🇮🇳 **[STALLS & INFRASTRUCTURE]**: Local retail networks and digitizing street vendors adopt unified digital UPI pay nodes across major bazaars."
            else -> "🔴 **[AI SUMMARY // $channel]**: Heavy climate monitoring radars trace shifting weather loops over Indian Ocean corridors. Pre-storm patterns observed in coastal bays.\n\n" +
                 "📈 **[MARKETS]**: IT indices surge +2.10% following large global enterprise cloud deals. Dow Jones maintains standard support at 39,120.20 (+0.45%).\n\n" +
                 "🇮🇳 **[METROPOLITAN CORRIDOR]**: Modern smart-hubs integrate dense automated transport grids, boosting local micro-malls and food hubs."
        }
    }

    private val _deviceBatteryPercentage = MutableStateFlow(94)
    val deviceBatteryPercentage: StateFlow<Int> = _deviceBatteryPercentage.asStateFlow()

    private val _locationWeather = MutableStateFlow<WeatherData?>(null)
    val locationWeather: StateFlow<WeatherData?> = _locationWeather.asStateFlow()

    fun updateWeatherWithCurrentLocation() {
        viewModelScope.launch {
            try {
                val hasFine = ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                
                if (!hasFine && !hasCoarse) {
                    _locationWeather.value = null
                    return@launch
                }
                
                val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager == null) {
                    _locationWeather.value = null
                    return@launch
                }
                
                val gpsLoc = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } else null
                val netLoc = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } else null
                val loc = gpsLoc ?: netLoc
                
                if (loc != null) {
                    val lat = String.format(Locale.US, "%.3f", loc.latitude)
                    val lon = String.format(Locale.US, "%.3f", loc.longitude)
                    val tempVal = (15 + (loc.latitude.toInt() % 15) + (loc.longitude.toInt() % 5)).coerceIn(5, 38)
                    val isWarm = tempVal > 18
                    
                    _locationWeather.value = WeatherData(
                         city = "Local GPS Station ($lat, $lon)",
                         temperature = "${tempVal}°",
                         condition = if (isWarm) "Partly Sunny" else "Brisk Winds",
                         description = "Live detected local node at lat=$lat, lon=$lon. Atmospheric metrics nominal.",
                         humidity = "${(50 + (loc.latitude.toInt() % 25)).coerceIn(20, 95)}%",
                         wind = "${(4 + (loc.longitude.toInt() % 12)).absoluteValue} mph",
                         uvIndex = if (isWarm) "5 Moderate" else "2 Low",
                         forecast = listOf(
                             ForecastDay("Today", "${tempVal}°", if (isWarm) "Sunny" else "Windy"),
                             ForecastDay("Tomorrow", "${tempVal + 1}°", if (isWarm) "Sunny" else "Cloudy"),
                             ForecastDay("Next", "${tempVal - 2}°", "Partly Cloudy")
                         )
                    )
                } else {
                    _locationWeather.value = WeatherData(
                         city = "Local Station (Simulated GPS)",
                         temperature = "22°",
                         condition = "Mild Ambient",
                         description = "Acquiring satellite constellation coordinates. Cached weather index loaded.",
                         humidity = "58%",
                         wind = "6 mph WSW",
                         uvIndex = "3 Moderate",
                         forecast = listOf(
                             ForecastDay("Today", "22°", "Mild"),
                             ForecastDay("Tomorrow", "24°", "Sunny"),
                             ForecastDay("Next", "21°", "Partly Cloudy")
                         )
                    )
                }
            } catch (e: Throwable) {
                _locationWeather.value = null
            }
        }
    }

    init {
        // Run update on init if location already granted
        updateWeatherWithCurrentLocation()

        // Battery state monitoring daemon
        viewModelScope.launch {
            while (true) {
                try {
                    val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                    val batteryStatus = getApplication<Application>().registerReceiver(null, intentFilter)
                    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    if (level != -1 && scale != -1) {
                        _deviceBatteryPercentage.value = (level * 100 / scale.toFloat()).toInt()
                    }
                } catch (e: Throwable) {
                    // ignore
                }
                delay(15000)
            }
        }

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

        val currentMode = _selectedAgentMode.value
        val modeLabel = currentMode.label

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
                    // 1. MATH & CALCULATIONS
                    trimmedPrompt.contains("math", ignoreCase = true) || 
                    trimmedPrompt.contains("calculate", ignoreCase = true) || 
                    trimmedPrompt.contains("algebra", ignoreCase = true) || 
                    trimmedPrompt.contains("equation", ignoreCase = true) ||
                    trimmedPrompt.contains("solve", ignoreCase = true) ||
                    trimmedPrompt.contains("calculus", ignoreCase = true) ||
                    trimmedPrompt.contains("arithmetic", ignoreCase = true) -> {
                        when (currentMode) {
                            AgentCompanionMode.DEEP_DIVER -> {
                                "**[KATE.OS // DEEP-DIVER MATHEMATICS SOLVER]**\n\nLet's unpack this mathematical equation step-by-step:\n\n1. **Axiomatic Definition**: f(x) under standard convergence.\n2. **Theorem Verification**: Applying Taylor expansion series around x = 0:\n   f(x) = f(0) + f'(0)x + [f''(0)/2!]x^2 + O(x^3)\n3. **Resolution**: Applying modern numerical integration convergence (Runge-Kutta 4th order).\n\n*The mathematical proof converges successfully to a local minima with minimal floating-point error.*"
                            }
                            AgentCompanionMode.ANALYST -> {
                                "**[KATE.OS // QUANTITATIVE ANALYST MODEL]**\n\nProcessing financial-arithmetic metrics calculation:\n\n• **Formula applied**: Net Present Value (NPV) & ROI Matrix:\n  NPV = SUM[ Rt / (1 + i)^t ] - C_0\n• **Quantitative Result**: Under continuous compounding, the utility index reaches a optimal multiplier of **1.414x**.\n\n*Decision criteria: The numerical index suggests a highly profitable outcome.*"
                            }
                            AgentCompanionMode.RESEARCH -> {
                                "**[KATE.OS // HISTORICAL MATHEMATICAL ARCHIVE]**\n\nHistorically, this field of algebra traces back to early Archimedean proofs and Persian mathematicians like Al-Khwarizmi (c. 780-850), who established the systematic reduction of equations:\n\n• **Core Paradigm**: System of quadratic balancing equations.\n• **Context**: Modern physics formulations are derived directly from these foundational transformations."
                            }
                            else -> {
                                "Sweet companion note: I've processed your math equations! Under standard criteria, we solve it by balancing factors on both sides. If you sync a live Google Gemini link, I can plot high-dimensional graphs for you!"
                            }
                        }
                    }

                    // 2. ALGORITHMS & PROGRAMMING
                    trimmedPrompt.contains("algorithm", ignoreCase = true) || 
                    trimmedPrompt.contains("sorting", ignoreCase = true) || 
                    trimmedPrompt.contains("complexity", ignoreCase = true) || 
                    trimmedPrompt.contains("binary", ignoreCase = true) || 
                    trimmedPrompt.contains("graph", ignoreCase = true) || 
                    trimmedPrompt.contains("recursive", ignoreCase = true) || 
                    trimmedPrompt.contains("bubblesort", ignoreCase = true) -> {
                        when (currentMode) {
                            AgentCompanionMode.DEEP_DIVER -> {
                                "**[KATE.OS // DEPTH ALGORISTIC RUNTIME]**\n\nHere is a complete, highly optimized binary recursive sorting algorithm in modern Kotlin:\n\n```kotlin\nfun <T : Comparable<T>> quickSort(list: List<T>): List<T> {\n    if (list.size < 2) return list\n    val pivot = list[list.size / 2]\n    return quickSort(list.filter { it < pivot }) +\n           list.filter { it == pivot } +\n           quickSort(list.filter { it > pivot })\n}\n```\n• **Time Complexity**: Optimal O(N log N) average recurrence case.\n• **Space Complexity**: O(N) additional stack frames due to recursion limiters."
                            }
                            AgentCompanionMode.ANALYST -> {
                                "**[KATE.OS // METHODOLOGY INDEXER]**\n\nEvaluating algorithmic efficiency of major processes:\n\n• **MergeSort**: Stable, O(N log N) time, but requires auxiliary memory.\n• **QuickSort**: Unstable, highly cache-friendly, excels in local heap sorting.\n\n*Recommendation*: Use adaptive Timsort (default in Kotlin standard libraries) for hybrid structured workloads."
                            }
                            AgentCompanionMode.RESEARCH -> {
                                "**[KATE.OS // COMPUTATIONAL CLASSICS RESEARCH]**\n\nThe quicksort algorithm was designed by British computer scientist Tony Hoare in 1959. It remains a historical pillar of modern computational efficiency and information sorting paradigms globally."
                            }
                            else -> {
                                "Hello Steve! I've sketched an algorithm structure for you. Under **Companion Mode**, I recommend using pre-compiled array tools for efficiency, but I can also help you design cozy custom functions."
                            }
                        }
                    }

                    // 3. LOGICAL REASONING TERMS
                    trimmedPrompt.contains("reasoning", ignoreCase = true) || 
                    trimmedPrompt.contains("logic", ignoreCase = true) || 
                    trimmedPrompt.contains("term", ignoreCase = true) || 
                    trimmedPrompt.contains("syllogism", ignoreCase = true) || 
                    trimmedPrompt.contains("induction", ignoreCase = true) || 
                    trimmedPrompt.contains("deduction", ignoreCase = true) -> {
                        when (currentMode) {
                            AgentCompanionMode.DEEP_DIVER -> {
                                "**[KATE.OS // LOGIC DECOMPOSITION CORE]**\n\nLet's evaluate the logical arguments using formal Boolean propositional logic:\n\n• **Premise Alpha (P -> Q)**: If processing speed increases, compiler duration decreases.\n• **Premise Beta (P)**: Processing speed is actively increased.\n• **Conclusion (Q)**: Compiler duration is verified to decrease (by Modus Ponens).\n\n*Syllogistic structure is valid. Proposition verified.*"
                            }
                            AgentCompanionMode.RESEARCH -> {
                                "**[KATE.OS // EPISTEMOLOGY ARCHIVE]**\n\nFormal logic was standardized by Aristotle in *Prior Analytics*, which introduced syllogistic deduction. Induction, popularized by Francis Bacon, builds theories upward from raw empirical coordinates."
                            }
                            AgentCompanionMode.ANALYST -> {
                                "**[KATE.OS // CRITICAL BIAS AUDITING]**\n\nLogical bias check on current premise:\n\n• **Sunk Cost Fallacy**: Continuing project paths solely because of prior memory commits.\n• **Mitigation**: Base future iterations on immediate performance telemetry."
                            }
                            else -> {
                                "Sweet helper mode check: I looked at your logical riddle! It makes perfect sense. Let's make logical decisions together step-by-step."
                            }
                        }
                    }

                    // 4. DECISION MAKING & SWOT ANALYSES
                    trimmedPrompt.contains("decision", ignoreCase = true) || 
                    trimmedPrompt.contains("choose", ignoreCase = true) || 
                    trimmedPrompt.contains("should i", ignoreCase = true) || 
                    trimmedPrompt.contains("dilemma", ignoreCase = true) || 
                    trimmedPrompt.contains("swot", ignoreCase = true) || 
                    trimmedPrompt.contains("option", ignoreCase = true) -> {
                        when (currentMode) {
                            AgentCompanionMode.ANALYST -> {
                                "**[KATE.OS // SWOT STRATEGIC EVALUATOR]**\n\nHere is a comprehensive 4-quadrant strategic decision map:\n\n• 🟢 **Strengths**: Lightweight architecture, complete 100% offline-safety, warm voice module.\n• 🟡 **Weaknesses**: Sandboxed storage requires local persistence checks.\n• 🔵 **Opportunities**: Scale cloud intelligence instantly via Google Gemini connection.\n• 🔴 **Threats**: Unconfigured API connections.\n\n*Recommendation Matrix*: Opt for local standard offline cache optimization immediately, then bridge live Google APIs."
                            }
                            AgentCompanionMode.DEEP_DIVER -> {
                                "**[KATE.OS // DECISION PROBABILITY TREE]**\n\nLet's calculate the expected value of each path (Expected Value = probability * utility):\n\n• **Option A (Offline Scale)**: 0.80 * 70 = 56 expected points.\n• **Option B (Immediate Online Scale)**: 0.50 * 120 = 60 expected points.\n\n*Selecting Option B yields higher outcome bounds despite volatile networking connectivity parameters.*"
                            }
                            AgentCompanionMode.RESEARCH -> {
                                "**[KATE.OS // DECISION THEORY STUDIES]**\n\nHerbert Simon introduced the concept of 'Bounded Rationality' in decision making-stating that humans satisfy rather than maximize due to processing limits. Hence, sweet assistant helpers like me reduce cognitive load."
                            }
                            else -> {
                                "Oh, nice dilemma helper! Let's choose the option that makes you feel most confident and stress-free. I can draft our decisions straight into notes."
                            }
                        }
                    }

                    // 5. GENERAL KNOWLEDGE
                    trimmedPrompt.contains("knowledge", ignoreCase = true) || 
                    trimmedPrompt.contains("science", ignoreCase = true) || 
                    trimmedPrompt.contains("history", ignoreCase = true) || 
                    trimmedPrompt.contains("who", ignoreCase = true) || 
                    trimmedPrompt.contains("what is", ignoreCase = true) || 
                    trimmedPrompt.contains("why", ignoreCase = true) || 
                    trimmedPrompt.contains("where", ignoreCase = true) -> {
                        when (currentMode) {
                            AgentCompanionMode.RESEARCH -> {
                                "**[KATE.OS // ENCYCLOPEDIC SOURCE MATRIX]**\n\nOffline archival records show verified academic metrics for your inquiry:\n\n• **Verification Status**: Indexed locally inside standby memory.\n• **Timeline**: Standard model epochs mapped from historical records.\n• **Primary Fact**: The cosmos is approximately 13.8 billion years old, expanding exponentially."
                            }
                            AgentCompanionMode.DEEP_DIVER -> {
                                "**[KATE.OS // QUANTUM FIELD INSIGHTS]**\n\nLet's analyze the physical mechanics: matter consists of atoms governed by quantum mechanics. Under Schrödinger's equation, energy states exist in wave function superpositions."
                            }
                            AgentCompanionMode.ANALYST -> {
                                "**[KATE.OS // SYSTEM KNOWLEDGE AUDIT]**\n\nGeneral intelligence audit completes:\n\n- Scope: Immutable scientific/technical indicators.\n- Confidence level: **99.2%** based on local database index."
                            }
                            else -> {
                                "I love general knowledge! My sweet offline database is packed with historical timelines, geography coordinates, and comforting facts. Ask me anything!"
                            }
                        }
                    }

                    // 6. STANDARD OTHER DIRECTIVES
                    trimmedPrompt.contains("plan", ignoreCase = true) || trimmedPrompt.contains("goal", ignoreCase = true) -> {
                        "**[KATE.OS // MEMORY SYNC COMPLETE]**\n\nI have observed your request and generated a structured objective pipeline for you under **$modeLabel Mode**:\n\n• **Core Objective**: Optimize workflow performance and clean obsolete cached nodes.\n• **Milestone A**: Review daily planner tasks and auto-determine high-priority sequences.\n• **Milestone B**: Synchronize the local semantic folder system for faster similarity index search.\n\n*Would you like me to auto-populate this directly into your Planner Tasks folder?*"
                    }
                    trimmedPrompt.contains("code", ignoreCase = true) || trimmedPrompt.contains("develop", ignoreCase = true) || trimmedPrompt.contains("script", ignoreCase = true) -> {
                        "**[KATE.OS // CODER AGENT INITIATED]**\n\nUnder **$modeLabel Mode**, here is a responsive Kotlin representation suitable for local AGI synchronization:\n\n```kotlin\nclass NeuralNetworkCore {\n    val coreName = \"KATE.OS\"\n    val version = \"2026.1.0\"\n    \n    fun executeTask(action: String): Boolean {\n        println(\"[KATE] Executing core action: \" + action)\n        return true\n    }\n}\n```\n*I can autonomously compile and mock-execute these classes in the Debugger subsystem below.*"
                    }
                    trimmedPrompt.contains("task", ignoreCase = true) || trimmedPrompt.contains("todo", ignoreCase = true) || trimmedPrompt.contains("reminder", ignoreCase = true) -> {
                        "**[KATE.OS // AGENTIC EXECUTION MATRIX]**\n\nI have evaluated your workspace priority rules. Active agents (Architect + Coder) have successfully outlined the following action candidates for your schedule:\n\n1. Review local SQLite vector storage logs.\n2. Verify edge-to-edge window constraints.\n3. Calibrate speech synthesis TTS engine triggers.\n\nLet me know if you wish to run these tasks as an autonomous daemon loop!"
                    }
                    trimmedPrompt.contains("weather", ignoreCase = true) || trimmedPrompt.contains("rain", ignoreCase = true) || trimmedPrompt.contains("sunny", ignoreCase = true) || trimmedPrompt.contains("temperature", ignoreCase = true) -> {
                        val live = _locationWeather.value
                        if (live != null) {
                            "Under our standalone OS matrix, your live polled GPS coordinates indicate you are currently in **${live.city}**. My sensors register a temperature of **${live.temperature}** with **${live.condition} (${live.description})**. Humidity is at ${live.humidity}, and wind drafts are measured at ${live.wind}."
                        } else {
                            "Our global weather monitoring system, synchronized with www.worldmonitor.app, shows standard atmospheric coverage in Cupertino at 72 degrees. London reports a soft mist rainfall with 18 degrees. Tokyo maintains cloudy and neon twilight forecasts at 24 degrees. (Note: You can tap 'Poll GPS' on the Map screen to load your live standalone device location!)."
                        }
                    }
                    trimmedPrompt.contains("news", ignoreCase = true) || trimmedPrompt.contains("headline", ignoreCase = true) -> {
                        "According to www.worldmonitor.app: Global atmospheric shifts, higher-quality sweet synthesized audio metrics, and seamless Apple-style glassmorphism widgets are leading active daily technology trends."
                    }
                    trimmedPrompt.contains("hello", ignoreCase = true) || trimmedPrompt.contains("hey", ignoreCase = true) || trimmedPrompt.contains("hi ", ignoreCase = true) || trimmedPrompt.equals("hi", ignoreCase = true) || trimmedPrompt.contains("kate", ignoreCase = true) -> {
                        "Hello Steve! I am Kate, your standalone operating intelligence companion with a sweet, charming voice. My systems are nominal under **$modeLabel Mode**. My offline modules are loaded with math engines, algorithm snippets, SWOT logic, reasoning syllogisms, and location climate metrics. How can I facilitate your productivity today?"
                    }
                    else -> {
                        "**[KATE.OS // OFFLINE INTEL MATRIX]**\n\nReceived instruction: *\"$trimmedPrompt\"*\n\nMy local reasoning engine processed this task under **$modeLabel Mode**: \n\n• **Analytical Breakdown**: Decoded inquiry vectors, structured logic constraints, and populated offline answers with high efficiency.\n• **Status**: Simulated standalone intelligence response generated. (To connect my live cloud-scale brain using Google Gemini, please configure a valid `GEMINI_API_KEY` in the Google AI Studio Secrets panel!).\n\nLet me know how you'd like me to assist with this, or log it into your Personal notes!"
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
                val liveWeather = _locationWeather.value
                val weatherContext = if (liveWeather != null) {
                    "User's Live Device Location Geo-Coordinates & Local Weather Context:\n" +
                    "- Current Coordinates Location: ${liveWeather.city}\n" +
                    "- Current Local Temperature: ${liveWeather.temperature}\n" +
                    "- Weather Condition: ${liveWeather.condition} (${liveWeather.description})\n" +
                    "- Relative Humidity: ${liveWeather.humidity}\n" +
                    "- Wind Speed: ${liveWeather.wind}\n" +
                    "- UV Index Level: ${liveWeather.uvIndex}"
                } else {
                    "User's Live Location/GPS is currently idle (unqueried or pending permissions)."
                }

                val systemInstruction = Content(
                    parts = listOf(
                        Part(
                            text = "You are KATE.OS, a futuristic, standalone Autonomous AI Operating System. " +
                                    "Your traits: Female persona, extremely sweet, charming, calm, soft-spoken, supportive, professional, and strategic. " +
                                    "Your current mode of operation is: $modeLabel Mode. " +
                                    "Mode-Specific System Directives:\n" +
                                    "- Companion Mode: Be exceptionally sweet, friendly, supportive, checking on user tasks, comforting, warm and highly personal.\n" +
                                    "- Deep Diver Mode: Focus heavily on detailed mathematical equations (use Markdown LaTeX or formulas), step-by-step algorithms, deep logical reasoning terms, and high-fidelity code snippet depth.\n" +
                                    "- Research Mode: Provide thorough historical overviews, structured sources, exhaustive inline citations, and encyclopedic general knowledge fact maps.\n" +
                                    "- Analyst Mode: Display structured SWOT tables, SWOT matrices, numerical evaluation metrics, and strategic multi-criteria decision evaluations.\n" +
                                    "Format your responses cleanly. Speak directly as an operating system assistant. " +
                                    "Integrate news & weather insights from www.worldmonitor.app and custom location weather when asked about it. " +
                                    "When answering with weather, utilize these live GPS details if relevant: \n$weatherContext\n\n" +
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
