package ru.bingosoft.teploInspector.di

import dagger.Module
import dagger.Provides
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.ui.mainactivity.UserLocationReceiver
import ru.bingosoft.teploInspector.util.OtherUtil
import ru.bingosoft.teploInspector.util.PhotoHelper
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.Toaster
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
    fun provideOtherUtil(toaster: Toaster): OtherUtil {
        return OtherUtil(toaster)
    }

    /*@Provides
    @Singleton
    fun provideUserLocationNative(): UserLocationNative {
        return UserLocationNative()
    }*/

    @Provides
    @Singleton
    fun provideUserLocationReceiver(apiService: ApiService, db: AppDatabase): UserLocationReceiver {
        return UserLocationReceiver(apiService, db)
    }
}