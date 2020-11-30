package ru.bingosoft.teploInspector.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ru.bingosoft.teploInspector.util.Const.LocationStatus.NOT_AVAILABLE
import ru.bingosoft.teploInspector.util.Const.LocationStatus.PROVIDER_DISABLED
import ru.bingosoft.teploInspector.util.Const.LocationStatus.PROVIDER_ENABLED
import timber.log.Timber
import java.util.*


class UserLocationService: Service() {

    private var locationManager: LocationManager?=null
    private val locationInterval = 2000L // минимальное время (в миллисекундах) между получением данных.
    private val locationDistance = 3f //минимальное расстояние (в метрах). Т.е. если ваше местоположение изменилось на указанное кол-во метров, то вам придут новые координаты

    var startTimeService: Long = 0L


    var userLocationListener =
        arrayOf(
            UserLocationListener(LocationManager.GPS_PROVIDER,this),
            UserLocationListener(LocationManager.NETWORK_PROVIDER,this)
        )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTimeService=Date().time
        Timber.d("onStartCommand in time=$startTimeService")
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        Timber.d("service_onCreate")
        initializeLocationManager()

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, locationInterval, locationDistance,
                userLocationListener[0]
            )
        } catch (e: SecurityException) {
            Timber.d("Не удается запросить обновление местоположения, игнорировать ${e.printStackTrace()}")
            //stopSelf()
        } catch (e: IllegalArgumentException) {
            Timber.d("GPS провайдер не существует ${e.printStackTrace()}")
            //stopSelf()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, locationInterval, locationDistance,
                userLocationListener[1]
            )
        } catch (e: SecurityException) {
            Timber.d("Не удается запросить обновление местоположения, игнорировать ${e.printStackTrace()}")
            //stopSelf()
        } catch (e: IllegalArgumentException) {
            Timber.d("GPS провайдер не существует ${e.printStackTrace()}")
            //stopSelf()
        }

        getSharedPreferences("AppSettings", MODE_PRIVATE).edit().putBoolean(
            Const.SharedPrefConst.LOCATION_TRACKING,true).apply()
    }



    override fun onDestroy() {
        super.onDestroy()
        val lm=locationManager
        if (lm!=null) {
            userLocationListener.forEach {
                try {
                    lm.removeUpdates(it)
                } catch (e: Exception) {
                    Timber.d("Не удается удалить прослушиватели местоположения, игнорировать ${e.printStackTrace()}")
                }
            }
        }

        getSharedPreferences("AppSettings", MODE_PRIVATE).edit().putBoolean(
            Const.SharedPrefConst.LOCATION_TRACKING,false).apply()
    }

    private fun initializeLocationManager() {
        Timber.d("initializeLocationManager")
        if (locationManager==null) {
            locationManager=applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    class UserLocationListener(val provider: String, private val ctx: Context): LocationListener {
        var lastLocation=Location(provider)

        override fun onLocationChanged(location: Location?) {
            Timber.d("onLocationChanged $location")
            // Изменение координат отслеживаем через MapkitLocationService
            /*sendIntent(location, AVAILABLE)
            lastLocation.set(location)*/
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Timber.d("onStatusChanged $provider $status")
            if (status!=LocationProvider.AVAILABLE) {
                sendIntent(null, NOT_AVAILABLE)
            }
        }

        override fun onProviderEnabled(provider: String?) {
            Timber.d("onProviderEnabled $provider")
            if (provider=="gps") {
                sendIntent(null, PROVIDER_ENABLED)
            }

        }

        override fun onProviderDisabled(provider: String?) {
            Timber.d("onProviderDisabled $provider")
            if (provider=="gps") {
                sendIntent(null, PROVIDER_DISABLED)
            }
        }

        private fun sendIntent(location: Location?, status: String) {
            val intent=Intent("userLocationUpdates")
            if (provider == LocationManager.GPS_PROVIDER) {
                intent.putExtra("provider","GPS_PROVIDER")
            }
            intent.putExtra("status",status)
            if (location != null) {
                intent.putExtra("lat",location.latitude)
                intent.putExtra("lon",location.longitude)
            }
            // Получим разницу времени старта слежения и текущего времении
            val currentTime=Date().time
            val diffTimeMinute=OtherUtil().getDifferenceTime((ctx as UserLocationService).startTimeService, Date().time)
            Timber.d("diffTimeMinute=$diffTimeMinute")
            if (diffTimeMinute>=3) {
                intent.putExtra("sendRouteToServer",true)
                ctx.startTimeService=currentTime
            }

            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
        }
    }


}