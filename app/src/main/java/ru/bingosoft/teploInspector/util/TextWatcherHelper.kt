package ru.bingosoft.teploInspector.util

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import ru.bingosoft.teploInspector.models.Models
import java.util.*

class TextWatcherHelper(private val control: Models.TemplateControl, val view: View): TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        control.answered = s.toString().isNotEmpty()
        if (control.type=="numeric") {
            if (s.toString().isNotEmpty()) {
                control.resvalue=s?.toString()?.replace(',', '.')
            } else {
                control.resvalue=null
            }

        } else {
            control.resvalue=s.toString()
        }

        control.datetime= Date().time
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        //TODO реализую при необходимости
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        //TODO реализую при необходимости
    }
}