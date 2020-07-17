package ru.bingosoft.teploInspector.ui.checkup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.item_general_information.view.*
import ru.bingosoft.teploInspector.R
import timber.log.Timber

class GeneralInformationAdapter(private val lists: List<String>) : RecyclerView.Adapter<GeneralInformationAdapter.GiItemsViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GeneralInformationAdapter.GiItemsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_general_information, parent, false)
        return GeneralInformationAdapter.GiItemsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    override fun onBindViewHolder(holder: GeneralInformationAdapter.GiItemsViewHolder, position: Int) {
        holder.giName.text=lists[position]
        holder.giValue.setText("Значение")
    }

    class GiItemsViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("11_recyclerViewListClicked")
            //listener.recyclerViewListClicked(v, this.layoutPosition)
        }

        var giName:TextView=itemView.gi_name
        var giValue:TextInputEditText=itemView.gi_value_edit
    }
}