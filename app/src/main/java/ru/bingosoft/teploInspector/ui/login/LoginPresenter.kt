package ru.bingosoft.teploInspector.ui.login

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.Const.LogTags.LOGTAG
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.ThrowHelper
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class LoginPresenter @Inject constructor(
    private val apiService: ApiService,
    private val db: AppDatabase,
    private val sharedPrefSaver: SharedPrefSaver

) {
    var view: LoginContractView? = null
    private var stLogin: String = ""
    private var stPassword: String = ""

    private lateinit var disposable: Disposable

    fun attachView(view: LoginContractView) {
        this.view = view
    }

    fun authorization(stLogin: String?, stPassword: String?){
        Timber.d("authorization1 $stLogin _ $stPassword")
        val fingerprint: String = random()

        if (stLogin!=null && stPassword!=null) {
            disposable=apiService.getAuthorization(fingerprint,stLogin,stPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.d("Авторизовались")
                    this.stLogin=stLogin
                    this.stPassword=stPassword

                    val v=view
                    if (v!=null) {
                        Timber.d("startService_LoginPresenter")
                        v.startLocationService()
                        v.alertRepeatSync()
                    }

                    getInfoCurrentUser()
                    saveTokenGCM()


                },  {
                    Timber.d("Ошибка сети!!")
                    view?.showFailureTextView()
                })
        }

    }

    private fun getInfoCurrentUser()  {
        Timber.d("getInfoCurrentUser")
        disposable=apiService.getInfoAboutCurrentUser(action="getUserInfo")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({
                Timber.d("Получили информацию о пользователе")
                Timber.d(it.fullname)
                // сохраним данные в SharedPreference
                view?.saveInfoUserToSharedPreference(it)

            },{
                it.printStackTrace()
            })

    }

    private fun saveTokenGCM() {
        Timber.d("saveTokenGCM")

        disposable=apiService.saveGCMToken(action="saveGCMToken",token = sharedPrefSaver.getTokenGCM())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({
                Timber.d(it.msg)

            },{
                it.printStackTrace()
            })
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

        sharedPrefSaver.clearAuthData() // Очистим информацию об авторизации
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
                Timber.d("checkups.success=${checkups.success}")

                if (!checkups.success) {
                    throw ThrowHelper("Нет обследований")
                } else {
                    Timber.d("Обследования есть")
                    val data: Models.CheckupList = checkups
                    Single.fromCallable{
                        db.checkupDao().clearCheckup() // Перед вставкой очистим таблицу
                        data.checkups.forEach{
                            Timber.d(it.toString())
                            db.checkupDao().insert(it)
                        }
                    }
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe()/*{ _ ->
                            Timber.d("Сохранили обследования в БД")

                        }*/

                    view?.saveDateSyncToSharedPreference(Calendar.getInstance().time)
                }

            },{throwable ->
                Timber.d("throwable syncDB")
                throwable.printStackTrace()
                if (throwable is ThrowHelper) {
                    view?.showMessageLogin("${throwable.message}")
                }
            })
    }

    private fun syncOrder() :Completable =apiService.getListOrder(action="getAllOrders")
        .subscribeOn(Schedulers.io())
        //.observeOn(AndroidSchedulers.mainThread())
        .map{ response ->
            if (!response.success) {
                throw ThrowHelper("Нет заявок")
            } else {
                val data: Models.OrderList = response
                Single.fromCallable{
                    db.ordersDao().clearOrders() // Перед вставкой очистим таблицу
                    Timber.d("data.orders=${data.orders}")
                    data.orders.forEach{
                        db.ordersDao().insert(it)
                    }

                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe{_->
                        view?.showMessageLogin(R.string.order_refresh)
                        view?.saveLoginPasswordToSharedPreference(stLogin,stPassword)
                    }
            }


        }
        .ignoreElement()


    private fun syncCheckupGuide() :Completable =apiService.getCheckupGuide(action="getCheckupGuide")
        .subscribeOn(Schedulers.io())
        //.observeOn(AndroidSchedulers.mainThread())
        .map{ response ->
            Timber.d("Получили справочник чеклистов")
            Timber.d(response.toString())

            val data: Models.CheckupGuideList = response
            Single.fromCallable{
                db.checkupGuideDao().clearCheckupGuide() // Перед вставкой очистим таблицу
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