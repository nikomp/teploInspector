package ru.bingosoft.teploInspector.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.FIREBASE_MESSAGE
import timber.log.Timber

//см подробности тут https://stackoverflow.com/questions/51123197/firebaseinstanceidservice-is-deprecated
class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("${remoteMessage.priority}_${remoteMessage.notification}")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val NOTIFICATION_CHANNEL_ID = "TeploInspectorFCMChennel"

        //val sound = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.mute}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "FCM уведомления",
                NotificationManager.IMPORTANCE_HIGH
            )

            /*val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()*/

            notificationChannel.description = "Уведомления для пробуждения телефона"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            //notificationChannel.setSound(sound, audioAttributes)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            channel.canBypassDnd()
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)

        notificationBuilder
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(this, R.color.colorAccent))
            .setPriority(PRIORITY_MAX)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(remoteMessage.notification!!.body)
            //.setDefaults(Notification.DEFAULT_ALL)
            //.setSound(sound)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_clock)

        notificationManager.notify(1002, notificationBuilder.build())
    }

    override fun onNewToken(p0: String) {
        //super.onNewToken(p0)
        Timber.d("MyFirebaseMessagingService $p0")
        getSharedPreferences("AppSettings", MODE_PRIVATE).edit().putString(FIREBASE_MESSAGE, p0).apply()
    }



}