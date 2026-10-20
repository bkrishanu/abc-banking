package com.abcbank.mobile.core.security

import com.abcbank.mobile.core.common.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/** Lets a ViewModel get a StepUpController for its own scope through injection. */
class StepUpControllerFactory @Inject constructor(
    private val verifier: MpinVerifier,
    private val manager: StepUpManager,
    private val clock: Clock
) {
    fun create(scope: CoroutineScope): StepUpController =
        StepUpController(verifier, manager, clock, scope)
}
