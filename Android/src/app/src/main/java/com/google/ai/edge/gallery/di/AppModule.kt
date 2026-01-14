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

package com.google.ai.edge.gallery.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import androidx.room.Room
import com.google.ai.edge.gallery.AppLifecycleProvider
import com.google.ai.edge.gallery.GalleryLifecycleProvider
import com.google.ai.edge.gallery.SettingsSerializer
import com.google.ai.edge.gallery.UserDataSerializer
import com.google.ai.edge.gallery.data.DataStoreRepository
import com.google.ai.edge.gallery.data.DefaultDataStoreRepository
import com.google.ai.edge.gallery.data.DefaultDownloadRepository
import com.google.ai.edge.gallery.data.DownloadRepository
import com.google.ai.edge.gallery.data.chathistory.ChatAttachmentStorage
import com.google.ai.edge.gallery.data.chathistory.ChatCryptoManager
import com.google.ai.edge.gallery.data.chathistory.ChatHistoryDao
import com.google.ai.edge.gallery.data.chathistory.ChatHistoryDatabase
import com.google.ai.edge.gallery.data.chathistory.ChatHistoryKeyManager
import com.google.ai.edge.gallery.data.chathistory.ChatHistoryRepository
import com.google.ai.edge.gallery.proto.Settings
import com.google.ai.edge.gallery.proto.UserData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

  // Provides the SettingsSerializer
  @Provides
  @Singleton
  fun provideSettingsSerializer(): Serializer<Settings> {
    return SettingsSerializer
  }

  // Provides the UserDataSerializer
  @Provides
  @Singleton
  fun provideUserDataSerializer(): Serializer<UserData> {
    return UserDataSerializer
  }

  // Provides DataStore<Settings>
  @Provides
  @Singleton
  fun provideSettingsDataStore(
    @ApplicationContext context: Context,
    settingsSerializer: Serializer<Settings>,
  ): DataStore<Settings> {
    return DataStoreFactory.create(
      serializer = settingsSerializer,
      produceFile = { context.dataStoreFile("settings.pb") },
    )
  }

  // Provides DataStore<UserData>
  @Provides
  @Singleton
  fun provideUserDataDataStore(
    @ApplicationContext context: Context,
    userDataSerializer: Serializer<UserData>,
  ): DataStore<UserData> {
    return DataStoreFactory.create(
      serializer = userDataSerializer,
      produceFile = { context.dataStoreFile("user_data.pb") },
    )
  }

  // Provides AppLifecycleProvider
  @Provides
  @Singleton
  fun provideAppLifecycleProvider(): AppLifecycleProvider {
    return GalleryLifecycleProvider()
  }

  // Provides DataStoreRepository
  @Provides
  @Singleton
  fun provideDataStoreRepository(
    dataStore: DataStore<Settings>,
    userDataDataStore: DataStore<UserData>,
  ): DataStoreRepository {
    return DefaultDataStoreRepository(dataStore, userDataDataStore)
  }

  // Provides DownloadRepository
  @Provides
  @Singleton
  fun provideDownloadRepository(
    @ApplicationContext context: Context,
    lifecycleProvider: AppLifecycleProvider,
  ): DownloadRepository {
    return DefaultDownloadRepository(context, lifecycleProvider)
  }

  @Provides
  @Singleton
  fun provideChatCryptoManager(): ChatCryptoManager {
    return ChatCryptoManager()
  }

  @Provides
  @Singleton
  fun provideChatHistoryKeyManager(
    @ApplicationContext context: Context,
    cryptoManager: ChatCryptoManager,
  ): ChatHistoryKeyManager {
    return ChatHistoryKeyManager(context, cryptoManager)
  }

  @Provides
  @Singleton
  fun provideChatAttachmentStorage(
    @ApplicationContext context: Context,
    cryptoManager: ChatCryptoManager,
  ): ChatAttachmentStorage {
    return ChatAttachmentStorage(context, cryptoManager)
  }

  @Provides
  @Singleton
  fun provideChatHistoryDatabase(
    @ApplicationContext context: Context,
    keyManager: ChatHistoryKeyManager,
  ): ChatHistoryDatabase {
    val passphrase = keyManager.getOrCreateDbPassphrase()
    val factory = SupportFactory(passphrase)
    return Room.databaseBuilder(context, ChatHistoryDatabase::class.java, "chat_history.db")
      .openHelperFactory(factory)
      .fallbackToDestructiveMigration(true)
      .build()
  }

  @Provides
  @Singleton
  fun provideChatHistoryDao(db: ChatHistoryDatabase): ChatHistoryDao {
    return db.chatHistoryDao()
  }

  @Provides
  @Singleton
  fun provideChatHistoryRepository(
    dao: ChatHistoryDao,
    attachmentStorage: ChatAttachmentStorage,
    @ApplicationContext context: Context,
  ): ChatHistoryRepository {
    return ChatHistoryRepository(dao, attachmentStorage, context)
  }
}
