package ru.bingosoft.mapquestapp2.ui.checkup

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.mapquestapp2.db.AppDatabase
import timber.log.Timber
import javax.inject.Inject

class CheckupPresenter @Inject constructor(val db: AppDatabase) {
    var view: CheckupContractView? = null

    lateinit var disposable: Disposable

    fun attachView(view: CheckupContractView) {
        this.view=view
    }

    fun loadCheckup(id: Long) {
        Timber.d("loadCheckups")
        disposable=db.checkupDao().getById(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Обследования получили из БД")
                Timber.d(it.toString())

                view?.dataIsLoaded(it)
            }

        Timber.d("ОК")

    }
}