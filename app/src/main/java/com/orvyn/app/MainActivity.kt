package com.orvyn.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

const val GEMINI_API_KEY = "AQ.Ab8RN6LitSVW_vuXcnJmzxNTgzmCeJoh0cvfswbBKZA2tZPoeA"

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            OrvynTheme {
                OrvynDashboard(
                    speakOut = { text -> speakText(text) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.0f)
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ORVYN_VOICE")
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
                    colors = listOf(
                        Color(0xFF0F141C),
                        Color(0xFF080B0F),
                        Color(0xFF040608)
                    )
                )
            )
    ) {
        content()
    }
}

enum class CoreState { IDLE, LISTENING, THINKING, RESPONDING }

@Composable
fun OrvynDashboard(speakOut: (String) -> Unit) {
    val context = LocalContext.current
    var coreState by remember { mutableStateOf(CoreState.IDLE) }
    var spokenQuery by remember { mutableStateOf("Core tap karke command do...") }
    var aiResponse by remember { mutableStateOf("ORVYN Gemini Core ready hai. Boliye!") }

    var d1Done by remember { mutableStateOf(false) }
    var d2Done by remember { mutableStateOf(false) }
    var d3Done by remember { mutableStateOf(false) }
    val totalXp = (if (d1Done) 25 else 0) + (if (d2Done) 30 else 0) + (if (d3Done) 20 else 0)

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            if (spoken.isNotBlank()) {
                spokenQuery = spoken
                coreState = CoreState.THINKING
                fetchGeminiResponse(spoken) { res ->
                    aiResponse = res
                    coreState = CoreState.RESPONDING
                    speakOut(res)
                }
            } else {
                coreState = CoreState.IDLE
            }
        } else {
            coreState = CoreState.IDLE
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ORVYN",
                    color = Color(0xFFE2E8F0),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "ENGINE: GEMINI 2.5 FLASH",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp
                )
            }
            LiquidGlassBadge(text = "XP: $totalXp")
        }

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "VOICE INPUT:",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = spokenQuery,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ORVYN INTELLIGENCE:",
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = aiResponse,
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "DAILY ACTIVE DIRECTIVES",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                InteractiveDirective(
                    title = "1. Tech Module Complete",
                    xp = "+25 XP",
                    isDone = d1Done,
                    onToggle = { d1Done = !d1Done }
                )
                InteractiveDirective(
                    title = "2. Client Pipeline Outreach",
                    xp = "+30 XP",
                    isDone = d2Done,
                    onToggle = { d2Done = !d2Done }
                )
                InteractiveDirective(
                    title = "3. Video Script Creation",
                    xp = "+20 XP",
                    isDone = d3Done,
                    onToggle = { d3Done = !d3Done }
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            LiquidChromeCore(
                state = coreState,
                onClick = {
                    try {
                        coreState = CoreState.LISTENING
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "en-US"))
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "ORVYN sun raha hai, bolo bhai...")
                        }
                        speechLauncher.launch(intent)
                    } catch (e: Exception) {
                        coreState = CoreState.IDLE
                        Toast.makeText(context, "Google Voice service missing ya disabled hai", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "STATUS: [${coreState.name}] • TAP CORE TO TALK",
                color = when (coreState) {
                    CoreState.IDLE -> Color(0xFF64748B)
                    CoreState.LISTENING -> Color(0xFF38BDF8)
                    CoreState.THINKING -> Color(0xFFF59E0B)
                    CoreState.RESPONDING -> Color(0xFFA855F7)
                },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun fetchGeminiResponse(userPrompt: String, callback: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            val systemInstruction = "Tu ORVYN hai - Lucky ka smart AI mentor aur collaborator. Hamesha simple Hinglish aur Hindi mein motivating aur seedha 2-3 lines mein jawab do."

            val jsonBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemInstruction\n\nUser: $userPrompt")
                            })
                        })
                    })
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string()

            if (response.isSuccessful && resStr != null) {
                val json = JSONObject(resStr)
                val textResult = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                withContext(Dispatchers.Main) {
                    callback(textResult.trim())
                }
            } else {
                withContext(Dispatchers.Main) {
                    callback("API connection failed. Key permissions verify karo.")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback("Error: ${e.localizedMessage}")
            }
        }
    }
}

@Composable
fun InteractiveDirective(title: String, xp: String, isDone: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDone) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onToggle() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (if (isDone) "[✓] " else "[ ] ") + title,
            color = if (isDone) Color(0xFF10B981) else Color(0xFFE2E8F0),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = xp,
            color = if (isDone) Color(0xFF10B981) else Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun LiquidChromeCore(state: CoreState, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "CorePulse")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val coreColor = when(state) {
        CoreState.IDLE -> Color(0xFF94A3B8)
        CoreState.LISTENING -> Color(0xFF38BDF8)
        CoreState.THINKING -> Color(0xFFF59E0B)
        CoreState.RESPONDING -> Color(0xFFA855F7)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(150.dp)
            .clickable { onClick() }
    ) {
        Canvas(modifier = Modifier.size(130.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) * pulseScale

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.1f),
                        coreColor.copy(alpha = 0.85f),
                        coreColor.copy(alpha = 0.1f)
                    )
                ),
                radius = radius,
                style = Stroke(width = 3.dp.toPx())
            )

            val waveCount = 12
            for (i in 0 until waveCount) {
                val angle = Math.toRadians((i * (360.0 / waveCount) + rotation)).toFloat()
                val x = center.x + (radius * 0.65f) * cos(angle)
                val y = center.y + (radius * 0.65f) * sin(angle)

                drawCircle(
                    color = coreColor.copy(alpha = 0.4f),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.7f),
                        coreColor.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.7f
                ),
                radius = radius * 0.7f
            )
        }
    }
}

@Composable
fun LiquidGlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.45f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        content()
    }
}

@Composable
fun LiquidGlassBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = Color(0xFF38BDF8).copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFF38BDF8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
