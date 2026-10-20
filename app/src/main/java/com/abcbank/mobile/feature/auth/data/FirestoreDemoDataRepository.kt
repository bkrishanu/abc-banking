package com.abcbank.mobile.feature.auth.data

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.seed.DemoDataSeeder
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.feature.auth.domain.DemoDataRepository
import javax.inject.Inject

class FirestoreDemoDataRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val seeder: DemoDataSeeder
) : DemoDataRepository {
    // These run in the background after login, so they wait for sign-in rather than fail offline.

    override suspend fun storedSeedVersion(personaId: PersonaId): DataResult<Long?> {
        val uid = auth.awaitSignedInUid()
        // A user created in this launch is brand new: its tree cannot exist on the server yet.
        return seeder.storedSeedVersion(uid, personaId, mayExistOnServer = !auth.createdThisLaunch)
    }

    override suspend fun bundledSeedVersion(personaId: PersonaId): DataResult<Long> =
        seeder.bundledSeedVersion(personaId)

    override suspend fun seedDemoData(personaId: PersonaId): DataResult<Unit> =
        seeder.seed(auth.awaitSignedInUid(), personaId)
}
