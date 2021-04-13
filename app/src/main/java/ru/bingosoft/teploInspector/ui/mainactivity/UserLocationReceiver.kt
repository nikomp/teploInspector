package ru.bingosoft.teploInspector.ui.mainactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.User.TrackingUserLocation
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.Const
import ru.bingosoft.teploInspector.util.Const.LocationStatus.PROVIDER_DISABLED
import ru.bingosoft.teploInspector.util.Const.LocationStatus.PROVIDER_ENABLED
import ru.bingosoft.teploInspector.util.Const.MessageCode.DISABLE_LOCATION
import ru.bingosoft.teploInspector.util.Const.MessageCode.ENABLE_LOCATION
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class UserLocationReceiver @Inject constructor(
    private val apiService: ApiService,
    private val db: AppDatabase
): BroadcastReceiver() {
    private lateinit var disposable: Disposable
    private lateinit var disposableSendMsg: Disposable

    var lastKnownLocation=Location(LocationManager.GPS_PROVIDER)

    lateinit var ctx: Context

    private lateinit var userLocationListener: UserLocationListener

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("onReceive")
        if("userLocationUpdates" == intent?.action) {
            if (context != null) {
                ctx=context
            }

            userLocationListener =UserLocationListener(LocationManager.GPS_PROVIDER, ctx)

            val lat= intent.getDoubleExtra("lat",0.0)
            val lon= intent.getDoubleExtra("lon",0.0)
            val accuracy=intent.getDoubleExtra("accuracy",0.0)
            val speed=intent.getDoubleExtra("speed",0.0)
            val provider= intent.getStringExtra("provider")

            lastKnownLocation=Location(provider)
            lastKnownLocation.longitude=lon
            lastKnownLocation.latitude=lat

            val status= intent.getStringExtra("status")
            Timber.d("statusGPS=$status")
            if (status==PROVIDER_DISABLED) {
                Timber.d("PROVIDER_DISABLED")
                sendMessageToAdmin(DISABLE_LOCATION)
            }
            if (status== PROVIDER_ENABLED) {
                Timber.d("ENABLE_LOCATION")
                sendMessageToAdmin(ENABLE_LOCATION)
            }
            Timber.d("UserLocationReceiver=${lat}_${lon}_ ${accuracy}_${speed}")
            if (provider != null && status != null) {
                saveLocation(lat,lon,provider,status)
            }
        }

    }

    fun isInitLocation() :Boolean {
        //return ::lastKnownLocation.isInitialized
        return true
    }

    private fun saveLocation(lat: Double?, lon: Double?, provider: String, status: String) {
        Timber.d("saveLocation_${Date()}")
        val movingUser=TrackingUserLocation(lat=lat,lon=lon,dateLocation = Date(),provider = provider, status = status)

        disposable=Single.fromCallable{
            db.trackingUserDao().insert(movingUser)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("Сохранили локацию пользователя в БД")
                disposable.dispose()
            },{throwable ->
                throwable.printStackTrace()
                disposable.dispose()
            })

    }

    private fun sendMessageToAdmin(codeMsg: Int) {
        Timber.d("sendMessageToAdmin_codeMsg=$codeMsg")

        val textMessage: String
        val eventType: Int
        when (codeMsg) {
            1-> {
                textMessage="Пользователь отказался выдать разрешение на Геолокацию"
                eventType=1 // Геолокация отключена
            }
            2-> {
                textMessage="Пользователь повторно отказался включить GPS"
                eventType=1 // Геолокация отключена
            }
            3-> {
                textMessage="Пользователь выключил GPS"
                eventType=1 // Геолокация отключена
            }
            4-> {
                textMessage="Пользователь включил GPS"
                eventType=3 // Геолокация включена
            }
            else -> {
                textMessage=""
                eventType=0
            }
        }


        val messageData= Models.MessageData(
            text = textMessage,
            date= Date().time,
            event_type = eventType,
            lat = lastKnownLocation.latitude,
            lon = lastKnownLocation.longitude
        )

        Timber.d("Данные4=${Gson().toJson(messageData)}")

        val jsonBody = Gson().toJson(messageData)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        disposableSendMsg=apiService.sendMessageToAdmin(jsonBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Timber.d("Сообщение_отправлено")
                    disposableSendMsg.dispose()
                },
                {
                    Timber.d(it.printStackTrace().toString())
                    ctx.sendBroadcast(Intent("unauthorized"))
                    disposableSendMsg.dispose()
                }
            )

    }

    class UserLocationListener(val provider: String, private val ctx: Context): LocationListener {

        override fun onLocationChanged(location: Location?) {
            Timber.d("USERLOCATION_onLocationChanged $location")
            // Изменение координат отслеживаем через MapkitLocationService
            /*sendIntent(location, AVAILABLE)
            lastLocation.set(location)*/
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Timber.d("onStatusChanged $provider $status")
            if (status!= LocationProvider.AVAILABLE) {
                sendIntent(Const.LocationStatus.NOT_AVAILABLE)
            }
        }

        override fun onProviderEnabled(provider: String?) {
            Timber.d("onProviderEnabled $provider")
            if (provider=="gps") {
                sendIntent(PROVIDER_ENABLED)
            }

        }

        override fun onProviderDisabled(provider: String?) {
            Timber.d("onProviderDisabled $provider")
            if (provider=="gps") {
                sendIntent(PROVIDER_DISABLED)
            }
        }

        private fun sendIntent(status: String) {
            val intent=Intent("userLocationUpdates")
            if (provider == LocationManager.GPS_PROVIDER) {
                intent.putExtra("provider","GPS_PROVIDER")
            }
            intent.putExtra("status",status)

            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
        }
    }

    fun onDestroy() {
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }
}