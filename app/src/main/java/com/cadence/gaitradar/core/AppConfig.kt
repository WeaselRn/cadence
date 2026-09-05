package com.cadence.gaitradar.core

/**
 * Global application configuration flags.
 */
object AppConfig {
    const val SUPPORT_EMAIL = "support@cadencegait.com"

    val isDebugBuild: Boolean
        get() = try {
            com.cadence.gaitradar.BuildConfig.DEBUG
        } catch (_: Throwable) {
            false
        }
}
