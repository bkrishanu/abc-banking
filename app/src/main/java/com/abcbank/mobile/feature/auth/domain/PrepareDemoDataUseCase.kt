package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.PersonaId
import javax.inject.Inject

/**
 * Makes sure [PersonaId]'s data tree exists on this install (ARCHITECTURE.md section 04, Auth and
 * session). An existing tree is kept unless the bundled seed is newer; a missing one is written.
 * Other personas' trees are never touched.
 */
class PrepareDemoDataUseCase @Inject constructor(private val repository: DemoDataRepository) {
    suspend operator fun invoke(personaId: PersonaId): DataResult<Unit> {
        val storedVersion = when (val result = repository.storedSeedVersion(personaId)) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val bundledVersion = when (val result = repository.bundledSeedVersion(personaId)) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val keep = storedVersion != null && storedVersion >= bundledVersion
        return if (keep) DataResult.Success(Unit) else repository.seedDemoData(personaId)
    }
}
