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
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import kotlinx.android.synthetic.main.item_cardview_order.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.map.MapFragment
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class OrderListAdapter (val orders: List<Orders>, private val itemListener: OrdersRVClickListeners, private val parentFragment: OrderFragment, private val userLocation: Location) : RecyclerView.Adapter<OrderListAdapter.OrderViewHolder>(), Filterable {

    var ordersFilterList: List<Orders> = listOf()

    var SPINNER_TYPE_TRANSPORTATION =
        arrayOf("Самостоятельно на общественном транспорте",
            "Самостоятельно на личном транспорте",
            "Самостоятельно пешком",
            "Транспортировка выполняется заказчиком")

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
        holder.orderNumber.text = ordersFilterList[position].number

        if (ordersFilterList[position].typeOrder.isNullOrEmpty()){
            holder.orderType.text="Тип заявки"
        } else {
            holder.orderType.text=ordersFilterList[position].typeOrder
        }
        when (ordersFilterList[position].state) {
            "1" -> holder.orderState.text="В работе"
        }
        holder.orderDate.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(ordersFilterList[position].dateCreate)
        holder.orderName.text = ordersFilterList[position].name
        holder.orderadress.text = ordersFilterList[position].address
        holder.fio.text = ordersFilterList[position].contactFio
        /*if (ordersFilterList[position].typeTransportation.isNullOrEmpty()) {
            holder.typeTransportation.text="Нет данных"
        } else {
            holder.typeTransportation.text=orders[position].typeTransportation
        }*/


        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            parentFragment.requireContext(),
            R.layout.template_multiline_spinner_item, //android.R.layout.simple_dropdown_item_1line,
            SPINNER_TYPE_TRANSPORTATION
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

        if (ordersFilterList[position].checked) {
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
        }
    }

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("11_recyclerViewListClicked")
            listener.recyclerViewListClicked(v, this.layoutPosition)
        }

        var orderNumber: TextView = itemView.number
        var orderType: TextView = itemView.order_type
        var orderState: TextView = itemView.order_state
        var orderDate: TextView = itemView.date
        var orderName: TextView = itemView.name
        var orderadress: TextView = itemView.adress
        var fio: TextView = itemView.fio
        //var typeTransportation: TextView = itemView.type_transportation
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
                Timber.d(charSearch)

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
                notifyDataSetChanged()
            }

        }
    }

    val typeTransportationChange=object:TextWatcher{
        override fun afterTextChanged(s: Editable?) {

        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            TODO("Not yet implemented")
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            TODO("Not yet implemented")
        }

    }

}