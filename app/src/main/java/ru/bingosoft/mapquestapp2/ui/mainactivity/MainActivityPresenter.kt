package ru.bingosoft.mapquestapp2.ui.mainactivity

import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.api.ApiService
import ru.bingosoft.mapquestapp2.db.AppDatabase
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.models.Models
import ru.bingosoft.mapquestapp2.util.PhotoHelper
import ru.bingosoft.mapquestapp2.util.ThrowHelper
import timber.log.Timber
import javax.inject.Inject

class MainActivityPresenter @Inject constructor(val db: AppDatabase, private val photoHelper: PhotoHelper) {
    var view: MainActivityContractView? = null

    @Inject
    lateinit var apiService: ApiService

    private lateinit var disposable: Disposable
    private lateinit var checkupsWasSync: List<Checkup>

    fun attachView(view: MainActivityContractView) {
        this.view=view
    }

    fun sendData() {
        Timber.d("sendData")

        disposable =
            db.checkupDao()
                .getResultAll()
                .subscribeOn(Schedulers.io())
                .takeWhile{
                    if (it.isEmpty()) {
                        throw ThrowHelper("Нет данных для передачи на сервер")
                    } else {
                        it.isNotEmpty()
                    }
                }
                .map { checkups ->
                    val actionBody = "reverseSync".toRequestBody("multipart/form-data".toMediaType())
                    val jsonBody=Gson().toJson(checkups)
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                    checkupsWasSync=checkups // Сохраняю данные, которые должны быть переданы

                    return@map actionBody to jsonBody
                }
                .flatMap { actionAndJsonBodies ->
                    Timber.d(actionAndJsonBodies.toString())
                    // Архив с файлами
                    var fileBody: MultipartBody.Part? = null

                    val syncDirs=getDirForSync(checkupsWasSync)

                    val zipF= photoHelper.prepareZip(syncDirs)
                    if (zipF!=null) {
                        Timber.d("Есть ZIP отправляем ${zipF.name}")

                        val requestBody = zipF.asRequestBody("multipart/form-data".toMediaType())
                        fileBody = MultipartBody.Part.createFormData(
                            "zip", zipF.name,
                            requestBody
                        )

                    } else {
                        Timber.d( "zipFile == null")
                    }
                    apiService.doReverseSync(actionAndJsonBodies.first, actionAndJsonBodies.second, fileBody).toFlowable()
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response ->
                        Timber.d(response.toString())
                        view?.dataSyncOK() // Пометим чеклисты как переданные
                        view?.showMainActivityMsg(R.string.msgDataSendOk)

                    }, { throwable ->
                        throwable.printStackTrace()
                        if (throwable is ThrowHelper) {
                            view?.showMainActivityMsg("${throwable.message}")
                        } else {
                            view?.showMainActivityMsg(R.string.msgDataSendError)
                        }
                    }
                )
    }

    fun updData() {
        Timber.d("updData")
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
        Single.fromCallable{
            Timber.d("Single")
            checkupsWasSync.forEach {
                it.sync=true
                db.checkupDao().update(it)
            }
        }
        .subscribeOn(Schedulers.io())
        .subscribe()
    }

    private fun getDirForSync(checkups: List<Checkup>) :List<String> {
        Timber.d("getDirForSync")
        val list= mutableListOf<String>()
        checkups.forEach { it ->
            val controlList = Gson().fromJson(it.textResult, Models.ControlList::class.java)
            val controlPhoto=controlList.list.filter { it.type=="photo" }
            controlPhoto.forEach{
                list.add(it.resvalue)
            }
        }

        return list
    }



    fun onDestroy() {
        this.view = null
        if (this::disposable.isInitialized) {
            disposable.dispose()
        }
    }

}