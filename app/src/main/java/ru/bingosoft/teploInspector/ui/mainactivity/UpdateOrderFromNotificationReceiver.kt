package ru.bingosoft.teploInspector.ui.mainactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.reactivex.disposables.Disposable
import ru.bingosoft.teploInspector.db.AppDatabase
import timber.log.Timber
import javax.inject.Inject

class UpdateOrderFromNotificationReceiver @Inject constructor(
    //private val mainActivityPresenter: MainActivityPresenter
    private val db: AppDatabase
): BroadcastReceiver() {
    private lateinit var disposableUpdateDateVisit: Disposable
    var activity: MainActivity?=null

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d( "UpdateOrderFromNotificationReceiver_onReceive")
        if("updateFromNotification" == intent?.action) {
            Timber.d("intent=$intent")
            val idOrder = intent.getLongExtra("idOrder",0L)
            val newDate = intent.getStringExtra("dateVisit")
            if (idOrder!=0L && newDate!="") {
                //updateDateVisit(idOrder,newDate!!)
                (activity as MainActivity).mainPresenter.updateDateVisit(idOrder,newDate!!)
            }
        }
    }

    fun setMainActivity(activity: MainActivity) {
        this.activity=activity
    }

    /*private fun updateDateVisit(idOrder: Long, newDateVisit: String) {
        disposableUpdateDateVisit= Single.fromCallable {
            db.ordersDao().updateDateVisit(idOrder = idOrder, newDate = newDateVisit)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                disposableUpdateDateVisit.dispose()
                activity?.refreshOrderListFromMA()
                Timber.d("Обновили_дату_визита")
            },{throwable ->
                disposableUpdateDateVisit.dispose()
                throwable.printStackTrace()
            })
    }*/
}