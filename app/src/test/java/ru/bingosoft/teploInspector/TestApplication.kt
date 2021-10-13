package ru.bingosoft.teploInspector

import android.app.Application
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class TestApplication: Application(), HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.AppTheme)

/*        DaggerTestAppComponent.builder()
            .application(this)
            .build()
            .inject(this)*/
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

}