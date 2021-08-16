package ru.bingosoft.teploInspector.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import dagger.android.AndroidInjection
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import javax.inject.Inject


class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private var stUrl: String = ""
    private var stLogin: String = ""
    private var stPassword: String = ""
    private var isEntering: Boolean = false

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var sharedPref: SharedPrefSaver

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val toolbar: Toolbar =findViewById(R.id.logintoolbar)
        setSupportActionBar(toolbar)

        // Попытка решить ошибку в крашлитикс Editor$SelectionModifierCursorController.getMinTouchOffset()
        // Подробнее тут https://stackoverflow.com/questions/53435237/nullpointerexception-int-android-widget-editorselectionmodifiercursorcontrolle
        // https://question-it.com/questions/1614991/nullpointerexception-int-androidwidgeteditor-selectionmodifiercursorcontrollergetmintouchoffset
        (findViewById<TextInputEditText>(R.id.edUrl)).setOnLongClickListener { true }

        val cbEnter=findViewById<CheckBox>(R.id.cbEnter)

        if (sharedPref.getLogin().isNotEmpty()) {
            Timber.d("LOGIN_exist")
            findViewById<TextInputEditText>(R.id.edLogin).setText(sharedPref.getLogin())
        }
        if (sharedPref.getPassword().isNotEmpty()) {
            findViewById<TextInputEditText>(R.id.edPassword).setText(sharedPref.getPassword())
        }
        cbEnter.isChecked = sharedPref.getEnterType().isNotEmpty() && sharedPref.getEnterType()=="directory_service"
    }

    override fun onBackPressed() {
        if (isEntering) {
            super.onBackPressed()
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.btnGo -> {
                    if (isNetworkConnected()) {
                        Timber.d("LoginActivity_onClick")
                        isEntering=true

                        stUrl = findViewById<TextInputEditText>(R.id.edUrl).text.toString()
                        stLogin = findViewById<TextInputEditText>(R.id.edLogin).text.toString()
                        stPassword = findViewById<TextInputEditText>(R.id.edPassword).text.toString()

                        var stEnterType="default"
                        if (findViewById<CheckBox>(R.id.cbEnter).isChecked) {
                            stEnterType="directory_service"
                        }
                        sharedPref.saveEnterType(stEnterType)

                        // Авторизация
                        if (stUrl.isNotEmpty() && stLogin.isNotEmpty() && stPassword.isNotEmpty()) {
                            val intent = Intent()
                            intent.putExtra("login", stLogin)
                            intent.putExtra("password", stPassword)
                            intent.putExtra("url", stUrl)
                            intent.putExtra("enter_type", stEnterType)
                            setResult(Activity.RESULT_OK, intent)

                            this.finish()
                        } else {
                            toaster.showErrorToast(getString(R.string.fill_all_fields))
                        }


                    } else {
                        toaster.showErrorToast(R.string.not_internet)
                    }
                }
            }
        }

    }

    @Suppress("DEPRECATION") //activeNetworkInfo, isConnected - deprecated, нужно для старых API
    private fun isNetworkConnected(): Boolean {
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager?.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //для других устройств, которые умеют соединяться с Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager?.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }


}