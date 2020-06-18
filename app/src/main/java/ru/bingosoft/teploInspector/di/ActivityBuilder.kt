package ru.bingosoft.teploInspector.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.ui.checkuplist.CheckupListFragment
import ru.bingosoft.teploInspector.ui.checkuplist_bottom.CheckupListBottomSheet
import ru.bingosoft.teploInspector.ui.login.LoginActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.ui.map_bottom.MapBottomSheet
import ru.bingosoft.teploInspector.ui.order.OrderFragment
import ru.bingosoft.teploInspector.ui.route_detail.RouteDetailFragment

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

    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindMapBottomSheet(): MapBottomSheet

    @ContributesAndroidInjector(modules = [PresentersModule::class])
    abstract fun bindRouteDetailFragment(): RouteDetailFragment

}