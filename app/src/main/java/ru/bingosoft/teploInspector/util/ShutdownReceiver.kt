package ru.bingosoft.teploInspector.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import timber.log.Timber
import java.util.*

/*Для Android Q нужно регистрировать сервис программно, см. MainActivity.registerReceiver()*/
class ShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("ShutdownReceiver_onReceive")
        if("android.intent.action.ACTION_SHUTDOWN" == intent?.action) {
            OtherUtil().writeToFile("Logger_Выключение устройства ${Date()}")
            val activity=(context as MainActivity)
            activity.finish()
        }
    }
}