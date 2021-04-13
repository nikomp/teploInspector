package ru.bingosoft.teploInspector.ui.checkup

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.AddLoad.AddLoad
import timber.log.Timber

class AdditionalLoad(private val lists: List<AddLoad>, private val rootView: View) {
    private var listllGroupAL: MutableList<LinearLayout> = mutableListOf()
    val ctx: Context =rootView.context

    fun create() {

        lists.forEach { al ->

            var parentGroup: LinearLayout?=null

            // Создадим группу по Системе потребления
            if (al.system_consumption.isNotEmpty()) {
                val llGroup=if (parentGroup==null) {
                    createGroup(al.system_consumption, rootView.findViewById(R.id.llMain))
                } else {
                    createGroup(al.system_consumption, parentGroup, parentGroup.tag.toString())
                }
                parentGroup=llGroup
            }

            // Создадим группу по назначению
            if (al.purpose.isNotEmpty()) {
                val llGroup=createGroup(al.purpose, parentGroup!!, al.code.toString())
                parentGroup=llGroup
            }

            if (al.contractor==null){
                // Выводим строки Код, Принадлежность, Нагрузка
                val llGroup=if (parentGroup==null) {
                    rootView.findViewById(R.id.llMain)
                }else {
                    parentGroup
                }

                if (parentGroup==null) {
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(24, 0, 24, 0)
                    llGroup.layoutParams = params
                }

                val templateStepCode=fillData(ctx.getString(R.string.code), al.code.toString())
                doAssociateParent(templateStepCode, llGroup)
                val templateStepAffiliation=fillData(ctx.getString(R.string.affiliation), al.affiliation.toString())
                doAssociateParent(templateStepAffiliation, llGroup)
                val templateStepLoad=fillData(ctx.getString(R.string.load), String.format("%.4f", al.loading))
                doAssociateParent(templateStepLoad, llGroup)
            } else {
                val llGroup=createGroup(ctx.getString(R.string.contractors), parentGroup!!, al.code.toString())
                val templateStepCode=fillData(ctx.getString(R.string.contractor), al.contractor.toString())
                doAssociateParent(templateStepCode, llGroup)
                val templateStepAffiliation=fillData(ctx.getString(R.string.load), String.format("%.4f", al.loading))
                doAssociateParent(templateStepAffiliation, llGroup)
            }

        }
    }

    private fun fillData(name: String, value: String): LinearLayout {
        val templateStep= LayoutInflater.from(rootView.context).inflate(
            R.layout.item_additional_load, rootView.parent as ViewGroup?, false
        ) as LinearLayout

        templateStep.findViewById<TextView>(R.id.gi_name).text=name
        templateStep.findViewById<TextView>(R.id.gi_value_edit).text = value

        val layoutParams= LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        layoutParams.bottomMargin=4

        templateStep.findViewById<LinearLayout>(R.id.txll).layoutParams=layoutParams

        return templateStep
    }

    private fun createGroup(name: String, parent: LinearLayout, node: String = ""): LinearLayout {
        // Проверим, возможно группа уже создана
        val ll=listllGroupAL.filter { it.tag=="$name$node" }
        if (ll.isNotEmpty()) {
            return ll[0]
        } else {
            Timber.d("генерим группу $name$node")
            val templateStep= LayoutInflater.from(rootView.context).inflate(
                R.layout.template_group_al, parent as ViewGroup?, false
            ) as LinearLayout


            templateStep.findViewById<TextView>(R.id.question).text=name

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llGroup=templateStep.findViewById<LinearLayout>(R.id.containerTh)
            llGroup.tag="$name$node" // Сохраним имя группы
            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

            clTitle.setOnClickListener {
                if (llGroup.visibility==View.VISIBLE) {
                    llGroup.visibility=View.GONE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.arrow_up
                        )
                    )
                } else {
                    llGroup.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.arrow_down
                        )
                    )
                }
            }

            doAssociateParent(templateStep, parent)

            listllGroupAL.add(llGroup)


            return llGroup
        }


    }

    /**
     * Метод, в котором осуществляется привязка дочернего View к родительскому
     */
    private fun doAssociateParent(v: View, mainView: View, index: Int? = null){
        if (mainView is LinearLayout) {
            if (index!=null) {
                mainView.addView(v, index)
            } else {
                mainView.addView(v)
            }

        }
    }
}