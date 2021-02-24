package ru.bingosoft.teploInspector.ui.login

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.Const
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.ThrowHelper
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import java.lang.reflect.Type
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class LoginPresenter @Inject constructor(
    private val apiService: ApiService,
    private val db: AppDatabase,
    private val sharedPrefSaver: SharedPrefSaver,
    val toaster: Toaster
) {
    var view: LoginContractView? = null
    private var stLogin: String = ""
    private var stPassword: String = ""

    private lateinit var disposable: Disposable
    private lateinit var disposableFCM: Disposable
    private lateinit var disposableRouteInterval: Disposable

    fun attachView(view: LoginContractView) {
        this.view = view
    }

    fun authorization(url: String, stLogin: String?, stPassword: String?){
        Timber.d("authorization_LoginPresenter $stLogin _ $stPassword")
        if (stLogin!=null && stPassword!=null) {

            Timber.d("jsonBody=${Gson().toJson(Models.LP(login = stLogin, password = stPassword))}")


            val jsonBody = Gson().toJson(Models.LP(login = stLogin, password = stPassword))
                .toRequestBody("application/json".toMediaType())

            disposable = apiService.getAuthentication(url, jsonBody)
                .subscribeOn(Schedulers.io())
                .flatMap { uuid ->
                    Timber.d("uuid=$uuid")
                    Timber.d("jsonBody2=${Gson().toJson(uuid)}")
                    val jsonBody2 = Gson().toJson(uuid)
                        .toRequestBody("application/json".toMediaType())
                    apiService.getAuthorization(jsonBody2)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { token ->
                        sendRoute()
                        this.stLogin = stLogin
                        this.stPassword = stPassword
                        view?.saveLoginPasswordToSharedPreference(stLogin, stPassword)
                        view?.registerReceiverMainActivity()
                        view?.saveToken(token.token)
                        view?.saveInfoUserToSharedPreference(Models.User(fullname = token.name))
                        view?.startNotificationService(token.token)
                        view?.checkMessageId() // Уведомление прочитано
                        Timber.d("LoginPresenter_getAllMessage")
                        view?.getAllMessage()
                        view?.sendMessageUserLogged()

                        val v = view
                        if (v != null) {
                            Timber.d("startService_LoginPresenter")
                            v.alertRepeatSync()
                        }

                        saveTokenGCM()
                        disposable.dispose()

                    }, { throwable ->
                        throwable.printStackTrace()
                        view?.showAlertNotInternet()
                        disposable.dispose()
                    }
                )

        }

    }

    private fun sendRoute() {
        Timber.d("test_sendRoute_LoginPresenter")
        disposableRouteInterval= Flowable.interval(
            Const.LocationStatus.INTERVAL_SENDING_ROUTE,
            TimeUnit.MINUTES
        ).map {
            Timber.d("ДанныеМаршрутаПолучили_LP_${Date()}")
            db.trackingUserDao().getTrackingForCurrentDay()
        }
            .subscribe(
                {trackingUserLocation ->
                    if (trackingUserLocation.isNotEmpty()) {
                        Timber.d("ОтправляемМаршрут")
                        val route=Models.FileRoute()
                        val jsonStr=Gson().toJson(trackingUserLocation)
                        route.fileRoute=jsonStr

                        val jsonBody=Gson().toJson(route)
                            .toRequestBody("application/json; charset=utf-8".toMediaType())

                        apiService.sendTrackingUserLocation(jsonBody).subscribe(
                            {Timber.d("ОтправилиМаршрут")},
                            {throwable ->
                                throwable.printStackTrace()
                                if (view!=null) {
                                    view?.errorReceived(throwable)
                                } else {
                                    errorHandler(throwable)
                                }
                            }
                        )
                    } else {
                        Timber.d("Нет данных о маршруте")
                    }
                },{throwable ->
                    throwable.printStackTrace()
                    if (view!=null) {
                        view?.errorReceived(throwable)
                    } else {
                        errorHandler(throwable)
                    }
                }
            )
    }



    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
        if (this::disposableRouteInterval.isInitialized) {
            Timber.d("disposableRouteInterval_destroy")
            disposableRouteInterval.dispose()
        }
    }

    fun syncDB() {
        Timber.d("syncDB_x")
        disposable = syncOrder()
            .subscribeOn(Schedulers.io())
            .subscribe({},{
                it.printStackTrace()
                //view?.showFailureTextView()
            })

    }

    private fun syncOrder() :Completable=apiService.getListOrder()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map{orders ->
            if (orders.isEmpty()) {
                throw ThrowHelper("Нет заявок")
            } else {
                Timber.d("ordersXXX=$orders")
                Single.fromCallable{
                    //db.ordersDao().clearOrders() // Перед вставкой очистим таблицу
                    // Получим id всех присланных заявок
                    val idsList= mutableListOf<String>()
                    orders.forEach{
                        idsList.add(it.id.toString())
                    }
                    Timber.d("idsList=$idsList")
                    db.ordersDao().deleteOrders(idsList)

                    db.historyOrderStateDao().clearHistory() // Очистим таблицу с историей смены статуса заявок
                    orders.forEach{
                        db.ordersDao().insert(it)
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                },{throwable ->
                    if (view!=null) {
                        view?.errorReceived(throwable)
                    } else {
                        errorHandler(throwable)
                    }

                    throwable.printStackTrace()
                })
            }
        }
        .doOnError { throwable ->
            Timber.d("CXCX_$view")
            if (view!=null) {
                view?.showFailureTextView("Нет заявок")
                view?.errorReceived(throwable)
            } else {
                errorHandler(throwable)
            }
            throwable.printStackTrace()}
        .ignoreElement()
        .andThen(syncTechParams(sharedPrefSaver.getUserId()))


    private fun syncTechParams(userId:Int) :Completable=apiService.getTechParams(user=userId)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map{ response ->
            Timber.d("Получили тех. характеристики всех заявок")
            Timber.d(response.toString())

            Single.fromCallable{
                db.techParamsDao().clearTechParams() // Перед вставкой очистим таблицу
                response.forEach{ techParams ->
                    db.techParamsDao().insert(techParams)
                    // Если ставить ограничение TechParams.value not NULL, то лучше инсертить данные так
                    /*try {
                        db.techParamsDao().insert(techParams)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }*/
                }

            }
                .subscribeOn(Schedulers.io())
                .subscribe ({
                    Timber.d("Сохранили тех характеристики в БД")
                    //#Группировка_List
                    val groupTechParams=response.groupBy { it.idOrder }
                    groupTechParams.forEach{
                        db.ordersDao().updateTechParamsCount(it.key,it.value.size)
                    }
                },{throwable ->
                    throwable.printStackTrace()
                })

        }
        .doOnError { throwable ->
            view?.errorReceived(throwable)
            throwable.printStackTrace()
        }
        .ignoreElement()
        .andThen(syncCheckups())


    private fun syncCheckups() :Completable=apiService.getCheckups()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map{checkups ->
            Timber.d("Получили обследования")
            Timber.d("checkups=$checkups")
            if (checkups.isNullOrEmpty()) {
                throw ThrowHelper("Нет обследований")
            } else {
                Timber.d("Обследования есть")
                //val data: Models.CheckupList = checkups

                disposable=Single.fromCallable {
                    //db.checkupDao().clearCheckup() // Перед вставкой очистим таблицу
                    checkups.forEach {
                        Timber.d("VVV_it=$it")
                        db.checkupDao().insert(it)

                        // Обновим число вопросов для заявки
                        val listType: Type = object : TypeToken<List<Models.TemplateControl?>?>() {}.type
                        val controlList: List<Models.TemplateControl> =Gson().fromJson(it.text, listType)
                        Timber.d("updateQuestionCount_${it.idOrder}_${controlList.size}")
                        db.ordersDao().updateQuestionCount(it.idOrder,controlList.size)

                    }

                }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe ({
                        disposable.dispose()
                        Timber.d("Сохранили обследования в БД")
                        view?.showOrders()

                    }, {error ->
                        error.printStackTrace()
                        disposable.dispose()
                    })

                Timber.d("View123=$view")
                if (view!=null) {
                    Timber.d("ZXCXC")
                    view?.saveDateSyncToSharedPreference(Calendar.getInstance().time)
                    view?.showMessageLogin(R.string.order_refresh)
                }
            }
        }
        .doOnError { throwable ->
            Timber.d("throwable syncCheckups")
            throwable.printStackTrace()
            if (view!=null) {
                view?.errorReceived(throwable)
            } else {
                errorHandler(throwable)
            }
        }
        .ignoreElement()

    private fun errorHandler(throwable: Throwable) {
        Timber.d("errorHandler")
        when (throwable) {
            is HttpException -> {
                Timber.d("throwable.code()=${throwable.code()}")
                when (throwable.code()) {
                    401 -> {
                        //toaster.showToast(R.string.unauthorized)
                        if (sharedPrefSaver.getLogin().isNotEmpty() && sharedPrefSaver.getPassword().isNotEmpty()) {

                            val login = this.sharedPrefSaver.getLogin()
                            val password = this.sharedPrefSaver.getPassword()

                            val url = if (BuildConfig.BUILD_TYPE=="presentation") {
                                // Для презентации
                                if (this.sharedPrefSaver.getEnterType()=="directory_service") {
                                    "http://teplomi.bingosoft-office.ru/ldapauthentication/auth/login"
                                } else {
                                    "http://teplomi.bingosoft-office.ru/defaultauthentication/auth/login"
                                }
                            } else {
                                if (this.sharedPrefSaver.getEnterType()=="directory_service") {
                                    "https://mi.teploenergo-nn.ru/ldapauthentication/auth/login"
                                } else {
                                    "https://mi.teploenergo-nn.ru/defaultauthentication/auth/login"
                                }
                            }


                            authorization(url, login, password) // Проверим есть ли авторизация
                        }
                    }
                    else -> {
                        toaster.showErrorToast("Ошибка! ${throwable.message}")
                    }
                }
            }
            is UnknownHostException ->{
                toaster.showErrorToast(R.string.no_address_hostname)
            }
            else -> {
                toaster.showErrorToast("Ошибка! ${throwable.message}")
            }
        }

    }

    private fun saveTokenGCM() {
        Timber.d("saveTokenGCM")

        val fcmToken=Models.FCMToken(
            token =sharedPrefSaver.getTokenGCM()
        )

        Timber.d("Данные4=${Gson().toJson(fcmToken)}")

        val jsonBody = Gson().toJson(fcmToken)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        disposableFCM=apiService.saveGCMToken(jsonBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({response ->
                Timber.d(response.toString())
                disposableFCM.dispose()
            },{ throwable ->
                Timber.d("ошибка!!!")
                throwable.printStackTrace()
                view?.errorReceived(throwable)
                disposableFCM.dispose()
            })

    }

}