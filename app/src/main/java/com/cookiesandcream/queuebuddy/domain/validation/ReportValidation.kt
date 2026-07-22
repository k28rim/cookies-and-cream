package com.cookiesandcream.queuebuddy.domain.validation

import com.cookiesandcream.queuebuddy.domain.model.StatusReport

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val reason: String) : ValidationResult
}

// Extra facts a rule may need that aren't on the report itself.
data class ValidationContext(
    val nowMillis: Long,
    val lastReportFromReporterMillis: Long?
)

// Chain of Responsibility: each validator either rejects the report or passes it to
// the next link. Adding a new rule is just one more class in the chain.
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

// A report must say something.
class HasContentValidator : ReportValidator() {
    override fun check(report: StatusReport, context: ValidationContext): ValidationResult =
        if (report.hasAnyField()) ValidationResult.Valid
        else ValidationResult.Invalid("Pick at least one status (crowd, wait, seats, noise, or printer) before submitting.")
}

// One reporter can't spam the same location.
class RateLimitValidator(private val cooldownMinutes: Int = 10) : ReportValidator() {
    override fun check(report: StatusReport, context: ValidationContext): ValidationResult {
        val last = context.lastReportFromReporterMillis ?: return ValidationResult.Valid
        val elapsed = (context.nowMillis - last) / 60_000
        return if (elapsed < cooldownMinutes) {
            ValidationResult.Invalid("You reported here recently. Try again in about ${cooldownMinutes - elapsed} min.")
        } else {
            ValidationResult.Valid
        }
    }
}

// Keep notes short.
class NoteValidator(private val maxLength: Int = 140) : ReportValidator() {
    override fun check(report: StatusReport, context: ValidationContext): ValidationResult {
        val note = report.note ?: return ValidationResult.Valid
        return if (note.length > maxLength) ValidationResult.Invalid("Notes must be $maxLength characters or fewer.")
        else ValidationResult.Valid
    }
}

object ReportValidationChain {
    fun default(): ReportValidator {
        val head = HasContentValidator()
        head.setNext(RateLimitValidator()).setNext(NoteValidator())
        return head
    }
}
