package ru.bingosoft.teploInspector.ui.mainactivity

import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import com.google.gson.JsonArray
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.util.Const.Photo.DCIM_DIR
import ru.bingosoft.teploInspector.util.ThrowHelper
import ru.bingosoft.teploInspector.util.UserLocationNative
import timber.log.Timber
import java.io.File
import java.util.*
import javax.inject.Inject

class MainActivityPresenter @Inject constructor(val db: AppDatabase) {
    var view: MainActivityContractView? = null

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var userLocationNative: UserLocationNative

    private lateinit var disposable: Disposable
    private lateinit var checkupsWasSync: MutableList<Int>


    fun attachView(view: MainActivityContractView) {
        this.view=view
    }

    fun sendUserRoute() {
        Timber.d("sendUserRoute")
        disposable=db.trackingUserDao()
            .getAll()
            .subscribeOn(Schedulers.io())
            .map{trackingUserLocation ->

                val route=Models.FileRoute()
                val jsonStr=Gson().toJson(trackingUserLocation)
                route.fileRoute=jsonStr

                //return@map jsonStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                return@map Gson().toJson(route)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

            }
            .flatMap { jsonBodies ->
                Timber.d("jsonBodies=${jsonBodies}")

                apiService.sendTrackingUserLocation(jsonBodies).toFlowable()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    Timber.d(response.toString())
                    view?.showMainActivityMsg(R.string.msgRouteUserSendOk)

                }, { throwable ->
                    view?.errorReceived(throwable)
                    throwable.printStackTrace()

                }
            )
    }

    private fun isCheckupWithResult(msg: String) {
        Single.fromCallable{
            db.checkupDao().existCheckupWithResult()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{ result ->
                Timber.d("result_existCheckupWithResult=$result")
                if (result>0) {
                    view?.showMainActivityMsg("$msg Есть чеклисты с неподтвержденными шагами")
                } else {
                    view?.showMainActivityMsg(msg)
                }
            }
    }

    fun sendData2() {
        Timber.d("sendData")
        disposable =
            db.checkupDao()
                .getResultAll2()
                .subscribeOn(Schedulers.io())
                .takeWhile { listResult ->
                    Timber.d("listResult=$listResult")
                    if (listResult.isEmpty()) {
                        throw ThrowHelper("Ошибка! Нет данных для передачи на сервер")
                    } else {
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
                            result.history_order_state=Gson().fromJson(it.history_order_state, JsonArray::class.java)
                        }
                        result.controls=Gson().fromJson(it.controls, JsonArray::class.java)

                        resultX.add(result)
                    }

                    val reverseData=Models.ReverseData()
                    reverseData.data=resultX

                    //Timber.d("Данные2=${Gson().toJson(reverseData)}")
                    longInfo("Данные222=${Gson().toJson(reverseData)}")

                    val jsonBody = Gson().toJson(reverseData)
                        .toRequestBody("application/json; charset=utf-8".toMediaType())


                    Timber.d("Данные3=${jsonBody}")

                    apiService.doReverseSync(jsonBody)?.toFlowable()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {response: List<Models.DataFile> ->
                        if (response.isNotEmpty()) {
                            Timber.d("response=${response[0].id}")
                            sendFile(response)
                        } else {
                            view?.dataSyncOK()
                            view?.showMainActivityMsg(R.string.msgDataSendOk)
                        }
                    }, { throwable ->
                        throwable.printStackTrace()
                        if (throwable is ThrowHelper) {
                            isCheckupWithResult("${throwable.message}")
                        } else {
                            view?.errorReceived(throwable)
                            view?.showMainActivityMsg(R.string.msgDataSendError)
                        }
                    }
                )
    }

    fun sendData3(idOrder:Long) {
        Timber.d("sendData3")
        disposable =
            db.checkupDao()
                .getResultByOrderId(idOrder)
                .subscribeOn(Schedulers.io())
                .takeWhile { listResult ->
                    Timber.d("listResult=$listResult")
                    if (listResult.isEmpty()) {
                        throw ThrowHelper("Ошибка! Нет данных для передачи на сервер")
                    } else {
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

                    //Timber.d("Данные2=${Gson().toJson(reverseData)}")
                    longInfo("Данные222=${Gson().toJson(reverseData)}")

                    val jsonBody = Gson().toJson(reverseData)
                        .toRequestBody("application/json; charset=utf-8".toMediaType())


                    Timber.d("Данные3=${jsonBody}")

                    apiService.doReverseSync(jsonBody)?.toFlowable()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {response: List<Models.DataFile> ->
                        if (response.isNotEmpty()) {
                            Timber.d("response=${response[0].id}")
                            sendFile(response)
                        } else {
                            view?.dataSyncOK()
                            view?.showMainActivityMsg(R.string.msgDataSendOk)
                        }
                    }, { throwable ->
                        throwable.printStackTrace()
                        if (throwable is ThrowHelper) {
                            isCheckupWithResult("${throwable.message}")
                        } else {
                            view?.showMainActivityMsg(R.string.msgDataSendError)
                        }
                    }
                )
    }

    private fun getHistory(order: Orders) {
        val hos=db.historyOrderStateDao()

        Single.fromCallable {
            db.historyOrderStateDao().getHistoryStateByIdOrder(order.id)
        }
            .subscribeOn(Schedulers.io())
            .subscribe ()
    }

    private fun sendFile(dataFileArray: List<Models.DataFile>) {
        dataFileArray.forEachIndexed {index, datafile->
            Timber.d("$DCIM_DIR/PhotoForApp/${datafile.dir}")
            val directory = File("$DCIM_DIR/PhotoForApp/${datafile.dir}")
            val filesBody= mutableListOf<MultipartBody.Part>()
            val filesBody2= mutableListOf<MultipartBody.Part>()
            if (directory.exists()) {
                val files = directory.listFiles()

                Timber.d("files=${files.size}")

                files?.forEach {
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

            apiService.sendFiles(datafile.id, 2518, filesBody)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {response ->
                        Timber.d("sendingFiles $response")
                        //view?.filesSend(dataFileArray.size,index+1)
                        // Фотографии сохраним еще и для заявки
                        apiService.sendFiles(datafile.idOrder, 2380, filesBody2)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                {_ ->
                                    view?.filesSend(dataFileArray.size,index+1)
                                },
                                {throwable ->
                                    view?.errorReceived(throwable)
                                    throwable.printStackTrace()
                                }
                            )
                    },
                    {throwable ->
                        view?.errorReceived(throwable)
                        throwable.printStackTrace()
                    }
                )

        }
    }

    private fun longInfo(str: String) {
        if (str.length > 3000) {
            Timber.d( str.substring(0, 3000))
            longInfo(str.substring(3000))
        } else Timber.d(str)
    }



    fun sendMessageToAdmin(codeMsg: Int) {
        Timber.d("sendMessageToAdmin codeMsg=$codeMsg")
        val textMessage: String
        val eventType: Int
        when (codeMsg) {
            1-> {
                textMessage="Пользователь отказался выдать разрешение на Геолокацию"
                eventType=1 // Геолокация отключена
            }
            2-> {
                textMessage="Пользователь повторно отказался включить GPS"
                eventType=1 // Геолокация отключена
            }
            3-> {
                textMessage="Пользователь выключил GPS"
                eventType=1 // Геолокация отключена
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
            lat = userLocationNative.userLocation.latitude,
            lon = userLocationNative.userLocation.longitude
        )

        Timber.d("Данные4=${Gson().toJson(messageData)}")

        val jsonBody = Gson().toJson(messageData)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        disposable=apiService.sendMessageToAdmin(jsonBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({response ->
                Timber.d(response.toString())
            },{ throwable ->
                Timber.d("ошибка!!!")
                throwable.printStackTrace()
                view?.errorReceived(throwable)
            })
    }

    fun updData() {
        Timber.d("updData")
        if (this::disposable.isInitialized) {
            Timber.d("disposable.dispose()")
            disposable.dispose()
        }
        Single.fromCallable {
            Timber.d("${checkupsWasSync}")

            checkupsWasSync.forEach {
                db.checkupDao().updateSync(it)
            }
        }
            .subscribeOn(Schedulers.io())
            .subscribe ()
    }

    fun openCheckup(fragmentManager: FragmentManager, orderId: Long) {
        Timber.d("openCheckup")
        // Получим информацию о чеклисте, по orderId
        Single.fromCallable {
            val idCheckup=db.checkupDao().getCheckupIdByOrder(orderId)
            Timber.d("idCheckup=$idCheckup")
            //Загружаем чеклист
            val bundle = Bundle()
            bundle.putBoolean("loadCheckupById", true)
            bundle.putLong("checkupId",idCheckup)

            val fragmentCheckup= CheckupFragment()
            fragmentCheckup.arguments=bundle

            fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragmentCheckup, "checkup_fragment_tag")
                .addToBackStack(null)
                .commit()

            fragmentManager.executePendingTransactions()
        }
            .subscribeOn(Schedulers.io())
            .subscribe()

    }


    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }

}