package ru.bingosoft.mapquestapp2.ui.login

interface LoginContractView {
    fun showMessageLogin(resID: Int)
    fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String)
}