package ru.bingosoft.teploInspector.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.FIREBASE_MESSAGE
import timber.log.Timber

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("${remoteMessage.priority}_${remoteMessage.notification}")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val NOTIFICATION_CHANNEL_ID = "TeploInspectorFCM"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "FCM уведомление", NotificationManager.IMPORTANCE_HIGH)

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

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)

        val resultIntent=Intent(this, MainActivity::class.java)
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(resultIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        notificationBuilder.setAutoCancel(true)
            .setColor(ContextCompat.getColor(this, R.color.colorAccent))
            .setPriority(PRIORITY_MAX)
            .setContentTitle(getString(R.string.app_name))
            //.setContentText(remoteMessage.data["body"])
            .setContentText(remoteMessage.notification!!.body)
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_clock)
            //.setContentIntent(resultPendingIntent)
            .setAutoCancel(true)


        notificationManager.notify(1002, notificationBuilder.build())
    }

    override fun onNewToken(p0: String) {
        //super.onNewToken(p0)
        Timber.d("MyFirebaseMessagingService $p0")
        getSharedPreferences("AppSettings",MODE_PRIVATE).edit().putString(FIREBASE_MESSAGE,p0).apply()
    }


}