package ru.bingosoft.mapquestapp2

import android.app.Application
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector

import ru.bingosoft.mapquestapp2.di.DaggerAppComponent
import timber.log.Timber
import javax.inject.Inject

class App : Application(), HasAndroidInjector  {

    /*companion object {
        lateinit var db: AppDatabase
    }*/

    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        DaggerAppComponent.builder()
            .application(this)
            .build()
            .inject(this)

        /*db=Room.databaseBuilder(
            this, AppDatabase::class.java, "mydatabase.db"
        ).build()*/

    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector


}