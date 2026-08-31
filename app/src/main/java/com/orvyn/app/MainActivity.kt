package com.orvyn.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

val KEY_PART_A = "AQ.Ab8RN6LitSVW_"
val KEY_PART_B = "vuXcnJmzxNTgzmCeJoh0cvfswbBKZA2tZPoeA"
val ACTIVE_KEY: String get() = KEY_PART_A + KEY_PART_B

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    var tts: TextToSpeech? = null
    var onSpeechComplete: (() -> Unit)? = null
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("winter_arc_vault", Context.MODE_PRIVATE)
        tts = TextToSpeech(this, this)

        checkAndApplyMidnightReset(prefs)

        setContent {
            OrvynTheme {
                OrvynMainHub(
                    prefs = prefs,
                    speakOut = { text, onDone -> speakText(text, onDone) }
                )
            }
        }
    }

    private fun checkAndApplyMidnightReset(p: SharedPreferences) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastDate = p.getString("last_active_date", "")
        if (lastDate != today) {
            val allDoneYesterday = p.getBoolean("w_h1", false) &&
                    p.getBoolean("w_h2", false) &&
                    p.getBoolean("w_h3", false) &&
                    p.getBoolean("w_h4", false)

            val currentStreak = p.getInt("streak_count", 0)
            val currentDay = p.getInt("arc_day", 1)

            p.edit().apply {
                if (lastDate.isNullOrEmpty()) {
                    putInt("streak_count", 1)
                    putInt("arc_day", 1)
                } else if (allDoneYesterday) {
                    putInt("streak_count", currentStreak + 1)
                    putInt("arc_day", (currentDay + 1).coerceAtMost(90))
                } else {
                    putInt("streak_count", 0)
                }
                putBoolean("w_h1", false)
                putBoolean("w_h2", false)
                putBoolean("w_h3", false)
                putBoolean("w_h4", false)
                putString("last_active_date", today)
                apply()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val hindi = Locale("hi", "IN")
            tts?.language = hindi

            try {
                val voices = tts?.voices
                val maleVoice = voices?.firstOrNull {
                    (it.name.contains("male", ignoreCase = true) || it.name.contains("in-language", ignoreCase = true)) &&
                            !it.name.contains("female", ignoreCase = true)
                }
                if (maleVoice != null) tts?.voice = maleVoice
            } catch (e: Exception) { }

            tts?.setPitch(0.82f)
            tts?.setSpeechRate(1.05f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    runOnUiThread { onSpeechComplete?.invoke() }
                }
                override fun onError(utteranceId: String?) {
                    runOnUiThread { onSpeechComplete?.invoke() }
                }
            })
        }
    }

    private fun speakText(text: String, onDone: () -> Unit) {
        onSpeechComplete = onDone
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ORVYN_AI")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ORVYN_AI")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun OrvynTheme(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070A0F),
                        Color(0xFF04060A),
                        Color(0xFF020305)
                    )
                )
            )
    ) {
        content()
    }
}

enum class CoreState { IDLE, LISTENING, THINKING, RESPONDING }

@Composable
fun OrvynMainHub(prefs: SharedPreferences, speakOut: (String, () -> Unit) -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    var coreState by remember { mutableStateOf(CoreState.IDLE) }
    var spokenQuery by remember { mutableStateOf("Core tap karke command dijiye, Sir...") }
    var aiResponse by remember { mutableStateOf("ORVYN OS Online. Day ${prefs.getInt("arc_day", 1)} of 90 Protocol Active.") }

    var h1 by remember { mutableStateOf(prefs.getBoolean("w_h1", false)) }
    var h2 by remember { mutableStateOf(prefs.getBoolean("w_h2", false)) }
    var h3 by remember { mutableStateOf(prefs.getBoolean("w_h3", false)) }
    var h4 by remember { mutableStateOf(prefs.getBoolean("w_h4", false)) }

    val completedCount = (if (h1) 1 else 0) + (if (h2) 1 else 0) + (if (h3) 1 else 0) + (if (h4) 1 else 0)
    val progressPercent = (completedCount / 4f)
    val streak = prefs.getInt("streak_count", 0)
    val arcDay = prefs.getInt("arc_day", 1)

    // Robust Compose-Native Timer
    var timerRunning by remember { mutableStateOf(false) }
    var timeLeftSeconds by remember { mutableStateOf(45 * 60) }

    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timeLeftSeconds > 0 && timerRunning) {
                delay(1000L)
                timeLeftSeconds -= 1
            }
            if (timeLeftSeconds <= 0 && timerRunning) {
                timerRunning = false
                timeLeftSeconds = 45 * 60
                speakOut("Session complete, Sir. Focus protocol logged.") {}
            }
        }
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    fun startListeningInternal() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "en-US"))
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                spokenQuery = "Sun raha hu, Sir... boliye"
                coreState = CoreState.LISTENING
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { coreState = CoreState.THINKING }
            override fun onError(error: Int) {
                coreState = CoreState.IDLE
                spokenQuery = "Voice capture failed, Sir. Tap core to retry."
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val query = matches[0]
                    spokenQuery = query
                    coreState = CoreState.THINKING
                    fetchGeminiResponse(query) { resultText ->
                        aiResponse = resultText
                        coreState = CoreState.RESPONDING
                        speakOut(resultText) { coreState = CoreState.IDLE }
                    }
                } else { coreState = CoreState.IDLE }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty()) spokenQuery = partial[0]
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListeningInternal() }

    fun triggerPanicProtocol() {
        coreState = CoreState.THINKING
        spokenQuery = "RELAPSE SOS TRIGGERED"
        val prompt = "Sir distraction feel kar rahe hain aur unka focus toot raha hai. Ek high-discipline Winter Arc general ki tarah strict, wake-up aur motivating Hinglish command do 2 lines mein."
        fetchGeminiResponse(prompt) { speech ->
            aiResponse = speech
            coreState = CoreState.RESPONDING
            speakOut(speech) { coreState = CoreState.IDLE }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ORVYN OS",
                        color = Color(0xFFF1F5F9),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "DAY $arcDay OF 90 • STREAK: $streak 🔥",
                        color = Color(0xFF00F0FF),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp
                    )
                }
                WinterArcBadge(text = if (progressPercent == 1f) "UNSTOPPABLE ⚔️" else "LOCKED IN 🔒")
            }

            // Tab 0: Core AI & Directives
            if (selectedTab == 0) {
                // Progress Bar
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DAILY COMPLETION",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(progressPercent * 100).toInt()}%",
                                color = Color(0xFF00F0FF),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressPercent)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF00F0FF), Color(0xFF38BDF8), Color(0xFF6366F1))
                                        )
                                    )
                            )
                        }
                    }
                }

                // AI Voice Output Panel
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (coreState == CoreState.LISTENING) Color(0xFF00F0FF) else Color(0xFF64748B)
                                    )
                            )
                            Text(
                                text = "VOICE STREAM:",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = spokenQuery,
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ORVYN INTELLIGENCE:",
                            color = Color(0xFF00F0FF),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = aiResponse,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Checklist
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CORE HABIT PROTOCOL",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "AUTO-RESETS AT 00:00",
                                color = Color(0xFF475569),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        HabitItem(
                            title = "05:00 AM Wakeup & Cold Splash",
                            tag = "ENERGY",
                            isDone = h1,
                            onToggle = {
                                h1 = !h1
                                prefs.edit().putBoolean("w_h1", h1).apply()
                            }
                        )
                        HabitItem(
                            title = "Hardcore Workout & Gym Routine",
                            tag = "PHYSICAL",
                            isDone = h2,
                            onToggle = {
                                h2 = !h2
                                prefs.edit().putBoolean("w_h2", h2).apply()
                            }
                        )
                        HabitItem(
                            title = "Deep Focus: Skill & Project Building",
                            tag = "WEALTH",
                            isDone = h3,
                            onToggle = {
                                h3 = !h3
                                prefs.edit().putBoolean("w_h3", h3).apply()
                            }
                        )
                        HabitItem(
                            title = "Clean Nutrition & Mindset Routine",
                            tag = "FOCUS",
                            isDone = h4,
                            onToggle = {
                                h4 = !h4
                                prefs.edit().putBoolean("w_h4", h4).apply()
                            }
                        )
                    }
                }

                // AI Core & Relapse SOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
             
