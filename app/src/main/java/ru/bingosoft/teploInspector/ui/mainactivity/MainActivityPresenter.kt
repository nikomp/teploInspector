package ru.bingosoft.teploInspector.ui.mainactivity

import android.view.View
import com.google.gson.Gson
import com.google.gson.JsonArray
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.User.TrackingUserLocation
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.Const.LocationStatus.INTERVAL_SENDING_ROUTE
import ru.bingosoft.teploInspector.util.Const.MessageCode.DISABLE_LOCATION
import ru.bingosoft.teploInspector.util.Const.MessageCode.REFUSED_PERMISSION
import ru.bingosoft.teploInspector.util.Const.MessageCode.REPEATEDLY_REFUSED
import ru.bingosoft.teploInspector.util.Const.MessageCode.USER_LOGIN
import ru.bingosoft.teploInspector.util.Const.MessageCode.USER_LOGOUT
import ru.bingosoft.teploInspector.util.Const.Photo.DCIM_DIR
import ru.bingosoft.teploInspector.util.OtherUtil
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.ThrowHelper
import timber.log.Timber
import java.io.File
import java.io.FilenameFilter
import java.util.*
import javax.inject.Inject

class MainActivityPresenter @Inject constructor(val db: AppDatabase) {
    var view: MainActivityContractView? = null

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var userLocationReceiver: UserLocationReceiver

    @Inject
    lateinit var otherUtil: OtherUtil

    @Inject
    lateinit var sharedPrefSaver: SharedPrefSaver


    private lateinit var disposable: Disposable
    private lateinit var disposableSendMsg: Disposable
    private lateinit var disposableRouteInterval: Disposable
    private lateinit var disposableFiles: Disposable
    private lateinit var disposableFiles0: Disposable
    private lateinit var disposableAuth: Disposable
    private lateinit var disposableSendGi: Disposable
    private lateinit var disposableGetAllMessage: Disposable
    private lateinit var disposableMarkAllMessage: Disposable
    private lateinit var disposableUpdateLocation: Disposable
    private lateinit var disposableUpdateDateVisit: Disposable
    private lateinit var checkupsWasSync: MutableList<Int>

    private var compositeDisposable= CompositeDisposable()

    private var filesToSync: Array<File>? = arrayOf()


    fun attachView(view: MainActivityContractView) {
        this.view=view
    }


    fun authorization(url:String, stLogin: String?, stPassword: String?, msgId: Int=R.string.auth_ok){
        Timber.d("authorization1_MainActPres $stLogin _ $stPassword")
        otherUtil.writeToFile("Logger_authorization_from_MainActivityPresenter")
        if (!stLogin.isNullOrEmpty() && !stPassword.isNullOrEmpty()) {

            Timber.d("jsonBody=${Gson().toJson(Models.LP(login = stLogin, password = stPassword))}")


            val jsonBody = Gson().toJson(Models.LP(login = stLogin, password = stPassword))
                .toRequestBody("application/json".toMediaType())


            disposableAuth = apiService.getAuthentication(url, jsonBody)
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
                        Timber.d("авторизовалисьZZ=${token.token}")
                        view?.sendRoute()

                        disposableAuth.dispose()
                        view?.saveLoginPasswordToSharedPreference(stLogin, stPassword)
                        view?.saveToken(token.token)
                        view?.saveInfoUserToSharedPreference(Models.User(fullname = token.name))
                        view?.registerReceiver()
                        view?.startNotificationService(token.token)
                        view?.showMainActivityMsg(msgId)
                        view?.checkMessageId()
                        view?.getAllMessage()

                        val v = view
                        if (v != null) {
                            Timber.d("startService_LoginPresenter")
                            v.repeatSync()
                        }
                    }, { throwable ->
                        throwable.printStackTrace()
                        view?.errorReceived(throwable)
                        disposableAuth.dispose()
                    }
                )

        } else {
            view?.errorReceived(Throwable("Не заданы логин или пароль"))
            otherUtil.writeToFile("Ошибка! Не заданы логин или пароль ${Date()}")
        }

    }

    fun getAllMessage() {
        Timber.d("getAllMessage")
        disposableGetAllMessage=apiService.getAllMessages()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({listNotifications ->
                Timber.d("Получили_все_уведомления")
                val unreadNotifications=listNotifications.filter { it.read_date==null }
                Timber.d("unreadNotifications=${unreadNotifications.size}")
                if (unreadNotifications.isNotEmpty()) {
                    Timber.d("view=$view")
                    view?.showUnreadNotification(unreadNotifications)
                }
                disposableGetAllMessage.dispose()
            },{throwable ->
                throwable.printStackTrace()
                disposableGetAllMessage.dispose()
            })

    }

    fun markMessageAsRead(msgId: Int) {
        Timber.d("markMessageAsRead")
        val jsonBody = Gson().toJson(Models.MessageId(id=msgId))
            .toRequestBody("application/json".toMediaType())

        disposable = apiService.markMessageAsRead(jsonBody)
            .subscribeOn(Schedulers.io())
            .subscribe(
                {

                    view?.setEmptyMessageId()
                    disposable.dispose()

                }, { throwable ->
                    throwable.printStackTrace()
                    disposable.dispose()
                }
            )
    }

    fun markAllMessageAsRead() {
        Timber.d("markAllMessageAsRead")
        disposableMarkAllMessage = apiService.markAllMessageAsRead()
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    disposableMarkAllMessage.dispose()
                }, { throwable ->
                    throwable.printStackTrace()
                    disposableMarkAllMessage.dispose()
                }
            )
    }

    fun isCheckupWithResult(msg: String) {
        val disposable=Single.fromCallable{
            db.checkupDao().existCheckupWithResult()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                Timber.d("result_existCheckupWithResult=$result")
                if (result>0) {
                    view?.showMainActivityMsg("$msg Есть чеклисты с неподтвержденными шагами")
                    //disposable.dispose()
                } else {
                    view?.showMainActivityMsg(msg)
                    //disposable.dispose()
                }
            },{
                it.printStackTrace()
            })
        compositeDisposable.add(disposable)
    }

    fun repeatSendData() {
        Timber.d("repeatSendData")
        disposable =
            db.checkupDao()
                .getResultAll2()
                .subscribeOn(Schedulers.io())
                .takeWhile { listResult ->
                    Timber.d("listResult=$listResult")
                    if (listResult.isEmpty()) {
                        throw ThrowHelper("Нет данных для передачи на сервер")
                    } else {
                        listResult.isNotEmpty()
                    }
                }
                .flatMap { results ->
                    Timber.d("100_${results.size}")
                    // Конвертируем строку controls в JsonArray
                    Timber.d("Данные65=${Gson().toJson(results)}")
                    val resultX= mutableListOf<Models.Result2>()


                    checkupsWasSync= mutableListOf()
                    results.forEach {
                        Timber.d("controls=${it.controls}")
                        checkupsWasSync.add(it.id_order) // Сохраняю данные, которые должны быть переданы

                        val result=Models.Result2()
                        result.id_order=it.id_order

                        Timber.d("it.history_order_state=${it.history_order_state}")
                        if (it.history_order_state!=null) {
                            result.history_order_state=Gson().fromJson(it.history_order_state?.trim(), JsonArray::class.java)
                        }
                        result.controls=Gson().fromJson(it.controls, JsonArray::class.java)

                        resultX.add(result)
                    }

                    val reverseData=Models.ReverseData()
                    reverseData.data=resultX

                    // включать с осторожностью, может привести к OutOfMemmory, если строка слишком длинная
                    //longInfo("Данные222=${Gson().toJson(reverseData)}")

                    val jsonBody = Gson().toJson(reverseData)
                        .toRequestBody("application/json; charset=utf-8".toMediaType())


                    Timber.d("Данные3=${jsonBody}")

                    apiService.doReverseSync(jsonBody)?.toFlowable()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {response: List<Models.DataFile> ->
                        //disposable.dispose()
                        if (response.isNotEmpty()) {
                            Timber.d("response=${response[0].id}")
                            sendFile(response)
                        } else {
                            view?.dataSyncOK(null)
                            view?.showMainActivityMsg(R.string.msgDataSendOk)
                            view?.refreshRecyclerView()
                        }
                    }, { throwable ->
                        Timber.d("MainActivityPresenter_throwable")
                        throwable.printStackTrace()
                        view?.errorReceived(throwable)
                        //view?.dataNotSync(idOrder,throwable)
                        //disposable.dispose()
                    }
                )
    }

    fun updateGiOrder(order: Orders) {
        Timber.d("saveDateTime_$order")
        val disposable=Single.fromCallable {
            db.ordersDao().update(order)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                Timber.d("view=$view")
                sendGiOrder(order.id)
                //disposable.dispose()
                Timber.d("Обновили_дату_время")
            },{throwable ->
                //disposable.dispose()
                throwable.printStackTrace()
            })
        compositeDisposable.add(disposable)
    }

    fun updateDateVisit(idOrder: Long, newDateVisit: String) {
        Timber.d("idOrder_$idOrder _newDateVisit_$newDateVisit")
        val disposableUpdateDateVisit= Single.fromCallable {
            db.ordersDao().updateDateVisit(idOrder = idOrder, newDate = newDateVisit)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                //disposableUpdateDateVisit.dispose()
                view?.refreshOrderListFromMA()
                Timber.d("Обновили_дату_визита")
            },{throwable ->
                //disposableUpdateDateVisit.dispose()
                throwable.printStackTrace()
            })
        compositeDisposable.add(disposableUpdateDateVisit)
    }

    fun sendGiOrder(idOrder:Long) {
        disposableSendGi =
            db.ordersDao().getById(idOrder)
                .subscribeOn(Schedulers.io())
                .flatMap { orders ->
                    // Конвертируем строку controls в JsonArray
                    Timber.d("Данные=${Gson().toJson(orders)}")

                    // Обернем данные в JsonArray, нужно Доктрине на сервере
                    val result= mutableListOf<Orders>()
                    result.add(orders)
                    val resultX=Models.ResultOrder()
                    resultX.data=Gson().fromJson(Gson().toJson(result), JsonArray::class.java)

                    val ordersData=Gson().toJson(resultX)
                    // включать с осторожностью, может привести к OutOfMemmory, если строка слишком длинная
                    //longInfo("ДанныеOrder=$ordersData")

                    val jsonBody = ordersData
                        .toRequestBody("application/json; charset=utf-8".toMediaType())

                    apiService.sendGiOrder(jsonBody).toFlowable()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Timber.d("Общие_сведения_отправлены")
                        disposableSendGi.dispose()
                    }, { throwable ->
                        Timber.d("MainActivityPresenter_throwable")
                        disposableSendGi.dispose()
                        throwable.printStackTrace()
                        view?.errorReceived(throwable)

                    }
                )
    }

    //#Проверка_JSON
    private fun checkJsonValid(src: String): Boolean {
        Timber.d("checkJsonValid")

        // см. подробнее тут https://stackoverflow.com/questions/10174898/how-to-check-whether-a-given-string-is-valid-json-in-java
        // https://jsonlint.com/ OnLine JSON Validator
        try {
            JSONArray(src)
        } catch (e: JSONException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun sendData3(idOrder: Long, syncView: View? = null) {
        Timber.d("sendData3_$idOrder")
        disposable =
            db.checkupDao()
                .getResultByOrderId(idOrder)
                .subscribeOn(Schedulers.io())
                .takeWhile { listResult ->
                    Timber.d("listResult=$listResult")
                    Timber.d("listResult=${listResult.size}")
                    if (listResult.isEmpty()) {
                        throw ThrowHelper("Нет данных для передачи на сервер")
                    } else {
                        if (!checkJsonValid(listResult[0].controls)) {
                            throw ThrowHelper("Некорректные данные по заявке ID: $idOrder")
                        }
                        listResult.isNotEmpty()
                    }

                }
                .flatMap { results ->
                    // Конвертируем строку controls в JsonArray
                    Timber.d("Данные=${Gson().toJson(results)}")
                    val resultX= mutableListOf<Models.Result2>()


                    checkupsWasSync= mutableListOf()
                    results.forEach {
                        Timber.d("controls=${it.controls}")
                        checkupsWasSync.add(it.id_order) // Сохраняю данные, которые должны быть переданы

                        val result=Models.Result2()
                        result.id_order=it.id_order

                        Timber.d("it.history_order_state=${it.history_order_state}")
                        if (it.history_order_state!=null) {
                            /*val gson = Gson()
                            val reader = JsonReader(StringReader(it.history_order_state))
                            reader.setLenient(true)*/
                            result.history_order_state=Gson().fromJson(it.history_order_state?.trim(), JsonArray::class.java)
                        }
                        result.controls=Gson().fromJson(it.controls, JsonArray::class.java)

                        resultX.add(result)
                    }

                    val reverseData=Models.ReverseData()
                    reverseData.data=resultX

                    // включать с осторожностью, может привести к OutOfMemmory, если строка слишком длинная
                    //longInfo("Данные222=${Gson().toJson(reverseData)}")

                    val jsonBody = Gson().toJson(reverseData)
                        .toRequestBody("application/json; charset=utf-8".toMediaType())


                    Timber.d("Данные3=${jsonBody}")

                    apiService.doReverseSync(jsonBody)?.toFlowable()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {response: List<Models.DataFile> ->
                        disposable.dispose()
                        view?.showMainActivityMsg(R.string.msgDataSendOk)
                        if (response.isNotEmpty()) {
                            Timber.d("response=${response[0].id}")
                            sendFile(response)
                        } else {
                            view?.enabledSaveButton()
                            view?.dataSyncOK(idOrder)
                            if (syncView!=null) {
                                syncView.visibility=View.GONE
                            }
                        }
                    }, { throwable ->
                        Timber.d("MainActivityPresenter_throwable")
                        throwable.printStackTrace()
                        view?.errorReceived(throwable)
                        view?.dataNotSync(idOrder,throwable)
                        view?.enabledSaveButton()
                        //disposable.dispose()
                    }
                )
    }

    private fun sendFile(dataFileArray: List<Models.DataFile>) {
        Timber.d("sendFile")
        dataFileArray.forEachIndexed {index, datafile->
            Timber.d("$DCIM_DIR/PhotoForApp/${datafile.dir}")
            val directory = File("$DCIM_DIR/PhotoForApp/${datafile.dir}")
            val filesBody= mutableListOf<MultipartBody.Part>()
            val filesBody2= mutableListOf<MultipartBody.Part>()
            if (directory.exists()) {

                // На сервер отправляем только несинхронизированные файлы
                val notSyncFileFilter= FilenameFilter { _, name -> name?.contains("_synced")==false }

                filesToSync = if (!directory.listFiles(notSyncFileFilter).isNullOrEmpty()) {
                    Timber.d("files_exist")
                    directory.listFiles(notSyncFileFilter)
                } else {
                    arrayOf()
                }

                Timber.d("filesToSync.size=${filesToSync?.size}")


                filesToSync?.forEach {
                    val part = MultipartBody.Part.createFormData(
                        "attr_2614_[]", it.name, it.asRequestBody("multipart/form-data".toMediaType())
                    )
                    filesBody.add(part)

                    val part2 = MultipartBody.Part.createFormData(
                        "attr_2530_[]", it.name, it.asRequestBody("multipart/form-data".toMediaType())
                    )
                    filesBody2.add(part2)
                }
            }

            if (filesBody.isNotEmpty() && filesBody2.isNotEmpty()) {
                Timber.d("disposableFiles0")
                disposableFiles0=apiService.sendFiles(datafile.id, 2518, filesBody)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {response ->
                            Timber.d("sendingFiles_$response")
                            disposableFiles0.dispose()
                            // Фотографии сохраним еще и для заявки
                            disposableFiles=apiService.sendFiles(datafile.idOrder, 2380, filesBody2)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                    {
                                        Timber.d("sendingFiles_OK")
                                        disposableFiles.dispose()
                                        view?.filesSend(dataFileArray.size,index+1)
                                        view?.renameSyncedFiles(filesToSync)
                                        view?.enabledSaveButton()

                                    },
                                    {throwable ->
                                        disposableFiles.dispose()
                                        Timber.d("sendingFiles_throwable")
                                        view?.errorReceived(throwable)
                                        view?.enabledSaveButton()
                                        throwable.printStackTrace()
                                    }
                                )
                        },
                        {throwable ->
                            Timber.d("sendingFiles_throwable2")
                            view?.errorReceived(throwable)
                            view?.enabledSaveButton()
                            throwable.printStackTrace()
                            disposableFiles0.dispose()
                        }
                    )
            } else {
                view?.enabledSaveButton()
            }


        }
    }

    //включать с осторожностью, может привести к OutOfMemmory, если строка слишком длинная
    private fun longInfo(str: String) {
        if (str.length > 3000) {
            Timber.d( str.substring(0, 3000))
            longInfo(str.substring(3000))
        } else Timber.d(str)
    }



    fun sendMessageToAdmin(codeMsg: Int, currentVersion:String ="") {
        Timber.d("sendMessageToAdmin codeMsg=$codeMsg")
        val textMessage: String
        val eventType: Int
        when (codeMsg) {
            REFUSED_PERMISSION-> {
                textMessage="Пользователь отказался выдать разрешение на Геолокацию"
                eventType=1 // Геолокация отключена
            }
            REPEATEDLY_REFUSED-> {
                textMessage="Пользователь повторно отказался включить GPS"
                eventType=1 // Геолокация отключена
            }
            DISABLE_LOCATION-> {
                textMessage="Пользователь выключил GPS"
                eventType=1 // Геолокация отключена
            }
            /*ENABLE_LOCATION-> {
                textMessage="Пользователь включил GPS"
                eventType=3 // Геолокация включена
            }*/
            USER_LOGOUT-> {
                textMessage="Пользователь вышел из приложения"
                eventType=4 // Пользователь вышел из приложения
                view?.clearAuthData()
            }
            USER_LOGIN-> {
                textMessage="Пользователь вошел в приложение версия_$currentVersion"
                eventType=5 // Пользователь вошел в приложение
            }
            else -> {
                textMessage=""
                eventType=0
            }
        }


        val messageData=Models.MessageData(
            text = textMessage,
            date= Date().time,
            event_type = eventType,
            lat = userLocationReceiver.lastKnownLocation.latitude,
            lon = userLocationReceiver.lastKnownLocation.longitude
        )

        Timber.d("Данные4=${Gson().toJson(messageData)}")

        val jsonBody = Gson().toJson(messageData)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        disposableSendMsg=apiService.sendMessageToAdmin(jsonBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({response ->
                Timber.d(response.toString())
                disposableSendMsg.dispose()
            },{ throwable ->
                Timber.d("ошибка!!!")
                throwable.printStackTrace()
                view?.errorReceived(throwable)
                disposableSendMsg.dispose()
            })
    }

    fun updData(sync:Int=1) {
        /*if (this::disposable.isInitialized) {
            disposable.dispose()
        }*/
        Single.fromCallable {
            checkupsWasSync.forEach {
                // sync=1 Данные сохранены и отправлены на сервер,
                // sync=2 Данные сохранены но не отправлены на сервер
                db.checkupDao().updateSync(it,sync)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe ()
    }

    fun getAllOrderNotSync() {
        Timber.d("getAllOrderNotSync")
        val disposable=Single.fromCallable {
            db.checkupDao().getIdAllOrdersNotSync()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ response ->
                Timber.d("orderIdsNotSync=$response")
                view?.setIdsOrdersNotSync(response)
                //disposable.dispose()
            },{throwable ->
                println("ERROR_$db")
                throwable.printStackTrace()
                //disposable.dispose()
            })
        compositeDisposable.add(disposable)
    }

    //#RxJava #interval
    fun sendRoute() {
        //Нет обновления координат в БД, стартуем при каждой автоматической авторизации
        Timber.d("test_sendRoute_MainActivityPresenter")
        sharedPrefSaver.saveRouteIntervalFlag() // Отметим, что передача маршрута включена
        otherUtil.writeToFile("Logger_sendRoute_MainActivityPresenter_${Date()}_возможно дублирование координат")

        disposableRouteInterval= Flowable.interval(INTERVAL_SENDING_ROUTE,
            java.util.concurrent.TimeUnit.MINUTES,
            Schedulers.computation() // Scheduler добавил для тестирования см. тест LoginPresenterTest.testSendRoute, до этого было пусто
        ).map {
            Timber.d("ДанныеМаршрутаПолучили_${Date()}")
            //db.trackingUserDao().getTrackingForLastMinutes()
            /*val trackingList=db.trackingUserDao().getTrackingForCurrentDay()
            if (trackingList.size> Const.TrackingUser.LIMIT_RECORDS) {
                Timber.d("MA_getTrackingForCurrentDay")
                return@map trackingList
            } else {
                Timber.d("MA_getTrackingForLastMinutes")
                return@map db.trackingUserDao().getTrackingForLastMinutes()
            }*/
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
                        {
                            Timber.d("ОтправилиМаршрут_MainActivityPresenter")
                            Timber.d("trackingUserLocation=${trackingUserLocation}")
                            updateLocationPoints(trackingUserLocation)
                        },
                        {throwable ->
                            throwable.printStackTrace()
                            if (view!=null) {
                                view?.errorReceived(throwable)
                            }
                        }
                    )
                } else {
                    Timber.d("Нет данных о маршруте")
                    otherUtil.writeToFile("Logger_Нет данных о маршруте ${Date()} GPS доступен:${otherUtil.checkOnOffGPS()}")
                }
            },{throwable ->
                throwable.printStackTrace()
                view?.errorReceived(throwable)
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
        Timber.d("MainActivityPresenter_onDestroy")
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
        if (this::disposableRouteInterval.isInitialized) {
            disposableRouteInterval.dispose()
        }
        compositeDisposable.dispose()
    }

}