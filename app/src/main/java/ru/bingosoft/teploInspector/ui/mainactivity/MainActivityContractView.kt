package ru.bingosoft.teploInspector.ui.mainactivity

import ru.bingosoft.teploInspector.models.Models
import java.io.File

interface MainActivityContractView {
    fun showMainActivityMsg(resID: Int)
    fun showMainActivityMsg(msg: String)
    fun dataSyncOK(idOrder: Long?)
    fun dataNotSync(idOrder: Long, throwable: Throwable)
    //fun updDataOK()
    fun filesSend(countFiles: Int, indexCurrentFile: Int)
    fun renameSyncedFiles(files: Array<File>?)
    fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String)
    fun saveToken(token: String)
    fun startNotificationService(token: String)

    fun checkMessageId()
    fun setEmptyMessageId()
    fun getAllMessage()
    fun showUnreadNotification(listNotification: List<Models.Notification>)

    fun setIdsOrdersNotSync(list: List<Long>)


    fun errorReceived(throwable: Throwable)

    fun refreshRecyclerView()

    fun registerReceiver()
    fun enabledSaveButton()

    fun sendMessageUserLogged()



}