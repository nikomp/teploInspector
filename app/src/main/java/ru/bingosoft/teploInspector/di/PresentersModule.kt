package ru.bingosoft.teploInspector.di

import dagger.Module
import dagger.Provides
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.ui.map.MapPresenter

@Module
class PresentersModule {
    @Provides
    fun providesMapPresenter(database: AppDatabase)=
        MapPresenter(database)

}