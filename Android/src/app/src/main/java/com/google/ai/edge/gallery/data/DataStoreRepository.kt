/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery.data

import android.util.Log
import androidx.datastore.core.DataStore
import com.google.ai.edge.gallery.proto.AccessTokenData
import com.google.ai.edge.gallery.proto.BenchmarkResult
import com.google.ai.edge.gallery.proto.BenchmarkResults
import com.google.ai.edge.gallery.proto.Cutout
import com.google.ai.edge.gallery.proto.CutoutCollection
import com.google.ai.edge.gallery.proto.ImportedModel
import com.google.ai.edge.gallery.proto.Settings
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.proto.UserData
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

interface DataStoreRepository {
  suspend fun saveTextInputHistory(history: List<String>)

  fun readTextInputHistory(): List<String>

  suspend fun saveTheme(theme: Theme)

  fun readTheme(): Theme

  suspend fun saveAccessTokenData(accessToken: String, refreshToken: String, expiresAt: Long)

  suspend fun clearAccessTokenData()

  fun readAccessTokenData(): AccessTokenData?

  suspend fun saveImportedModels(importedModels: List<ImportedModel>)

  suspend fun upsertImportedModel(importedModel: ImportedModel)

  suspend fun removeImportedModel(fileName: String)

  fun readImportedModels(): List<ImportedModel>

  fun isTosAccepted(): Boolean

  suspend fun acceptTos()

  fun isGemmaTermsOfUseAccepted(): Boolean

  suspend fun acceptGemmaTermsOfUse()

  fun getHasRunTinyGarden(): Boolean

  suspend fun setHasRunTinyGarden(value: Boolean)

  suspend fun addCutout(cutout: Cutout)

  fun getAllCutouts(): List<Cutout>

  suspend fun setCutout(newCutout: Cutout)

  suspend fun setCutouts(cutouts: List<Cutout>)

  suspend fun setHasSeenBenchmarkComparisonHelp(seen: Boolean)

  fun getHasSeenBenchmarkComparisonHelp(): Boolean

  suspend fun addBenchmarkResult(result: BenchmarkResult)

  suspend fun setBenchmarkResults(results: List<BenchmarkResult>)

  fun getAllBenchmarkResults(): List<BenchmarkResult>

  suspend fun deleteBenchmarkResult(index: Int)

  suspend fun addViewedPromoId(promoId: String)

  suspend fun removeViewedPromoId(promoId: String)

  fun hasViewedPromo(promoId: String): Boolean
}

/** Repository for managing data using Proto DataStore. */
class DefaultDataStoreRepository(
  private val dataStore: DataStore<Settings>,
  private val userDataDataStore: DataStore<UserData>,
  private val cutoutDataStore: DataStore<CutoutCollection>,
  private val benchmarkResultsDataStore: DataStore<BenchmarkResults>,
) : DataStoreRepository {
  companion object {
    private const val TAG = "AGDataStoreRepository"
  }

  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val settingsState = MutableStateFlow(Settings.getDefaultInstance())
  private val userDataState = MutableStateFlow(UserData.getDefaultInstance())
  private val cutoutsState = MutableStateFlow(CutoutCollection.getDefaultInstance())
  private val benchmarkResultsState = MutableStateFlow(BenchmarkResults.getDefaultInstance())
  private val settingsReady = CountDownLatch(1)
  private val userDataReady = CountDownLatch(1)
  private val cutoutsReady = CountDownLatch(1)
  private val benchmarkResultsReady = CountDownLatch(1)
  private val settingsFailure = AtomicReference<Throwable?>(null)
  private val userDataFailure = AtomicReference<Throwable?>(null)
  private val cutoutsFailure = AtomicReference<Throwable?>(null)
  private val benchmarkResultsFailure = AtomicReference<Throwable?>(null)

  init {
    launchStateCollector(dataStore, settingsState, settingsReady, settingsFailure)
    launchStateCollector(userDataDataStore, userDataState, userDataReady, userDataFailure)
    launchStateCollector(cutoutDataStore, cutoutsState, cutoutsReady, cutoutsFailure)
    launchStateCollector(
      benchmarkResultsDataStore,
      benchmarkResultsState,
      benchmarkResultsReady,
      benchmarkResultsFailure,
    )
  }

  private fun <T> launchStateCollector(
    store: DataStore<T>,
    state: MutableStateFlow<T>,
    ready: CountDownLatch,
    failure: AtomicReference<Throwable?>,
  ) {
    repositoryScope.launch {
      try {
        store.data.collect {
          state.value = it
          ready.countDown()
        }
      } catch (t: Throwable) {
        if (ready.count > 0) {
          failure.compareAndSet(null, t)
          ready.countDown()
        } else {
          Log.e(TAG, "DataStore collector failed after initialization; keeping last snapshot", t)
        }
      }
    }
  }

  private fun <T> awaitState(
    ready: CountDownLatch,
    failure: AtomicReference<Throwable?>,
    state: MutableStateFlow<T>,
    label: String,
  ): T {
    ready.await()
    failure.get()?.let { throw IllegalStateException("Failed to initialize $label datastore", it) }
    return state.value
  }

  private fun awaitSettings(): Settings {
    return awaitState(settingsReady, settingsFailure, settingsState, "settings")
  }

  private fun awaitUserData(): UserData {
    return awaitState(userDataReady, userDataFailure, userDataState, "userData")
  }

  private fun awaitCutouts(): CutoutCollection {
    return awaitState(cutoutsReady, cutoutsFailure, cutoutsState, "cutouts")
  }

  private fun awaitBenchmarkResults(): BenchmarkResults {
    return awaitState(
      benchmarkResultsReady,
      benchmarkResultsFailure,
      benchmarkResultsState,
      "benchmarkResults",
    )
  }

  override suspend fun saveTextInputHistory(history: List<String>) {
    val updatedSettings =
      dataStore.updateData { settings ->
        settings.toBuilder().clearTextInputHistory().addAllTextInputHistory(history).build()
      }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override fun readTextInputHistory(): List<String> {
    return awaitSettings().textInputHistoryList
  }

  override suspend fun saveTheme(theme: Theme) {
    val updatedSettings = dataStore.updateData { settings -> settings.toBuilder().setTheme(theme).build() }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override fun readTheme(): Theme {
    val curTheme = awaitSettings().theme
    return if (curTheme == Theme.THEME_UNSPECIFIED) Theme.THEME_AUTO else curTheme
  }

  override suspend fun saveAccessTokenData(
    accessToken: String,
    refreshToken: String,
    expiresAt: Long,
  ) {
    val accessTokenData =
      AccessTokenData.newBuilder()
        .setAccessToken(accessToken)
        .setRefreshToken(refreshToken)
        .setExpiresAtMs(expiresAt)
        .build()

    // Clear the entry in old data store.
    val updatedSettings =
      dataStore.updateData { settings ->
        settings.toBuilder().setAccessTokenData(AccessTokenData.getDefaultInstance()).build()
      }
    settingsState.value = updatedSettings
    settingsReady.countDown()

    val updatedUserData =
      userDataDataStore.updateData { userData ->
        userData.toBuilder().setAccessTokenData(accessTokenData).build()
      }
    userDataState.value = updatedUserData
    userDataReady.countDown()
  }

  override suspend fun clearAccessTokenData() {
    val updatedSettings = dataStore.updateData { settings -> settings.toBuilder().clearAccessTokenData().build() }
    settingsState.value = updatedSettings
    settingsReady.countDown()
    val updatedUserData =
      userDataDataStore.updateData { userData -> userData.toBuilder().clearAccessTokenData().build() }
    userDataState.value = updatedUserData
    userDataReady.countDown()
  }

  override fun readAccessTokenData(): AccessTokenData? {
    return awaitUserData().accessTokenData
  }

  override suspend fun saveImportedModels(importedModels: List<ImportedModel>) {
    val updatedSettings =
      dataStore.updateData { settings ->
        settings.toBuilder().clearImportedModel().addAllImportedModel(importedModels).build()
      }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override suspend fun upsertImportedModel(importedModel: ImportedModel) {
    val updatedSettings =
      dataStore.updateData { settings ->
        val builder = settings.toBuilder().clearImportedModel()
        builder.addAllImportedModel(settings.importedModelList.filter { it.fileName != importedModel.fileName })
        builder.addImportedModel(importedModel)
        builder.build()
      }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override suspend fun removeImportedModel(fileName: String) {
    val updatedSettings =
      dataStore.updateData { settings ->
        settings
          .toBuilder()
          .clearImportedModel()
          .addAllImportedModel(settings.importedModelList.filter { it.fileName != fileName })
          .build()
      }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override fun readImportedModels(): List<ImportedModel> {
    return awaitSettings().importedModelList
  }

  override fun isTosAccepted(): Boolean {
    return awaitSettings().isTosAccepted
  }

  override suspend fun acceptTos() {
    val updatedSettings =
      dataStore.updateData { settings -> settings.toBuilder().setIsTosAccepted(true).build() }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override fun isGemmaTermsOfUseAccepted(): Boolean {
    return awaitSettings().isGemmaTermsAccepted
  }

  override suspend fun acceptGemmaTermsOfUse() {
    val updatedSettings =
      dataStore.updateData { settings -> settings.toBuilder().setIsGemmaTermsAccepted(true).build() }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override fun getHasRunTinyGarden(): Boolean {
    return awaitSettings().hasRunTinyGarden
  }

  override suspend fun setHasRunTinyGarden(value: Boolean) {
    val updatedSettings =
      dataStore.updateData { settings -> settings.toBuilder().setHasRunTinyGarden(value).build() }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override suspend fun addCutout(cutout: Cutout) {
    val updatedCutouts =
      cutoutDataStore.updateData { cutouts -> cutouts.toBuilder().addCutout(cutout).build() }
    cutoutsState.value = updatedCutouts
    cutoutsReady.countDown()
  }

  override fun getAllCutouts(): List<Cutout> {
    return awaitCutouts().cutoutList
  }

  override suspend fun setCutout(newCutout: Cutout) {
    val updatedCutouts =
      cutoutDataStore.updateData { cutouts ->
        var index = -1
        for (i in 0 until cutouts.cutoutCount) {
          val cutout = cutouts.cutoutList[i]
          if (cutout.id == newCutout.id) {
            index = i
            break
          }
        }
        if (index >= 0) cutouts.toBuilder().setCutout(index, newCutout).build() else cutouts
      }
    cutoutsState.value = updatedCutouts
    cutoutsReady.countDown()
  }

  override suspend fun setCutouts(cutouts: List<Cutout>) {
    val updatedCutouts =
      cutoutDataStore.updateData { CutoutCollection.newBuilder().addAllCutout(cutouts).build() }
    cutoutsState.value = updatedCutouts
    cutoutsReady.countDown()
  }

  override suspend fun setHasSeenBenchmarkComparisonHelp(seen: Boolean) {
    val updatedSettings =
      dataStore.updateData { settings ->
        settings.toBuilder().setHasSeenBenchmarkComparisonHelp(seen).build()
      }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override fun getHasSeenBenchmarkComparisonHelp(): Boolean {
    return awaitSettings().hasSeenBenchmarkComparisonHelp
  }

  override suspend fun addBenchmarkResult(result: BenchmarkResult) {
    val updatedResults =
      benchmarkResultsDataStore.updateData { results -> results.toBuilder().addResult(0, result).build() }
    benchmarkResultsState.value = updatedResults
    benchmarkResultsReady.countDown()
  }

  override suspend fun setBenchmarkResults(results: List<BenchmarkResult>) {
    val updatedResults =
      benchmarkResultsDataStore.updateData { currentResults ->
        currentResults.toBuilder().clearResult().addAllResult(results).build()
      }
    benchmarkResultsState.value = updatedResults
    benchmarkResultsReady.countDown()
  }

  override fun getAllBenchmarkResults(): List<BenchmarkResult> {
    return awaitBenchmarkResults().resultList
  }

  override suspend fun deleteBenchmarkResult(index: Int) {
    val updatedResults =
      benchmarkResultsDataStore.updateData { results -> results.toBuilder().removeResult(index).build() }
    benchmarkResultsState.value = updatedResults
    benchmarkResultsReady.countDown()
  }

  override suspend fun addViewedPromoId(promoId: String) {
    val updatedSettings =
      dataStore.updateData { settings ->
        if (settings.viewedPromoIdList.contains(promoId)) settings
        else settings.toBuilder().addViewedPromoId(promoId).build()
      }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override suspend fun removeViewedPromoId(promoId: String) {
    val updatedSettings =
      dataStore.updateData { settings ->
        val newList = settings.viewedPromoIdList.filter { it != promoId }
        settings.toBuilder().clearViewedPromoId().addAllViewedPromoId(newList).build()
      }
    settingsState.value = updatedSettings
    settingsReady.countDown()
  }

  override fun hasViewedPromo(promoId: String): Boolean {
    return awaitSettings().viewedPromoIdList.contains(promoId)
  }

}
