package ru.bingosoft.mapquestapp2.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import ru.bingosoft.mapquestapp2.ui.checkup.CheckupFragment
import ru.bingosoft.mapquestapp2.ui.checkuplist.CheckupListFragment
import ru.bingosoft.mapquestapp2.ui.checkuplist_bottom.CheckupListBottomSheet
import ru.bingosoft.mapquestapp2.ui.login.LoginActivity
import ru.bingosoft.mapquestapp2.ui.mainactivity.MainActivity
import ru.bingosoft.mapquestapp2.ui.map.MapFragment
import ru.bingosoft.mapquestapp2.ui.order.OrderFragment

@Module
abstract class ActivityBuilder {
    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindMapActivity(): MapFragment

    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindOrderActivity(): OrderFragment

    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindCheckupActivity(): CheckupFragment

    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindCheckupListFragment(): CheckupListFragment

    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindCheckupListBottomSheet(): CheckupListBottomSheet

    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindLoginActivity(): LoginActivity

    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindMainActivity(): MainActivity

}