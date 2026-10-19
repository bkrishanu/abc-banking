package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.common.DataResult
import javax.inject.Inject

/** Which persona's data is on this install, so the login screen can warn before replacing it. */
class GetStoredDemoDataUseCase @Inject constructor(private val repository: DemoDataRepository) {
    /** Null when nothing is seeded or the stored data cannot be read; then there is no warning. */
    suspend operator fun invoke(): StoredDemoData? =
        (repository.storedDemoData() as? DataResult.Success)?.value
}
