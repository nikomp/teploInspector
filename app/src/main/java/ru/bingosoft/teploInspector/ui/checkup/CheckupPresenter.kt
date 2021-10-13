package ru.bingosoft.teploInspector.ui.checkup

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
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

    private var compositeDisposable= CompositeDisposable()

    fun attachView(view: CheckupContractView) {
        this.view=view
    }

    fun loadCheckupByOrder(orderId: Long) {
        Timber.d("loadCheckupByOrder orderId=$orderId")
        // Получим информацию о чеклисте, по orderId
        val disposable=Single.fromCallable {
            db.checkupDao().getCheckupByOrder(orderId)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { checkup ->
                    view?.dataIsLoaded(checkup)
                }, { throwable ->
                    throwable.printStackTrace()
                    view?.errorReceived(Throwable("Чеклист пуст"))
                }
            )
        compositeDisposable.add(disposable)

    }

    fun saveCheckup(uiCreator: UICreator, send: Boolean = true) {
        Timber.d("Сохраняем данные чеклиста")
        //#sleep_без_блокировки_UI
        /*val handler = Handler()
        handler.postDelayed(Runnable {
            // Actions to do after 10 seconds
        }, 10000)*/

        Timber.d("controlListCheckup_${uiCreator.controlList.size}_${uiCreator.controlList}")

        val listType: Type = object : TypeToken<List<Models.TemplateControl?>?>() {}.type

        // Исключаем ненужные поля
        val gson= GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
        val resCheckup= gson.toJsonTree(uiCreator.controlList, listType)

        Timber.d("resCheckup=$resCheckup")
        uiCreator.checkup.textResult=resCheckup as JsonArray


        val disposable=Single.fromCallable{
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
        compositeDisposable.add(disposable)
    }

    private fun updateAnsweredCount(uiCreator: UICreator) {
        // Отфильтруем только вопросы у которых answered=true
        val filterControls = uiCreator.controlList.filter { it.answered }

        val disposable = Single.fromCallable {
            db.ordersDao().updateAnsweredCount(uiCreator.checkup.idOrder, filterControls.size)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                view?.setAnsweredCount(filterControls.size)
            }, { throwable ->
                view?.errorReceived(throwable)
            })
        compositeDisposable.add(disposable)

    }

    fun onDestroy() {
        this.view = null
        /*if (this::disposable.isInitialized) {
            disposable.dispose()
        }*/
        compositeDisposable.dispose()
    }

    fun getTechParams(idOrder: Long) {
        Timber.d("getTechParams")
        Timber.d("techParams=$idOrder")
        val disposableTH=Single.fromCallable {
            db.techParamsDao().getTechParamsOrder(idOrder)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ techParams ->
                //disposableTH.dispose()
                view?.techParamsLoaded(techParams)
            }, { throwable ->
                Timber.d("th_error $throwable")
                throwable.printStackTrace()
                //disposableTH.dispose()
                view?.errorReceived(throwable)
            })
        compositeDisposable.add(disposableTH)
    }

    fun getAddLoads(idOrder: Long) {
        Timber.d("getAddLoads")
        val disposableAL=Single.fromCallable {
            db.addLoadDao().getAddLoadOrder(idOrder)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ addLoads ->
                //disposableAL.dispose()
                view?.addLoadsLoaded(addLoads)
            }, { throwable ->
                Timber.d("al_error $throwable")
                throwable.printStackTrace()
                //disposableAL.dispose()
                view?.errorReceived(throwable)
            })
        compositeDisposable.add(disposableAL)
    }
}