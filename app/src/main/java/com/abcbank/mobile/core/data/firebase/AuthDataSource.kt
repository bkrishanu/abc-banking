package com.abcbank.mobile.core.data.firebase

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The install's anonymous Firebase user (ARCHITECTURE.md section 04, Auth and session). Each
 * install is one demo customer; the persona login happens on top of this user, locally, and never
 * waits for it. Firebase keeps the user across launches, so after the first launch this is
 * immediate and needs no network.
 */
@Singleton
class AuthDataSource @Inject constructor(private val auth: FirebaseAuth) {
    // Two callers racing on first launch must not create two anonymous users.
    private val signInMutex = Mutex()

    /**
     * True once this process created the anonymous user, so a brand-new install knows it has no
     * data on the server without asking.
     */
    @Volatile
    var createdThisLaunch: Boolean = false
        private set

    /** The signed-in user's uid, signing in anonymously on first use. */
    suspend fun signedInUid(): DataResult<String> = signInMutex.withLock {
        auth.currentUser?.let { return DataResult.Success(it.uid) }
        try {
            val uid = auth.signInAnonymously().await().user?.uid
            if (uid != null) {
                createdThisLaunch = true
                DataResult.Success(uid)
            } else {
                DataResult.Failure(DataError.UNKNOWN)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DataResult.Failure(error.toDataError())
        }
    }

    /**
     * Keeps trying until signed in, backing off up to [maxDelay]. For background work, such as
     * preparing a persona's data, that should simply happen once the device is back online.
     */
    suspend fun awaitSignedInUid(maxDelay: Duration = MAX_RETRY_DELAY): String {
        var wait = FIRST_RETRY_DELAY
        while (true) {
            when (val result = signedInUid()) {
                is DataResult.Success -> return result.value
                is DataResult.Failure -> {
                    delay(wait)
                    wait = (wait * 2).coerceAtMost(maxDelay)
                }
            }
        }
    }

    private companion object {
        val FIRST_RETRY_DELAY = 2.seconds
        val MAX_RETRY_DELAY = 30.seconds
    }
}
