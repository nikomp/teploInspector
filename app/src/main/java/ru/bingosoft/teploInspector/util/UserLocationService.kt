package ru.bingosoft.teploInspector.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import timber.log.Timber


class UserLocationService: Service() {

    private var locationManager: LocationManager?=null
    private val locationInterval = 5000L
    private val locationDistance = 10f


    private var userLocationListener =
        arrayOf(
            UserLocationListener(LocationManager.GPS_PROVIDER,this),
            UserLocationListener(LocationManager.NETWORK_PROVIDER,this)
        )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        Timber.d("onCreate")
        initializeLocationManager()
        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, locationInterval, locationDistance,
                userLocationListener[1]
            )
        } catch (e: SecurityException) {
            Timber.d("Не удается запросить обновление местоположения, игнорировать ${e.printStackTrace()}")
        } catch (e: IllegalArgumentException) {
            Timber.d("Сетевой провайдер не существует ${e.printStackTrace()}")
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, locationInterval, locationDistance,
                userLocationListener[0]
            )
        } catch (e: SecurityException) {
            Timber.d("Не удается запросить обновление местоположения, игнорировать ${e.printStackTrace()}")
        } catch (e: IllegalArgumentException) {
            Timber.d("GPS провайдер не существует ${e.printStackTrace()}")
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
            if (location != null) {
                val intent=Intent("userLocationUpdates")
                intent.putExtra("lat",location.latitude)
                intent.putExtra("lon",location.longitude)
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
            }
            lastLocation.set(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            Timber.d("onStatusChanged $provider")
        }

        override fun onProviderEnabled(provider: String?) {
            Timber.d("onProviderEnabled $provider")
        }

        override fun onProviderDisabled(provider: String?) {
            Timber.d("onProviderDisabled $provider")
        }


    }


}