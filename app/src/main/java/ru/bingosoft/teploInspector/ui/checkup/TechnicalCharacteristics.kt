package ru.bingosoft.teploInspector.ui.checkup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import timber.log.Timber


class TechnicalCharacteristics(private val parentFragment: CheckupFragment, private val lists: List<TechParams>) {
    private var listllGroupTX: MutableList<LinearLayout> = mutableListOf()
    private lateinit var rootView:View

    fun create(rootView: View) {
        this.rootView=rootView

        lists.forEach { th ->

            var parentGroup:LinearLayout?=null

            if (!th.long_group.isNullOrEmpty()) {
                val groupList=th.long_group!!.split("#")

                groupList.forEach { groupName ->
                    val llGroup=if (parentGroup==null) {
                        createGroup(groupName, rootView.findViewById(R.id.llMain))
                    } else {
                        createGroup(groupName, parentGroup!!, parentGroup!!.tag.toString())
                    }
                    parentGroup=llGroup

                }
            } else {
                if (!th.short_group.isNullOrEmpty()) {
                    val llGroup=createGroup(th.short_group!!, rootView.findViewById(R.id.llMain))
                    parentGroup=llGroup

                }
            }

            if (th.node!=null) {
                val llGroup=if (parentGroup==null) {
                    createGroup("Узел ${th.node}", rootView.findViewById(R.id.llMain))
                }else {
                    createGroup("Узел ${th.node}", parentGroup!!, parentGroup!!.tag.toString())
                }

                val templateStep=fillDataTH(th)
                doAssociateParent(templateStep, llGroup)

            } else {
                Timber.d("все_пусто")
                val llGroup=if (parentGroup==null) {
                    rootView.findViewById(R.id.llMain)
                }else {
                    parentGroup
                }

                val templateStep=fillDataTH(th)
                if (llGroup != null) {
                    if (parentGroup==null) {
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.setMargins(24, 0, 24, 0)
                        parentFragment.requireActivity().runOnUiThread{
                            llGroup.layoutParams = params
                        }

                    }

                    doAssociateParent(templateStep, llGroup)
                }
            }
        }
    }

    private fun fillDataTH(th: TechParams):LinearLayout {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.item_technical_characteristic, rootView.parent as ViewGroup?, false
        ) as LinearLayout //  R.layout.template_textinput

        if (parentFragment.isAdded) {
            parentFragment.requireActivity().runOnUiThread {
                templateStep.findViewById<TextView>(R.id.gi_name).text=th.technical_characteristic
                templateStep.findViewById<TextView>(R.id.gi_value_edit).text = th.value

                val layoutParams= LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                layoutParams.bottomMargin=4

                templateStep.findViewById<LinearLayout>(R.id.txll).layoutParams=layoutParams
            }
        } else {
            parentFragment.toaster.showErrorToast(R.string.error_unable_upload_checklist)
            parentFragment.otherUtil.writeToFile("Logger_Fragment CheckupFragment not attached to an activity.")
        }



        return templateStep
    }

    private fun createGroup(name: String, parent: LinearLayout, node: String = ""): LinearLayout {
        // Проверим, возможно группа уже создана
        val ll=listllGroupTX.filter { it.tag=="$name$node" }
        if (ll.isNotEmpty()) {
            return ll[0]
        } else {
            Timber.d("генерим группу $name$node")
            val templateStep= LayoutInflater.from(rootView.context).inflate(
                R.layout.template_group_tx, parent as ViewGroup?, false
            ) as LinearLayout


            templateStep.findViewById<TextView>(R.id.question).text=name

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llGroup=templateStep.findViewById<LinearLayout>(R.id.containerTh)
            llGroup.tag="$name$node" // Сохраним имя группы
            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

            ivExpand.setOnClickListener {
                if (llGroup.visibility==View.VISIBLE) {
                    llGroup.visibility=View.GONE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.ic_arrow_up
                        )
                    )
                } else {
                    llGroup.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(
                        ContextCompat.getDrawable(
                            rootView.context,
                            R.drawable.ic_arrow_down
                        )
                    )
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
    /*private fun doAssociateParent(v: View, mainView: View, index: Int? = null){
        parentFragment.requireActivity().runOnUiThread{
            if (mainView is LinearLayout) {
                if (index!=null) {
                    mainView.addView(v, index)
                } else {
                    mainView.addView(v)
                }

            }
        }

    }*/

    private fun doAssociateParent(v: View, mainView: View){
        parentFragment.requireActivity().runOnUiThread{
            if (mainView is LinearLayout) {
                mainView.addView(v)
            }
        }

    }

    fun saveUI() {
        Timber.d("saveUI")
        val ll=rootView.findViewById<LinearLayout>(R.id.llMain)
        parentFragment.llMainUiTX.addAll(ll.children)
    }
}