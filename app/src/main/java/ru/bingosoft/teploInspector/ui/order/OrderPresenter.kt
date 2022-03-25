package ru.bingosoft.teploInspector.ui.order

import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
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
import ru.bingosoft.teploInspector.util.OtherUtil
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
    /*private lateinit var disposable: Disposable
    private lateinit var disposableUpdateState: Disposable
    private lateinit var disposableSendState: Disposable
    private lateinit var disposableRollbackStateOrder: Disposable
    private lateinit var disposableUpdateOrder: Disposable
    private lateinit var disposableDeleteOrder: Disposable*/
    private var compositeDisposable=CompositeDisposable()

    @Inject
    lateinit var otherUtil: OtherUtil
    var isRollbackChangeStateOrder:Boolean=false

    fun attachView(view: OrderContractView) {
        this.view=view
    }

    private fun addHistoryState(order: Orders) {
        val disposable=Single.fromCallable {
            val date=Date()
            val history=HistoryOrderState(id = date.hashCode(), idOrder = order.id, stateOrder = order.status!!, dateChange = date)
            tempHistory=history
            db.historyOrderStateDao().insert(history)
        }
            .subscribeOn(Schedulers.io())
            .subscribe({id ->
                //disposable.dispose()
                Timber.d("inserted ID=$id")
                val history= Models.HistoryOrderOnServer(unique_id=id,
                    idOrder = tempHistory.idOrder,
                    stateOrder = tempHistory.stateOrder,
                    dateChange = tempHistory.dateChange.time)

                sendHistoryToServer(order, history)
            },{throwable ->
                //disposable.dispose()
                throwable.printStackTrace()
            })
        compositeDisposable.add(disposable)

    }

    private fun sendHistoryToServer(order:Orders, history: Models.HistoryOrderOnServer) {
        Timber.d("sendHistoryToServer=$history")
        val jsonBody2 = Gson().toJson(history)
            .toRequestBody("application/json".toMediaType())

        val disposableSendState=apiService.sendStatusOrder(jsonBody2)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                Timber.d("response=$response")
                Timber.d("Отправили_статус")
                if (order.status=="Отменена") {
                    deleteOrder(order)
                }
                otherUtil.writeToFile("Logger_Отправили_статус_заявки на сервер_$order")
                //disposableSendState.dispose()
            },{ throwable ->
                Timber.d("throwable.message=${throwable.message}")
                Timber.d("view=$view")
                throwable.printStackTrace()
                errorHandler(throwable)
                if (!isRollbackChangeStateOrder) {
                    rollbackChangeStateOrder(order)
                } else {
                    Timber.d("RollbackChangeStateOrder_только_один_раз")
                    isRollbackChangeStateOrder=false
                }

                //disposableSendState.dispose()
            })
        compositeDisposable.add(disposableSendState)

    }

    private fun deleteOrder(order: Orders) {
        Timber.d("deleteOrder")
        val disposableDeleteOrder=Single.fromCallable {
            db.ordersDao().delete(order)
        }
            .subscribeOn(Schedulers.io())
            .subscribe({
                Timber.d("Удалили_Отмененную_заявку_в_БД")
                //disposableDeleteOrder.dispose()
            },{
                //disposableDeleteOrder.dispose()
                it.printStackTrace()
            })
        compositeDisposable.add(disposableDeleteOrder)
    }

    /*Возврат к предыдущему состоянию Заявки нужен в случае если отправка данных по заявке уже прошла,
    а при смене состояния на Выполнена произошла ошибка, например нет Интернета. Если не будет возврата к
    предыдущему состоянию заявка пропадет из списка и на сервере данные не обновятся*/
    fun rollbackChangeStateOrder(order: Orders) {
        // Возврат к предыдущему состоянию делаем только если ошибка возникла при переводе в
        // Выполнена либо Отменена, т.к. заявка пропадает из списка и по сути это
        // последние состояния, в которых заявка может быть
        if (order.status=="Выполнена" || order.status=="Отменена") {
            // Получим предыдущее состояние Заявки
            Timber.d("rollbackChangeStateOrder")
            val disposableRollbackStateOrder=Single.fromCallable{
                db.historyOrderStateDao().getPreviousStateByIdOrder(order.id)
            }.subscribeOn(Schedulers.io())
                .subscribe({
                    Timber.d("XCX_$it")
                    otherUtil.writeToFile("Logger_Откатим_изменение_статуса_заявки_${order}_в_$it")
                    Timber.d("Откатим_изменение_статуса_заявки_${order}_в_$it")
                    order.status=it
                    updateOrderState(order)
                    isRollbackChangeStateOrder=true
                    //disposableRollbackStateOrder.dispose()
                },{
                    //disposableRollbackStateOrder.dispose()
                    it.printStackTrace()
                })
            compositeDisposable.add(disposableRollbackStateOrder)
        }

    }

    /*Сначала обновляем данные в локальной БД. Должна быть возсожность отслеживать историю изменения статуса по заявки
    * Нельзя блокировать смену статуса если отсутствует Интернет.*/
    fun updateOrderState(order: Orders) {
        val disposableUpdateState=Single.fromCallable {
            db.ordersDao().update(order)
        }
        .subscribeOn(Schedulers.io())
        .subscribe({
            otherUtil.writeToFile("Logger_Обновили_в_БД_Телефона_статус_заявки_$order")
            Timber.d("Данные_обновили_в_БД_Телефона")
            addHistoryState(order)
            //disposableUpdateState.dispose()
        },{
            //disposableUpdateState.dispose()
            it.printStackTrace()
        })
        compositeDisposable.add(disposableUpdateState)
    }

    fun updateOrder(order: Orders) {
        val disposableUpdateOrder=Single.fromCallable {
            db.ordersDao().update(order)
        }
        .subscribeOn(Schedulers.io())
        .subscribe({
            Timber.d("Данные_обновили_в_БД_Телефона")
        },{
            it.printStackTrace()
        })
        compositeDisposable.add(disposableUpdateOrder)

    }

    fun changeTypeTransportation(order: Orders) {
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
        val disposable=db.ordersDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Timber.d("Данные_получили_из_БД")
                    if (it.isNotEmpty()) {
                        view?.showOrders(it)
                    } else {
                        view?.showFailure(R.string.no_requests)
                    }

                },{
                    it.printStackTrace()
                }
            )
        compositeDisposable.add(disposable)

    }

    fun onDestroy() {
        this.view = null
        /*if (this::disposable.isInitialized) {
            disposable.dispose()
        }*/
        compositeDisposable.dispose()
    }


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