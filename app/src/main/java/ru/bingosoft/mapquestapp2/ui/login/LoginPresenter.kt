package ru.bingosoft.mapquestapp2.ui.login

import android.util.Log
import io.reactivex.Completable
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

    private lateinit var disposable: Disposable

    fun attachView(view: LoginContractView) {
        this.view = view
    }

    fun authorization(stLogin: String?, stPassword: String?){


        val fingerprint: String = random()

        if (stLogin!=null && stPassword!=null) {
            disposable=apiService.getAuthorization(fingerprint,stLogin,stPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d(LOGTAG, "Авторизация пройдена")
                    this.stLogin=stLogin
                    this.stPassword=stPassword

                    val v=view
                    if (v!=null) {
                        v.alertRepeatSync()
                    }

                },  {
                    Timber.d("Ошибка сети!!")
                    view?.showFailureTextView()
                })
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
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }

    fun syncDB() {
        Timber.d("syncDB")
        disposable = syncOrder()
            .andThen(syncCheckupGuide())
            .andThen(apiService.getCheckups(action="getCheckups"))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ checkups ->
                Timber.d("Получили обследования")
                Timber.d(checkups.toString())

                val data: Models.CheckupList = checkups
                Single.fromCallable{
                    data.checkups.forEach{
                        db.checkupDao().insert(it)
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{ _ ->
                    Timber.d("Сохранили обследования в БД")
                    view?.saveDateSyncToSharedPreference(Calendar.getInstance().time)
                }

            },{throwable ->

                throwable.printStackTrace()

            })

    }

    private fun syncOrder() :Completable =apiService.getListOrder(action="getAllOrders")
        .subscribeOn(Schedulers.io())
        //.observeOn(AndroidSchedulers.mainThread())
        .map{
            Timber.d("Получили заявки")
            Timber.d(it.toString())

            val data: Models.OrderList = it
            Single.fromCallable{
                data.orders.forEach{
                    db.ordersDao().insert(it)
                }

            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{_->
                view?.showMessageLogin(R.string.auth_ok)
                view?.saveLoginPasswordToSharedPreference(stLogin,stPassword)
            }

        }
        .ignoreElement()


    private fun syncCheckupGuide() :Completable =apiService.getCheckupGuide(action="getCheckupGuide")
        .subscribeOn(Schedulers.io())
        //.observeOn(AndroidSchedulers.mainThread())
        .map{
            Timber.d("Получили справочник чеклистов")
            Timber.d(it.toString())

            val data: Models.CheckupGuideList = it
            Single.fromCallable{
                data.guides.forEach{
                    db.checkupGuideDao().insert(it)
                }

            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{_->
                Timber.d("Сохранили справочник чеклистов в БД")
            }

        }
        .ignoreElement()

}