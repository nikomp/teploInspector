package ru.bingosoft.teploInspector.ui.order

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
import ru.bingosoft.teploInspector.db.HistoryOrderState.HistoryOrderState
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject

class OrderPresenter @Inject constructor(
    val db: AppDatabase,
    private val apiService: ApiService,
    val toaster: Toaster
) {

    private var tempHistory= HistoryOrderState()

    var view: OrderContractView? = null

    private lateinit var disposable: Disposable

    fun attachView(view: OrderContractView) {
        this.view=view
    }

    fun saveDateTime(order: Orders) {
        disposable=Single.fromCallable {
            db.ordersDao().update(order)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                disposable.dispose()
                Timber.d("Обновили_дату_время")

            },{throwable ->
                disposable.dispose()
                throwable.printStackTrace()
            })
    }

    fun addHistoryState(order: Orders) {
        disposable=Single.fromCallable {
            db.ordersDao().update(order)
            val date=Date()
            val history=HistoryOrderState(id = date.hashCode(), idOrder = order.id, stateOrder = order.status!!, dateChange = date)
            tempHistory=history
            db.historyOrderStateDao().insert(history)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({id ->
                disposable.dispose()
                Timber.d("inserted ID=$id")
                val history= Models.HistoryOrderOnServer(unique_id=id,
                    idOrder = tempHistory.idOrder,
                    stateOrder = tempHistory.stateOrder,
                    dateChange = tempHistory.dateChange.time)
                sendHistoryToServer(history)
            },{throwable ->
                disposable.dispose()
                throwable.printStackTrace()
            })
    }

    private fun sendHistoryToServer(history: Models.HistoryOrderOnServer) {
        Timber.d("sendHistoryToServer=$history")
        val jsonBody2 = Gson().toJson(history)
            .toRequestBody("application/json".toMediaType())

        disposable=apiService.sendStatusOrder(jsonBody2)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                Timber.d("response=$response")
                Timber.d("Отправили статус")
                disposable.dispose()
            },{ throwable ->
                Timber.d("throwable.message=${throwable.message}")
                Timber.d("view=$view")
                throwable.printStackTrace()
                errorHandler(throwable)
                disposable.dispose()

            })

    }

    fun changeTypeTransortation(order: Orders) {
        Single.fromCallable {
            db.ordersDao().update(order)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe()
    }

    private fun errorHandler(throwable: Throwable) {
        Timber.d("errorHandler")
        when (throwable) {
            is HttpException -> {
                Timber.d("throwable.code()=${throwable.code()}")
                when (throwable.code()) {
                    401 -> {
                        if (view!=null) {
                            view?.errorReceived(throwable)
                        } else {
                            toaster.showErrorToast(R.string.unauthorized)
                        }
                    }
                    else -> toaster.showErrorToast("Ошибка! ${throwable.message}")
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

    fun loadOrders() {
        Timber.d("loadOrders")
        disposable=db.ordersDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Данные_получили_из_БД")
                Timber.d(it.toString())
                Timber.d("view=$view")
                view?.showOrders(it)
                disposable.dispose()
            }

    }

    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }

    /*fun getAllMessage() {
        Timber.d("getAllMessage")
        disposable=apiService.getAllMessages()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({listNotifications ->
                Timber.d("Получили_все_уведомления")
                val unreadNotifications=listNotifications.filter { it.read_date==null }
                Timber.d("unreadNotifications=${unreadNotifications.size}")
                if (unreadNotifications.isNotEmpty()) {
                    view?.showUnreadNotification(unreadNotifications)
                }
                disposable.dispose()
            },{throwable ->
                throwable.printStackTrace()
                disposable.dispose()
            })

    }*/

    /*fun getTechParams(idOrder: Long) {
        Timber.d("techParams=$idOrder")
        Single.fromCallable {
            db.techParamsDao().getTechParamsOrder(idOrder)
            //Timber.d("checkupxxxx=$checkup")
            //view?.checkupIsLoaded(checkup)

        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { techParams ->
                Timber.d("techParams=$techParams")
                view?.techParamsLoaded(techParams)
            }
    }*/

    /*fun importData() {
        // Вставка данных в БД
        Single.fromCallable{
            val order1 = Orders(
                number="A-001",
                //name = "Обследование жилого здания",
                guid = "05d9365f-f176-45a7-b2df-7bf939d0c1e6",
                address = "Нижний Новгород, Россия, 603146, Михайловская улица, 24",
                contactFio = "Иванов Иван Иванович",
                phone = "+79503795388",
                status = "1",
                lat = 56.298322,
                lon = 44.024007,
                dateCreate = SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).parse("30.01.2020")
            )
            db.ordersDao().insert(order1)

            val order2 = Orders(
                number="A-002",
                //name = "Обследование нежилого здания",
                guid = "c4d40211-1687-4485-9b4a-aea7b5353f2b",
                address = "Нижний Новгород, Россия, 603146, улица Ванеева, 147",
                contactFio = "Петров Петр Петрович",
                phone = "+79503795388",
                status = "1",
                lat = 56.299301,
                lon = 44.032029,
                dateCreate = SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).parse("29.01.2020")
            )
            db.ordersDao().insert(order2)
        }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }*/




}