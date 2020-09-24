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
import ru.bingosoft.teploInspector.util.UICreator
import timber.log.Timber

//#Компонент_аккордион
//Используется RecyclerView, в item, которого добавляется скрытый элемент,
// при нажатии на item, он разворачивается в onBindViewHolder
class StepsAdapter (private val lists: List<String>, val parentFragment: CheckupFragment) : RecyclerView.Adapter<StepsAdapter.StepsViewHolder>() {

    private var expandedPosition=-1
    private var checupIsCreated=false

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
        when (position) {
            0-> holder.countQuestion.text="${Const.GeneralInformation.list.size}/${Const.GeneralInformation.list.size}"
            1-> {
                //holder.countQuestion.text="${Const.TechnicalСharacteristicList.list.size}/${Const.TechnicalСharacteristicList.list.size}"
                //holder.countQuestion.text="0/0"
                holder.countQuestion.text="${parentFragment.currentOrder.techParamsCount}/${parentFragment.currentOrder.techParamsCount}"
            }
            else -> {
                //holder.countQuestion.text="6/6"
                holder.countQuestion.text="${parentFragment.currentOrder.questionCount}/${parentFragment.currentOrder.answeredCount}"
                holder.countQuestion.tag="countQuestionChecklist"
            }
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
            Timber.d("parentFragment.currentOrder=${parentFragment.currentOrder}")

            val rvgi=LayoutInflater.from(holder.itemView.context).inflate(R.layout.order_general_information, holder.details, false) as RecyclerView
            rvgi.layoutManager = LinearLayoutManager(holder.itemView.context)
            val adapter=GeneralInformationAdapter(Const.GeneralInformation.list, parentFragment.currentOrder)
            rvgi.adapter = adapter

            //val llGeneralInformation=LayoutInflater.from(holder.itemView.context).inflate(R.layout.order_general_information, holder.details, false) as LinearLayout
            holder.details.addView(rvgi)
        }
        if (position==1 && isExpanded) {
            holder.details.removeAllViews()
            val rvtc=LayoutInflater.from(holder.itemView.context).inflate(R.layout.order_technical_characteristics, holder.details, false) as RecyclerView
            rvtc.layoutManager = LinearLayoutManager(holder.itemView.context)
            //val adapter=TechnicalCharacteristicAdapter(Const.TechnicalСharacteristicList.list)
            val adapter=TechnicalCharacteristicAdapter(parentFragment.techParams)
            rvtc.adapter = adapter

            holder.details.addView(rvtc)
        }
        if (position>1 && isExpanded){
            //holder.details.removeAllViews()

            //Генерируем чеклист
            if (!checupIsCreated) {
                if (parentFragment.isCheckupInitialized() ) {
                    val uiCreator=UICreator(parentFragment, parentFragment.checkup)
                    uiCreator.create(holder.details)
                    parentFragment.uiCreator=uiCreator
                    checupIsCreated=true
                } else {
                    parentFragment.toaster.showToast(R.string.checklist_is_empty)
                }
            }


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