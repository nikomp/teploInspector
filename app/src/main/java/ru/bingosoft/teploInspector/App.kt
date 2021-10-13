package ru.bingosoft.teploInspector

import android.app.Application
import androidx.annotation.VisibleForTesting
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.Directions
import com.yandex.mapkit.transport.Transport
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.plugins.RxJavaPlugins
import ru.bingosoft.teploInspector.di.DaggerAppComponent
import timber.log.Timber
import javax.inject.Inject


class App : Application(), HasAndroidInjector  {



    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    lateinit var directions: Directions
    lateinit var transports: Transport
    lateinit var mkInstances: MapKit
    var lastExceptionAppForTest: Exception?=null


    override fun onCreate() {
        super.onCreate()
        setThemeForTest()

        appInstance=this

        setMapKitApiKey()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        DaggerAppComponent.builder()
            .application(this)
            .build()
            .inject(this)


        //https://proandroiddev.com/rxjava2-undeliverableexception-f01d19d18048
        //https://stackoverflow.com/questions/52631581/rxjava2-undeliverableexception-when-orientation-change-is-happening-while-fetchi
        RxJavaPlugins.setErrorHandler { error: Throwable? -> error?.printStackTrace()}

    }


    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    @VisibleForTesting
    internal fun setThemeForTest() {
        setTheme(R.style.AppTheme)
    }

    private fun setMapKitApiKey() {
        MapKitFactory.setApiKey(BuildConfig.yandex_mapkit_api)
        MapKitFactory.setLocale("ru_RU")
    }

    companion object {
        lateinit var appInstance: App
            private set
    }

}