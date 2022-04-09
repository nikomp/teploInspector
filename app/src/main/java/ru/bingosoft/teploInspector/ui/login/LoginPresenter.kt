package ru.bingosoft.teploInspector.ui.login

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.User.TrackingUserLocation
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.*
import ru.bingosoft.teploInspector.util.Const.FinishTime.FINISH_CHECK_INTERVAL
import timber.log.Timber
import java.io.IOException
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

    //private lateinit var disposable: Disposable
    //private lateinit var disposableFCM: Disposable
    private lateinit var disposableRouteInterval: Disposable
    private lateinit var disposableFinishInterval: Disposable
    //private lateinit var disposableClearOrdersFromDB: Disposable
    //private lateinit var disposableUpdateLocation: Disposable
    private var compositeDisposable= CompositeDisposable()

    @Inject
    lateinit var otherUtil: OtherUtil

    fun attachView(view: LoginContractView) {
        this.view = view
    }

    fun authorization(url: String, stLogin: String?, stPassword: String?){
        Timber.d("authorization_LoginPresenter $url")
        Timber.d("authorization_LoginPresenter $stLogin _ $stPassword")
        otherUtil.writeToFile("Logger_authorization_from_LoginPresenter")
        if (!stLogin.isNullOrEmpty() && !stPassword.isNullOrEmpty()) {

            Timber.d("jsonBody=${Gson().toJson(Models.LP(login = stLogin, password = stPassword))}")


            val jsonBody = Gson().toJson(Models.LP(login = stLogin, password = stPassword))
                .toRequestBody("application/json".toMediaType())

            val disposable = apiService.getAuthentication(url, jsonBody)
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
                        setAutoFinish()
                        this.stLogin = stLogin
                        this.stPassword = stPassword
                        view?.saveLoginPasswordToSharedPreference(stLogin, stPassword)
                        view?.requestGPSPermission()
                        view?.saveToken(token.token)
                        view?.saveAppVersionName()
                        view?.sendMessageUserLogged()
                        view?.startFinishWorker()
                        view?.registerReceiverMainActivity()
                        view?.saveInfoUserToSharedPreference(Models.User(fullname = token.name))
                        view?.startNotificationService(token.token)
                        view?.checkMessageId() // Уведомление прочитано

                        view?.getAllMessage()

                        val v = view
                        if (v != null) {
                            Timber.d("startService_LoginPresenter")
                            v.alertRepeatSync()
                        }

                        saveTokenFCM()

                        //disposable.dispose()

                    }, { throwable ->
                        throwable.printStackTrace()
                        Timber.d("throwable=${throwable.message}")
                        //#throwable #response #error
                        if (throwable is HttpException) {
                            val body= throwable.response()?.errorBody()
                            val gson = Gson()
                            val adapter: TypeAdapter<Models.Error> = gson.getAdapter( Models.Error::class.java)
                            try {
                                val errorBody: Models.Error = adapter.fromJson(body?.string())
                                Timber.d("errorBody_${errorBody.error}")
                                if (errorBody.error=="user_not_found") {
                                    view?.showFailureTextView("Неверный логин")
                                }
                                if (errorBody.error=="user_password_is_invalid") {
                                    view?.showFailureTextView("Неверный пароль")
                                }
                            } catch (e: IOException) {
                                Timber.d("${e.printStackTrace()}")
                                view?.showFailureTextView("Ошибка при авторизации")
                            }
                        } else {
                            view?.showAlertNotInternet()
                        }
                        //disposable.dispose()
                    }
                )
            compositeDisposable.add(disposable)

        } else {
            view?.errorReceived(Throwable("Не заданы логин или пароль"))
            otherUtil.writeToFile("Logger_Не заданы логин или пароль")
        }

    }

    private fun setAutoFinish() {
        println("setAutoFinish")
        // Срабатывает периодически
        Timber.d("setAutoFinish_${Date()}")
        otherUtil.writeToFile("Logger_setAutoFinish_${Date()}")
        disposableFinishInterval=Flowable.interval(
            FINISH_CHECK_INTERVAL,
            TimeUnit.MINUTES,
            Schedulers.computation() // Scheduler добавил для тестирования см. тест LoginPresenterTest.testSetAutoFinish, до этого было пусто
        ).subscribe({
            Timber.d("setAutoFinish_trigger_${Date()}")
            otherUtil.writeToFile("Logger_setAutoFinish_trigger_${Date()}")
            val date=Calendar.getInstance()
            val calendar = Calendar.getInstance()
            calendar.set(date.get(Calendar.YEAR),date.get(Calendar.MONTH),date.get(Calendar.DATE),
                Const.FinishTime.FINISH_HOURS_DOUBLER,
                Const.FinishTime.FINISH_MINUTES_DOUBLER,0)

            if (date.timeInMillis>calendar.timeInMillis) { //System.currentTimeMillis()
                Timber.d("FINISH_APP")
                view?.finishAppDoubler()

            }
        },
        {throwable ->
            throwable.printStackTrace()
        })
    }


    private fun sendRoute() {
        Timber.d("test_sendRoute_LoginPresenter")
        view?.saveRouteIntervalFlag() // Отметим, что передача маршрута включена
        otherUtil.writeToFile("Logger_sendRoute_LoginPresenter_${Date()}")

        disposableRouteInterval= Flowable.interval(
            Const.LocationStatus.INTERVAL_SENDING_ROUTE,
            TimeUnit.MINUTES,
            Schedulers.computation() // Scheduler добавил для тестирования см. тест LoginPresenterTest.testSendRoute, до этого было пусто
        ).map {
            Timber.d("ДанныеМаршрутаПолучили_LP_${Date()}")
            /*val trackingList=db.trackingUserDao().getTrackingForCurrentDay()
            if (trackingList.size>LIMIT_RECORDS) {
                Timber.d("getTrackingForCurrentDay")
                return@map trackingList
            } else {
                Timber.d("getTrackingForLastMinutes")
                return@map db.trackingUserDao().getTrackingForLastMinutes()
            }*/
            db.trackingUserDao().getTrackingForCurrentDay()

        }
            .subscribe(
                {trackingUserLocation ->
                    if (trackingUserLocation.isNotEmpty()) {
                        Timber.d("ОтправляемМаршрут")
                        val route=Models.FileRoute()
                        val gson=GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
                        val jsonStr=gson.toJson(trackingUserLocation)
                        route.fileRoute=jsonStr
                        Timber.d("jsonStr=${route.fileRoute}")

                        val jsonBody=Gson().toJson(route)
                            .toRequestBody("application/json; charset=utf-8".toMediaType())

                        apiService.sendTrackingUserLocation(jsonBody).subscribe(
                            {
                                Timber.d("ОтправилиМаршрут_LoginPresenter")
                                Timber.d("trackingUserLocation=${trackingUserLocation}")
                                updateLocationPoints(trackingUserLocation)
                            },
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
                        otherUtil.writeToFile("Logger_Нет данных о маршруте ${Date()} GPS доступен:${otherUtil.checkOnOffGPS()}")
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

    private fun updateLocationPoints(location: List<TrackingUserLocation>) {
        Timber.d("updateLocationPoints")
        val disposableUpdateLocation=Single.fromCallable{
            val ids=location.map { it.dateLocation.time }
            db.trackingUserDao().updateLocationSynced(ids)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("updateLocationPoints_OK")
                //disposableUpdateLocation.dispose()
            },{throwable ->
                throwable.printStackTrace()
                //disposableUpdateLocation.dispose()
            })
        compositeDisposable.add(disposableUpdateLocation)

    }


    fun onDestroy() {
        this.view = null
        /*if (this::disposable.isInitialized) {
            disposable.dispose()
        }*/
        if (this::disposableRouteInterval.isInitialized) {
            Timber.d("disposableRouteInterval_destroy")
            disposableRouteInterval.dispose()
        }
        if (this::disposableFinishInterval.isInitialized) {
            Timber.d("disposableFinishInterval_destroy")
            disposableFinishInterval.dispose()
        }
        compositeDisposable.dispose()
    }

    fun syncDB() {
        Timber.d("syncDB_x")
        val disposable = syncOrder()
            .subscribeOn(Schedulers.io())
            .subscribe({},{
                it.printStackTrace()
            })
        compositeDisposable.add(disposable)

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
                    // Получим id всех присланных заявок
                    val idsList= mutableListOf<String>()
                    orders.forEach{
                        idsList.add(it.id.toString())
                    }
                    Timber.d("idsList=$idsList")
                    db.ordersDao().deleteOrders(idsList)

                    db.historyOrderStateDao().clearHistory() // Очистим таблицу с историей смены статуса заявок
                    Timber.d("clearHistory")
                    orders.forEach{
                        Timber.d("ordersDao_insert_$it")
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
            if (throwable.message=="Нет заявок") {
                clearOrders()
                if (view!=null) {
                    view?.showFailureTextView("Нет заявок")
                }
            }
            if (view!=null) {
                 view?.showFailureTextView("")
                 view?.errorReceived(throwable)
                 view?.showOrders()
            } else {
                errorHandler(throwable)
            }
            throwable.printStackTrace()}
        .ignoreElement()
        .andThen(syncTechParams(sharedPrefSaver.getUserId()))

    private fun clearOrders() {
        Timber.d("clearOrders")
        val disposableClearOrdersFromDB=Single.fromCallable{
            db.ordersDao().clearOrders()
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
            Timber.d("clearOrders_OK")
            //disposableClearOrdersFromDB.dispose()
            view?.clearOrdersList()
            view?.showOrders()

        },{throwable ->
            throwable.printStackTrace()
            //disposableClearOrdersFromDB.dispose()
        })
        compositeDisposable.add(disposableClearOrdersFromDB)
    }

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
                },{throwable ->
                    throwable.printStackTrace()
                })

        }
        .doOnError { throwable ->
            view?.errorReceived(throwable)
            throwable.printStackTrace()
        }
        .ignoreElement()
        .andThen(syncAddLoad(sharedPrefSaver.getUserId()))


    private fun syncAddLoad(userId:Int): Completable=apiService.getAddLoad(user=userId)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map{ response ->
            Timber.d("Получили_доп_нагрузку")
            Timber.d(response.toString())

            Single.fromCallable{
                db.addLoadDao().clearAddLoad() // Перед вставкой очистим таблицу
                response.forEach{ addLoad ->
                    db.addLoadDao().insert(addLoad)
                }

            }
                .subscribeOn(Schedulers.io())
                .subscribe ({
                    Timber.d("Сохранили_доп_нагрузку")
                    //#Группировка_List
                    val groupAddLoad=response.groupBy { it.idOrder }
                    groupAddLoad.forEach{

                        Single.fromCallable{
                            db.ordersDao().updateAddLoadCount(it.key,it.value.size)

                        }
                            .subscribeOn(Schedulers.io())
                            .subscribe({
                                Timber.d("Обновили_кол_во_доп_нагрузки")
                            },{throwable ->
                                throwable.printStackTrace()
                            })

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

                val disposable=Single.fromCallable {
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
                        //disposable.dispose()
                        Timber.d("Сохранили обследования в БД")
                        view?.showOrders()

                    }, {error ->
                        error.printStackTrace()
                        //disposable.dispose()
                    })
                compositeDisposable.add(disposable)

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

    private fun saveTokenFCM() {
        Timber.d("saveTokenGCM")

        val fcmToken=Models.FCMToken(
            token =sharedPrefSaver.getTokenGCM()
        )

        Timber.d("Данные4=${Gson().toJson(fcmToken)}")

        val jsonBody = Gson().toJson(fcmToken)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val disposableFCM=apiService.saveGCMToken(jsonBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({response ->
                Timber.d(response.toString())
                //disposableFCM.dispose()
            },{ throwable ->
                Timber.d("ошибка!!!")
                throwable.printStackTrace()
                view?.errorReceived(throwable)
                //disposableFCM.dispose()
            })
        compositeDisposable.add(disposableFCM)

    }

}