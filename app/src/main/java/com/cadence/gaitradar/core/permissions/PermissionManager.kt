package com.cadence.gaitradar.core.permissions

/**
 * Abstraction layer for handling runtime permissions in Phase 0 foundation.
 */
interface PermissionManager {
    fun hasRequiredPermissions(): Boolean
}
