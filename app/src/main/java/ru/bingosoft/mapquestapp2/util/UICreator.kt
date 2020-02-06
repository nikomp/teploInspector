package ru.bingosoft.mapquestapp2.util

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
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

    fun create() {
        //val tc= Gson().fromJson(checkup.text, Models.TemplateControl::class.java)
        val controlList=Gson().fromJson(checkup.text, Models.ControlList::class.java)
        Timber.d(controlList.controls[0].type)

        controlList.controls.forEach{
            when (it.type) {
                "combobox"->{
                    Timber.d("генерим combobox")

                    val templateSpinner=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_material_spinner, rootView.parent as ViewGroup?, false) as LinearLayout

                    templateSpinner.id=it.id
                    templateSpinner.findViewById<TextView>(R.id.question).text=it.question

                    Timber.d(it.question)

                    val materialSpinner=templateSpinner.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)

                    doAssociateParent(templateSpinner, rootView.findViewById(R.id.mainControl))

                    val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
                        rootView.context,
                        R.layout.template_multiline_spinner_item,
                        it.value
                    )

                    // Заполним spinner
                    materialSpinner.setAdapter(spinnerArrayAdapter)

                }
                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
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
}