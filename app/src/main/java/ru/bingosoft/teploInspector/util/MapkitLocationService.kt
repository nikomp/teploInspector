package ru.bingosoft.teploInspector.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.location.*
import com.yandex.mapkit.transport.TransportFactory
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.util.Const.LocationStatus.INTERVAL_SAVE_LOCATION
import ru.bingosoft.teploInspector.util.Const.LocationStatus.LOCATION_UPDATED
import ru.bingosoft.teploInspector.util.Const.WebSocketConst.LOCATION_SERVICE_NOTIFICATION_ID
import ru.bingosoft.teploInspector.util.Const.WebSocketConst.NOTIFICATION_CHANNEL_ID_GPS_SERVICES
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class MapkitLocationService: Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    var startTimeService: Long = 0L
    private lateinit var locationManager: LocationManager
    private val locationInterval = 60000L // 2000L минимальное время (в миллисекундах) между получением данных.
    private val locationDistance = 0.0 // 3.0 минимальное расстояние (в метрах). Т.е. если ваше местоположение изменилось на указанное кол-во метров И прошло минимальное время locationInterval, то вам придут новые координаты


    private val locationListener=UserLocationListener(this)



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.P) {
            val pm=(getSystemService(Context.POWER_SERVICE) as PowerManager)
            wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag")
            if (!wakeLock.isHeld) {
                wakeLock.acquire(480*60*1000L /*480 minutes*/)
            }
        }

        startTimeService= Date().time
        Timber.d("MapkitLocationService_onStartCommand in time=$startTimeService")
        MapKitFactory.getInstance().onStart()
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        Timber.d("service_onCreate")
        super.onCreate()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_GPS_SERVICES,
                "Сервис геолокации",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            val notification: Notification = Notification.Builder(
                this,
                NOTIFICATION_CHANNEL_ID_GPS_SERVICES
            )
                .setContentTitle(getText(R.string.location_service_title))
                .setContentText(getText(R.string.location_service_content))
                .setSmallIcon(R.drawable.ic_service_location)
                .build()

            if (Build.VERSION.SDK_INT >= 29) {
                Timber.d("FOREGROUND_SERVICE_TYPE_LOCATION")
                startForeground(
                    LOCATION_SERVICE_NOTIFICATION_ID,
                    notification,
                    FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(LOCATION_SERVICE_NOTIFICATION_ID, notification)
            }

        }


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
            FilteringMode.ON, locationListener
        )
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Timber.d("MapkitDestroy")
        super.onDestroy()
        MapKitFactory.getInstance().onStop()
        if (this::wakeLock.isInitialized) {
            wakeLock.release()
        }

    }

    class UserLocationListener(private val ctx: Context): LocationListener {
        private var lastLocation: Location?=null
        @Inject
        lateinit var otherUtil: OtherUtil

        override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
            // Статус GPS отслеживаем через UserLocationService
        }

        override fun onLocationUpdated(location: Location) {
            Timber.d("onLocationChangedMapKit ${Date()} ")
            sendIntent(location, LOCATION_UPDATED)
            lastLocation=location
        }

        private fun sendIntent(location: Location?, status: String) {
            val intent=Intent("userLocationUpdates")

            intent.putExtra("provider", "GPS_PROVIDER")

            intent.putExtra("status", status)
            if (location!=null) {
                intent.putExtra("lat", location.position.latitude)
                intent.putExtra("lon", location.position.longitude)
                intent.putExtra("accuracy", location.accuracy)
                intent.putExtra("speed", location.speed)
            }


            // Получим разницу времени старта слежения и текущего времении
            // Данные сохраняем в БД раз в минуту, Mapkit делает это непонятно как
            val currentTime=Date().time
            val diffTimeMinute=getDifferenceTime(
                (ctx as MapkitLocationService).startTimeService,
                currentTime
            )
            Timber.d("diffTimeMinute=$diffTimeMinute")
            if (diffTimeMinute>= INTERVAL_SAVE_LOCATION) {
                Timber.d("MLS_sendIntent_${location?.position?.latitude}__${location?.position?.longitude}__${location?.accuracy}__${location?.speed}")
                OtherUtil(Toaster(ctx)).writeToFile("Logger_Location_Parameter_${location?.position?.latitude}__${location?.position?.longitude}__${location?.accuracy}__${location?.speed}")
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
                ctx.startTimeService=currentTime
            }


        }

        fun getDifferenceTime(startTime: Long, endTime: Long): Long {
            val seconds=(endTime-startTime)/1000
            return seconds/60
        }


    }
}