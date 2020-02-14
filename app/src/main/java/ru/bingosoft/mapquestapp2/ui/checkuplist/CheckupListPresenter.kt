package ru.bingosoft.mapquestapp2.ui.checkuplist

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.mapquestapp2.db.AppDatabase
import timber.log.Timber
import javax.inject.Inject

class CheckupListPresenter @Inject constructor(val db: AppDatabase) {
    var view: CheckupListContractView? = null

    private lateinit var disposable: Disposable

    fun attachView(view: CheckupListContractView) {
        this.view=view
    }

    fun loadCheckupList() {
        disposable=db.checkupDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Обследования получили из БД")
                Timber.d(it.toString())

                view?.showCheckups(it)
            }

    }

    fun loadCheckupListByOrder(idOrder: Long) {
        disposable=db.checkupDao().getCheckupsOrder(idOrder)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                view?.showCheckups(it)
            }
    }

    fun onDestroy() {
        this.view = null
        disposable.dispose()
    }
}