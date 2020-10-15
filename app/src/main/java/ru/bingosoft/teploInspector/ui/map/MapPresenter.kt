package ru.bingosoft.teploInspector.ui.map

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.teploInspector.db.AppDatabase
import timber.log.Timber

class MapPresenter (val db: AppDatabase)  {
    var view: MapContractView? = null

    private lateinit var disposable: Disposable

    fun attachView(view: MapContractView) {
        this.view=view
    }

    fun loadMarkers() {
        disposable= db.ordersDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("Данные получили")
                Timber.d(it.toString())
                view?.showMarkers(it)
                disposable.dispose()
            },{throwable ->
                Timber.d("loadMarkers_Error")
                throwable.printStackTrace()
            })

    }

    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }
}