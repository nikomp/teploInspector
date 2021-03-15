package ru.bingosoft.teploInspector.util

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.APP_PREFERENCES
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.LOGIN
import timber.log.Timber
import java.util.*

//#Автовыход #WorkManager
class FinishAppWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    val ctx=appContext

    override fun doWork(): Result {
        Timber.d("FINISH_FROM_WORKER")
        //OtherUtil().writeToFile("Logger_FINISH_FROM_WORKER")

        val sp=ctx.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        val login=sp.getString(LOGIN, "") ?: ""
        Timber.d("ZZZZ_$login")

        if (login!="") {
            OtherUtil().writeToFile("Logger_FINISH_FROM_WORKER_${Date()}")
            val intent = Intent(ctx, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("EXIT", true)
            ctx.startActivity(intent)
        }


        return Result.success()
    }

}