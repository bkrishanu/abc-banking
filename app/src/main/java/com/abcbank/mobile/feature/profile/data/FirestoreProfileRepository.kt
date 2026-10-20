package com.abcbank.mobile.feature.profile.data

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.common.DispatcherProvider
import com.abcbank.mobile.core.data.firebase.AuthDataSource
import com.abcbank.mobile.core.data.firebase.FirestorePaths
import com.abcbank.mobile.core.data.firebase.UserProfileDto
import com.abcbank.mobile.core.data.firebase.snapshotFlow
import com.abcbank.mobile.core.data.firebase.toDataError
import com.abcbank.mobile.core.data.seed.ActivePersonaSource
import com.abcbank.mobile.feature.profile.domain.ProfileData
import com.abcbank.mobile.feature.profile.domain.ProfileRepository
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

class FirestoreProfileRepository @Inject constructor(
    private val auth: AuthDataSource,
    private val activePersona: ActivePersonaSource,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : ProfileRepository {

    override fun observeProfile(): Flow<DataResult<ProfileData?>> = flow {
        val uid = when (val result = auth.signedInUid()) {
            is DataResult.Success -> result.value
            is DataResult.Failure -> {
                emit(result)
                return@flow
            }
        }
        // Nobody logged in: nothing to show.
        val personaId = activePersona.activePersona()?.key ?: run {
            emit(DataResult.Success(null))
            return@flow
        }
        val persona = firestore.document(FirestorePaths.persona(uid, personaId))
        emitAll(
            persona.snapshotFlow().mapNotNull { snapshot ->
                // Still being written after login: stay loading.
                val data = snapshot.data ?: return@mapNotNull null
                // A document that does not map is reported, never shown half-read.
                val profile = UserProfileDto.fromMap(data).toUserProfile()
                    ?: return@mapNotNull DataResult.Failure(DataError.MALFORMED_DATA)
                DataResult.Success(ProfileData(profile, snapshot.metadata.isFromCache))
            }
        )
    }
        .catch { error -> emit(DataResult.Failure(error.toDataError())) }
        .flowOn(dispatchers.default)
}
