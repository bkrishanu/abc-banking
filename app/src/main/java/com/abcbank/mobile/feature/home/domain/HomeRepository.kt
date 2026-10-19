package com.abcbank.mobile.feature.home.domain

import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.Account
import com.abcbank.mobile.core.model.Card
import com.abcbank.mobile.core.model.Transaction
import com.abcbank.mobile.core.model.UserProfile
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Everything Home shows, as last read from Firestore. */
data class HomeData(
    val profile: UserProfile,
    val accounts: List<Account>,
    val cards: List<Card>,
    /** Every transaction since the requested instant, on any account. */
    val recentTransactions: List<Transaction>,
    val unreadNotifications: Int,
    /** True while any part came from the offline cache rather than the server. */
    val isFromCache: Boolean
)

interface HomeRepository {
    /** Live: emits again whenever the data changes. */
    fun observeHome(transactionsSince: Instant): Flow<DataResult<HomeData>>
}
