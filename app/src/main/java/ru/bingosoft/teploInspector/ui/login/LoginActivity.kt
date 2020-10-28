package ru.bingosoft.teploInspector.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_login.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.util.SharedPrefSaver
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import javax.inject.Inject


class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private var stUrl: String = ""
    private var stLogin: String = ""
    private var stPassword: String = ""

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var sharedPref: SharedPrefSaver

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val toolbar=logintoolbar
        setSupportActionBar(toolbar)

        if (sharedPref.getLogin().isNotEmpty()) {
            edLogin.setText(sharedPref.getLogin())
        }
        if (sharedPref.getPassword().isNotEmpty()) {
            edPassword.setText(sharedPref.getPassword())
        }


    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.btnGo -> {
                    if (isNetworkConnected()) {
                        Timber.d("LoginActivity onClick")

                        stUrl = edUrl.text.toString()
                        stLogin = edLogin.text.toString()
                        stPassword = edPassword.text.toString()

                        // Авторизация
                        val intent = Intent()
                        intent.putExtra("login", stLogin)
                        intent.putExtra("password", stPassword)
                        intent.putExtra("url", stUrl)
                        setResult(Activity.RESULT_OK, intent)

                        this.finish()
                    } else {
                        toaster.showToast(R.string.not_internet)
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