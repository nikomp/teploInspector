package ru.bingosoft.teploInspector.wsnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const.WebSocketConst.NORMAL_CLOSURE_STATUS
import ru.bingosoft.teploInspector.util.Const.WebSocketConst.NOTIFICATION_CHANGE_DATE
import ru.bingosoft.teploInspector.util.Const.WebSocketConst.NOTIFICATION_CHANNEL_ID
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


/**
 * Слушаем WebSocket, выполняем действия в UI потоке
 */
class EchoWebSocketListener(private var ctx: Context) :WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Timber.d("WS onOpen")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Timber.d("onMessage")

        val notification= Gson().fromJson(text, Models.Notification::class.java)
        Timber.d("notification=$notification")
        createNotification(notification)

    }

    private fun createNotification(notification: Models.Notification) {
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Уведомления о получении заявок",
                NotificationManager.IMPORTANCE_HIGH
            )

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),audioAttributes)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            channel.canBypassDnd()
        }

        val notificationBuilder = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)

        val resultIntent= Intent(ctx, MainActivity::class.java).putExtra(
            "messageId",
            notification.id
        )

        //подробнее тут https://stackoverflow.com/questions/28258404/singletask-and-singleinstance-not-respected-when-using-pendingintent
        //+ переустановка приложения на эмуляторе
        val resultPendingIntent = PendingIntent.getActivity(ctx, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val customNotification=notificationBuilder.setAutoCancel(true)
            .setContentTitle(notification.title)
            .setContentText(notification.content)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_new_notification)
            .setContentIntent(resultPendingIntent)
            .build()

        notificationManager.notify(1000, customNotification)
        checkNotification(notification)
    }

    private fun checkNotification(notification: Models.Notification) {
        if (notification.title==NOTIFICATION_CHANGE_DATE) {
            /*val newDate=getDateFromContent(notification.content)
            if (newDate!=null) {
                Timber.d("обновим дату визиту")
                val dateVisit=SimpleDateFormat("yyyy-MM-dd", Locale("ru","RU")).format(newDate)
                val intent=Intent("updateFromNotification")

                intent.putExtra("idOrder", 50368L)
                intent.putExtra("dateVisit", dateVisit)
                LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent)
            }*/
        }
    }

    private fun getDateFromContent(content: String) : Date? {
        val newDatePosition=content.indexOf("Новая согласованная дата:")+26
        val newDateStr=content.substring(newDatePosition,newDatePosition+10)
        Timber.d("newDateStr=$newDateStr")
        var date: Date?=null
        try {
            date= SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).parse(newDateStr)
        } catch (e: Exception) {
            e.printStackTrace()

        }
        return date
    }

    override fun onClosing(
        webSocket: WebSocket,
        code: Int,
        reason: String
    ) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        Timber.d("Closing : $code / $reason")
    }

    override fun onFailure(
        webSocket: WebSocket,
        t: Throwable,
        response: Response?
    ) {
        Timber.d(t)
    }



}