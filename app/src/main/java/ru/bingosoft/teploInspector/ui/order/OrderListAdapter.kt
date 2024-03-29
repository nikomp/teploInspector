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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import ru.bingosoft.teploInspector.App
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const
import ru.bingosoft.teploInspector.util.Const.StatusOrder.STATE_CANCELED
import ru.bingosoft.teploInspector.util.Const.StatusOrder.STATE_COMPLETED
import ru.bingosoft.teploInspector.util.Const.TypeTransportation.TRANSPORTATION_PERFORMED_CUSTOMER
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class OrderListAdapter (val orders: List<Orders>, private val itemListener: OrdersRVClickListeners, private val parentFragment: OrderFragment, private var userLocation: Location) : RecyclerView.Adapter<OrderListAdapter.OrderViewHolder>(), Filterable {

    var ordersFilterList: List<Orders> = listOf()

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


        holder.orderType.text=ordersFilterList[position].typeOrder

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
                if (position>=ordersFilterList.size) {
                    return
                }
                if (ordersFilterList[position].answeredCount==0 && s.toString()==STATE_COMPLETED) {
                    parentFragment.toaster.showToast(R.string.checklist_not_changed_status)
                    holder.orderState.removeTextChangedListener(this)
                    holder.orderState.setText(ordersFilterList[position].status?.uppercase(Locale.ROOT),false)
                    holder.orderState.addTextChangedListener(this)
                    return
                }

                if (s.toString().uppercase(Locale.ROOT) != ordersFilterList[position].status?.uppercase(
                        Locale.ROOT
                    )
                ) {
                    ordersFilterList[position].status= s.toString().lowercase(Locale.ROOT)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    changeColorMBSState(holder.orderState, ordersFilterList[position].status)
                    try {
                        parentFragment.orderPresenter.updateOrderState(ordersFilterList[position])
                    } catch (e: Throwable) {
                        parentFragment.errorReceived(e)
                    }

                }


                holder.orderState.removeTextChangedListener(this)
                holder.orderState.setText(s.toString().uppercase(Locale.ROOT),false)
                holder.orderState.addTextChangedListener(this)

                //Фильтруем по статусу
                if (s.toString()==STATE_COMPLETED || s.toString()==STATE_CANCELED) {
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
                    return
                }

                holder.btnRoute.isEnabled = s.toString() != TRANSPORTATION_PERFORMED_CUSTOMER
                if (holder.btnRoute.isEnabled) {
                    holder.btnRoute.setTextColor(Color.parseColor("#2D3239"))
                } else {
                    holder.btnRoute.setTextColor(Color.parseColor("#C7CCD1"))
                }

                try {
                    if (s.toString().uppercase(Locale.ROOT) != ordersFilterList[position].typeTransportation?.uppercase(
                            Locale.ROOT
                        )
                    ) {
                        ordersFilterList[position].typeTransportation=s.toString()
                        parentFragment.orderPresenter.changeTypeTransportation(ordersFilterList[position])
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

            if (holder.orderState.tag !=null) {
                holder.orderState.removeTextChangedListener(holder.orderState.tag as TextWatcher)
            }
            if (holder.typeTransportation.tag !=null) {
                holder.typeTransportation.removeTextChangedListener(holder.typeTransportation.tag as TextWatcher)
            }


        }


        holder.orderState.setText(ordersFilterList[position].status?.uppercase(Locale.ROOT),false)
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
                    App.appInstance.lastExceptionAppForTest=e
                    e.printStackTrace()
                }

            }

            // Разрешаем менять дату только у заявок определенного списка
            newDate.isEnabled=Const.OrderTypeForDateChangeAvailable.types.contains(ordersFilterList[position].typeOrder)
            newDate.setOnClickListener{
                (parentFragment.requireContext() as MainActivity).showDateTimeDialog(Const.Dialog.DIALOG_DATE, newDate)
            }

            val newTime=dialogView.findViewById<TextView>(R.id.newTime)
            newTime.text=ordersFilterList[position].timeVisit
            newTime.setOnClickListener{
                (parentFragment.requireContext() as MainActivity).showDateTimeDialog(Const.Dialog.DIALOG_TIME, newTime)
            }

            dialogView.findViewById<MaterialButton>(R.id.btnOk).setOnClickListener{
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
                    App.appInstance.lastExceptionAppForTest=e
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
                App.appInstance.lastExceptionAppForTest=e
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
            (parentFragment.requireActivity() as MainActivity).currentOrder=ordersFilterList[position]

            //Включаем фрагмент со списком Маршрутов для конкретной заявки
            val bundle = Bundle()
            bundle.putBoolean("isOrderFragment",true)

            println(Navigation.findNavController(parentFragment.requireView()))
            Navigation.findNavController(parentFragment.root).navigate(R.id.nav_slideshow,bundle)

        }

        Timber.d("ordersIdsNotSync=${(parentFragment.requireActivity() as MainActivity).ordersIdsNotSync}")
        if ((parentFragment.requireActivity() as MainActivity).ordersIdsNotSync.contains(ordersFilterList[position].id)) {
            holder.ivSync.visibility=View.VISIBLE
            holder.ivSync.setOnClickListener {
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
            Log.d("myLogs","orderlistadapter_recyclerViewListClicked")
            listener.recyclerViewListClicked(v, this.layoutPosition)
        }


        var orderNumber: TextView = itemView.findViewById(R.id.number)
        var orderType: TextView = itemView.findViewById(R.id.order_type)
        var orderState: MaterialBetterSpinner = itemView.findViewById(R.id.order_state)
        var orderCountNode: TextView = itemView.findViewById(R.id.count_node)
        var btnChangeDateTime: Button = itemView.findViewById(R.id.btnChangeDateTime)
        var orderPurposeObject: TextView = itemView.findViewById(R.id.name)
        var orderadress: TextView = itemView.findViewById(R.id.adress)
        var orderNote: TextView=itemView.findViewById(R.id.orderNote)
        var fio: TextView = itemView.findViewById(R.id.fio)
        var typeTransportation: MaterialBetterSpinner = itemView.findViewById(R.id.type_transportation)
        var btnPhone: Button=itemView.findViewById(R.id.btnPhone)
        var btnRoute: Button=itemView.findViewById(R.id.btnRoute)
        lateinit var listener: OrdersRVClickListeners

        var ivSync: ImageView=itemView.findViewById(R.id.ivSync)

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

    fun setUserLocationForTest(lat:Double,lon:Double) {
        userLocation=Location("")
        userLocation.latitude=lat
        userLocation.longitude=lon
    }

}