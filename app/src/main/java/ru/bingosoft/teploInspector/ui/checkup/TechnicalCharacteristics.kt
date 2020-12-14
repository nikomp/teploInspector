package ru.bingosoft.teploInspector.ui.checkup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import timber.log.Timber

class TechnicalCharacteristics(private val lists: List<TechParams>, private val rootView: View) {
    private var listllGroupTX: MutableList<LinearLayout> = mutableListOf()

    fun create() {

        lists.forEach { th ->
            if (th.node!=null) {
                val llGroup=createGroup("Узел ${th.node}",rootView.findViewById(R.id.llMain))

                val templateStep=fillDataTH(th)

                doAssociateParent(templateStep,llGroup)

            } else {
                fillDataTH(th)

            }
        }
    }

    private fun fillDataTH(th:TechParams):LinearLayout {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.item_technical_characteristic, rootView.parent as ViewGroup?, false
        ) as LinearLayout

        templateStep.findViewById<TextView>(R.id.gi_name).text=th.technical_characteristic
        templateStep.findViewById<TextView>(R.id.gi_value_edit).setText(th.value)

        val layoutParams= LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)

        layoutParams.bottomMargin=4

        templateStep.findViewById<LinearLayout>(R.id.txll).layoutParams=layoutParams

        return templateStep
    }

    private fun createGroup(name: String, parent: LinearLayout, node: String=""): LinearLayout {
        // Проверим, возможно группа уже создана
        val ll=listllGroupTX.filter { it.tag=="$name$node" }
        if (ll.isNotEmpty()) {
            return ll[0]
        } else {
            Timber.d("генерим группу $name$node")
            val templateStep= LayoutInflater.from(rootView.context).inflate(
                R.layout.template_group_tx, parent as ViewGroup?, false) as LinearLayout


            templateStep.findViewById<TextView>(R.id.question).text=name

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llGroup=templateStep.findViewById<LinearLayout>(R.id.container)
            llGroup.tag="$name$node" // Сохраним имя группы
            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

            clTitle.setOnClickListener {
                if (llGroup.visibility==View.VISIBLE) {
                    llGroup.visibility=View.GONE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context, R.drawable.arrow_up))
                } else {
                    llGroup.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context, R.drawable.arrow_down))
                }
            }

            doAssociateParent(templateStep, parent)

            listllGroupTX.add(llGroup)


            return llGroup
        }


    }

    /**
     * Метод, в котором осуществляется привязка дочернего View к родительскому
     */
    private fun doAssociateParent(v: View, mainView: View, index: Int?=null){
        if (mainView is LinearLayout) {
            if (index!=null) {
                mainView.addView(v,index)
            } else {
                mainView.addView(v)
            }

        }
    }
}