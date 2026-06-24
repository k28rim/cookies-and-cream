package com.cookiesandcream.queuebuddy.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookiesandcream.queuebuddy.domain.model.CrowdLevel
import com.cookiesandcream.queuebuddy.domain.model.NoiseLevel
import com.cookiesandcream.queuebuddy.domain.model.SeatAvailability
import com.cookiesandcream.queuebuddy.domain.model.WaitEstimate
import com.cookiesandcream.queuebuddy.ui.components.CrowdBadge
import com.cookiesandcream.queuebuddy.ui.components.relativeTime
import com.cookiesandcream.queuebuddy.ui.components.reportSummaryLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(viewModel: LocationDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val detail = state.detail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.location?.name ?: "Location") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openSheet,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Report status") }
            )
        }
    ) { padding ->
        if (detail == null) {
            Column(Modifier.padding(padding).padding(16.dp)) { Text("Location not found.") }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(detail.location.description, style = MaterialTheme.typography.bodyMedium)
                        CrowdBadge(detail.status.crowdLevel)
                        Text(
                            detail.status.waitLabel?.let { "Estimated wait: $it" } ?: "Estimated wait: no data",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item { Text("Recent reports", style = MaterialTheme.typography.titleMedium) }
            if (detail.reports.isEmpty()) {
                item { Text("No reports yet. Be the first to share one!", style = MaterialTheme.typography.bodyMedium) }
            }
            items(detail.reports, key = { it.id }) { report ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(reportSummaryLine(report), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Anonymous student · ${relativeTime(report.timestampMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (state.sheetOpen) {
        ReportSheet(
            draft = state.draft,
            onUpdate = viewModel::updateDraft,
            onSubmit = viewModel::submit,
            onDismiss = viewModel::closeSheet
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(
    draft: ReportDraft,
    onUpdate: ((ReportDraft) -> ReportDraft) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Quick report", style = MaterialTheme.typography.titleLarge)
            ChipRow("How crowded?", CrowdLevel.entries.map { it.displayName }, draft.crowdLevel?.ordinal) { idx ->
                onUpdate { d -> d.copy(crowdLevel = idx?.let { CrowdLevel.entries[it] }) }
            }
            ChipRow("Wait time", WaitEstimate.entries.map { it.displayName }, draft.waitEstimate?.ordinal) { idx ->
                onUpdate { d -> d.copy(waitEstimate = idx?.let { WaitEstimate.entries[it] }) }
            }
            ChipRow("Seats", SeatAvailability.entries.map { it.displayName }, draft.seatAvailability?.ordinal) { idx ->
                onUpdate { d -> d.copy(seatAvailability = idx?.let { SeatAvailability.entries[it] }) }
            }
            ChipRow("Noise", NoiseLevel.entries.map { it.displayName }, draft.noiseLevel?.ordinal) { idx ->
                onUpdate { d -> d.copy(noiseLevel = idx?.let { NoiseLevel.entries[it] }) }
            }
            OutlinedTextField(
                value = draft.note,
                onValueChange = { value -> onUpdate { d -> d.copy(note = value) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Optional note") },
                singleLine = true
            )
            Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
                Text("Submit report")
            }
        }
    }
}

@Composable
private fun ChipRow(label: String, options: List<String>, selectedIndex: Int?, onSelect: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { index, option ->
                FilterChip(
                    selected = selectedIndex == index,
                    onClick = { onSelect(if (selectedIndex == index) null else index) },
                    label = { Text(option) }
                )
            }
        }
    }
}
