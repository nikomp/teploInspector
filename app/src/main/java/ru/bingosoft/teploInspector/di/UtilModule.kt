package ru.bingosoft.teploInspector.di

import dagger.Module
import dagger.Provides
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.util.*
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

    @Provides
    @Singleton
    fun providePhotoHelper(): PhotoHelper {
        return PhotoHelper()
    }

    @Provides
    @Singleton
    fun provideOtherUtil(): OtherUtil {
        return OtherUtil()
    }

    @Provides
    @Singleton
    fun provideUserLocationNative(): UserLocationNative {
        return UserLocationNative()
    }
}