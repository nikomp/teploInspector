package ru.bingosoft.mapquestapp2.di

import dagger.Module
import dagger.Provides
import ru.bingosoft.mapquestapp2.db.AppDatabase
import ru.bingosoft.mapquestapp2.ui.map.MapPresenter

@Module
class PresentersModule {
    @Provides
    fun providesMapPresenter(database: AppDatabase)=
        MapPresenter(database)

}