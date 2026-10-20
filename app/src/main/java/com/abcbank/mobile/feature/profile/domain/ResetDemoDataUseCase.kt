package com.abcbank.mobile.feature.profile.domain

import com.abcbank.mobile.core.common.DataResult
import javax.inject.Inject

/** Rewrites the logged-in persona's demo data from the bundled file. */
interface DemoDataResetter {
    suspend fun resetActivePersona(): DataResult<Unit>
}

/**
 * Profile > Reset Demo Data (PRD section 08): puts the current persona back to its seeded state,
 * so a presenter can run the demo again. Other personas and the device binding are kept.
 */
class ResetDemoDataUseCase @Inject constructor(private val resetter: DemoDataResetter) {
    suspend operator fun invoke(): DataResult<Unit> = resetter.resetActivePersona()
}
