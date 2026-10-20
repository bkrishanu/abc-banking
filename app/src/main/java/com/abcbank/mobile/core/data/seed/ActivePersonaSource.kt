package com.abcbank.mobile.core.data.seed

import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.core.security.RememberedCustomerStore
import javax.inject.Inject

/**
 * The persona whose data the screens show: the logged-in customer ID, looked up locally. No
 * Firestore read is needed to know whose tree to open (ARCHITECTURE.md section 04).
 */
class ActivePersonaSource @Inject constructor(
    private val rememberedCustomerStore: RememberedCustomerStore
) {
    /** Null when nobody is logged in. */
    suspend fun activePersona(): PersonaId? =
        rememberedCustomerStore.rememberedCustomer()?.let(PersonaCatalog::personaFor)
}
