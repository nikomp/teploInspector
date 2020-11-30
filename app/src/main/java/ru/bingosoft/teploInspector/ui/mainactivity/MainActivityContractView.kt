package ru.bingosoft.teploInspector.ui.mainactivity

interface MainActivityContractView {
    fun showMainActivityMsg(resID: Int)
    fun showMainActivityMsg(msg: String)
    fun dataSyncOK(idOrder: Long?)
    fun dataNotSync(idOrder: Long, throwable: Throwable)
    //fun updDataOK()
    fun filesSend(countFiles: Int, indexCurrentFile: Int)
    fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String)
    fun saveToken(token: String)
    fun startNotificationService(token: String)

    fun checkMessageId()
    fun setEmptyMessageId()

    fun setIdsOrdersNotSync(list: List<Long>)


    fun errorReceived(throwable: Throwable)

    fun refreshRecyclerView()

    fun registerReceiver()
}