package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.PersonaId

/**
 * Each persona's demo data tree on this install (DEMO_DATA.md section 01). Calls wait for the
 * install's Firebase sign-in, so they run in the background, never on the login path.
 */
interface DemoDataRepository {
    /** The `seedVersion` of [personaId]'s tree on this install, or null when it has none. */
    suspend fun storedSeedVersion(personaId: PersonaId): DataResult<Long?>

    /** The `seedVersion` of the persona file bundled with this build. */
    suspend fun bundledSeedVersion(personaId: PersonaId): DataResult<Long>

    /** Writes [personaId]'s tree. Returns once queued locally, without waiting for the server. */
    suspend fun seedDemoData(personaId: PersonaId): DataResult<Unit>
}
