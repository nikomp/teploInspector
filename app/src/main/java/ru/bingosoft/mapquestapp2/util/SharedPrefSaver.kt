package ru.bingosoft.mapquestapp2.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import ru.bingosoft.mapquestapp2.util.Const.LogTags.SPS
import ru.bingosoft.mapquestapp2.util.Const.SharedPrefConst.APP_PREFERENCES
import ru.bingosoft.mapquestapp2.util.Const.SharedPrefConst.LOGIN
import ru.bingosoft.mapquestapp2.util.Const.SharedPrefConst.PASSWORD
import ru.bingosoft.mapquestapp2.util.Const.SharedPrefConst.SESSION
import timber.log.Timber

class SharedPrefSaver(ctx: Context) {
    private val sharedPreference: SharedPreferences = ctx.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)


    fun saveLogin(login: String) {
        Timber.d("saveLogin")
        val editor: SharedPreferences.Editor = sharedPreference.edit()
        editor.putString(LOGIN, login)
        editor.apply()
        Log.d(SPS, login)

    }

    fun getLogin(): String {
        return sharedPreference.getString(LOGIN, "") ?: ""
    }

    fun savePassword(password: String) {
        Log.d(SPS, "savePassword")

        /*val coder = Coder(ctx) // Создадим экземпляр шифратора
        //ШИФРУЕМ
        val eList = coder.encode(password)*/

        val editor: SharedPreferences.Editor = this.sharedPreference.edit()
        editor.putString(PASSWORD, password) //eList[0]
        //editor.putString(IVPASS, ) //eList[1]
        editor.apply()

    }


    fun getPassword(): String {
        Log.d(SPS, "getPassword")
        //СЧИТАЕМ
        if (sharedPreference.contains(PASSWORD)) {
            /*val coder = Coder(ctx) // Создадим экземпляр шифратора

            return coder.decode(
                sharedPreference.getString(PASSWORD, "") ?: "",
                sharedPreference.getString(IVPASS, "") ?: ""
            )*/

            return sharedPreference.getString(PASSWORD, "") ?: ""
        }

        return ""

    }

    fun saveSessionId(sessionId: String) {
        Log.d(SPS, "saveSessionId")
        //СОХРАНИМ
        val editor: SharedPreferences.Editor = this.sharedPreference.edit()
        editor.putString(SESSION, sessionId)
        editor.apply()
    }

    fun getSessionId(): String? {
        Log.d(SPS, "getSessionId")
        //СЧИТАЕМ
        if (sharedPreference.contains(SESSION)) {
            return sharedPreference.getString(SESSION, "")
        }

        return ""

    }
}