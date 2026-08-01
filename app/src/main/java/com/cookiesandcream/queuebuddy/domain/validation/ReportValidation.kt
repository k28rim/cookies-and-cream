package com.cookiesandcream.queuebuddy.domain.validation

import com.cookiesandcream.queuebuddy.domain.model.StatusReport

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val reason: String) : ValidationResult
}

// Chain of Responsibility: each validator does one check, then hands the report
// to the next link. The first failure stops the chain with a reason.
abstract class ReportValidator {
    private var next: ReportValidator? = null

    fun setNext(validator: ReportValidator): ReportValidator {
        next = validator
        return validator
    }

    fun validate(report: StatusReport, context: ValidationContext): ValidationResult {
        val result = check(report, context)
        if (result is ValidationResult.Invalid) return result
        return next?.validate(report, context) ?: ValidationResult.Valid
    }

    protected abstract fun check(report: StatusReport, context: ValidationContext): ValidationResult
}

data class ValidationContext(
    val nowMillis: Long,

    val lastReportFromReporterMillis: Long?
)

class HasContentValidator : ReportValidator() {
    override fun check(report: StatusReport, context: ValidationContext): ValidationResult =
        if (report.hasAnyStatusField()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Please select at least one status (crowd, wait, seats, noise, or resource) before submitting.")
        }
}

class RateLimitValidator(private val cooldownMinutes: Int = 10) : ReportValidator() {
    override fun check(report: StatusReport, context: ValidationContext): ValidationResult {
        val last = context.lastReportFromReporterMillis ?: return ValidationResult.Valid
        val elapsedMinutes = (context.nowMillis - last) / 60_000
        return if (elapsedMinutes < cooldownMinutes) {
            val wait = cooldownMinutes - elapsedMinutes
            ValidationResult.Invalid("You already reported on this location recently. Try again in about $wait min.")
        } else {
            ValidationResult.Valid
        }
    }
}

// Keep notes short.
class NoteValidator(private val maxLength: Int = 140) : ReportValidator() {
    override fun check(report: StatusReport, context: ValidationContext): ValidationResult {
        val note = report.note ?: return ValidationResult.Valid
        return if (note.length > maxLength) {
            ValidationResult.Invalid("Notes must be $maxLength characters or fewer.")
        } else {
            ValidationResult.Valid
        }
    }
}

object ReportValidationChain {
    fun default(): ReportValidator {
        val head = HasContentValidator()
        head.setNext(RateLimitValidator()).setNext(NoteValidator())
        return head
    }
}
