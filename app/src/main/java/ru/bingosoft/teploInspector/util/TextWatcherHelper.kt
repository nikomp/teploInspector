package ru.bingosoft.teploInspector.util

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import ru.bingosoft.teploInspector.models.Models
import timber.log.Timber
import java.util.*

class TextWatcherHelper(private val control: Models.TemplateControl, val view: View): TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        if (s!=null) {
            control.answered = s.toString().isNotEmpty()
            when (control.type) {
                "numeric"-> {
                    if (s.toString().isNotEmpty()) {
                        control.resvalue= s.toString().replace(',', '.')
                    } else {
                        control.resvalue=null
                    }
                }
                "combobox" -> {
                    if (s.toString().isNotEmpty() && s.toString().contains("\n")) {
                        control.resvalue=s.toString().replace("\n", "")
                    } else {
                        control.resvalue=s.toString()
                    }
                }
                else -> {
                    control.resvalue=s.toString()
                }
            }

            control.datetime= Date().time
            Timber.d("ControlXZ_$control")
        } else {
            control.answered = false
            control.resvalue="null"
        }

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}