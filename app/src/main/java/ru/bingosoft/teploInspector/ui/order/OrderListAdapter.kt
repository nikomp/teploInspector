package ru.bingosoft.teploInspector.ui.order

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_cardview_order.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.map.MapFragment
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class OrderListAdapter (val orders: List<Orders>, private val itemListener: OrdersRVClickListeners, private val parentFragment: OrderFragment, private val userLocation: Location) : RecyclerView.Adapter<OrderListAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cardview_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun getItemCount(): Int {
        return orders.size
    }

    fun getOrder(position: Int): Orders {
        return orders[position]
    }



    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.orderNumber.text = orders[position].number

        if (orders[position].typeOrder.isNullOrEmpty()){
            holder.orderType.text="Нет данных"
        } else {
            holder.orderType.text=orders[position].typeOrder
        }
        when (orders[position].state) {
            "1" -> holder.orderState.text="В работе"
        }
        holder.orderDate.text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(orders[position].dateCreate)
        holder.orderName.text = orders[position].name
        holder.orderadress.text = orders[position].address
        holder.fio.text = orders[position].contactFio
        if (orders[position].typeTransportation.isNullOrEmpty()) {
            holder.typeTransportation.text="Нет данных"
        } else {
            holder.typeTransportation.text=orders[position].typeTransportation
        }

        if (orders[position].phone.isNullOrEmpty()) {
            holder.btnPhone.text=parentFragment.requireContext().getString(R.string.no_contact)
            holder.btnPhone.isEnabled=false
            holder.btnPhone.setTextColor(R.color.enabledText)
        } else {
            holder.btnPhone.text=orders[position].phone
        }

        if (userLocation.latitude!=0.0 && userLocation.longitude!=0.0) {
            val distance=parentFragment.otherUtil.getDistance(userLocation, orders[position])
            holder.btnRoute.text=parentFragment.requireContext().getString(R.string.distance, distance.toString())//"Маршрут 3.2 км"
        } else {
            holder.btnRoute.text=parentFragment.requireContext().getString(R.string.route)
        }

        holder.btnPhone.setOnClickListener { _ ->
            Timber.d("fabButton.setOnClickListener ${orders[position].phone}")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${orders[position].phone}"))
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

        if (orders[position].checked) {
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
            listener.recyclerViewListClicked(v, this.layoutPosition)
        }

        var orderNumber: TextView = itemView.number
        var orderType: TextView = itemView.order_type
        var orderState: TextView = itemView.order_state
        var orderDate: TextView = itemView.date
        var orderName: TextView = itemView.name
        var orderadress: TextView = itemView.adress
        var fio: TextView = itemView.fio
        var typeTransportation: TextView = itemView.type_transportation
        var btnPhone: Button=itemView.btnPhone
        var btnRoute: Button=itemView.btnRoute
        lateinit var listener: OrdersRVClickListeners

        var cardView: CardView = itemView.cv

        init {


            view.setOnClickListener(this)
        }


    }

}