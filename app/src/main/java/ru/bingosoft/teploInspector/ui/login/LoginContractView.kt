package ru.bingosoft.teploInspector.ui.login

import ru.bingosoft.teploInspector.models.Models
import java.util.*

interface LoginContractView {
    fun showMessageLogin(resID: Int)
    fun showMessageLogin(msg: String)
    fun showOrders()
    fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String)
    fun saveToken(token: String)
    fun showFailureTextView(failureMessage: String="")
    fun showAlertNotInternet()
    fun alertRepeatSync()
    fun saveDateSyncToSharedPreference(date: Date)
    fun saveInfoUserToSharedPreference(user: Models.User)
    //fun startLocationService()

    fun startNotificationService(token: String)
    fun checkMessageId()
    fun getAllMessage()

    fun errorReceived(throwable: Throwable)

    fun registerReceiverMainActivity()

    fun sendMessageUserLogged()

    fun saveAppVersionName()
    fun startFinishWorker()

}