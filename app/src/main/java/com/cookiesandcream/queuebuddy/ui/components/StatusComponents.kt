package com.cookiesandcream.queuebuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cookiesandcream.queuebuddy.domain.model.CrowdLevel
import com.cookiesandcream.queuebuddy.domain.model.Freshness
import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import java.util.concurrent.TimeUnit

fun relativeTime(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(nowMillis - timestampMillis)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60} h ago"
    }
}

// Crowd label that pairs a colour with text, so colour is never the only signal.
@Composable
fun CrowdBadge(crowdLevel: CrowdLevel?, modifier: Modifier = Modifier) {
    val (label, colour) = when (crowdLevel) {
        CrowdLevel.LOW -> "Low" to Color(0xFF2E7D32)
        CrowdLevel.MEDIUM -> "Medium" to Color(0xFFB26A00)
        CrowdLevel.HIGH -> "High" to Color(0xFFC62828)
        null -> "No data" to Color(0xFF616161)
    }
    Text(
        text = "Crowd: $label",
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier = modifier
            .background(colour, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// How fresh the status is (Live / Recent / Stale / No recent data).
@Composable
fun FreshnessBadge(freshness: Freshness, modifier: Modifier = Modifier) {
    val colour = when (freshness) {
        Freshness.LIVE -> Color(0xFF2E7D32)
        Freshness.RECENT -> Color(0xFFB26A00)
        Freshness.STALE -> Color(0xFF616161)
        Freshness.NO_DATA -> Color(0xFF9E9E9E)
    }
    Text(
        text = freshness.displayName,
        style = MaterialTheme.typography.labelMedium,
        color = colour,
        modifier = modifier
            .background(colour.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .semantics { contentDescription = "Status freshness: ${freshness.displayName}" }
    )
}

fun reportSummaryLine(report: StatusReport): String {
    val parts = buildList {
        report.crowdLevel?.let { add("Crowd ${it.displayName}") }
        report.waitEstimate?.let { add("Wait ${it.displayName}") }
        report.seatAvailability?.let { add(it.displayName) }
        report.noiseLevel?.let { add("Noise: ${it.displayName}") }
        report.resourceStatus?.let { add(it.displayName) }
    }
    return if (parts.isEmpty()) "Note only" else parts.joinToString(" · ")
}
