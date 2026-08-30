package com.orvyn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrvynTheme {
                OrvynDashboard()
            }
        }
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
fun OrvynDashboard() {
    var coreState by remember { mutableStateOf(CoreState.IDLE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
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
                    text = "BUILD BEYOND LIMITS",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
            }
            LiquidGlassBadge(text = "[SYS_ONLINE]")
        }

        // Winter Arc Glass Card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "WINTER ARC 120D",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "DAY 34 / 120",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "PHASE: BUILD & MOMENTUM",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Intel & Directives Preview Card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACTIVE DIRECTIVES",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                DirectiveItem(title = "1. Complete Tech Module", xp = "+25 XP")
                DirectiveItem(title = "2. Freelance Client Pipeline", xp = "+30 XP")
                DirectiveItem(title = "3. Video Script Breakdown", xp = "+20 XP")
            }
        }

        // Animated Liquid Chrome Core
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            LiquidChromeCore(
                state = coreState,
                onClick = {
                    coreState = when (coreState) {
                        CoreState.IDLE -> CoreState.LISTENING
                        CoreState.LISTENING -> CoreState.THINKING
                        CoreState.THINKING -> CoreState.RESPONDING
                        CoreState.RESPONDING -> CoreState.IDLE
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ORVYN CORE: [${coreState.name}]",
                color = when(coreState) {
                    CoreState.IDLE -> Color(0xFF64748B)
                    CoreState.LISTENING -> Color(0xFF38BDF8)
                    CoreState.THINKING -> Color(0xFFF59E0B)
                    CoreState.RESPONDING -> Color(0xFFA855F7)
                },
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun LiquidChromeCore(
    state: CoreState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CorePulse")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
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
            .size(160.dp)
            .clickable { onClick() }
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) * pulseScale

            // Outer Liquid Ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.1f),
                        coreColor.copy(alpha = 0.8f),
                        coreColor.copy(alpha = 0.1f)
                    )
                ),
                radius = radius,
                style = Stroke(width = 3.dp.toPx())
            )

            // Inner Chrome Mesh Simulation
            val waveCount = 12
            for (i in 0 until waveCount) {
                val angle = Math.toRadians((i * (360.0 / waveCount) + rotation)).toFloat()
                val x = center.x + (radius * 0.65f) * cos(angle)
                val y = center.y + (radius * 0.65f) * sin(angle)

                drawCircle(
                    color = coreColor.copy(alpha = 0.35f),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Central Glass Sphere Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.6f),
                        coreColor.copy(alpha = 0.15f),
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
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.45f))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.03f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        content()
    }
}

@Composable
fun LiquidGlassBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
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

@Composable
fun DirectiveItem(title: String, xp: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFFE2E8F0),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = xp,
            color = Color(0xFF10B981),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
