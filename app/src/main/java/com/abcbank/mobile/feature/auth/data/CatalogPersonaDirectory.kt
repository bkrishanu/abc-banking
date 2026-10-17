package com.abcbank.mobile.feature.auth.data

import com.abcbank.mobile.core.data.seed.PersonaCatalog
import com.abcbank.mobile.core.model.CustomerId
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.feature.auth.domain.PersonaDirectory
import javax.inject.Inject

class CatalogPersonaDirectory @Inject constructor() : PersonaDirectory {
    override fun personaFor(customerId: CustomerId): PersonaId? =
        PersonaCatalog.personaFor(customerId)
}
