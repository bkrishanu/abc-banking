package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The install's anonymous Firebase user (ARCHITECTURE.md section 04, Auth and session). Each
 * install is one demo customer; the persona login happens on top of this user.
 */
@Singleton
class AuthDataSource @Inject constructor(private val auth: FirebaseAuth) {
    // Two callers racing on first launch must not create two anonymous users.
    private val signInMutex = Mutex()

    /** The signed-in user's uid, signing in anonymously on first use. */
    suspend fun signedInUid(): DataResult<String> = signInMutex.withLock {
        auth.currentUser?.let { return DataResult.Success(it.uid) }
        try {
            val uid = auth.signInAnonymously().await().user?.uid
            if (uid != null) DataResult.Success(uid) else DataResult.Failure(DataError.UNKNOWN)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DataResult.Failure(error.toDataError())
        }
    }
}
