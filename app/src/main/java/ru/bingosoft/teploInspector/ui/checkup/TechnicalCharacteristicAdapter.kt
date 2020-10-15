package ru.bingosoft.teploInspector.ui.checkup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.item_general_information.view.gi_name
import kotlinx.android.synthetic.main.item_general_information.view.gi_value
import kotlinx.android.synthetic.main.item_general_information.view.gi_value_edit
import kotlinx.android.synthetic.main.item_technical_characteristic.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import timber.log.Timber

class TechnicalCharacteristicAdapter(private val lists: List<TechParams>) : RecyclerView.Adapter<TechnicalCharacteristicAdapter.TcItemsViewHolder>() {

    private var listllGroupTX: MutableList<LinearLayout> = mutableListOf()
    private lateinit var parentView: ViewGroup

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TcItemsViewHolder {
        Timber.d("onCreateViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_technical_characteristic, parent, false)
        parentView=parent

        return TcItemsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    override fun onBindViewHolder(holder: TcItemsViewHolder, position: Int) {
        Timber.d("holder.itemView=${holder.itemView}")
        //Проверим возможно есть группировка ТХ
        if (lists[position].node!=null) {
            val llGroup= lists[position].node?.let {
                createGroup("Узел ${lists[position].node}",(holder.itemView as LinearLayout),
                    it
                )
            }

            (holder.itemView as ViewGroup).removeView(holder.giName)
            (holder.itemView).removeView(holder.giValue)

            val templateStep=LayoutInflater.from(parentView.context).inflate(
                R.layout.item_technical_characteristic, parentView, false) as LinearLayout
            templateStep.findViewById<TextView>(R.id.gi_name).text=lists[position].technical_characteristic
            templateStep.findViewById<TextInputEditText>(R.id.gi_value_edit).setText(lists[position].value)

            llGroup?.addView(templateStep)
        } else {
            holder.giName.text=lists[position].technical_characteristic
            holder.giValueEdit.setText(lists[position].value)

            val layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)

            layoutParams.bottomMargin=4

            holder.txll.layoutParams=layoutParams

        }


    }

    class TcItemsViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("11_recyclerViewListClicked")
            //listener.recyclerViewListClicked(v, this.layoutPosition)
        }

        var txll:LinearLayout=itemView.txll
        var giName:TextView=itemView.gi_name
        var giValue:TextInputLayout=itemView.gi_value
        var giValueEdit:TextInputEditText=itemView.gi_value_edit
    }

    private fun createGroup(name: String, parent: LinearLayout, node: String=""): LinearLayout {
        Timber.d("генерим группу $name$node")

        // Проверим, возможно группа уже создана
        val ll=listllGroupTX.filter { it.tag=="$name$node" }
        if (ll.isNotEmpty()) {
            return ll[0]
        } else {
            val templateStep=LayoutInflater.from(parentView.context).inflate(
                R.layout.template_group_tx, parentView, false) as LinearLayout

            templateStep.findViewById<TextView>(R.id.question).text=name

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llGroup=templateStep.findViewById<LinearLayout>(R.id.container)
            llGroup.tag="$name$node" // Сохраним имя группы
            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

            clTitle.setOnClickListener {
                if (llGroup.visibility==View.VISIBLE) {
                    llGroup.visibility=View.GONE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(parentView.context,R.drawable.arrow_up))
                } else {
                    llGroup.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(parentView.context,R.drawable.arrow_down))
                }
            }

            doAssociateParent(templateStep, parent)
            Timber.d("llGroup=$llGroup")

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