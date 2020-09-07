package ru.bingosoft.teploInspector.ui.order

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import kotlinx.android.synthetic.main.item_cardview_order.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.util.Const
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class OrderListAdapter (val orders: List<Orders>, private val itemListener: OrdersRVClickListeners, private val parentFragment: OrderFragment, private val userLocation: Location) : RecyclerView.Adapter<OrderListAdapter.OrderViewHolder>(), Filterable {

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
        Timber.d("XXX=$ordersFilterList")
        holder.orderNumber.text = "№ ${ordersFilterList[position].number}"

        if (ordersFilterList[position].typeOrder.isNullOrEmpty()){
            holder.orderType.text="Тип заявки"
        } else {
            holder.orderType.text=ordersFilterList[position].typeOrder
        }


        holder.orderState.setText(ordersFilterList[position].status?.toUpperCase())
        changeColorMBSState(holder.orderState, ordersFilterList[position].status)

        val adapterStatus: ArrayAdapter<String> = ArrayAdapter<String>(
            parentFragment.requireContext(),
            R.layout.template_multiline_spinner_item_state_order,
            R.id.text1,
            Const.StatusOrder.list
        )

        holder.orderState.setAdapter(adapterStatus)

        holder.orderState.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Timber.d("bool=${(parentFragment.activity as MainActivity).isSearchView}")
                if (!(parentFragment.activity as MainActivity).isBackPressed &&
                    !(parentFragment.activity as MainActivity).isSearchView) {
                    Timber.d("addTextChangedListener=${s.toString()}")
                    ordersFilterList[position].status=s.toString()
                    changeColorMBSState(holder.orderState, ordersFilterList[position].status)
                    parentFragment.orderPresenter.addHistoryState(ordersFilterList[position])

                    holder.orderState.removeTextChangedListener(this)
                    holder.orderState.setText(s.toString().toUpperCase())
                    holder.orderState.addTextChangedListener(this)
                }

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }

        })

        if (ordersFilterList[position].dateVisit!=null && ordersFilterList[position].timeVisit!=null) {
            val strDateTimeVisit="${ordersFilterList[position].dateVisit} ${ordersFilterList[position].timeVisit}"
            val date=SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ru","RU")).parse(strDateTimeVisit)
            holder.orderDate.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(date)
        } else {
            holder.orderDate.text=parentFragment.getString(R.string.not_date_visit)
        }

        holder.orderPurposeObject.text = ordersFilterList[position].purposeObject
        holder.orderadress.text = ordersFilterList[position].address
        holder.fio.text = ordersFilterList[position].contactFio
        /*if (ordersFilterList[position].typeTransportation.isNullOrEmpty()) {
            holder.typeTransportation.text="Нет данных"
        } else {
            holder.typeTransportation.text=orders[position].typeTransportation
        }*/

        if (!ordersFilterList[position].typeTransportation.isNullOrEmpty()) {
            holder.typeTransportation.setText(ordersFilterList[position].typeTransportation)
        }


        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            parentFragment.requireContext(),
            R.layout.template_multiline_spinner_item, //android.R.layout.simple_dropdown_item_1line,
            Const.TypeTransportation.list
        )
        holder.typeTransportation.setAdapter(adapter)

        holder.typeTransportation.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                holder.btnRoute.isEnabled = s.toString() != parentFragment.requireContext().getString(R.string.strTypeTransportationClient)
                if (holder.btnRoute.isEnabled) {
                    holder.btnRoute.setTextColor(Color.parseColor("#2D3239"))
                } else {
                    holder.btnRoute.setTextColor(Color.parseColor("#C7CCD1"))
                }
                ordersFilterList[position].typeTransportation=s.toString()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }

        })


        if (ordersFilterList[position].phone.isNullOrEmpty()) {
            holder.btnPhone.text=parentFragment.requireContext().getString(R.string.no_contact)
            holder.btnPhone.isEnabled=false
            holder.btnPhone.setTextColor(R.color.enabledText)
        } else {
            holder.btnPhone.text=ordersFilterList[position].phone
        }

        if (userLocation.latitude!=0.0 && userLocation.longitude!=0.0) {
            val distance=parentFragment.otherUtil.getDistance(userLocation, ordersFilterList[position])
            holder.btnRoute.text=parentFragment.requireContext().getString(R.string.distance, distance.toString())//"Маршрут 3.2 км"
        } else {
            holder.btnRoute.text=parentFragment.requireContext().getString(R.string.route)
        }

        holder.btnPhone.setOnClickListener { _ ->
            Timber.d("fabButton.setOnClickListener ${ordersFilterList[position].phone}")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ordersFilterList[position].phone}"))
            if (intent.resolveActivity(parentFragment.requireContext().packageManager) != null) {
                startActivity(parentFragment.requireContext(),intent,null)
            }
        }

        holder.btnRoute.setOnClickListener { _ ->
            Timber.d("btnRoute.setOnClickListener")

            //Включаем фрагмент со списком Маршрутов для конкретной заявки
            val bundle = Bundle()
            bundle.putBoolean("isOrderFragment",true)
            val fragmentMap= MapFragment()
            fragmentMap.arguments=bundle
            val fragmentManager=(parentFragment.requireContext() as MainActivity).supportFragmentManager

            fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragmentMap, "map_fragment_from_orders_tag")
                .addToBackStack(null)
                .commit()

            fragmentManager.executePendingTransactions()


            (parentFragment.requireContext() as FragmentsContractActivity).setOrder(orders[position])

        }



        holder.listener=itemListener

        /*if (ordersFilterList[position].checked) {
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    parentFragment.requireContext(),
                    R.color.colorCardSelect
                ))
        } else {
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    parentFragment.requireContext(),
                    R.color.colorCardItem
                ))
        }*/
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
        var orderDate: TextView = itemView.date
        var orderPurposeObject: TextView = itemView.name
        var orderadress: TextView = itemView.adress
        var fio: TextView = itemView.fio
        var typeTransportation: MaterialBetterSpinner = itemView.type_transportation
        var btnPhone: Button=itemView.btnPhone
        var btnRoute: Button=itemView.btnRoute
        lateinit var listener: OrdersRVClickListeners

        var cardView: CardView = itemView.cv

        init {
            view.setOnClickListener(this)
        }


    }

    override fun getFilter(): Filter {
        return object: Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {

                val charSearch = constraint.toString()
                Timber.d("charSearch=$charSearch")

                if (charSearch.isEmpty()) {
                    //originalOrdersList.addAll(orders)
                    ordersFilterList=orders
                } else {
                    val resultList=
                        ordersFilterList.filter { it.address!!.contains(charSearch,true)}
                    Timber.d("resultList=${resultList.size}")

                    ordersFilterList=resultList
                }


                val results = FilterResults()
                results.count=ordersFilterList.size
                results.values=ordersFilterList

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                Timber.d("publishResults")
                ordersFilterList=results?.values as List<Orders>
                Timber.d("ordersFilterList=$ordersFilterList")
                //notifyDataSetChanged()
                //refreshData(ordersFilterList)

                if ((parentFragment.requireActivity() as MainActivity).isMapFragmentShow) {
                    (parentFragment.requireContext() as FragmentsContractActivity).showMarkers(ordersFilterList)
                } else {
                    (parentFragment.requireContext() as FragmentsContractActivity).showSearchedOrders(ordersFilterList)
                }

            }

        }
    }


}