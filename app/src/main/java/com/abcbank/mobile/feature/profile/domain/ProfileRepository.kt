package com.abcbank.mobile.feature.profile.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** The signed-in persona's profile, the `users/{uid}` document. */
interface ProfileRepository {
    /** Live. Success(null) when nothing has been seeded on this install. */
    fun observeProfile(): Flow<DataResult<ProfileData?>>
}

data class ProfileData(val profile: UserProfile, val isFromCache: Boolean)
