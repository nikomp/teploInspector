package ru.bingosoft.mapquestapp2.ui.login

import android.util.Log
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.api.ApiService
import ru.bingosoft.mapquestapp2.db.AppDatabase
import ru.bingosoft.mapquestapp2.models.Models
import ru.bingosoft.mapquestapp2.util.Const.LogTags.LOGTAG
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class LoginPresenter @Inject constructor(
    private val apiService: ApiService,
    private val db: AppDatabase

) {
    var view: LoginContractView? = null
    private var stLogin: String = ""
    private var stPassword: String = ""

    private var disposable: Disposable? = null


    fun attachView(view: LoginContractView) {
        this.view = view
    }

    fun authorization(stLogin: String, stPassword: String){

        val fingerprint: String = random()

        /*return apiService.getAuthorization(fingerprint,stLogin,stPassword)
            .map { response ->
                Log.d(LOGTAG, response.newToken)
                Log.d(LOGTAG, response.session_id)

                val auth=Models.Auth(response.success,response.newToken,response.session_id)

                sessionId=response.session_id

                return@map auth

            }*/

        disposable=apiService.getAuthorization(fingerprint,stLogin,stPassword)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{ response ->
                Log.d(LOGTAG, "Авторизация пройдена")
                this.stLogin=stLogin
                this.stPassword=stPassword
                syncDB()
            }

    }

    /**
     * Метод для генерации ГУИДа, нужен для первичного формирования fingerprint
     *
     * @return - возвращается строка содержащая ГУИД
     */
    private fun random(): String {
        var stF = UUID.randomUUID().toString()
        stF = stF.replace("-".toRegex(), "")
        stF = stF.substring(0, 32)
        Log.d(LOGTAG, "random()=$stF")

        return stF
    }

    fun onDestroy() {
        this.view = null
    }

    fun syncDB() {
        disposable=apiService.getListOrder(action="getAllOrders")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                Timber.d("Получаем данные с сервера")
                Timber.d(response.toString())

                val data: Models.OrderList = response
                Single.fromCallable{
                    data.orders.forEach{
                        db.ordersDao()?.insert(it)
                    }

                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe{ response ->
                        view?.showMessageLogin(R.string.auth_ok)
                        view?.saveLoginPasswordToSharedPreference(stLogin,stPassword)
                    }


            },{throwable ->

                Timber.d(throwable.message)

            })

    }
}