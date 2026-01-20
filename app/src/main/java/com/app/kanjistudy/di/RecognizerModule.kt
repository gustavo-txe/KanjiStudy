package com.app.kanjistudy.di

import com.app.kanjistudy.MlKitJapaneseTextRecognizer
import com.app.kanjistudy.TextRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecognizerModule {

    @Binds
    @Singleton
    abstract fun bindTextRecognizer(
        impl: MlKitJapaneseTextRecognizer
    ): TextRecognizer
}
