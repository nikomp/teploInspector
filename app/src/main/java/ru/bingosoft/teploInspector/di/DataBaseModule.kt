package ru.bingosoft.teploInspector.di

import androidx.room.Room
import dagger.Module
import dagger.Provides
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.db.AppDatabase
import timber.log.Timber
import javax.inject.Singleton


@Module
class DataBaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(application: App): AppDatabase {
        Timber.d("provideAppDatabase")
        return Room.databaseBuilder(
            application.applicationContext,
            AppDatabase::class.java, "mydatabase.db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

}