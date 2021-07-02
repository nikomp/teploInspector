package ru.bingosoft.teploInspector.ui.order

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import kotlinx.android.synthetic.main.alert_change_date_time.view.*
import kotlinx.android.synthetic.main.item_cardview_order.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class OrderListAdapter (val orders: List<Orders>, private val itemListener: OrdersRVClickListeners, private val parentFragment: OrderFragment, private val userLocation: Location) : RecyclerView.Adapter<OrderListAdapter.OrderViewHolder>(), Filterable {

    var ordersFilterList: List<Orders> = listOf()
    //var stateTextWatcherList: MutableList<TextWatcher> = mutableListOf()


    init {
        ordersFilterList=orders
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cardview_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun getItemCount(): Int {
        return ordersFilterList.size
    }

    fun getOrder(position: Int): Orders {
        return ordersFilterList[position]
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        Timber.d("Order_number=${ordersFilterList[position].number}")
        holder.orderNumber.text = parentFragment.getString(R.string.order_number,ordersFilterList[position].number)
        holder.orderCountNode.text=parentFragment.getString(R.string.count_node,ordersFilterList[position].countNode)


        if (ordersFilterList[position].typeOrder.isNullOrEmpty()){
            holder.orderType.text="Тип заявки"
        } else {
            holder.orderType.text=ordersFilterList[position].typeOrder
        }

        if (ordersFilterList[position].orderNote.isNullOrEmpty()) {
            holder.orderNote.visibility=View.GONE
        } else {
            holder.orderNote.text=ordersFilterList[position].orderNote
            holder.orderNote.visibility=View.VISIBLE
            holder.orderNote.tag=true
            holder.orderNote.setOnClickListener {
                (it as TextView).tag=!(it.tag as Boolean)
                it.isSingleLine = (it.tag as Boolean)
                it.ellipsize=TextUtils.TruncateAt.END
            }
        }

        val adapterStatus: ArrayAdapter<String> = ArrayAdapter(
            parentFragment.requireContext(),
            R.layout.template_multiline_spinner_item_state_order,
            R.id.text1,
            Const.StatusOrder.list
        )

        holder.orderState.setAdapter(adapterStatus)

        val orderStateListener=object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {

                // Если статус меняется на Выполнена, а чек лист пуст, выдаем сообщение
                Timber.d("ordersFilterList_size=${ordersFilterList.size}__position_$position")
                if (position>=ordersFilterList.size) {
                    Timber.d("IOIO")
                    return
                }
                if (ordersFilterList[position].answeredCount==0 && s.toString()=="Выполнена") {
                    parentFragment.toaster.showToast(R.string.checklist_not_changed_status)
                    holder.orderState.removeTextChangedListener(this)
                    holder.orderState.setText(ordersFilterList[position].status?.toUpperCase(Locale.ROOT),false)
                    holder.orderState.addTextChangedListener(this)
                    return
                }

                Timber.d("ZZZ5_${s.toString().toUpperCase(Locale.ROOT)}_${ordersFilterList[position].status?.toUpperCase(
                    Locale.ROOT
                )}")
                if (s.toString().toUpperCase(Locale.ROOT) != ordersFilterList[position].status?.toUpperCase(
                        Locale.ROOT
                    )
                ) {
                    ordersFilterList[position].status=s.toString().toLowerCase(Locale.ROOT)
                        .capitalize()
                    changeColorMBSState(holder.orderState, ordersFilterList[position].status)
                    try {
                        //parentFragment.orderPresenter.addHistoryState(ordersFilterList[position])
                        parentFragment.orderPresenter.updateOrderState(ordersFilterList[position])
                    } catch (e: Throwable) {
                        parentFragment.errorReceived(e)
                    }

                }


                holder.orderState.removeTextChangedListener(this)
                holder.orderState.setText(s.toString().toUpperCase(Locale.ROOT),false)
                holder.orderState.addTextChangedListener(this)

                //Фильтруем по статусу
                if (s.toString()=="Выполнена" || s.toString()=="Отменена") {
                    parentFragment.filteredOrderByState("all_without_Done_and_Cancel")
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        }

        val adapter: ArrayAdapter<String> = ArrayAdapter(
            parentFragment.requireContext(),
            R.layout.template_multiline_spinner_item,
            Const.TypeTransportation.list
        )
        holder.typeTransportation.setAdapter(adapter)

        val typeTransportationTextWatcher=object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Timber.d("typeTransportationTextWatcher")
                Timber.d("ordersFilterList_size=${ordersFilterList.size}__position_$position")
                if (position>=ordersFilterList.size) {
                    Timber.d("IOIO2")
                    return
                }


                holder.btnRoute.isEnabled = s.toString() != parentFragment.requireContext().getString(R.string.strTypeTransportationClient)
                if (holder.btnRoute.isEnabled) {
                    holder.btnRoute.setTextColor(Color.parseColor("#2D3239"))
                } else {
                    holder.btnRoute.setTextColor(Color.parseColor("#C7CCD1"))
                }

                try {
                    if (s.toString().toUpperCase(Locale.ROOT) != ordersFilterList[position].typeTransportation?.toUpperCase(
                            Locale.ROOT
                        )
                    ) {
                        ordersFilterList[position].typeTransportation=s.toString()
                        parentFragment.orderPresenter.changeTypeTransortation(ordersFilterList[position])
                    }
                } catch (e: Throwable) {
                    parentFragment.errorReceived(e)
                }

                holder.typeTransportation.removeTextChangedListener(this)
                holder.typeTransportation.setText(s.toString(),false)
                holder.typeTransportation.addTextChangedListener(this)


            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        }


        if (!(parentFragment.activity as MainActivity).isBackPressed &&
            !(parentFragment.activity as MainActivity).isSearchView) {
            Timber.d("removeTextChangedListener_${(parentFragment.activity as MainActivity).isBackPressed}_${(parentFragment.activity as MainActivity).isSearchView}")
            //holder.orderState.removeTextChangedListener(orderStateListener)
            //holder.typeTransportation.removeTextChangedListener(typeTransportationTextWatcher)

            if (holder.orderState.tag !=null) {
                holder.orderState.removeTextChangedListener(holder.orderState.tag as TextWatcher)
            }
            if (holder.typeTransportation.tag !=null) {
                holder.typeTransportation.removeTextChangedListener(holder.typeTransportation.tag as TextWatcher)
            }


        }


        Timber.d("SDR_${holder.orderState.text}__${ordersFilterList[position].status?.toUpperCase(Locale.ROOT)}__${ordersFilterList[position]}")
        holder.orderState.setText(ordersFilterList[position].status?.toUpperCase(Locale.ROOT),false)
        changeColorMBSState(holder.orderState, ordersFilterList[position].status)

        if (!ordersFilterList[position].typeTransportation.isNullOrEmpty()) {
            holder.typeTransportation.setText(ordersFilterList[position].typeTransportation,false)
        }


        //Не переносить эти строки иначе, перестанут срабывать слушатели, т.к. они могут быть отключены
        //... removeTextChangedListener(orderStateListener), removeTextChangedListener(typeTransportationTextWatcher)
        holder.orderState.addTextChangedListener(orderStateListener)
        holder.orderState.tag=orderStateListener
        holder.typeTransportation.addTextChangedListener(typeTransportationTextWatcher)
        holder.typeTransportation.tag=typeTransportationTextWatcher

        holder.btnChangeDateTime.setOnClickListener {
            Timber.d("btnChangeDateTime_setOnClickListener")
            lateinit var alertDialog: AlertDialog
            val layoutInflater = LayoutInflater.from(parentFragment.requireContext())
            val dialogView: View =
                layoutInflater.inflate(R.layout.alert_change_date_time, (parentFragment.root as ViewGroup), false)

            val builder = AlertDialog.Builder(parentFragment.requireContext())

            val newDate=dialogView.findViewById<TextView>(R.id.newDate)
            if  (!ordersFilterList[position].dateVisit.isNullOrEmpty()) {
                try {
                    val dateVisit=SimpleDateFormat("yyyy-MM-dd", Locale("ru","RU")).parse(ordersFilterList[position].dateVisit!!)
                    newDate.text=SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU")).format(dateVisit!!)
                } catch (e:Exception) {
                    e.printStackTrace()
                }

            }

            // Разрешаем менять дату только у заявок определенного списка
            newDate.isEnabled=Const.OrderTypeForDateChangeAvailable.types.contains(ordersFilterList[position].typeOrder)
            newDate.setOnClickListener{
                Timber.d("newDate_setOnClickListener")
                (parentFragment.requireContext() as MainActivity).showDateTimeDialog(Const.Dialog.DIALOG_DATE, newDate)
            }

            val newTime=dialogView.findViewById<TextView>(R.id.newTime)
            newTime.text=ordersFilterList[position].timeVisit
            newTime.setOnClickListener{
                (parentFragment.requireContext() as MainActivity).showDateTimeDialog(Const.Dialog.DIALOG_TIME, newTime)
            }

            dialogView.btnOk.setOnClickListener{
                Timber.d("dialogView.buttonOK")

                val strDateTimeVisit="${newDate.text} ${newTime.text}"

                Timber.d("strDateTimeVisit_$strDateTimeVisit")
                try {
                    val date=SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).parse(strDateTimeVisit)
                    if (date!=null) {
                        holder.btnChangeDateTime.text=
                            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(date)
                        ordersFilterList[position].dateVisit=SimpleDateFormat("yyyy-MM-dd", Locale("ru","RU")).format(date)
                        ordersFilterList[position].timeVisit=SimpleDateFormat("HH:mm:ss", Locale("ru","RU")).format(date)

                        //parentFragment.orderPresenter.saveDateTime(ordersFilterList[position])
                        parentFragment.mainPresenter.updateGiOrder(ordersFilterList[position])
                        (parentFragment.requireActivity() as MainActivity).currentOrder=ordersFilterList[position]

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }


                alertDialog.dismiss()

            }

            builder.setView(dialogView)
            builder.setCancelable(true)
            alertDialog=builder.create()
            alertDialog.show()
        }



        if (ordersFilterList[position].dateVisit!=null && ordersFilterList[position].timeVisit!=null) {
            val strDateTimeVisit="${ordersFilterList[position].dateVisit} ${ordersFilterList[position].timeVisit}"
            try {
                val date=SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ru","RU")).parse(strDateTimeVisit)
                if (date!=null) {
                    holder.btnChangeDateTime.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(date)
                }
            } catch ( e:Exception) {
                e.printStackTrace()
            }

        } else {
            holder.btnChangeDateTime.text=parentFragment.getString(R.string.not_date_visit)
        }

        holder.orderPurposeObject.text = ordersFilterList[position].purposeObject
        holder.orderadress.text = ordersFilterList[position].address
        holder.fio.text = ordersFilterList[position].contactFio



        if (ordersFilterList[position].phone.isNullOrEmpty()) {
            holder.btnPhone.text=parentFragment.requireContext().getString(R.string.no_contact)
            holder.btnPhone.isEnabled=false
            holder.btnPhone.setTextColor(ContextCompat.getColor(parentFragment.requireContext(),R.color.enabledText))
        } else {
            holder.btnPhone.text=ordersFilterList[position].phone
        }

        if (userLocation.latitude!=0.0 && userLocation.longitude!=0.0) {
            val distance=parentFragment.otherUtil.getDistance(userLocation, ordersFilterList[position])
            holder.btnRoute.text=parentFragment.requireContext().getString(R.string.distance, distance.toString())//"Маршрут 3.2 км"
        } else {
            holder.btnRoute.text=parentFragment.requireContext().getString(R.string.route)
        }

        holder.btnPhone.setOnClickListener {
            Timber.d("fabButton.setOnClickListener ${ordersFilterList[position].phone}")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ordersFilterList[position].phone}"))
            if (intent.resolveActivity(parentFragment.requireContext().packageManager) != null) {
                startActivity(parentFragment.requireContext(),intent,null)
            }
        }

        holder.btnRoute.setOnClickListener {
            Timber.d("btnRoute.setOnClickListener")

            (parentFragment.requireActivity() as MainActivity).currentOrder=ordersFilterList[position]

            //Включаем фрагмент со списком Маршрутов для конкретной заявки
            val bundle = Bundle()
            bundle.putBoolean("isOrderFragment",true)

            Navigation.findNavController(parentFragment.root).navigate(R.id.nav_slideshow,bundle)

        }

        Timber.d("ordersIdsNotSync=${(parentFragment.requireActivity() as MainActivity).ordersIdsNotSync}")
        if ((parentFragment.requireActivity() as MainActivity).ordersIdsNotSync.contains(ordersFilterList[position].id)) {
            holder.ivSync.visibility=View.VISIBLE
            holder.ivSync.setOnClickListener {
                Timber.d("ivSync_setOnClickListener order=${getOrder(position)}")
                (parentFragment.requireActivity() as MainActivity).mainPresenter.sendData3(
                    ordersFilterList[position].id,
                    holder.ivSync
                )
            }
        } else {
            holder.ivSync.visibility=View.GONE
        }

        holder.listener=itemListener

    }


    private fun changeColorMBSState(view: MaterialBetterSpinner, status:String?) {
        Timber.d("changeColorMBSState=$status")
        when (status) {
            parentFragment.requireContext().getString(R.string.status_CANCEL) -> {
                view.setTextColor(Color.parseColor("#E94435"))
            }
            parentFragment.requireContext().getString(R.string.status_TESTED) -> {
                view.setTextColor(Color.parseColor("#B370D7"))
            }
            parentFragment.requireContext().getString(R.string.status_DONE) -> {
                view.setTextColor(Color.parseColor("#3DB650"))
            }
            parentFragment.requireContext().getString(R.string.status_PAUSED) -> {
                view.setTextColor(Color.parseColor("#A5AEB6"))
            }
            parentFragment.requireContext().getString(R.string.status_IN_WORK) -> {
                view.setTextColor(Color.parseColor("#309EFD"))
            }
            parentFragment.requireContext().getString(R.string.status_IN_WAY) -> {
                view.setTextColor(Color.parseColor("#F28D17"))
            }
            parentFragment.requireContext().getString(R.string.status_OPEN) -> {
                view.setTextColor(Color.parseColor("#56D5BE"))
            }

        }
    }

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("orderlistadapter_recyclerViewListClicked")
            listener.recyclerViewListClicked(v, this.layoutPosition)
        }


        var orderNumber: TextView = itemView.number
        var orderType: TextView = itemView.order_type
        var orderState: MaterialBetterSpinner = itemView.order_state
        var orderCountNode: TextView = itemView.count_node
        var btnChangeDateTime: Button = itemView.btnChangeDateTime
        var orderPurposeObject: TextView = itemView.name
        var orderadress: TextView = itemView.adress
        var orderNote: TextView=itemView.orderNote
        var fio: TextView = itemView.fio
        var typeTransportation: MaterialBetterSpinner = itemView.type_transportation
        var btnPhone: Button=itemView.btnPhone
        var btnRoute: Button=itemView.btnRoute
        lateinit var listener: OrdersRVClickListeners

        var ivSync: ImageView=itemView.ivSync

        //var cardView: CardView = itemView.cv

        init {
            view.setOnClickListener(this)
        }


    }

    override fun getFilter(): Filter {
        return object: Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {

                val charSearch = constraint.toString()
                Timber.d("charSearch=$charSearch")

                ordersFilterList = if (charSearch.isEmpty()) {
                    (parentFragment.requireActivity() as MainActivity).orders
                } else {
                    val resultList=
                        ordersFilterList.filter { it.address!=null && it.address!!.contains(charSearch,true)}

                    resultList
                }


                val results = FilterResults()
                results.count=ordersFilterList.size
                results.values=ordersFilterList

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                Timber.d("publishResults")
                if (results?.values!=null) {

                    val resultOrders= results.values
                    val result = mutableListOf<Orders>()
                    if (resultOrders is List<*>) {
                        for (i in 0 until resultOrders.size) {
                            val item = resultOrders[i]
                            if (item is Orders) {
                                result.add(item)
                            }
                        }
                    }
                    ordersFilterList=result

                    (parentFragment.requireContext() as MainActivity).filteredOrders=ordersFilterList
                    (parentFragment.requireContext() as FragmentsContractActivity).showMarkers(ordersFilterList)

                }
            }
        }
    }

}