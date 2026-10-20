package com.abcbank.mobile.feature.auth.domain

import com.abcbank.mobile.core.common.DataError
import com.abcbank.mobile.core.common.DataResult
import com.abcbank.mobile.core.model.PersonaId
import com.abcbank.mobile.testing.FakeDemoDataRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PrepareDemoDataUseCaseTest {
    private val repository = FakeDemoDataRepository()
    private val prepare = PrepareDemoDataUseCase(repository)

    @Test
    fun `a persona without a tree gets one`() = runTest {
        assertEquals(DataResult.Success(Unit), prepare(PersonaId.SAMANTA))

        assertEquals(listOf(PersonaId.SAMANTA), repository.seeded)
    }

    @Test
    fun `an existing tree at the bundled version is kept, with no writes`() = runTest {
        repository.storedVersions[PersonaId.MARCUS] = 3L

        assertEquals(DataResult.Success(Unit), prepare(PersonaId.MARCUS))

        assertEquals(emptyList<PersonaId>(), repository.seeded)
    }

    @Test
    fun `an older tree is written again`() = runTest {
        repository.storedVersions[PersonaId.MARCUS] = 2L

        prepare(PersonaId.MARCUS)

        assertEquals(listOf(PersonaId.MARCUS), repository.seeded)
    }

    @Test
    fun `switching persona leaves the other persona's tree alone`() = runTest {
        repository.storedVersions[PersonaId.SAMANTA] = 3L

        prepare(PersonaId.DANIEL)
        prepare(PersonaId.SAMANTA)

        assertEquals(listOf(PersonaId.DANIEL), repository.seeded)
        assertEquals(setOf(PersonaId.SAMANTA, PersonaId.DANIEL), repository.storedVersions.keys)
    }

    @Test
    fun `a failure is reported and nothing is written`() = runTest {
        repository.failWith = DataError.OFFLINE

        assertEquals(DataResult.Failure(DataError.OFFLINE), prepare(PersonaId.GRACE))

        assertEquals(emptyList<PersonaId>(), repository.seeded)
    }
}
