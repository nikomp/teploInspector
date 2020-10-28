package ru.bingosoft.teploInspector.ui.login

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Completable
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
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.ThrowHelper
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import java.lang.reflect.Type
import java.net.UnknownHostException
import java.util.*
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

    fun attachView(view: LoginContractView) {
        this.view = view
    }

    fun authorization(stLogin: String?, stPassword: String?){
        Timber.d("authorization1 $stLogin _ $stPassword")
        if (stLogin!=null && stPassword!=null) {

            Timber.d("jsonBody=${Gson().toJson(Models.LP(login = stLogin, password = stPassword))}")


            val jsonBody = Gson().toJson(Models.LP(login = stLogin, password = stPassword))
                .toRequestBody("application/json".toMediaType())

            disposable = apiService.getAuthentication(jsonBody)
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
                        Timber.d(token.token)
                        this.stLogin = stLogin
                        this.stPassword = stPassword
                        view?.saveLoginPasswordToSharedPreference(stLogin, stPassword)
                        view?.saveToken(token.token)

                        val v = view
                        if (v != null) {
                            Timber.d("startService_LoginPresenter")
                            v.alertRepeatSync()
                        }

                        saveTokenGCM()

                    }, { throwable ->
                        throwable.printStackTrace()
                        view?.showFailureTextView()
                    }
                )

        }

    }

    /*private fun getInfoCurrentUser()  {
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

    }*/


    /**
     * Метод для генерации ГУИДа, нужен для первичного формирования fingerprint
     *
     * @return - возвращается строка содержащая ГУИД
     */
    /*private fun random(): String {
        var stF = UUID.randomUUID().toString()
        stF = stF.replace("-".toRegex(), "")
        stF = stF.substring(0, 32)
        Log.d(LOGTAG, "random()=$stF")

        return stF
    }*/

    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }

    fun syncDB() {
        Timber.d("syncDB")
        disposable = syncOrder()
            .subscribeOn(Schedulers.io())
            .subscribe({},{
                it.printStackTrace()
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
            if (view!=null) {
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
                    Timber.d("techParamsDao=${techParams}")
                    db.techParamsDao().insert(techParams)
                    // Если ставить ограничение TechParams.value not NULL, то лучше инсертить данные так
                    /*try {
                        db.techParamsDao().insert(techParams)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }*/


                    //#Группировка_List
                    val groupTechParams=response.groupBy { it.idOrder }
                    groupTechParams.forEach{
                        db.ordersDao().updateTechParamsCount(it.key,it.value.size)
                    }

                }

            }
                .subscribeOn(Schedulers.io())
                .subscribe ({ _->
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
        .andThen(syncCheckups())


    private fun syncCheckups() :Completable=apiService.getCheckups()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .map{
                checkups ->
            Timber.d("Получили обследования")
            Timber.d("checkups=$checkups")
            if (checkups.isNullOrEmpty()) {
                throw ThrowHelper("Нет обследований")
            } else {
                Timber.d("Обследования есть")
                //val data: Models.CheckupList = checkups

                Single.fromCallable {
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
                        Timber.d("Сохранили обследования в БД")
                        view?.showOrders()
                    }, {error ->
                        Timber.d("error")
                        error.printStackTrace()
                    })

                if (view!=null) {
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

    private fun saveTokenGCM() {
        Timber.d("saveTokenGCM")

        val fcmToken=Models.FCMToken(
            token =sharedPrefSaver.getTokenGCM()
        )

        Timber.d("Данные4=${Gson().toJson(fcmToken)}")

        val jsonBody = Gson().toJson(fcmToken)
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        disposable=apiService.saveGCMToken(jsonBody)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({response ->
                Timber.d(response.toString())
            },{ throwable ->
                Timber.d("ошибка!!!")
                throwable.printStackTrace()
                view?.errorReceived(throwable)
            })

        /*disposable=apiService.saveGCMToken(action="saveGCMToken",token = sharedPrefSaver.getTokenGCM())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({
                Timber.d(it.msg)
            },{
                it.printStackTrace()
            })*/
    }

}