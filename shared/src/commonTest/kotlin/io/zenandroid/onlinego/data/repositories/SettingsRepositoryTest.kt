package io.zenandroid.onlinego.data.repositories

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.zenandroid.onlinego.data.model.ogs.Speed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsRepositoryTest {

  private fun TestScope.newRepository() = SettingsRepository(
    dataStore = PreferenceDataStoreFactory.createWithPath(scope = backgroundScope) {
      FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "settings-${
        Random.nextLong().toULong()
      }.preferences_pb"
    },
    applicationScope = backgroundScope,
  )

  @Test
  fun aFreshStoreServesTheDefaults() = runTest {
    val repository = newRepository()

    assertEquals(true, repository.showRanksFlow.first())
    assertEquals(false, repository.showCoordinatesFlow.first())
    assertEquals(listOf(Speed.BLITZ, Speed.RAPID, Speed.LIVE), repository.searchGameSpeeds.first())
    assertNull(repository.ogsEbiFlow.first())
  }

  @Test
  fun searchSpeedsRoundTrip() = runTest {
    val repository = newRepository()

    repository.setSearchGameSpeeds(listOf(Speed.LIVE, Speed.LONG))

    assertEquals(listOf(Speed.LIVE, Speed.LONG), repository.searchGameSpeeds.first())
  }

  @Test
  fun aFaceToFaceGameIsStoredAsOneEdit() = runTest {
    val repository = newRepository()

    repository.setFaceToFaceGame(history = "aa,bb", boardSize = "19x19", handicap = 2)

    assertEquals("aa,bb", repository.faceToFaceHistoryFlow.first())
    assertEquals("19x19", repository.faceToFaceBoardSizeFlow.first())
    assertEquals(2, repository.faceToFaceHandicapFlow.first())
  }

  @Test
  fun completedTutorialsRoundTrip() = runTest {
    val repository = newRepository()

    repository.setCompletedTutorials(setOf("basics", "capturing"))

    assertEquals(setOf("basics", "capturing"), repository.completedTutorialsFlow.first())
  }
}
