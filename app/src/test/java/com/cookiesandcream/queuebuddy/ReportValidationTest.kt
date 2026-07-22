package com.cookiesandcream.queuebuddy

import com.cookiesandcream.queuebuddy.domain.model.CrowdLevel
import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import com.cookiesandcream.queuebuddy.domain.validation.ReportValidationChain
import com.cookiesandcream.queuebuddy.domain.validation.ValidationContext
import com.cookiesandcream.queuebuddy.domain.validation.ValidationResult
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportValidationTest {

    private val chain = ReportValidationChain.default()
    private val now = 1_000_000L

    private fun context(lastReportMillis: Long? = null) =
        ValidationContext(nowMillis = now, lastReportFromReporterMillis = lastReportMillis)

    @Test
    fun reportWithAStatusFieldIsValid() {
        val report = StatusReport.Builder("loc", "rep").crowdLevel(CrowdLevel.LOW).build(now)
        assertTrue(chain.validate(report, context()) is ValidationResult.Valid)
    }

    @Test
    fun emptyReportIsRejected() {
        val report = StatusReport.Builder("loc", "rep").build(now)
        assertTrue(chain.validate(report, context()) is ValidationResult.Invalid)
    }

    @Test
    fun secondReportWithinCooldownIsRejected() {
        val report = StatusReport.Builder("loc", "rep").crowdLevel(CrowdLevel.LOW).build(now)
        val twoMinutesAgo = now - 2 * 60_000
        assertTrue(chain.validate(report, context(twoMinutesAgo)) is ValidationResult.Invalid)
    }

    @Test
    fun overlyLongNoteIsRejected() {
        val report = StatusReport.Builder("loc", "rep").note("x".repeat(200)).build(now)
        assertTrue(chain.validate(report, context()) is ValidationResult.Invalid)
    }
}
