package ru.bingosoft.mapquestapp2.ui.order

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.item_cardview_order.view.*
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Orders.Orders
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class OrderListAdapter (val orders: List<Orders>, val itemListener: OrdersRVClickListeners, val ctx: Context) : RecyclerView.Adapter<OrderListAdapter.OrderViewHolder>() {

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

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.orderNumber.text = orders[position].number
        holder.orderDate.text = SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).format(orders[position].dateCreate)
        holder.orderName.text = orders[position].name
        holder.orderadress.text = orders[position].adress
        holder.fio.text = orders[position].contactFio
        holder.phone.text = orders[position].phone
        holder.listener=itemListener

        holder.fabButton.setOnClickListener{
            Timber.d("fabButton.setOnClickListener ${orders[position].phone}")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${orders[position].phone}"))
            if (intent.resolveActivity(ctx.packageManager) != null) {
                startActivity(ctx,intent,null)
            }
        }


        if (orders[position].checked) {
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    ctx,
                    R.color.colorCardSelect
                ))
        } else {
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    ctx,
                    R.color.colorCardItem
                ))
        }
    }

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            listener.recyclerViewListClicked(v, this.layoutPosition)
        }

        var orderNumber: TextView
        var orderDate: TextView
        var orderName: TextView
        var orderadress: TextView
        var fio: TextView
        var phone: TextView
        private var targetImage: ImageView
        var fabButton: FloatingActionButton
        lateinit var listener: OrdersRVClickListeners

        var cardView: CardView = itemView.cv

        init {
            orderNumber = itemView.number
            orderDate = itemView.date
            orderName = itemView.name
            orderadress = itemView.adress
            fio = itemView.fio
            phone = itemView.phone
            this.targetImage = itemView.targetImage as ImageView
            fabButton=itemView.fab2 as FloatingActionButton


            view.setOnClickListener(this)
        }


    }

}