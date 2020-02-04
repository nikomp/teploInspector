package ru.bingosoft.mapquestapp2.di

import dagger.Module
import dagger.Provides
import ru.bingosoft.mapquestapp2.App
import ru.bingosoft.mapquestapp2.util.SharedPrefSaver
import ru.bingosoft.mapquestapp2.util.Toaster
import javax.inject.Singleton

@Module
class UtilModule {
    @Provides
    @Singleton
    fun provideToaster(application: App): Toaster {
        return Toaster(application.applicationContext)
    }

    @Provides
    @Singleton
    fun provideSharedPrefSaver(application: App): SharedPrefSaver {
        return SharedPrefSaver(application.applicationContext)
    }
}