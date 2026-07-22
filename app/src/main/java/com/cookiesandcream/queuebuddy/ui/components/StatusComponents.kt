package com.cookiesandcream.queuebuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    val diff = nowMillis - timestampMillis
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${TimeUnit.MILLISECONDS.toHours(diff)} h ago"
        else -> "${TimeUnit.MILLISECONDS.toDays(diff)} d ago"
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
            .semantics { contentDescription = "Crowd level $label" }
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

// One report row with a flag toggle. Anonymous: we only ever show "Anonymous student".
@Composable
fun ReportCard(
    report: StatusReport,
    alreadyFlagged: Boolean,
    onFlag: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(reportSummaryLine(report), style = MaterialTheme.typography.bodyMedium)
                report.note?.let {
                    Text(
                        text = "\"$it\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Anonymous student · ${relativeTime(report.timestampMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (report.isFlagged) {
                        Text(
                            text = "Flagged x${report.flagCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            IconButton(onClick = onFlag) {
                Icon(
                    imageVector = if (alreadyFlagged) Icons.Filled.Flag else Icons.Outlined.Flag,
                    contentDescription = if (alreadyFlagged) "Remove your flag from this report"
                    else "Flag this report as inaccurate",
                    tint = if (alreadyFlagged) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
