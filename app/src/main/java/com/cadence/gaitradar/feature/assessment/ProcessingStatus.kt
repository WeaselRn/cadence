package com.cadence.gaitradar.feature.assessment

/**
 * Processing state machine for post-collection on-device analysis pipeline.
 */
enum class ProcessingStatus(val userMessage: String) {
    IDLE("Idle"),
    VALIDATING("Session validated"),
    PREPARING_DATA("Movement data prepared"),
    CALCULATING_METRICS("Calculating movement metrics"),
    RUNNING_MODEL("Running local model on your device"),
    CALCULATING_BASELINE("Comparing with your personal baseline"),
    SAVING("Saving assessment locally"),
    COMPLETED("Analysis complete"),
    QUALITY_FAILED("We couldn't get a reliable assessment. The movement data wasn't clear enough to analyze this time."),
    PROCESSING_FAILED("An error occurred during local analysis.")
}
