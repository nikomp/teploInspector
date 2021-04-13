package ru.bingosoft.teploInspector.ui.checkup

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
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
class StepsAdapter(
    private val lists: List<String>,
    private val parentFragment: CheckupFragment
) : RecyclerView.Adapter<StepsAdapter.StepsViewHolder>() {

    private var expandedPosition=-1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_cardview_step,
            parent,
            false
        )
        return StepsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onBindViewHolder(holder: StepsViewHolder, position: Int) {
        Timber.d("onBindViewHolder_lists[position]")
        holder.stepNumber.text="${position+1}"
        holder.stepName.text=lists[position]
        when (position) {
            0 -> holder.countQuestion.text =
                "${Const.GeneralInformation.list.size}/${Const.GeneralInformation.list.size}"
            1 -> {
                Timber.d("VVV1_${parentFragment.currentOrder.techParamsCount}")
                Timber.d("VVV1_${parentFragment.currentOrder}")
                holder.countQuestion.text =
                    "${parentFragment.currentOrder.techParamsCount}/${parentFragment.currentOrder.techParamsCount}"
            }
            2 -> {
                Timber.d("Дополнительная нагрузка")
                holder.countQuestion.text ="${parentFragment.currentOrder.addLoadCount}/${parentFragment.currentOrder.addLoadCount}"
            }
            3 -> {
                Timber.d("questionCount=${parentFragment.currentOrder.questionCount}")
                holder.countQuestion.text =
                    "${parentFragment.currentOrder.questionCount}/${parentFragment.currentOrder.answeredCount}"
                holder.countQuestion.tag = "countQuestionChecklist"
            }
            else -> {
                Timber.d("Неизвестная секция чеклиста")
            }
        }


        val isExpanded = position == expandedPosition
        val drawable = if (isExpanded) {
            ContextCompat.getDrawable(
                (holder.itemView.context as MainActivity),
                R.drawable.arrow_up
            )
        } else {
            ContextCompat.getDrawable(
                (holder.itemView.context as MainActivity),
                R.drawable.arrow_down
            )
        }

        if (isExpanded) {
            holder.stepName.setTextColor(Color.parseColor("#007CCC"))
        } else {
            holder.stepName.setTextColor(Color.parseColor("#2D3239"))
        }
        holder.expandStep.setImageDrawable(drawable)


        holder.details.removeAllViews()
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        holder.details.layoutParams = params
        holder.details.visibility=View.GONE
        if (position==0 && isExpanded) {
            Timber.d("Общие_сведения")
            Timber.d("parentFragment.currentOrder=${parentFragment.currentOrder}")

            val rvgi=LayoutInflater.from(holder.itemView.context).inflate(
                R.layout.order_general_information,
                holder.details,
                false
            ) as RecyclerView
            rvgi.layoutManager = LinearLayoutManager(holder.itemView.context)
            val adapter=GeneralInformationAdapter(
                Const.GeneralInformation.list,
                parentFragment.currentOrder
            )
            rvgi.adapter = adapter
            holder.details.addView(rvgi)
            holder.details.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
        if (position==1 && isExpanded) {
            //Генерируем тех характеристики
            if (parentFragment.techParams.isNotEmpty()) {
                val uiCreator=TechnicalCharacteristics(parentFragment.techParams, holder.details)
                uiCreator.create()
            } else {
                parentFragment.toaster.showToast(R.string.th_is_empty)
            }
            holder.details.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }
        if (position==2 && isExpanded){
            //Генерируем доп. нагрузку
            if (parentFragment.addLoads.isNotEmpty()) {
                val uiCreator=AdditionalLoad(parentFragment.addLoads, holder.details)
                uiCreator.create()
            } else {
                parentFragment.toaster.showToast(R.string.al_is_empty)
            }
            holder.details.visibility = if (isExpanded) View.VISIBLE else View.GONE

        }
        if (position==3 && isExpanded){
            //Генерируем чеклист
            Timber.d("llMainTemp=${parentFragment.llMainUi}")
            if (parentFragment.llMainUi.isNotEmpty()) {
                parentFragment.llMainUi.forEach {
                    try {
                        holder.details.addView(it)
                    } catch (e: Exception) {
                        Timber.d("parentFragment_llMainUi_error")
                        e.printStackTrace()
                    }
                }
                parentFragment.llMainUi= mutableListOf()
                holder.details.visibility = if (isExpanded) View.VISIBLE else View.GONE

            } else {
                if (parentFragment.isCheckupInitialized() ) {
                    holder.pbStepLoad.visibility=View.VISIBLE
                    val uiCreator=UICreator(parentFragment, parentFragment.checkup)
                    val r= Runnable {
                        uiCreator.create(holder.details)
                        parentFragment.requireActivity().runOnUiThread {
                            holder.pbStepLoad.visibility=View.INVISIBLE
                            holder.details.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        }
                    }
                    val t=Thread(r)
                    t.start()

                    parentFragment.uiCreator=uiCreator

                } else {
                    parentFragment.toaster.showToast(R.string.checklist_is_empty)
                }
            }
        }

        // Если чеклист сворачивается сохраним его текущее состояние
        if (position==3 && !isExpanded) {
            parentFragment.uiCreator?.saveUI()
        }

        val clickListener= View.OnClickListener {
            it.setOnClickListener(null)
            expandedPosition = if (isExpanded) -1 else position
            Timber.d("ClickListener_$expandedPosition _$isExpanded")

            notifyItemChanged(position)

            Timber.d("pbStepLoad_VISIBLE")
            if ((!isExpanded) && (parentFragment.currentOrder.status=="Открыта"
                        || parentFragment.currentOrder.status=="В пути")) {
                parentFragment.toaster.showToast(R.string.checklist_is_blocked)

            }

        }

        holder.itemView.isActivated = isExpanded
        holder.itemView.setOnClickListener(clickListener)

    }

    class StepsViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("11_recyclerViewListClicked")
            //listener?.recyclerViewListClicked(v, this.layoutPosition)
        }

        var stepNumber:TextView = itemView.stepNumber
        var stepName:TextView = itemView.stepName
        var countQuestion:TextView=itemView.countQuestion
        var expandStep:ImageView=itemView.expandStep
        var details: LinearLayout =itemView.llMain
        var pbStepLoad: ProgressBar=itemView.pbStepLoad

        init {
            view.setOnClickListener(this)
        }


    }



}