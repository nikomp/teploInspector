package ru.bingosoft.teploInspector.ui.checkup

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_cardview_step.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const
import timber.log.Timber

//#Компонент_аккордион
//Используется RecyclerView, в item, которого добавляется скрытый элемент,
// при нажатии на item, он разворачивается в onBindViewHolder
class StepsAdapter (private val lists: List<String>) : RecyclerView.Adapter<StepsAdapter.StepsViewHolder>() {

    private var expandedPosition=-1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cardview_step, parent, false)
        return StepsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: StepsViewHolder, position: Int) {
        holder.stepNumber.text="${position+1}"
        holder.stepName.text=lists[position]
        if (position==0) {
            holder.countQuestion.text="${Const.GeneralInformation.list.size}/${Const.GeneralInformation.list.size}"
        } else {
            holder.countQuestion.text="6/6"
        }

        val isExpanded = position == expandedPosition
        val drawable = if (isExpanded) {
            ContextCompat.getDrawable((holder.itemView.context as MainActivity), R.drawable.arrow_up)
        } else {
            ContextCompat.getDrawable((holder.itemView.context as MainActivity), R.drawable.arrow_down)
        }

        if (isExpanded) {
            holder.stepName.setTextColor(Color.parseColor("#007CCC"))
        } else {
            holder.stepName.setTextColor(Color.parseColor("#2D3239"))
        }
        holder.expandStep.setImageDrawable(drawable)

        holder.details.visibility = if (isExpanded) View.VISIBLE else View.GONE
        Timber.d("position=$position")
        if (position==0 && isExpanded) {
            holder.details.removeAllViews()
            Timber.d("Общие_сведения")
            //val rvgi = holder.details.findViewById(R.id.rvgi) as RecyclerView
            val rvgi=LayoutInflater.from(holder.itemView.context).inflate(R.layout.order_general_information, holder.details, false) as RecyclerView
            rvgi.layoutManager = LinearLayoutManager(holder.itemView.context)
            val adapter=GeneralInformationAdapter(Const.GeneralInformation.list)
            rvgi.adapter = adapter

            //val llGeneralInformation=LayoutInflater.from(holder.itemView.context).inflate(R.layout.order_general_information, holder.details, false) as LinearLayout
            holder.details.addView(rvgi)
        }
        if (position==1 && isExpanded) {
            holder.details.removeAllViews()
            val textview=TextView(holder.itemView.context)
            textview.text="Тех. характеристики"
            holder.details.addView(textview)
        }
        if (position>1 && isExpanded){
            holder.details.removeAllViews()
            val textview=TextView(holder.itemView.context)
            textview.text="VVVVV"
            holder.details.addView(textview)
        }
        holder.itemView.isActivated = isExpanded
        holder.itemView.setOnClickListener {
            expandedPosition = if (isExpanded) -1 else position
            notifyItemChanged(position)
        }

    }

    class StepsViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("11_recyclerViewListClicked")
            //listener.recyclerViewListClicked(v, this.layoutPosition)
        }

        var stepNumber:TextView = itemView.stepNumber
        var stepName:TextView = itemView.stepName
        var countQuestion:TextView=itemView.countQuestion
        var expandStep:ImageView=itemView.expandStep
        var details: LinearLayout =itemView.llMain

        init {
            view.setOnClickListener(this)
        }


    }



}