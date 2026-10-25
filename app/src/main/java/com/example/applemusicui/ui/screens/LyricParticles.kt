package com.example.applemusicui.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Particle(
    val x: Float,
    val phase: Float,
    /** Kecepatan naik: 1 atau 2 siklus per animasi (bilangan bulat supaya putarannya mulus). */
    val speed: Int,
    val sway: Int,
    val radiusDp: Float,
)

/**
 * Titik-titik halus yang naik pelan di belakang lirik, seperti partikel di BitChord.
 * Warnanya [color] (biasanya warna cover).
 */
@Composable
fun LyricParticles(color: Color, modifier: Modifier = Modifier) {
    val particles = remember {
        val rnd = Random(7)
        List(22) {
            Particle(
                x = rnd.nextFloat(),
                phase = rnd.nextFloat(),
                speed = if (rnd.nextBoolean()) 1 else 2,
                sway = if (rnd.nextBoolean()) 1 else 2,
                radiusDp = 2f + rnd.nextFloat() * 4f,
            )
        }
    }
    val t by rememberInfiniteTransition(label = "lyricParticles").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "lyricParticlesT",
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        for (p in particles) {
            val progress = ((t * p.speed + p.phase) % 1f)
            val y = 1f - progress
            val x = p.x + 0.03f * sin(2f * PI.toFloat() * (t * p.sway + p.phase)).toFloat()
            // Muncul pelan dari bawah, hilang pelan di atas.
            val alpha = 0.35f * sin(PI.toFloat() * progress)
            drawCircle(
                color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = p.radiusDp.dp.toPx(),
                center = Offset(x * w, y * h),
            )
        }
    }
}
