package ru.bingosoft.teploInspector.ui.mainactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.User.TrackingUserLocation
import ru.bingosoft.teploInspector.util.Const.LocationStatus.PROVIDER_DISABLED
import ru.bingosoft.teploInspector.util.Const.MessageCode.DISABLE_LOCATION
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class UserLocationReceiver @Inject constructor(
    private val apiService: ApiService,
    private val db: AppDatabase
): BroadcastReceiver() {
    private lateinit var disposable: Disposable
    lateinit var lastKnownLocation: Location

    override fun onReceive(context: Context?, intent: Intent?) {
        val lat=intent?.getDoubleExtra("lat",0.0)
        val lon=intent?.getDoubleExtra("lon",0.0)
        val provider=intent?.getStringExtra("provider")
        /*lastKnownLocation=Location(provider)
        if (lat!=null && lon!=null) {
            lastKnownLocation.longitude=lon
            lastKnownLocation.latitude=lat
        }*/

        val status=intent?.getStringExtra("status")
        if (status==PROVIDER_DISABLED) {
            sendMessageToAdmin(DISABLE_LOCATION)
        }
        Timber.d("UserLocationReceiver=$lat _ $lon")
        if (lat != null && lon!=null) {
            sendLocation(lat,lon)
            if (provider != null && status != null) {
                    saveLocation(lat,lon,provider,status)
            }
        } else {
            if (provider != null && status != null) {
                saveLocation(lat,lon,provider,status)
            }
        }
    }

    private fun saveLocation(lat: Double?, lon: Double?, provider: String, status: String) {
        Timber.d("Сохранили локацию пользователя")
        val movingUser=TrackingUserLocation(lat=lat,lon=lon,dateLocation = Date(),provider = provider, status = status)

        Single.fromCallable{
            db.trackingUserDao().insert(movingUser)
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

    private fun sendMessageToAdmin(codeMsg: Int) {
        Timber.d("sendMessageToAdmin codeMsg=$codeMsg")
        disposable=apiService.sendMessageToAdmin(action="sendMessageToAdmin",codeMessage = codeMsg)
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