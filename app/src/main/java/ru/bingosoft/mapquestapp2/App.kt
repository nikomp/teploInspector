package ru.bingosoft.mapquestapp2

import android.app.Application
import android.util.Log
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector

import ru.bingosoft.mapquestapp2.di.DaggerAppComponent
import ru.bingosoft.mapquestapp2.util.Const
import timber.log.Timber
import java.util.*
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

    /**
     * Метод для генерации ГУИДа, нужен для первичного формирования fingerprint
     *
     * @return - возвращается строка содержащая ГУИД
     */
    private fun random(): String {
        var stF = UUID.randomUUID().toString()
        stF = stF.replace("-".toRegex(), "")
        stF = stF.substring(0, 32)
        Log.d(Const.LogTags.LOGTAG, "random()=$stF")

        return stF
    }

}