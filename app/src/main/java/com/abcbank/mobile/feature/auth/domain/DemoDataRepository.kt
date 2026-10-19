package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.PersonaId

/** The persona this install's data was last seeded with. */
data class StoredDemoData(val personaId: PersonaId, val name: String, val seedVersion: Long) {
    /** The stored persona's name if logging in as [personaId] would replace its data. */
    fun replacedNameFor(personaId: PersonaId): String? = name.takeIf { personaId != this.personaId }
}

/** The install's seeded demo data (DEMO_DATA.md section 01). */
interface DemoDataRepository {
    /** Null when nothing has been seeded yet. */
    suspend fun storedDemoData(): DataResult<StoredDemoData?>

    /** The `seedVersion` of the persona file bundled with this build. */
    suspend fun bundledSeedVersion(personaId: PersonaId): DataResult<Long>

    /** Deletes the install's demo data and writes [personaId]'s. */
    suspend fun seedDemoData(personaId: PersonaId): DataResult<Unit>
}
