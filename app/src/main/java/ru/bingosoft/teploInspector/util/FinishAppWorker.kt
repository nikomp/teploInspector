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
import javax.inject.Inject

//#Автовыход #WorkManager
// Есть дублирующая автовыход процедура, которая работает через Rx, см. LoginPresenter.setAutoFinish()
class FinishAppWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    val ctx=appContext

    @Inject
    lateinit var otherUtil: OtherUtil

    override fun doWork(): Result {
        Timber.d("FINISH_FROM_WORKER")

        val sp=ctx.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        val login=sp.getString(LOGIN, "") ?: ""

        if (login!="") {
            otherUtil.writeToFile("Logger_FINISH_FROM_WORKER_${Date()}")
            val intent = Intent("EXIT")
            (ctx as MainActivity).checkFinish(intent)
        }

        return Result.success()
    }

}