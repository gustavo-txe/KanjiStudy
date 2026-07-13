package com.app.kanjistudy.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.app.kanjistudy.data.remote.KanjiApiService
import com.app.kanjistudy.data.local.AppDatabase
import com.app.kanjistudy.data.local.KanjiDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val APP_PREFS = "kanji_study_prefs"
    private const val ONBOARDING_PREFS = "onboarding_prefs"
    private const val APP_DATASTORE = "kanji_study"

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            migrations = listOf(
                SharedPreferencesMigration(context, APP_PREFS),
                SharedPreferencesMigration(context, ONBOARDING_PREFS)
            ),
            produceFile = { context.preferencesDataStoreFile(APP_DATASTORE) }
        )
    }

    @Provides
    @Singleton
    fun provideKanjiApi(): KanjiApiService {
        return Retrofit.Builder()
            .baseUrl("https://kanjiapi.dev/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KanjiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "kanji_table"
        ).addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideKanjiDao(db: AppDatabase): KanjiDao {
        return db.kanjiDao()
    }
}
