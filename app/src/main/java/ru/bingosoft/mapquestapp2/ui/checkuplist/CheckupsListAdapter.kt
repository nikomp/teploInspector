package ru.bingosoft.mapquestapp2.ui.checkuplist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_cardview_checkuplist.view.*
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup

class CheckupsListAdapter(val checkups: List<Checkup>, val itemListener: CheckupListRVClickListeners, val ctx: Context): RecyclerView.Adapter<CheckupsListAdapter.CheckupsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckupsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cardview_checkuplist, parent, false)
        return CheckupsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return checkups.size
    }

    fun getCheckup(position: Int): Checkup {
        return checkups[position]
    }

    override fun onBindViewHolder(holder: CheckupsViewHolder, position: Int) {
        holder.checkupsGuid.text = checkups[position].guid
        holder.listener=itemListener

    }

    class CheckupsViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            listener.recyclerViewListClicked(v, this.layoutPosition)
        }


        var checkupsGuid: TextView
        lateinit var listener: CheckupListRVClickListeners

        var cardView: CardView = itemView.cvCheckupList

        init {
            checkupsGuid = itemView.guid
            view.setOnClickListener(this)
        }


    }


}