package com.google.ai.edge.gallery.data

import androidx.datastore.core.DataStoreFactory
import com.google.ai.edge.gallery.BenchmarkResultsSerializer
import com.google.ai.edge.gallery.CutoutsSerializer
import com.google.ai.edge.gallery.SettingsSerializer
import com.google.ai.edge.gallery.UserDataSerializer
import com.google.ai.edge.gallery.proto.BenchmarkResult
import com.google.ai.edge.gallery.proto.ImportedModel
import com.google.ai.edge.gallery.proto.Theme
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreRepositoryTest {
  @Test
  fun writeOperationsUpdateSnapshotsImmediately() = runBlocking {
    val tempDir = createTempDirectory(prefix = "datastore-repo-test")
    try {
      val repository = createRepository(tempDir.toString())

      repository.saveTextInputHistory(listOf("hello", "world"))
      repository.saveTheme(Theme.THEME_DARK)
      repository.setHasRunTinyGarden(true)

      assertEquals(listOf("hello", "world"), repository.readTextInputHistory())
      assertEquals(Theme.THEME_DARK, repository.readTheme())
      assertTrue(repository.getHasRunTinyGarden())
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }

  @Test
  fun importedModelsAndTokenWritesRemainReadableFromSnapshots() = runBlocking {
    val tempDir = createTempDirectory(prefix = "datastore-repo-test")
    try {
      val repository = createRepository(tempDir.toString())
      val importedModel = ImportedModel.newBuilder().setFileName("demo.task").setFileSize(42L).build()

      repository.saveImportedModels(listOf(importedModel))
      repository.saveAccessTokenData(
        accessToken = "access",
        refreshToken = "refresh",
        expiresAt = 1234L,
      )

      assertEquals(listOf(importedModel), repository.readImportedModels())
      assertEquals("access", repository.readAccessTokenData()?.accessToken)

      repository.clearAccessTokenData()

      assertFalse(repository.readAccessTokenData()?.hasAccessToken() == true)
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }

  @Test
  fun benchmarkWritesAndDeletesUpdateSnapshotList() = runBlocking {
    val tempDir = createTempDirectory(prefix = "datastore-repo-test")
    try {
      val repository = createRepository(tempDir.toString())
      val firstResult = BenchmarkResult.newBuilder().build()
      val secondResult = BenchmarkResult.newBuilder().build()

      repository.addBenchmarkResult(firstResult)
      repository.addBenchmarkResult(secondResult)

      assertEquals(listOf(secondResult, firstResult), repository.getAllBenchmarkResults())

      repository.deleteBenchmarkResult(index = 0)

      assertEquals(listOf(firstResult), repository.getAllBenchmarkResults())

      repository.setBenchmarkResults(listOf(secondResult))

      assertEquals(listOf(secondResult), repository.getAllBenchmarkResults())
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }

  @Test
  fun readsPersistedSettingsBeforeCollectorWarmsSnapshots() = runBlocking {
    val tempDir = createTempDirectory(prefix = "datastore-repo-test")
    try {
      val basePath = Path.of(tempDir.toString())
      DataStoreFactory.create(serializer = SettingsSerializer) { basePath.resolve("settings.pb").toFile() }
        .updateData { settings ->
          settings
            .toBuilder()
            .setTheme(Theme.THEME_DARK)
            .setIsTosAccepted(true)
            .setHasRunTinyGarden(true)
            .addTextInputHistory("persisted")
            .build()
        }
      DataStoreFactory.create(serializer = UserDataSerializer) { basePath.resolve("user-data.pb").toFile() }
        .updateData { userData ->
          userData
            .toBuilder()
            .setAccessTokenData(
              com.google.ai.edge.gallery.proto.AccessTokenData.newBuilder()
                .setAccessToken("stored")
                .build()
            )
            .build()
        }
      DataStoreFactory.create(serializer = BenchmarkResultsSerializer) {
        basePath.resolve("benchmark-results.pb").toFile()
      }.updateData { results ->
        results.toBuilder().addResult(BenchmarkResult.newBuilder().build()).build()
      }

      val repository = createRepository(tempDir.toString())

      assertEquals(Theme.THEME_DARK, repository.readTheme())
      assertTrue(repository.isTosAccepted())
      assertTrue(repository.getHasRunTinyGarden())
      assertEquals(listOf("persisted"), repository.readTextInputHistory())
      assertEquals("stored", repository.readAccessTokenData()?.accessToken)
      assertEquals(1, repository.getAllBenchmarkResults().size)
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }

  private fun createRepository(tempDir: String): DefaultDataStoreRepository {
    val basePath = Path.of(tempDir)
    val settingsPath = basePath.resolve("settings.pb")
    val userDataPath = basePath.resolve("user-data.pb")
    val cutoutsPath = basePath.resolve("cutouts.pb")
    val benchmarkResultsPath = basePath.resolve("benchmark-results.pb")

    return DefaultDataStoreRepository(
      dataStore = DataStoreFactory.create(serializer = SettingsSerializer) { settingsPath.toFile() },
      userDataDataStore =
        DataStoreFactory.create(serializer = UserDataSerializer) { userDataPath.toFile() },
      cutoutDataStore = DataStoreFactory.create(serializer = CutoutsSerializer) { cutoutsPath.toFile() },
      benchmarkResultsDataStore =
        DataStoreFactory.create(serializer = BenchmarkResultsSerializer) {
          benchmarkResultsPath.toFile()
        },
    )
  }
}
