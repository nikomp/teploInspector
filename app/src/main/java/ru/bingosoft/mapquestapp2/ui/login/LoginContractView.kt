package ru.bingosoft.mapquestapp2.ui.login

import ru.bingosoft.mapquestapp2.models.Models
import java.util.*

interface LoginContractView {
    fun showMessageLogin(resID: Int)
    fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String)
    fun showFailureTextView()
    fun alertRepeatSync()
    fun saveDateSyncToSharedPreference(date: Date)
    fun saveInfoUserToSharedPreference(user: Models.User)
}