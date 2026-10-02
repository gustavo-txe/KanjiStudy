package com.app.kanjistudy.di

import com.app.kanjistudy.data.ocr.MlKitJapaneseTextRecognizer
import com.app.kanjistudy.data.ocr.TextRecognizer
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
