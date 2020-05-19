package ru.bingosoft.teploInspector.ui.checkup

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.AppDatabase
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.UICreator
import timber.log.Timber
import javax.inject.Inject

class CheckupPresenter @Inject constructor(val db: AppDatabase) {
    var view: CheckupContractView? = null

    private lateinit var disposable: Disposable

    fun attachView(view: CheckupContractView) {
        this.view=view
    }

    fun loadCheckup(id: Long) {
        Timber.d("loadCheckups")
        disposable=db.checkupDao().getById(id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("Обследования получили из БД")
                Timber.d(it.toString())

                view?.dataIsLoaded(it)
            }

        Timber.d("ОК")

    }

    fun saveCheckup(uiCreator: UICreator) {
        Timber.d("Сохраняем данные чеклиста")

        // Проверим нет ли групповых контролов
        val groupControls=uiCreator.controlList.list.filter { it.type=="group_questions" }
        if (groupControls.size>0) {
            groupControls.forEach {
                //Сохраним групповы чеклисты
                val controlList2 = it.groupControlList
                Timber.d("${controlList2?.list?.get(0)?.list?.get(0)?.resvalue}")
                val gson= GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create()
                val resCheckup =gson.toJson(controlList2)
                it.resvalue=resCheckup.toString()

                // Заменим в главном чеклисте групповой чеклист
                val index=uiCreator.controlList.list.indexOf(it)
                if (index>-1) {
                    Timber.d("ОК!!")
                    uiCreator.controlList.list[index] = it
                }


            }
        }
        Timber.d("groupControls=${groupControls.size}")
        Timber.d("uiCreator.controlList=${uiCreator.controlList.list[0].groupControlList?.list?.get(0)?.list?.get(0)?.resvalue}")

        // Исключаем ненужные поля
        val gson= GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
        val resCheckup= gson.toJsonTree(uiCreator.controlList, Models.ControlList::class.java)

        Timber.d("resCheckup=$resCheckup")
        uiCreator.checkup.textResult=resCheckup as JsonObject


        disposable=Single.fromCallable{
            db.checkupDao().insert(uiCreator.checkup)
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({_->
            view?.showCheckupMessage(R.string.msgSaveCheckup)
        },{trowable ->
            trowable.printStackTrace()
        })
    }

    fun saveCheckup(controlList: Models.ControlList, checkup: Checkup) {
        Timber.d("Сохраняем данные чеклиста")
        Timber.d("controlList=${controlList.list[1].type}")
        Timber.d("controlList=${controlList.list[1].subcheckup[0]}")
        //Timber.d("controlList=${controlList.list[1].subcheckup[1]}")

        // Исключаем ненужные поля
        val gson= GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
        val resCheckup= gson.toJsonTree(controlList, Models.ControlList::class.java)
        checkup.textResult=resCheckup as JsonObject

        disposable=Single.fromCallable{
            db.checkupDao().insert(checkup)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({_->
                view?.showCheckupMessage(R.string.msgSaveCheckup)
            },{trowable ->
                trowable.printStackTrace()
            })
    }

    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }

    }
}