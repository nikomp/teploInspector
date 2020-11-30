package ru.bingosoft.teploInspector.wsnotification

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const.WebSocketConst.NORMAL_CLOSURE_STATUS
import ru.bingosoft.teploInspector.util.Const.WebSocketConst.NOTIFICATION_CHANNEL_ID
import timber.log.Timber

/**
 * Слушаем WebSocket, выполняем действия в UI потоке
 */
class EchoWebSocketListener (private var ctx: Context) :WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Timber.d("WS onOpen")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Timber.d("onMessage")

        val notification= Gson().fromJson(text, Models.Notification::class.java)
        Timber.d("notification=$notification")
        createNotofication(notification)

    }

    private fun createNotofication(notification: Models.Notification) {
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Your Notifications", NotificationManager.IMPORTANCE_HIGH)

            notificationChannel.description = "Description"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            channel.canBypassDnd()
        }

        val notificationBuilder = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)

        val resultIntent= Intent(ctx, MainActivity::class.java).putExtra("messageId",notification.id)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(ctx).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        notificationBuilder.setAutoCancel(true)
            .setColor(ContextCompat.getColor(ctx, R.color.colorAccent))
            .setContentTitle(notification.title)
            .setContentText(notification.content)
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentIntent(resultPendingIntent)
            .setAutoCancel(true)


        notificationManager.notify(1000, notificationBuilder.build())
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
        Timber.d("Error : " + t.message)
    }



}