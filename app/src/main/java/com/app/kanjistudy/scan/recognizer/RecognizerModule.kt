package com.app.kanjistudy.scan.recognizer

import com.app.kanjistudy.scan.mlkit.MlKitJapaneseTextRecognizer
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