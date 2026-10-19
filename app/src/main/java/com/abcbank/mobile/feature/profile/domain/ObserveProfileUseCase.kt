package com.abcbank.mobile.feature.profile.domain

import com.abcbank.mobile.core.common.DataResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveProfileUseCase @Inject constructor(private val repository: ProfileRepository) {
    operator fun invoke(): Flow<DataResult<ProfileData?>> = repository.observeProfile()
}
