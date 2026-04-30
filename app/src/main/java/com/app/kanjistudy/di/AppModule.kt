package com.app.kanjistudy.di

import android.content.Context
import android.content.SharedPreferences
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

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
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
        ).build()
    }

    @Provides
    fun provideKanjiDao(db: AppDatabase): KanjiDao {
        return db.kanjiDao()
    }
}