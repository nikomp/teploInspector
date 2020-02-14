package ru.bingosoft.mapquestapp2.ui.mainactivity

import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.api.ApiService
import ru.bingosoft.mapquestapp2.db.AppDatabase
import timber.log.Timber
import javax.inject.Inject

class MainActivityPresenter @Inject constructor(val db: AppDatabase) {
    var view: MainActivityContractView? = null

    @Inject
    lateinit var apiService: ApiService

    private lateinit var disposable: Disposable

    fun attachView(view: MainActivityContractView) {
        this.view=view
    }

    fun sendData() {

        disposable=db.checkupDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Обследования получили из БД")
                Timber.d(it.toString())

                val actionBody = "reverseSync".toRequestBody("multipart/form-data".toMediaType())
                val jsonBody= Gson().toJson(it).toRequestBody("application/json; charset=utf-8".toMediaType())

                Timber.d(jsonBody.toString())

                disposable=apiService.doReverseSync(actionBody, jsonBody)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ _ ->
                        view?.showMainActivityMsg(R.string.msgDataSendOk)
                    },  { _ ->
                        view?.showMainActivityMsg(R.string.msgDataSendError)
                    })
            }

    }
}