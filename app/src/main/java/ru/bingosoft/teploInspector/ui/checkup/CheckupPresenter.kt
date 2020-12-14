package ru.bingosoft.teploInspector.ui.checkup

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.UICreator
import timber.log.Timber
import java.lang.reflect.Type
import javax.inject.Inject

class CheckupPresenter @Inject constructor(
    val db: AppDatabase
) {
    var view: CheckupContractView? = null

    private lateinit var disposable: Disposable

    fun attachView(view: CheckupContractView) {
        this.view=view
    }

    /*fun loadCheckup(id: Long) {
        Timber.d("loadCheckups")
        disposable=db.checkupDao().getCheckupByOrderId(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Обследования получили из БД")
                Timber.d(it.toString())

                view?.dataIsLoaded(it)
                disposable.dispose()
            }

        Timber.d("ОК")

    }*/

    fun loadCheckupByOrder(orderId: Long) {
        Timber.d("loadCheckupByOrder orderId=$orderId")
        // Получим информацию о чеклисте, по orderId
        disposable=Single.fromCallable {
            db.checkupDao().getCheckupByOrder(orderId)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { checkup ->
                    Timber.d("checkupxxxx=$checkup")
                    view?.dataIsLoaded(checkup)
                    disposable.dispose()
                },{ throwable ->
                    disposable.dispose()
                    Timber.d("errorX")
                    throwable.printStackTrace()
                    view?.errorReceived(Throwable("Чеклист пуст"))

                }
            )

    }

    fun saveGeneralInformationOrder(orders: Orders) {
        Timber.d("saveGeneralInformationOrder")

        disposable=Single.fromCallable{
            db.ordersDao().update(orders)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                disposable.dispose()
                view?.sendGiOrder()
                view?.doSaveCheckup()
            },{error ->
                disposable.dispose()
                error.printStackTrace()
                view?.errorReceived(error)
            })
    }

    fun saveCheckup(uiCreator: UICreator) {
        Timber.d("Сохраняем данные чеклиста")

        val listType: Type = object : TypeToken<List<Models.TemplateControl?>?>() {}.type

        // Исключаем ненужные поля
        val gson= GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
        val resCheckup= gson.toJsonTree(uiCreator.controlList, listType)

        Timber.d("resCheckup=$resCheckup")
        uiCreator.checkup.textResult=resCheckup as JsonArray


        disposable=Single.fromCallable{
            Timber.d("uiCreator.checkup11=${uiCreator.checkup}")
            db.checkupDao().update(uiCreator.checkup)
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
            view?.showCheckupMessage(R.string.msgSaveCheckup)
            updateAnsweredCount(uiCreator)
        },{error ->
            error.printStackTrace()
            view?.errorReceived(error)
        })
    }

    private fun updateAnsweredCount(uiCreator: UICreator) {
        // Отфильтруем только вопросы у которых answered=true
        val filterControls = uiCreator.controlList.filter { it.answered }

        Timber.d("filterControls=${filterControls.size}")
        Timber.d("uiCreator.checkup=${uiCreator.checkup}")


        disposable = Single.fromCallable {
            Timber.d("uiCreator.checkup=${uiCreator.checkup.idOrder}__${filterControls.size}")
            db.ordersDao().updateAnsweredCount(uiCreator.checkup.idOrder, filterControls.size)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                disposable.dispose()
                view?.setAnsweredCount(filterControls.size)
            },{throwable ->
                disposable.dispose()
                view?.errorReceived(throwable)
            })


    }

    /*private fun updateTechParamsCount(uiCreator: UICreator) {
        // Отфильтруем только вопросы у которых answered=true
        val filterControls = uiCreator.controlList.filter { it.answered }

        Timber.d("filterControls=${filterControls.size}")


        disposable = Single.fromCallable {
            db.ordersDao().updateAnsweredCount(uiCreator.checkup.idOrder, filterControls.size)
        }
            .subscribeOn(Schedulers.io())
            .subscribe({
                view?.setAnsweredCount(filterControls.size)
                disposable.dispose()
            },{throwable ->
                throwable.printStackTrace()
                disposable.dispose()
            })


    }*/

    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }

    fun getTechParams(idOrder: Long) {
        Timber.d("techParams=$idOrder")
        disposable=Single.fromCallable {
            db.techParamsDao().getTechParamsOrder(idOrder)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {throwable ->
                throwable.printStackTrace()
            }
            .subscribe ({ techParams ->
                Timber.d("techParams_size=${techParams.size}")
                Timber.d("techParams_loaded=$techParams")
                view?.techParamsLoaded(techParams)
                disposable.dispose()
            },{ throwable ->
                throwable.printStackTrace()
                disposable.dispose()
            })
    }
}