package com.cookiesandcream.queuebuddy.domain.suggestion

import com.cookiesandcream.queuebuddy.domain.LocationSummary
import com.cookiesandcream.queuebuddy.domain.model.Freshness
import kotlin.math.roundToInt

// The pick shown highlighted at the top of a filtered Home list, with a short
// human-readable reason like "No wait · 2 min walk".
data class LocationSuggestion(
    val summary: LocationSummary,
    val reason: String
)

// Picks the best spot in a filtered list: lowest total time cost, where cost is the
// estimated wait (or a crowd-based stand-in) plus walking time when the user's
// coarse location is known.
object LocationSuggester {

    private const val WALK_METERS_PER_MINUTE = 80.0
    // No wait data: score by crowd instead (Low/Medium/High -> 0/10/20 min).
    private const val MINUTES_PER_CROWD_RANK = 10.0

    fun suggest(summaries: List<LocationSummary>): LocationSuggestion? {
        // Only worth suggesting when there is an actual choice to make.
        if (summaries.size < 2) return null
        val candidates = summaries.filter {
            it.status.freshness != Freshness.NO_DATA &&
                (it.status.estimatedWaitMinutes != null || it.status.crowdLevel != null)
        }
        val best = candidates.minByOrNull { costMinutes(it) } ?: return null
        return LocationSuggestion(best, reasonFor(best))
    }

    private fun costMinutes(summary: LocationSummary): Double {
        val status = summary.status
        val wait = status.estimatedWaitMinutes?.toDouble()
            ?: (status.crowdLevel?.rank ?: 0) * MINUTES_PER_CROWD_RANK
        val walk = summary.distanceMeters?.let { it / WALK_METERS_PER_MINUTE } ?: 0.0
        return wait + walk
    }

    private fun reasonFor(summary: LocationSummary): String {
        val status = summary.status
        val minutes = status.estimatedWaitMinutes
        val waitPart = when {
            minutes != null && minutes <= 1 -> "No wait"
            minutes != null -> "${status.waitLabel} wait"
            status.crowdLevel != null -> "${status.crowdLevel.displayName} crowd"
            else -> null
        }
        val walkPart = summary.distanceMeters?.let {
            "${maxOf(1, (it / WALK_METERS_PER_MINUTE).roundToInt())} min walk"
        }
        return listOfNotNull(waitPart, walkPart).joinToString(" · ")
    }
}
