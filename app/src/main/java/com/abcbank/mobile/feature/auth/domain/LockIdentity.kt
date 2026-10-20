package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.security.RememberedCustomerStore
import javax.inject.Inject

/** Who the lock screen greets: "Welcome back, Samanta" with their avatar. */
data class LockIdentity(val personaId: PersonaId, val firstName: String)

/** First names from the bundled persona files, so the lock screen never waits on Firestore. */
interface PersonaNames {
    suspend fun firstName(personaId: PersonaId): String?
}

/** The remembered customer's persona and name, or null when nobody is remembered. */
class GetLockIdentityUseCase @Inject constructor(
    private val rememberedCustomerStore: RememberedCustomerStore,
    private val personaDirectory: PersonaDirectory,
    private val personaNames: PersonaNames
) {
    suspend operator fun invoke(): LockIdentity? {
        val customer = rememberedCustomerStore.rememberedCustomer() ?: return null
        val persona = personaDirectory.personaFor(customer) ?: return null
        return LockIdentity(persona, personaNames.firstName(persona) ?: return null)
    }
}
