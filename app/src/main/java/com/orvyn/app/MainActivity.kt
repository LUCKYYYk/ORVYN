package com.orvyn.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

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
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ORVYN_TTS")
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
    var spokenQuery by remember { mutableStateOf("Core tap karke Hindi ya Hinglish mein bolo...") }
    var aiResponse by remember { mutableStateOf("ORVYN Intelligence standby par hai. Commands ke liye ready.") }
    val currentLangMode by remember { mutableStateOf("HINGLISH / HINDI") }

    var d1Done by remember { mutableStateOf(false) }
    var d2Done by remember { mutableStateOf(false) }
    var d3Done by remember { mutableStateOf(false) }
    val totalXp = (if (d1Done) 25 else 0) + (if (d2Done) 30 else 0) + (if (d3Done) 20 else 0)

    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechListening(speechRecognizer) { text ->
                spokenQuery = text
                coreState = CoreState.THINKING
                processMultiLingualQuery(text) { result ->
                    aiResponse = result
                    coreState = CoreState.RESPONDING
                    speakOut(result)
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(5000)
                        coreState = CoreState.IDLE
                    }
                }
            }
            coreState = CoreState.LISTENING
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
                    text = "MODE: $currentLangMode",
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
                    text = "USER QUERY [VOICE INPUT]:",
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
                    text = "ORVYN RESPONSE [HINDI / HINGLISH]:",
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
                    text = "TAP TO COMPLETE DIRECTIVES",
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
                    title = "2. Client Pipeline Review",
                    xp = "+30 XP",
                    isDone = d2Done,
                    onToggle = { d2Done = !d2Done }
                )
                InteractiveDirective(
                    title = "3. Video Script Breakdown",
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
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        coreState = CoreState.LISTENING
                        spokenQuery = "Listening... (Bolo bhai)"
                        startSpeechListening(speechRecognizer) { text ->
                            spokenQuery = text
                            coreState = CoreState.THINKING
                            processMultiLingualQuery(text) { result ->
                                aiResponse = result
                                coreState = CoreState.RESPONDING
                                speakOut(result)
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(5000)
                                    coreState = CoreState.IDLE
                                }
                            }
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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

fun startSpeechListening(recognizer: SpeechRecognizer, onResult: (String) -> Unit) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "en-US"))
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Boliye...")
    }

    recognizer.setRecognitionListener(object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResult(matches[0])
            }
        }
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            onResult("Voice clear nahi aayi, wapas bolo bhai.")
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })

    recognizer.startListening(intent)
}

fun processMultiLingualQuery(query: String, onComplete: (String) -> Unit) {
    val lower = query.lowercase()
    val reply = when {
        lower.contains("kaise ho") || lower.contains("kya haal") ->
            "Main ekdum badhiya hoon rockstar! Aaj ka kya mission hai?"
        lower.contains("status") || lower.contains("direct") ->
            "Active directives loaded hain. Winter Arc momentum full speed par hai."
        lower.contains("video") || lower.contains("script") ->
            "Viral hook script ready hai: 'Yeh AI trick 99% creators miss kar rahe hain!'"
        lower.contains("website") || lower.contains("client") ->
            "Website pipeline active hai. Aaj naye prospective clients ko pitch bhejna hai."
        else ->
            "Command received: '$query'. ORVYN engine analysis complete. Sabhi tasks under control hain!"
    }
    onComplete(reply)
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
