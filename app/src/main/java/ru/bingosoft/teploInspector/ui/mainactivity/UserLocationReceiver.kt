package ru.bingosoft.teploInspector.ui.mainactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.User.TrackingUserLocation
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class UserLocationReceiver @Inject constructor(
    private val apiService: ApiService,
    private val db: AppDatabase
): BroadcastReceiver() {
    private lateinit var disposable: Disposable

    override fun onReceive(context: Context?, intent: Intent?) {
        val lat=intent?.getDoubleExtra("lat",0.0)
        val lon=intent?.getDoubleExtra("lon",0.0)
        Timber.d("UserLocationReceiver=$lat _ $lon")
        if (lat != null && lon!=null) {
            sendLocation(lat,lon)
            saveLocation(lat,lon)
        }
    }

    private fun saveLocation(lat: Double, lon: Double) {
        Timber.d("Сохранили локацию пользователя")
        val movingUser=TrackingUserLocation(lat=lat,lon=lon,dateLocation = Date())

        Single.fromCallable{
            db.movingUserDao().insert(movingUser)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe() /*{_->
                Timber.d("Сохранили локацию пользователя")
            }*/

    }

    private fun sendLocation(lat: Double, lon: Double) {
        disposable=apiService.saveUserLocation(action="saveUserLocation",lat = lat, lon=lon)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({response ->
                Timber.d(response.toString())
            },{
                Timber.d("ошибка!!!")
                Timber.d(it.printStackTrace().toString())
            })
    }

    fun onDestroy() {
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }
}