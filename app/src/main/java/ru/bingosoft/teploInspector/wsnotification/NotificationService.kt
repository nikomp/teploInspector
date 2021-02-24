package ru.bingosoft.teploInspector.wsnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.util.Const.WebSocketConst.NOTIFICATION_CHANNEL_ID_SERVICES
import ru.bingosoft.teploInspector.util.Const.WebSocketConst.NOTIFICATION_SERVICE_NOTIFICATION_ID
import timber.log.Timber


// Сервис, который следит за поступлением новых сообщений в фоне
class NotificationService : Service() {
    var wsService: WebSocket? = null

    //#Загрузка_сервиса_в_фоне
    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID_SERVICES,
                "Сервис уведомлений",
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            channel.enableVibration(true)
            notificationManager.createNotificationChannel(channel)


            notificationManager.createNotificationChannel(channel)
            val notification: Notification = Notification.Builder(
                this,
                NOTIFICATION_CHANNEL_ID_SERVICES
            )
                .setContentTitle(getText(R.string.notification_service_title))
                .setContentText(getText(R.string.notification_service_content))
                .setSmallIcon(R.drawable.ic_service_notification)
                .build()


            startForeground(NOTIFICATION_SERVICE_NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("СЕРВИС_Уведомлений_СТАРТОВАЛ")

        //Получим токен пользователя
        val token=intent?.getStringExtra("Token")
        if (token.isNullOrEmpty()) {
            Timber.d("ТокенПустой")
        } else {
            val client = OkHttpClient()
            // Создаем WebSocket, который работает в фоне
            val webSocketUrl=BuildConfig.url_socket
            Timber.d("WebSockect $webSocketUrl")
            val request =
                Request.Builder()
                    .url(webSocketUrl)
                    .addHeader("Sec-Websocket-Protocol", token)
                    .build()
            val listener = EchoWebSocketListener(this)
            wsService = client.newWebSocket(request, listener)
            client.dispatcher.executorService.shutdown()

        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("СЕРВИС_ОСТАНОВЛЕН")

    }

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }
}


