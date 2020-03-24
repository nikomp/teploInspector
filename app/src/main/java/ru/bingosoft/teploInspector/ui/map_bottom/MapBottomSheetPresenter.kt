package ru.bingosoft.teploInspector.ui.map_bottom

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.teploInspector.db.AppDatabase
import timber.log.Timber
import javax.inject.Inject

class MapBottomSheetPresenter @Inject constructor(
    val db: AppDatabase
) {

    var view: MapBottomSheetContractView?=null
    private lateinit var disposable: Disposable

    fun attachView(view: MapBottomSheetContractView) {
        this.view=view
    }

    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }

    }

    /*fun loadData(symbolNumber: String) {
        Timber.d("loadData $symbolNumber")
        disposable=db.ordersDao().getByNumber(symbolNumber)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Данные получили из БД")
                Timber.d(it.toString())
                view?.showOrder(it)
            }
    }*/
}