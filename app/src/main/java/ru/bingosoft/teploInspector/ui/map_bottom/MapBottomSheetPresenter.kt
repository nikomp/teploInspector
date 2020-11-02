package ru.bingosoft.teploInspector.ui.map_bottom

import io.reactivex.disposables.Disposable
import ru.bingosoft.teploInspector.db.AppDatabase
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

}