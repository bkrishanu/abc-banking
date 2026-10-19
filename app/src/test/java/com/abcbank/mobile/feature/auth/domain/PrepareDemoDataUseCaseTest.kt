package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.testing.FakeDemoDataRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrepareDemoDataUseCaseTest {
    private val repository = FakeDemoDataRepository()
    private val prepare = PrepareDemoDataUseCase(repository)

    @Test
    fun `a fresh install seeds the persona`() = runTest {
        assertEquals(DataResult.Success(Unit), prepare(PersonaId.SAMANTA))

        assertEquals(listOf(PersonaId.SAMANTA), repository.seeded)
    }

    @Test
    fun `the same persona keeps its data`() = runTest {
        repository.stored = StoredDemoData(PersonaId.MARCUS, "Marcus Lee", seedVersion = 2L)

        assertEquals(DataResult.Success(Unit), prepare(PersonaId.MARCUS))

        assertEquals(emptyList<PersonaId>(), repository.seeded)
    }

    @Test
    fun `the same persona is seeded again when the bundled seed is newer`() = runTest {
        repository.stored = StoredDemoData(PersonaId.MARCUS, "Marcus Lee", seedVersion = 1L)

        prepare(PersonaId.MARCUS)

        assertEquals(listOf(PersonaId.MARCUS), repository.seeded)
    }

    @Test
    fun `a different persona replaces the stored data`() = runTest {
        repository.stored = StoredDemoData(PersonaId.SAMANTA, "Samanta Johns", seedVersion = 2L)

        prepare(PersonaId.DANIEL)

        assertEquals(listOf(PersonaId.DANIEL), repository.seeded)
        assertEquals(PersonaId.DANIEL, repository.stored?.personaId)
    }

    @Test
    fun `a failure is reported and nothing is seeded`() = runTest {
        repository.failWith = DataError.OFFLINE

        assertEquals(DataResult.Failure(DataError.OFFLINE), prepare(PersonaId.GRACE))

        assertEquals(emptyList<PersonaId>(), repository.seeded)
    }

    @Test
    fun `only a different persona is named in the replace warning`() {
        val stored = StoredDemoData(PersonaId.SAMANTA, "Samanta Johns", seedVersion = 2L)

        assertEquals("Samanta Johns", stored.replacedNameFor(PersonaId.MARCUS))
        assertNull(stored.replacedNameFor(PersonaId.SAMANTA))
    }
}
