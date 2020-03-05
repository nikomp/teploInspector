package ru.bingosoft.mapquestapp2.ui.order

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.mapquestapp2.db.AppDatabase
import ru.bingosoft.mapquestapp2.db.Orders.Orders
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class OrderPresenter @Inject constructor(val db: AppDatabase) {

    var view: OrderContractView? = null

    lateinit var disposable: Disposable

    fun attachView(view: OrderContractView) {
        this.view=view
    }

    fun loadOrders() {
        Timber.d("loadOrders")
        disposable=db.ordersDao().getAll()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Данные получили из БД")
                Timber.d(it.toString())
                view?.showOrders(it)
            }

        Timber.d("ОК")

    }

    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }

    fun importData() {
        // Вставка данных в БД
        Single.fromCallable{
            val order1 = Orders(
                number="A-001",
                name = "Обследование жилого здания",
                guid = "05d9365f-f176-45a7-b2df-7bf939d0c1e6",
                adress = "Нижний Новгород, Россия, 603146, Михайловская улица, 24",
                contactFio = "Иванов Иван Иванович",
                phone = "+79503795388",
                state = "1",
                lat = 56.298322,
                lon = 44.024007,
                dateCreate = SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).parse("30.01.2020")
            )
            db.ordersDao().insert(order1)

            val order2 = Orders(
                number="A-002",
                name = "Обследование нежилого здания",
                guid = "c4d40211-1687-4485-9b4a-aea7b5353f2b",
                adress = "Нижний Новгород, Россия, 603146, улица Ванеева, 147",
                contactFio = "Петров Петр Петрович",
                phone = "+79503795388",
                state = "1",
                lat = 56.299301,
                lon = 44.032029,
                dateCreate = SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).parse("29.01.2020")
            )
            db.ordersDao().insert(order2)
        }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }


}