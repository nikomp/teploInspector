package ru.bingosoft.teploInspector.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import org.json.JSONObject
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.APP_PREFERENCES
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.DATESYNC
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.ENTER_TYPE
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.FIREBASE_MESSAGE
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.LOGIN
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.PASSWORD
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.ROLE_ID
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.TOKEN
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.USER_FULLNAME
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.USER_ID
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.USER_PHOTO_URL
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.VERSION_NAME
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class SharedPrefSaver(ctx: Context) {
    var sptoken: String=""
    private val sharedPreference: SharedPreferences = ctx.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)


    fun saveLogin(login: String) {
        Timber.d("saveLogin")
        val editor: SharedPreferences.Editor = sharedPreference.edit()
        editor.putString(LOGIN, login)
        editor.apply()
    }

    fun getLogin(): String {
        return sharedPreference.getString(LOGIN, "") ?: ""
    }

    fun saveEnterType(type: String="default") {
        Timber.d("saveEnterType")
        val editor: SharedPreferences.Editor = sharedPreference.edit()
        editor.putString(ENTER_TYPE, type)
        editor.apply()
    }

    fun getEnterType(): String {
        return sharedPreference.getString(ENTER_TYPE, "") ?: ""
    }

    fun saveToken(token: String) {
        val editor: SharedPreferences.Editor = sharedPreference.edit()
        editor.putString(TOKEN, token)

        //#token #user_id
        val strTemp=token.split(".")
        Timber.d(strTemp.toString())
        //val decodedBytes = Base64.getDecoder().decode(strTemp[1])
        val decodedBytes = Base64.decode(strTemp[1],Base64.DEFAULT)
        val decodedString = String(decodedBytes)

        val tokenObject = JSONObject(decodedString)
        Timber.d("${tokenObject.getJSONObject("user").get("id")}")
        editor.putInt(USER_ID, tokenObject.getJSONObject("user").getInt("id"))
        editor.putInt(ROLE_ID,tokenObject.getJSONObject("user").getInt("role_id"))

        editor.apply()
    }

    fun getUserId(): Int {
        return sharedPreference.getInt(USER_ID,0)
    }

    fun getToken(): String {
        return sharedPreference.getString(TOKEN, "") ?: ""
    }

    fun savePassword(password: String) {
        /*val coder = Coder(ctx) // Создадим экземпляр шифратора
        //ШИФРУЕМ
        val eList = coder.encode(password)*/

        val editor: SharedPreferences.Editor = this.sharedPreference.edit()
        editor.putString(PASSWORD, password) //eList[0]
        //editor.putString(IVPASS, ) //eList[1]
        editor.apply()

    }


    fun getPassword(): String {
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

    fun clearAuthData() {
        val editor: SharedPreferences.Editor = this.sharedPreference.edit()
        editor.remove("login")
        editor.remove("password")
        editor.apply()
    }

    fun saveDateSyncDB(date: Date) {
        //СОХРАНИМ
        val editor: SharedPreferences.Editor = this.sharedPreference.edit()

        val dateFormat =
            SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("ru","RU"))
        try {
            val dateTime = dateFormat.format(date)
            editor.putString(DATESYNC, dateTime)
            editor.apply()
        } catch (e: ParseException) {
            e.printStackTrace()
        }

    }

    fun getDateSyncDB():String? {
         if (sharedPreference.contains(DATESYNC)) {
            return sharedPreference.getString(DATESYNC,"")
        }
        return ""
    }

    fun saveUser(user: Models.User) {
        //СОХРАНИМ
        val editor: SharedPreferences.Editor = this.sharedPreference.edit()
        editor.putString(USER_FULLNAME, user.fullname)
        editor.putString(USER_PHOTO_URL, user.photoUrl)
        editor.apply()
    }

    fun getUser(): Models.User {
        val user=Models.User()

        if (sharedPreference.contains(USER_FULLNAME)) {
            user.fullname=sharedPreference.getString(USER_FULLNAME, "") ?: ""
        }
        if (sharedPreference.contains(USER_PHOTO_URL)) {
            user.photoUrl=sharedPreference.getString(USER_PHOTO_URL, "") ?: ""
        }

        return user
    }

    fun getTokenGCM() :String {
        return if (sharedPreference.contains(FIREBASE_MESSAGE)) {
            sharedPreference.getString(FIREBASE_MESSAGE, "") ?: ""
        } else {
            ""
        }
    }

    fun saveVersionName(version: String) {
        this.sharedPreference.edit().putString(VERSION_NAME, version).apply()
    }

    fun getVersionName() :String {
        return if (sharedPreference.contains(VERSION_NAME)) {
            sharedPreference.getString(VERSION_NAME, "") ?: ""
        } else {
            ""
        }
    }

}