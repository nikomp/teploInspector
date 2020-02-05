package ru.bingosoft.mapquestapp2.ui.map

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.mapquestapp2.db.AppDatabase
import timber.log.Timber

class MapPresenter (val db: AppDatabase)  {
    var view: MapContractView? = null

    var disposable: Disposable? = null

    fun attachView(view: MapContractView) {
        this.view=view
    }

    fun viewIsReady() {
        disposable= db.ordersDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Данные получили")
                Timber.d(it.toString())
                view?.showMarkers(it)
            }

    }
}