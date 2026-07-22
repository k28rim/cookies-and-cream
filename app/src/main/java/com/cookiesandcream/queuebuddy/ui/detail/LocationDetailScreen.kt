package com.cookiesandcream.queuebuddy.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cookiesandcream.queuebuddy.domain.LocationDetail
import com.cookiesandcream.queuebuddy.domain.model.CrowdLevel
import com.cookiesandcream.queuebuddy.domain.model.LocationCategory
import com.cookiesandcream.queuebuddy.domain.model.NoiseLevel
import com.cookiesandcream.queuebuddy.domain.model.ResourceStatus
import com.cookiesandcream.queuebuddy.domain.model.SeatAvailability
import com.cookiesandcream.queuebuddy.domain.model.WaitEstimate
import com.cookiesandcream.queuebuddy.ui.components.CrowdBadge
import com.cookiesandcream.queuebuddy.ui.components.FreshnessBadge
import com.cookiesandcream.queuebuddy.ui.components.ReportCard
import com.cookiesandcream.queuebuddy.ui.components.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(viewModel: LocationDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val detail = state.detail

    // Show submit/flag feedback, then clear it so it isn't shown again on recompose.
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openSheet,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Report status") },
                modifier = Modifier.semantics {
                    contentDescription = "Submit a status report for this location"
                }
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
            item { StatusSection(detail) }
            item { Text("Recent reports", style = MaterialTheme.typography.titleMedium) }
            if (detail.reports.isEmpty()) {
                item {
                    Text(
                        "No recent reports. Be the first to share what it's like right now!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(detail.reports, key = { it.id }) { report ->
                ReportCard(
                    report = report,
                    alreadyFlagged = viewModel.alreadyFlaggedByMe(report),
                    onFlag = { viewModel.toggleFlag(report) }
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (state.sheetOpen) {
        ReportSheet(
            draft = state.draft,
            isPrinterLocation = detail?.location?.category == LocationCategory.PRINTER,
            onUpdate = viewModel::updateDraft,
            onSubmit = viewModel::submit,
            onDismiss = viewModel::closeSheet
        )
    }
}

@Composable
private fun StatusSection(detail: LocationDetail) {
    val status = detail.status
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(detail.location.description, style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CrowdBadge(status.crowdLevel)
                FreshnessBadge(status.freshness)
                Text(
                    text = status.lastUpdatedMillis?.let { "Updated ${relativeTime(it)}" }
                        ?: "No recent reports",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = status.waitLabel?.let { "Estimated wait: $it (from ${status.reportCount} recent reports)" }
                    ?: "Estimated wait: no recent data",
                style = MaterialTheme.typography.bodyMedium
            )
            status.seatAvailability?.let {
                Text("Seating: ${it.displayName}", style = MaterialTheme.typography.bodyMedium)
            }
            status.noiseLevel?.let {
                Text("Noise: ${it.displayName}", style = MaterialTheme.typography.bodyMedium)
            }
            status.resourceStatus?.let {
                Text("Equipment: ${it.displayName}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(
    draft: ReportDraft,
    isPrinterLocation: Boolean,
    onUpdate: ((ReportDraft) -> ReportDraft) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Quick report", style = MaterialTheme.typography.titleLarge)
            Text(
                "Anonymous · takes under 20 seconds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            if (isPrinterLocation) {
                ChipRow("Printer status", ResourceStatus.entries.map { it.displayName }, draft.resourceStatus?.ordinal) { idx ->
                    onUpdate { d -> d.copy(resourceStatus = idx?.let { ResourceStatus.entries[it] }) }
                }
            }
            OutlinedTextField(
                value = draft.note,
                onValueChange = { value -> onUpdate { d -> d.copy(note = value) } },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Optional note") },
                singleLine = true,
                supportingText = { Text("${draft.note.length}/140") }
            )
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Submit report" }
            ) {
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
