package ru.bingosoft.teploInspector.ui.map_bottom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import timber.log.Timber

class MapBottomOrdersListAdapter(val orders: List<Orders>, private val itemListener: MapBottomOrdersRVClickListeners): RecyclerView.Adapter<MapBottomOrdersListAdapter.OrdersViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrdersViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mab_bottom_orders, parent, false)
        return OrdersViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: OrdersViewHolder,
        position: Int
    ) {
        holder.orderNumber.text = holder.itemView.context.getString(R.string.order_number,orders[position].number)
        /*if (orders[position].typeOrder.isNullOrEmpty()){
            holder.orderType.text="Тип заявки"
        } else {
            holder.orderType.text=orders[position].typeOrder
        }*/
        holder.orderType.text=orders[position].typeOrder
        holder.listener=itemListener
    }

    override fun getItemCount(): Int {
        return orders.size
    }

    class OrdersViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("orderlistadapter_recyclerViewListClicked")
            listener.recyclerViewListClicked(v, this.layoutPosition)
        }

        var orderNumber: TextView = itemView.findViewById(R.id.orderNumber)
        var orderType: TextView = itemView.findViewById(R.id.orderType)
        lateinit var listener: MapBottomOrdersRVClickListeners

        init {
            view.setOnClickListener(this)
        }


    }
}