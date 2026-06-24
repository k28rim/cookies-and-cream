package com.cookiesandcream.queuebuddy.data

import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// Saves and loads reports as JSON in the app's private on-device storage.
class ReportStore(private val file: File) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    fun load(): List<StatusReport> {
        return try {
            if (!file.exists()) return emptyList()
            json.decodeFromString<List<StatusReport>>(file.readText())
        } catch (e: Exception) {

            emptyList()
        }
    }

    fun save(reports: List<StatusReport>) {
        try {
            file.writeText(json.encodeToString(reports))
        } catch (e: Exception) {

        }
    }
}
