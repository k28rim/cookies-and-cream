package com.cookiesandcream.queuebuddy.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cookiesandcream.queuebuddy.R
import com.cookiesandcream.queuebuddy.domain.ConditionFilter
import com.cookiesandcream.queuebuddy.domain.LocationSummary
import com.cookiesandcream.queuebuddy.domain.SortOption
import com.cookiesandcream.queuebuddy.domain.model.LocationCategory
import com.cookiesandcream.queuebuddy.domain.model.University
import com.cookiesandcream.queuebuddy.ui.components.CrowdBadge
import com.cookiesandcream.queuebuddy.ui.components.FreshnessBadge
import com.cookiesandcream.queuebuddy.ui.components.TagChip
import com.cookiesandcream.queuebuddy.ui.components.relativeTime
import kotlin.math.roundToInt

private val BrandFont = FontFamily(Font(R.font.righteous))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenLocation: (String) -> Unit,
    onOpenModeration: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            readCoarseLocation(context)?.let { (lat, lon) -> viewModel.setUserLocation(lat, lon) }
        }
    }

    // Ask for coarse location once at startup, so every card can show its distance
    // without the user having to opt into the "Nearest first" sort.
    LaunchedEffect(Unit) {
        if (!state.hasUserLocation) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                readCoarseLocation(context)?.let { (lat, lon) -> viewModel.setUserLocation(lat, lon) }
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    // Jump back to the top of the list whenever the search/filters/sort change, so the
    // user sees the new results from the start instead of being left mid-scroll.
    val listState = rememberLazyListState()
    LaunchedEffect(state.query, state.category, state.conditions, state.sort, state.university) {
        listState.scrollToItem(0)
    }

    // When new reports reorder the list and the user is already at (or near) the
    // top, follow the newest items so live updates stay visible. A reader further
    // down the list is never disturbed.
    LaunchedEffect(state.summaries) {
        if (listState.firstVisibleItemIndex <= 2) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            HomeHeader(
                university = state.university,
                moderatorMode = state.moderatorMode,
                suggestionsEnabled = state.suggestionsEnabled,
                onSelectUniversity = viewModel::setUniversity,
                onToggleModerator = viewModel::setModeratorMode,
                onToggleSuggestions = viewModel::setSuggestionsEnabled,
                onOpenModeration = onOpenModeration
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search campus locations by name" },
                placeholder = { Text("Search locations…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = state.category == null,
                    onClick = { viewModel.setCategory(null) },
                    label = { Text("All") }
                )
                LocationCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = {
                            viewModel.setCategory(if (state.category == category) null else category)
                        },
                        label = { Text(category.displayName) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortMenu(
                    current = state.sort,
                    onSelect = { option ->
                        if (option == SortOption.NEAREST && !state.hasUserLocation) {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                        viewModel.setSort(option)
                    }
                )
                ConditionFilter.entries.forEach { condition ->
                    FilterChip(
                        selected = condition in state.conditions,
                        onClick = { viewModel.toggleCondition(condition) },
                        label = { Text(condition.displayName) }
                    )
                }
            }

            if (!state.university.available) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${state.university.displayName} — coming soon",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "We're starting at the University of Waterloo. More campuses are on the way!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (state.summaries.isEmpty()) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No locations match your search and filters.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = viewModel::resetFilters) {
                        Text("Reset filters")
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    val priorityTags = state.conditions.map { it.displayName }.toSet()
                    val suggested = state.suggestion
                    if (suggested != null) {
                        item(key = "suggestion") {
                            LocationCard(
                                summary = suggested.summary,
                                priorityTags = priorityTags,
                                onClick = { onOpenLocation(suggested.summary.location.id) },
                                suggestionReason = suggested.reason,
                                onDismissSuggestion = viewModel::dismissSuggestion
                            )
                        }
                    }
                    // The suggested location moves to the top, so keep it out of the rest.
                    val rest = if (suggested != null) {
                        state.summaries.filterNot { it.location.id == suggested.summary.location.id }
                    } else {
                        state.summaries
                    }
                    items(rest, key = { it.location.id }) { summary ->
                        LocationCard(
                            summary = summary,
                            priorityTags = priorityTags,
                            onClick = { onOpenLocation(summary.location.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(
    moderatorMode: Boolean,
    suggestionsEnabled: Boolean,
    onToggleModerator: (Boolean) -> Unit,
    onToggleSuggestions: (Boolean) -> Unit,
    onOpenModeration: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = "More options" }
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Suggestions") },
                onClick = { onToggleSuggestions(!suggestionsEnabled) },
                trailingIcon = {
                    Switch(
                        checked = suggestionsEnabled,
                        onCheckedChange = { onToggleSuggestions(it) }
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Moderator mode") },
                onClick = { onToggleModerator(!moderatorMode) },
                trailingIcon = {
                    Switch(
                        checked = moderatorMode,
                        onCheckedChange = { onToggleModerator(it) }
                    )
                }
            )
            if (moderatorMode) {
                DropdownMenuItem(
                    text = { Text("Moderation queue") },
                    onClick = {
                        expanded = false
                        onOpenModeration()
                    },
                    leadingIcon = { Icon(Icons.Filled.Gavel, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    university: University,
    moderatorMode: Boolean,
    suggestionsEnabled: Boolean,
    onSelectUniversity: (University) -> Unit,
    onToggleModerator: (Boolean) -> Unit,
    onToggleSuggestions: (Boolean) -> Unit,
    onOpenModeration: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_brand_logo),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Queue Buddy",
                    fontFamily = BrandFont,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                OverflowMenu(
                    moderatorMode = moderatorMode,
                    suggestionsEnabled = suggestionsEnabled,
                    onToggleModerator = onToggleModerator,
                    onToggleSuggestions = onToggleSuggestions,
                    onOpenModeration = onOpenModeration
                )
            }
            UniversityMenu(
                current = university,
                onSelect = onSelectUniversity,
                modifier = Modifier.padding(start = 42.dp)
            )
        }
    }
}

@Composable
private fun UniversityMenu(
    current: University,
    onSelect: (University) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = true }
                .semantics { contentDescription = "Select university, currently ${current.displayName}" }
        ) {
            Text(
                current.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            University.entries.filter { it.available }.forEach { university ->
                DropdownMenuItem(
                    text = { Text(university.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(university)
                    }
                )
            }
            HorizontalDivider()
            University.entries.filterNot { it.available }.forEach { university ->
                DropdownMenuItem(
                    text = {
                        Text(
                            university.displayName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(university)
                    }
                )
            }
        }
    }
}

@Composable
private fun SortMenu(current: SortOption, onSelect: (SortOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text("Sort: ${current.displayName}") },
            leadingIcon = if (current == SortOption.NEAREST) {
                { Icon(Icons.Filled.MyLocation, contentDescription = null) }
            } else null,
            modifier = Modifier.semantics { contentDescription = "Change sort order, currently ${current.displayName}" }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun LocationCard(
    summary: LocationSummary,
    priorityTags: Set<String>,
    onClick: () -> Unit,
    suggestionReason: String? = null,
    onDismissSuggestion: (() -> Unit)? = null
) {
    val status = summary.status
    val suggested = suggestionReason != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (suggested) {
                    "Suggested location. Open details for ${summary.location.name}"
                } else {
                    "Open details for ${summary.location.name}"
                }
            },
        colors = if (suggested) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        border = if (suggested) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (suggestionReason != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Suggested · $suggestionReason",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (onDismissSuggestion != null) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Dismiss suggestion",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(onClick = onDismissSuggestion)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        summary.location.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp
                    )
                    Text(
                        "${summary.location.category.displayName} · ${summary.location.building}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FreshnessBadge(status.freshness)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CrowdBadge(status.crowdLevel)
                Text(
                    text = status.waitLabel?.let { "Est. wait: $it" } ?: "Wait: no data",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp
                )
                summary.distanceMeters?.let {
                    Text(
                        text = "${it.roundToInt()} m",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.lastUpdatedMillis?.let { "Updated ${relativeTime(it)}" }
                        ?: "No reports yet",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Tags matching an active filter (e.g. Quiet) come first so they
                // aren't cut off by take(2).
                summary.tags
                    .sortedByDescending { it in priorityTags }
                    .take(2)
                    .forEach { TagChip(it) }
            }
        }
    }
}

private fun readCoarseLocation(context: Context): Pair<Double, Double>? {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
    ) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return try {
        val location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        location?.let { it.latitude to it.longitude }
    } catch (e: SecurityException) {
        null
    }
}
