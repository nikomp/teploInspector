package ru.bingosoft.mapquestapp2.util

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.models.Models
import timber.log.Timber

/**
 * Класс, который создает интерфейс Фрагмента Обследования
 */
class UICreator(private val rootView: View, val checkup: Checkup) {
    lateinit var controlList: Models.ControlList

    fun create() {
        controlList=Gson().fromJson(checkup.text, Models.ControlList::class.java)

        controlList.list.forEach controls@ {
            when (it.type) {
                // Выпадающий список
                "combobox"->{
                    Timber.d("генерим combobox")

                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_material_spinner, rootView.parent as ViewGroup?, false) as LinearLayout

                    attachListenerToFab(templateStep,it)


                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val materialSpinner=templateStep.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)

                    doAssociateParent(templateStep, rootView.findViewById(R.id.mainControl))

                    val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
                        rootView.context,
                        R.layout.template_multiline_spinner_item,
                        it.value
                    )

                    // Заполним spinner
                    materialSpinner.setAdapter(spinnerArrayAdapter)
                    // Вешаем обработчик на spinner
                    materialSpinner.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    return@controls

                }
                // Строковое поле ввода однострочное
                "textinput"->{
                    // Строковое поле однострочное
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_textinput, rootView.parent as ViewGroup?, false) as LinearLayout

                    attachListenerToFab(templateStep,it)

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
                    textInputLayout.hint=it.hint


                    val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)
                    textInputEditText.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    doAssociateParent(templateStep, rootView.findViewById(R.id.mainControl))

                    return@controls
                }
                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
                    return@controls
                }
            }
        }

    }

    /**
     * Метод, в котором осуществляется привязка дочернего View к родительскому
     */
    private fun doAssociateParent(v: View, mainView: View){
        if (mainView is LinearLayout) {
            mainView.addView(v)
        }
    }

    /**
     * Найдем кнопку Fab и добавим для нее обработчик
     */
    private fun attachListenerToFab(v: View, control: Models.TemplateControl) {
        Timber.d("attachListenerToFab")

        val fab: FloatingActionButton = v.findViewById(R.id.fabCheck)
        fab.setOnClickListener { view ->
            Timber.d("Прошли шаг чеклиста")

            var isEmpty=false

            // Проверим введено ли значение
            when (control.type) {
                "combobox"->{
                    val controlView=v.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)
                    if (TextUtils.isEmpty(controlView.text.toString())) {
                        controlView.error = "Нужно выбрать значение из списка"
                        isEmpty=true
                    }
                }
                "textinput"->{
                    val controlView=v.findViewById<TextInputEditText>(R.id.tiet)
                    if (TextUtils.isEmpty(controlView.text.toString())) {
                        controlView.error = "Нужно заполнить поле"
                        isEmpty=true
                    }
                }
                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
                }

            }

            if (!isEmpty) {
                control.checked=!control.checked
                changeChecked(v,control)
            }

        }
    }

    /**
     * Сменим цвет шага
     */
    fun changeChecked(v: View, control: Models.TemplateControl) {
        val cardView = v.findViewById<CardView>(R.id.cv)
        if (control.checked) {
            cardView?.setCardBackgroundColor(
                ContextCompat.getColor(
                    v.context,
                    R.color.colorCardSelect
                )
            )
        } else {
            cardView?.setCardBackgroundColor(
                ContextCompat.getColor(
                    v.context,
                    R.color.colorCardItem
                )
            )
        }
    }

}