package ru.bingosoft.mapquestapp2.di

import android.content.Context
import dagger.Module
import dagger.Provides
import ru.bingosoft.mapquestapp2.App
import javax.inject.Singleton

@Module
class AppModule {
    @Provides
    @Singleton
    fun provideApplication(app: App): Context = app.applicationContext
}
