package ru.bingosoft.teploInspector.ui.mainactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.User.TrackingUserLocation
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.Const.LocationStatus.PROVIDER_DISABLED
import ru.bingosoft.teploInspector.util.Const.MessageCode.DISABLE_LOCATION
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject

class UserLocationReceiver @Inject constructor(
    private val apiService: ApiService,
    private val db: AppDatabase
): BroadcastReceiver() {
    private lateinit var disposable: Disposable
    lateinit var lastKnownLocation: Location
    @Inject
    lateinit var toaster: Toaster

    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("onReceive")
        val lat=intent?.getDoubleExtra("lat",0.0)
        val lon=intent?.getDoubleExtra("lon",0.0)
        val provider=intent?.getStringExtra("provider")
        val sendRouteToServer=intent?.getBooleanExtra("sendRouteToServer",false)
        if (sendRouteToServer!=null && sendRouteToServer==true) {
            Timber.d("Отправляем маршрут на серверXX")
            sendUserRoute()
        }
        lastKnownLocation=Location(provider)
        if (lat!=null && lon!=null) {
            lastKnownLocation.longitude=lon
            lastKnownLocation.latitude=lat
        }

        val status=intent?.getStringExtra("status")
        if (status==PROVIDER_DISABLED) {
            sendMessageToAdmin(DISABLE_LOCATION)
        }
        Timber.d("UserLocationReceiver=$lat _ $lon")
        if (lat != null && lon!=null) {
            if (provider != null && status != null) {
                    saveLocation(lat,lon,provider,status)
            }
        }
    }

    fun isInitLocation() :Boolean {
        return ::lastKnownLocation.isInitialized
    }

    private fun saveLocation(lat: Double?, lon: Double?, provider: String, status: String) {
        Timber.d("saveLocation")
        val movingUser=TrackingUserLocation(lat=lat,lon=lon,dateLocation = Date(),provider = provider, status = status)

        Single.fromCallable{
            db.trackingUserDao().insert(movingUser)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe{_->
                Timber.d("Сохранили локацию пользователя в БД")
            }

    }

    private fun sendMessageToAdmin(codeMsg: Int) {
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


        val messageData= Models.MessageData(
            text = textMessage,
            date= Date().time,
            event_type = eventType,
            lat = lastKnownLocation.latitude,
            lon = lastKnownLocation.longitude
        )

        Timber.d("Данные4=${Gson().toJson(messageData)}")

        val jsonBody = Gson().toJson(messageData)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        disposable=apiService.sendMessageToAdmin(jsonBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({response ->
                Timber.d(response.toString())
                disposable.dispose()
            },{
                Timber.d("ошибка!!!")
                Timber.d(it.printStackTrace().toString())
                disposable.dispose()
            })

    }

    private fun sendUserRoute() {
        Timber.d("sendUserRoute_from_UserLocationReceiver")
        disposable=db.trackingUserDao()
            //.getAll()
            .getTrackingForCurrentDay()
            .subscribeOn(Schedulers.io())
            .map{trackingUserLocation ->

                val route=Models.FileRoute()
                val jsonStr=Gson().toJson(trackingUserLocation)
                route.fileRoute=jsonStr

                val jsonBody=Gson().toJson(route)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                Timber.d("ДанныеМаршрута=${jsonStr}")

                //return@map jsonStr.toRequestBody("application/json; charset=utf-8".toMediaType())
                return@map jsonBody

            }
            .flatMap { jsonBodies ->
                Timber.d("jsonBodies=${jsonBodies}")

                apiService.sendTrackingUserLocation(jsonBodies).toFlowable()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { response ->
                    Timber.d(response.toString())
                    //view?.showMainActivityMsg(R.string.msgRouteUserSendOk)
                    disposable.dispose()

                }, { throwable ->
                    //view?.errorReceived(throwable)
                    disposable.dispose()
                    throwable.printStackTrace()
                    when (throwable) {
                        is HttpException -> {
                            Timber.d("throwable.code()=${throwable.code()}")
                            when (throwable.code()) {
                                401 -> toaster.showToast(R.string.unauthorized)
                                else -> toaster.showToast("Ошибка! ${throwable.message}")
                            }
                        }
                        is UnknownHostException ->{
                            toaster.showToast(R.string.no_address_hostname)
                        }
                        else -> {
                            toaster.showToast("Ошибка! ${throwable.message}")
                        }
                    }

                }
            )
    }

    fun onDestroy() {
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }
}