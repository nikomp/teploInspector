package ru.bingosoft.teploInspector.ui.checkup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.item_general_information.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import timber.log.Timber

class TechnicalCharacteristicAdapter(private val lists: List<TechParams>) : RecyclerView.Adapter<TechnicalCharacteristicAdapter.TcItemsViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TcItemsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_technical_characteristic, parent, false)
        return TcItemsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    override fun onBindViewHolder(holder: TcItemsViewHolder, position: Int) {
        holder.giName.text=lists[position].technical_characteristic
        holder.giValue.setText(lists[position].value)
    }

    class TcItemsViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("11_recyclerViewListClicked")
            //listener.recyclerViewListClicked(v, this.layoutPosition)
        }

        var giName:TextView=itemView.gi_name
        var giValue:TextInputEditText=itemView.gi_value_edit
    }
}