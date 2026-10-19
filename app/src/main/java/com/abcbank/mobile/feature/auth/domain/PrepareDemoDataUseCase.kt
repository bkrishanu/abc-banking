package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.PersonaId
import javax.inject.Inject

/**
 * Makes sure the install holds [PersonaId]'s data after login (ARCHITECTURE.md section 04, Auth
 * and session): the same persona keeps its data unless the bundled seed is newer; anything else
 * is deleted and seeded again.
 */
class PrepareDemoDataUseCase @Inject constructor(private val repository: DemoDataRepository) {
    suspend operator fun invoke(personaId: PersonaId): DataResult<Unit> {
        val stored = when (val result = repository.storedDemoData()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val bundledVersion = when (val result = repository.bundledSeedVersion(personaId)) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val keep = stored != null &&
            stored.personaId == personaId &&
            stored.seedVersion >= bundledVersion
        return if (keep) DataResult.Success(Unit) else repository.seedDemoData(personaId)
    }
}
