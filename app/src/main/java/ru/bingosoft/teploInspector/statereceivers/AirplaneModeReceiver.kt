package ru.bingosoft.teploInspector.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import timber.log.Timber


class AirplaneModeReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("AirplaneMode_onReceive")
        if( Intent.ACTION_AIRPLANE_MODE_CHANGED == intent?.action) {

            val activity=(context as MainActivity)
            activity.otherUtil.writeToFile("Logger_AirplaneMode_Changed_onReceive")
            if (activity.isAirplaneModeOn(context)) {
                activity.otherUtil.writeToFile("Logger_AirplaneMode_ON")
            } else {
                activity.otherUtil.writeToFile("Logger_AirplaneMode_OFF")
            }

            //activity.finish()
        }
    }
}