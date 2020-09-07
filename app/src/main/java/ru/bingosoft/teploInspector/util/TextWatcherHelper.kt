package ru.bingosoft.teploInspector.util

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import ru.bingosoft.teploInspector.models.Models
import java.util.*

class TextWatcherHelper(private val control: Models.TemplateControl, private val uiCreator: UICreator, val v: View): TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        control.answered = s.toString().isNotEmpty()
        control.resvalue=s.toString()
        control.datetime= Date().time
        //uiCreator.changeChecked(v,control)

        /*val parent=control.parent
        if (parent!=null) {
            Timber.d("parent=${parent.id}")
            val parentView=uiCreator.parentFragment.rootView.findViewById<View>(parent.id)
            parent.checked=false
            uiCreator.changeChecked(parentView, parent)
        }*/

    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        //TODO реализую при необходимости
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        //TODO реализую при необходимости
    }
}