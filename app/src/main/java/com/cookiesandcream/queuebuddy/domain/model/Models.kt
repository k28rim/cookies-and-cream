package com.cookiesandcream.queuebuddy.domain.model

import com.cookiesandcream.queuebuddy.domain.staleness.GymStalenessPolicy
import com.cookiesandcream.queuebuddy.domain.staleness.QueueStalenessPolicy
import com.cookiesandcream.queuebuddy.domain.staleness.ServiceStalenessPolicy
import com.cookiesandcream.queuebuddy.domain.staleness.StalenessPolicy
import com.cookiesandcream.queuebuddy.domain.staleness.StudySpaceStalenessPolicy
import kotlinx.serialization.Serializable

// Factory Method: each category builds its own staleness Strategy.
enum class LocationCategory(val displayName: String) {
    FOOD("Food") {
        override fun createStalenessPolicy(): StalenessPolicy = QueueStalenessPolicy()
    },
    STUDY("Study Spaces") {
        override fun createStalenessPolicy(): StalenessPolicy = StudySpaceStalenessPolicy()
    },
    GYM("Gyms") {
        override fun createStalenessPolicy(): StalenessPolicy = GymStalenessPolicy()
    },
    PRINTER("Printers") {
        override fun createStalenessPolicy(): StalenessPolicy = ServiceStalenessPolicy()
    },
    LIBRARY("Libraries") {
        override fun createStalenessPolicy(): StalenessPolicy = StudySpaceStalenessPolicy()
    },
    SERVICE("Services") {
        override fun createStalenessPolicy(): StalenessPolicy = ServiceStalenessPolicy()
    };

    abstract fun createStalenessPolicy(): StalenessPolicy
}

enum class CrowdLevel(val displayName: String, val rank: Int) {
    LOW("Low", 0),
    MEDIUM("Medium", 1),
    HIGH("High", 2)
}

enum class WaitEstimate(val displayName: String, val representativeMinutes: Int) {
    NONE("No wait", 0),
    SHORT("~5 min", 5),
    MEDIUM("5–15 min", 10),
    LONG("15+ min", 20)
}

enum class SeatAvailability(val displayName: String) {
    AVAILABLE("Seats available"),
    LIMITED("Few seats"),
    FULL("Full")
}

enum class NoiseLevel(val displayName: String) {
    QUIET("Quiet"),
    MODERATE("Moderate"),
    LOUD("Loud")
}

enum class ResourceStatus(val displayName: String) {
    WORKING("Working"),
    OUT_OF_ORDER("Out of order")
}

// How recent a location's status is.
enum class Freshness(val displayName: String) {
    LIVE("Live"),
    RECENT("Recent"),
    STALE("Stale"),
    NO_DATA("No recent data")
}

@Serializable
enum class University(val displayName: String, val available: Boolean) {
    WATERLOO("University of Waterloo", available = true),
    LAURIER("Wilfrid Laurier University", available = false),
    TORONTO("University of Toronto", available = false),
    WESTERN("Western University", available = false),
    MCMASTER("McMaster University", available = false),
    QUEENS("Queen's University", available = false)
}

@Serializable
data class CampusLocation(
    val id: String,
    val name: String,
    val building: String,
    val category: LocationCategory,
    val description: String,
    val latitude: Double,
    val longitude: Double,

    val amenities: List<String> = emptyList(),

    val university: University = University.WATERLOO
)

@Serializable
data class StatusReport(
    val id: String,
    val locationId: String,
    val reporterId: String,
    val timestampMillis: Long,
    val crowdLevel: CrowdLevel? = null,
    val waitEstimate: WaitEstimate? = null,
    val seatAvailability: SeatAvailability? = null,
    val noiseLevel: NoiseLevel? = null,
    val resourceStatus: ResourceStatus? = null,
    val note: String? = null,
    val flagCount: Int = 0,
    val flaggedByReporterIds: List<String> = emptyList()
) {
    val isFlagged: Boolean get() = flagCount > 0

    fun hasAnyStatusField(): Boolean =
        crowdLevel != null || waitEstimate != null || seatAvailability != null ||
            noiseLevel != null || resourceStatus != null || !note.isNullOrBlank()

    // Builder pattern: the report sheet sets only the fields the user picked, then builds.
    class Builder(private val locationId: String, private val reporterId: String) {
        private var crowdLevel: CrowdLevel? = null
        private var waitEstimate: WaitEstimate? = null
        private var seatAvailability: SeatAvailability? = null
        private var noiseLevel: NoiseLevel? = null
        private var resourceStatus: ResourceStatus? = null
        private var note: String? = null

        fun crowdLevel(value: CrowdLevel?) = apply { crowdLevel = value }
        fun waitEstimate(value: WaitEstimate?) = apply { waitEstimate = value }
        fun seatAvailability(value: SeatAvailability?) = apply { seatAvailability = value }
        fun noiseLevel(value: NoiseLevel?) = apply { noiseLevel = value }
        fun resourceStatus(value: ResourceStatus?) = apply { resourceStatus = value }
        fun note(value: String?) = apply { note = value?.takeIf { it.isNotBlank() } }

        fun build(nowMillis: Long = System.currentTimeMillis()): StatusReport = StatusReport(
            id = java.util.UUID.randomUUID().toString(),
            locationId = locationId,
            reporterId = reporterId,
            timestampMillis = nowMillis,
            crowdLevel = crowdLevel,
            waitEstimate = waitEstimate,
            seatAvailability = seatAvailability,
            noiseLevel = noiseLevel,
            resourceStatus = resourceStatus,
            note = note
        )
    }
}

data class LocationStatus(
    val locationId: String,
    val crowdLevel: CrowdLevel?,
    val estimatedWaitMinutes: Int?,
    val waitLabel: String?,
    val seatAvailability: SeatAvailability?,
    val noiseLevel: NoiseLevel?,
    val resourceStatus: ResourceStatus?,
    val lastUpdatedMillis: Long?,
    val freshness: Freshness,
    val reportCount: Int
)
