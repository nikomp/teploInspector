package ru.bingosoft.teploInspector.ui.login

import ru.bingosoft.teploInspector.models.Models
import java.util.*

interface LoginContractView {
    fun showMessageLogin(resID: Int)
    fun showMessageLogin(msg: String)
    fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String)
    fun showFailureTextView()
    fun alertRepeatSync()
    fun saveDateSyncToSharedPreference(date: Date)
    fun saveInfoUserToSharedPreference(user: Models.User)
}