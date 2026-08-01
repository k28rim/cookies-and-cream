package com.cookiesandcream.queuebuddy.data

import com.cookiesandcream.queuebuddy.domain.model.CampusLocation
import com.cookiesandcream.queuebuddy.domain.model.LocationCategory
import com.cookiesandcream.queuebuddy.domain.model.University

// Single source of every campus location (Repository pattern).
class LocationRepository(seed: List<CampusLocation> = SeedData.locations) {

    private val locationsById: Map<String, CampusLocation> = seed.associateBy { it.id }

    fun allLocations(): List<CampusLocation> = locationsById.values.toList()

    fun locationById(id: String): CampusLocation? = locationsById[id]

    fun locationsInCategory(category: LocationCategory): List<CampusLocation> =
        locationsById.values.filter { it.category == category }

    fun locationsForUniversity(university: University): List<CampusLocation> =
        locationsById.values.filter { it.university == university }
}
