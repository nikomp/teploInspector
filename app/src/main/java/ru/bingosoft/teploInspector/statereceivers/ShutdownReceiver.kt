package ru.bingosoft.teploInspector.statereceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import timber.log.Timber

/*Для Android Q нужно регистрировать сервис программно, см. MainActivity.registerReceiver()*/
class ShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("ShutdownReceiver_onReceive")
        if( Intent.ACTION_SHUTDOWN == intent?.action) { //"android.intent.action.ACTION_SHUTDOWN"
            val activity=(context as MainActivity)
            activity.otherUtil.writeToFile("Logger_ShutdownReceiver_onReceive")
            activity.finish()
        }
    }
}