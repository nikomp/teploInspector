package ru.bingosoft.mapquestapp2.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*
import ru.bingosoft.mapquestapp2.R

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private var stUrl: String = ""
    private var stLogin: String = ""
    private var stPassword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val toolbar=logintoolbar
        setSupportActionBar(toolbar)

        stUrl = edUrl.text.toString()
        stLogin = edLogin.text.toString()
        stPassword = edPassword.text.toString()
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.btnGo -> {

                    // Авторизация
                    val intent = Intent()
                    intent.putExtra("login", stLogin)
                    intent.putExtra("password", stPassword)
                    intent.putExtra("url", stUrl)
                    setResult(Activity.RESULT_OK, intent)

                    this.finish()
                }
            }
        }

    }
}