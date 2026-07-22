package com.cookiesandcream.queuebuddy.domain.staleness

import com.cookiesandcream.queuebuddy.domain.model.Freshness

// Strategy pattern: each category decides how quickly its reports go stale. The base
// interface holds the shared rules; subclasses just supply the per-category windows.
interface StalenessPolicy {

    // How long a report still counts as "Live".
    val liveMinutes: Int

    // Past this it is "Stale" (still shown, but flagged as old).
    val staleMinutes: Int

    // Hard cap: older than this and we treat the location as having no recent data.
    val maxAgeMinutes: Int get() = 60

    fun freshnessOf(ageMinutes: Long): Freshness = when {
        ageMinutes <= liveMinutes -> Freshness.LIVE
        ageMinutes <= staleMinutes -> Freshness.RECENT
        else -> Freshness.STALE
    }

    fun isUsable(ageMinutes: Long): Boolean = ageMinutes <= maxAgeMinutes
}

// Food queues move fast, so they go stale quickly.
class QueueStalenessPolicy : StalenessPolicy {
    override val liveMinutes = 10
    override val staleMinutes = 30
}

class StudySpaceStalenessPolicy : StalenessPolicy {
    override val liveMinutes = 20
    override val staleMinutes = 30
}

class GymStalenessPolicy : StalenessPolicy {
    override val liveMinutes = 15
    override val staleMinutes = 30
}

class ServiceStalenessPolicy : StalenessPolicy {
    override val liveMinutes = 15
    override val staleMinutes = 30
}
