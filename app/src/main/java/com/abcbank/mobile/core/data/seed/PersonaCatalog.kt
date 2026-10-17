package com.abcbank.mobile.core.data.seed

import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.PersonaId

/**
 * Demo customer IDs (DEMO_DATA.md section 02). Must match `profile.customerId` in
 * assets/seed/personas/ once the seed files exist.
 */
object PersonaCatalog {
    private val personaByCustomerId: Map<String, PersonaId> = mapOf(
        "SAMJOH01" to PersonaId.SAMANTA,
        "MARLEE01" to PersonaId.MARCUS,
        "GRAWHI01" to PersonaId.GRACE,
        "DANOKA01" to PersonaId.DANIEL
    )

    fun personaFor(customerId: CustomerId): PersonaId? = personaByCustomerId[customerId.value]
}
