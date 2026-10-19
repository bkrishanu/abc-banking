package com.abcbank.mobile.feature.auth.data

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.seed.DemoDataSeeder
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.feature.auth.domain.DemoDataRepository
import com.abcbank.mobile.feature.auth.domain.StoredDemoData
import javax.inject.Inject

class FirestoreDemoDataRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val seeder: DemoDataSeeder
) : DemoDataRepository {
    override suspend fun storedDemoData(): DataResult<StoredDemoData?> = withUid { uid ->
        when (val result = seeder.storedSeed(uid)) {
            is DataResult.Success -> DataResult.Success(
                result.value?.let { StoredDemoData(it.personaId, it.name, it.seedVersion) }
            )
            is DataResult.Failure -> result
        }
    }

    override suspend fun bundledSeedVersion(personaId: PersonaId): DataResult<Long> =
        seeder.bundledSeedVersion(personaId)

    override suspend fun seedDemoData(personaId: PersonaId): DataResult<Unit> =
        withUid { uid -> seeder.seed(uid, personaId) }

    // Signs in anonymously on first use (ARCHITECTURE.md section 04).
    private suspend fun <T> withUid(block: suspend (String) -> DataResult<T>): DataResult<T> =
        when (val uid = auth.signedInUid()) {
            is DataResult.Success -> block(uid.value)
            is DataResult.Failure -> uid
        }
}
