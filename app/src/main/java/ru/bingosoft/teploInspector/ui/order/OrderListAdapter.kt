package ru.bingosoft.teploInspector.ui.order

import android.annotation.SuppressLint
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
import kotlinx.android.synthetic.main.item_cardview_order.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class OrderListAdapter (val orders: List<Orders>, private val itemListener: OrdersRVClickListeners, private val ctx: Context) : RecyclerView.Adapter<OrderListAdapter.OrderViewHolder>() {

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
        holder.orderDate.text = SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).format(orders[position].dateCreate)
        holder.orderName.text = orders[position].name
        holder.orderadress.text = orders[position].adress
        holder.fio.text = orders[position].contactFio
        holder.phone.text = orders[position].phone

        holder.phone.setOnClickListener { _ ->
            Timber.d("fabButton.setOnClickListener ${orders[position].phone}")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${orders[position].phone}"))
            if (intent.resolveActivity(ctx.packageManager) != null) {
                startActivity(ctx,intent,null)
            }
        }

        if (orders[position].state.equals("1")) {
            holder.targetImage.setImageResource(R.drawable.ic_flash_on_black_24dp)
        } else {
            holder.targetImage.setImageResource(R.drawable.ic_flash_on_black_done24dp)
        }


        holder.listener=itemListener

        /*holder.fabButton.setOnClickListener{
            Timber.d("fabButton.setOnClickListener ${orders[position].phone}")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${orders[position].phone}"))
            if (intent.resolveActivity(ctx.packageManager) != null) {
                startActivity(ctx,intent,null)
            }
        }*/


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

        var orderNumber: TextView = itemView.number
        var orderDate: TextView = itemView.date
        var orderName: TextView = itemView.name
        var orderadress: TextView = itemView.adress
        var fio: TextView = itemView.fio
        var phone: TextView = itemView.phone
        var targetImage: ImageView = itemView.targetImage as ImageView
        //var fabButton: FloatingActionButton = itemView.fab2 as FloatingActionButton
        lateinit var listener: OrdersRVClickListeners

        var cardView: CardView = itemView.cv

        init {


            view.setOnClickListener(this)
        }


    }

}