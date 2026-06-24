package com.cookiesandcream.queuebuddy.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookiesandcream.queuebuddy.domain.LocationSummary
import com.cookiesandcream.queuebuddy.ui.components.CrowdBadge
import com.cookiesandcream.queuebuddy.ui.components.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpenLocation: (String) -> Unit) {
    val summaries by viewModel.summaries.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Queue Buddy") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(summaries, key = { it.location.id }) { summary ->
                LocationCard(summary) { onOpenLocation(summary.location.id) }
            }
        }
    }
}

@Composable
private fun LocationCard(summary: LocationSummary, onClick: () -> Unit) {
    val status = summary.status
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(summary.location.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${summary.location.category.displayName} · ${summary.location.building}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CrowdBadge(status.crowdLevel)
                Text(
                    text = status.waitLabel?.let { "Est. wait: $it" } ?: "Wait: no data",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = status.lastUpdatedMillis?.let { "Updated ${relativeTime(it)}" } ?: "No reports yet",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
