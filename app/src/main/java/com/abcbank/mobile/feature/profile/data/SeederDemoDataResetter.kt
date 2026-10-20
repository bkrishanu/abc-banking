package com.abcbank.mobile.feature.profile.data

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.core.data.seed.DemoDataSeeder
import com.abcbank.mobile.feature.profile.domain.DemoDataResetter
import javax.inject.Inject

class SeederDemoDataResetter @Inject constructor(
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val seeder: DemoDataSeeder
) : DemoDataResetter {
    override suspend fun resetActivePersona(): DataResult<Unit> {
        val uid = when (val result = auth.signedInUid()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> return result
        }
        val persona = activePersona.activePersona() ?: return DataResult.Failure(DataError.UNKNOWN)
        return seeder.reset(uid, persona)
    }
}
