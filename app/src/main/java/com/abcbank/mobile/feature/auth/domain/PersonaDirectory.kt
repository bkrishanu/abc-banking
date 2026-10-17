package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.PersonaId

/** Looks up which demo persona a customer ID logs in as. */
interface PersonaDirectory {
    fun personaFor(customerId: CustomerId): PersonaId?
}
