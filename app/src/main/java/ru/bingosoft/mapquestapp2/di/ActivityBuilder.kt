package ru.bingosoft.mapquestapp2.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import ru.bingosoft.mapquestapp2.ui.map.MapFragment
import ru.bingosoft.mapquestapp2.ui.order.OrderFragment

@Module
abstract class ActivityBuilder {
    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindMapActivity(): MapFragment

    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindOrderActivity(): OrderFragment
}