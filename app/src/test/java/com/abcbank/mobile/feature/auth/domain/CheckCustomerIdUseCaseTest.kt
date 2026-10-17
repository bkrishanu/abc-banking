package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.feature.auth.data.CatalogPersonaDirectory
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckCustomerIdUseCaseTest {
    private val check = CheckCustomerIdUseCase(CatalogPersonaDirectory())

    private fun personaOf(input: String) = (check(input) as CustomerIdCheck.Recognised).personaId

    @Test
    fun `each demo customer ID logs in as its persona`() {
        assertEquals(PersonaId.SAMANTA, personaOf("SAMJOH01"))
        assertEquals(PersonaId.MARCUS, personaOf("MARLEE01"))
        assertEquals(PersonaId.GRACE, personaOf("GRAWHI01"))
        assertEquals(PersonaId.DANIEL, personaOf("DANOKA01"))
    }

    @Test
    fun `matching ignores case and surrounding spaces`() {
        assertEquals(PersonaId.SAMANTA, personaOf("  samjoh01 "))
    }

    @Test
    fun `fewer than 8 letters or digits is still typing, not an error`() {
        assertEquals(CustomerIdCheck.Incomplete, check(""))
        assertEquals(CustomerIdCheck.Incomplete, check("SAMJOH0"))
    }

    @Test
    fun `anything other than ASCII letters and digits is a format error`() {
        assertEquals(CustomerIdCheck.WrongFormat, check("SAM.JOH"))
        assertEquals(CustomerIdCheck.WrongFormat, check("SAM JOH01"))
        assertEquals(CustomerIdCheck.WrongFormat, check("SAMJÖH01"))
    }

    @Test
    fun `more than 8 characters is a format error`() {
        assertEquals(CustomerIdCheck.WrongFormat, check("SAMJOH012"))
    }

    @Test
    fun `a well-formed ID that is not a demo customer is unknown`() {
        assertEquals(CustomerIdCheck.Unknown, check("ABCDEF12"))
    }
}
