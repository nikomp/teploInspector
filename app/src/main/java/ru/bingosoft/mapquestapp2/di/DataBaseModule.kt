package ru.bingosoft.mapquestapp.di

import androidx.room.Room
import dagger.Module
import dagger.Provides
import ru.bingosoft.mapquestapp2.App
import ru.bingosoft.mapquestapp2.db.AppDatabase
import javax.inject.Singleton


@Module
class DataBaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(application: App): AppDatabase {
        return Room.databaseBuilder(
            application.applicationContext,
            AppDatabase::class.java, "mydatabase.db"
        )
        .build()
    }

}