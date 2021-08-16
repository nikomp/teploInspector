package ru.bingosoft.teploInspector.statereceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import java.util.*

class DozeReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if( ACTION_DEVICE_IDLE_MODE_CHANGED == intent?.action) {

            val activity=(context as MainActivity)

            val powerManager=activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isInDozeMode=Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && powerManager.isDeviceIdleMode

            activity.otherUtil.writeToFile("Logger_DOZE_${isInDozeMode}_${Date()}")

        }
    }
}