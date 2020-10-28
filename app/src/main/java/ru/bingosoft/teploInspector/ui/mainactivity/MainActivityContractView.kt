package ru.bingosoft.teploInspector.ui.mainactivity

interface MainActivityContractView {
    fun showMainActivityMsg(resID: Int)
    fun showMainActivityMsg(msg: String)
    fun dataSyncOK()
    fun updDataOK()
    fun filesSend(countFiles: Int, indexCurrentFile: Int)
    fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String)
    fun saveToken(token: String)

    fun errorReceived(throwable: Throwable)
}