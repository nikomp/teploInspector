package ru.bingosoft.teploInspector.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.location.*
import com.yandex.mapkit.transport.TransportFactory
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.util.Const.LocationStatus.INTERVAL_SENDING_ROUTE
import ru.bingosoft.teploInspector.util.Const.LocationStatus.LOCATION_UPDATED
import timber.log.Timber
import java.util.*

class MapkitLocationService: Service() {
    var startTimeService: Long = 0L
    private lateinit var locationManager: LocationManager
    private val locationInterval = 30000L // 2000L минимальное время (в миллисекундах) между получением данных.
    private val locationDistance = 50.0 // 3.0 минимальное расстояние (в метрах). Т.е. если ваше местоположение изменилось на указанное кол-во метров, то вам придут новые координаты

    private val locationListener=UserLocationListener(this)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTimeService= Date().time
        Timber.d("MapkitLocationService_onStartCommand in time=$startTimeService")
        MapKitFactory.getInstance().onStart()
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        Timber.d("service_onCreate")
        super.onCreate()

        MapKitFactory.setApiKey(BuildConfig.yandex_mapkit_api)
        MapKitFactory.setLocale("ru_RU")
        MapKitFactory.initialize(this)
        DirectionsFactory.initialize(this)
        TransportFactory.initialize(this)


        locationManager= MapKitFactory.getInstance().createLocationManager()
        locationManager.subscribeForLocationUpdates(
            0.0,
            locationInterval,
            locationDistance,
            true,
            FilteringMode.ON,locationListener)
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        MapKitFactory.getInstance().onStop()
    }

    class UserLocationListener(private val ctx: Context): LocationListener {
        var lastLocation: Location?=null

        override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
            // Статус GPS отслеживаем через UserLocationService
            /*if (locationStatus == LocationStatus.NOT_AVAILABLE) {
                if (lastLocation!=null) {
                    sendIntent(lastLocation!!, PROVIDER_DISABLED)
                } else {
                    sendIntent(null, PROVIDER_DISABLED)
                }

            } else {
                if (lastLocation!=null) {
                    sendIntent(lastLocation!!, PROVIDER_ENABLED)
                } else {
                    sendIntent(null, PROVIDER_ENABLED)
                }
            }*/
        }

        override fun onLocationUpdated(location: Location) {
            Timber.d("onLocationChanged $location")
            sendIntent(location, LOCATION_UPDATED)
            lastLocation=location
        }

        private fun sendIntent(location: Location?, status: String) {
            val intent=Intent("userLocationUpdates")

            intent.putExtra("provider","GPS_PROVIDER")

            intent.putExtra("status",status)
            if (location!=null) {
                intent.putExtra("lat",location.position.latitude)
                intent.putExtra("lon",location.position.longitude)
            }


            // Получим разницу времени старта слежения и текущего времении
            val currentTime=Date().time
            val diffTimeMinute=OtherUtil().getDifferenceTime((ctx as MapkitLocationService).startTimeService, Date().time)
            Timber.d("diffTimeMinute=$diffTimeMinute")
            if (diffTimeMinute>=INTERVAL_SENDING_ROUTE) {
                intent.putExtra("sendRouteToServer",true)
                ctx.startTimeService=currentTime
            }

            LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
        }


    }
}