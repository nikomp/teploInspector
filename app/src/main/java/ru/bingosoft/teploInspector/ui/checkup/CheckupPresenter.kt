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
    private lateinit var disposableTH: Disposable
    private lateinit var disposableAL: Disposable

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

    /*fun saveGeneralInformationOrder(orders: Orders) {
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
    }*/

    fun saveCheckup(uiCreator: UICreator, send:Boolean=true) {
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
            if (send) {
                view?.showCheckupMessage(R.string.msgSaveCheckup)
            }
            updateAnsweredCount(uiCreator)
        },{error ->
            error.printStackTrace()
            view?.errorReceived(error)
        })
    }

    /*fun updateCheckup(uiCreator: UICreator) {
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
                updateAnsweredCount(uiCreator)
                Timber.d("Обновили_данные_после очищения_папки_с_фото")
            },{error ->
                error.printStackTrace()
                view?.errorReceived(error)
            })
    }*/

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

    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }

    fun getTechParams(idOrder: Long) {
        Timber.d("getTechParams")
        Timber.d("techParams=$idOrder")
        disposableTH=Single.fromCallable {
            db.techParamsDao().getTechParamsOrder(idOrder)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ techParams ->
                disposableTH.dispose()
                view?.techParamsLoaded(techParams)
            },{ throwable ->
                Timber.d("th_error $throwable")
                throwable.printStackTrace()
                disposableTH.dispose()
                view?.errorReceived(throwable)
            })
    }

    fun getAddLoads(idOrder: Long) {
        Timber.d("getAddLoads")
        disposableAL=Single.fromCallable {
            db.addLoadDao().getAddLoadOrder(idOrder)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe ({ addLoads ->
                disposableAL.dispose()
                view?.addLoadsLoaded(addLoads)
            },{ throwable ->
                Timber.d("al_error $throwable")
                throwable.printStackTrace()
                disposableAL.dispose()
                view?.errorReceived(throwable)
            })
    }
}