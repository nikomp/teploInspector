package ru.bingosoft.teploInspector.ui.map

import io.reactivex.disposables.Disposable
import ru.bingosoft.teploInspector.db.AppDatabase

class MapPresenter (val db: AppDatabase)  {
    var view: MapContractView? = null

    private lateinit var disposable: Disposable

    fun attachView(view: MapContractView) {
        this.view=view
    }

    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }
}