package ru.bingosoft.teploInspector.ui.checkup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.item_general_information.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class GeneralInformationAdapter(private val lists: List<String>, val order :Orders) : RecyclerView.Adapter<GeneralInformationAdapter.GiItemsViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GiItemsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_general_information, parent, false)
        return GiItemsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return lists.size
    }

    override fun onBindViewHolder(holder: GiItemsViewHolder, position: Int) {
        Timber.d("order=$order")
        holder.giName.text=lists[position]
        when (lists[position]) {
            "Номер договора"->holder.giValue.setText(order.giContractNumber)
            "Дата договора"->
            {
                if (order.giContractDate!=null) {
                    holder.giValue.setText(SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).format(order.giContractDate))
                }

            }
            "Юридический адрес"->holder.giValue.setText(order.giLegalAddress)
            "Телефон"->holder.giValue.setText(order.giPhone)
            "Электронная почта"->holder.giValue.setText(order.giEmail)
            "Почтовый адрес"->holder.giValue.setText(order.giPostAddress)
            "Контрагент"->holder.giValue.setText(order.giContractor)
            "Ответственный за ТХ"->holder.giValue.setText(order.gi_responsible_tx)
            "Руководитель"->holder.giValue.setText(order.giDirector)
            "Телефон ответственного гор."->holder.giValue.setText(order.giResponsiblePhoneCity)
            "Телефон ответственного моб."->holder.giValue.setText(order.giResponsiblePhoneMob)
            "Телефон руководителя гор."->holder.giValue.setText(order.giDirectorPhoneCity)
            "Телефон руководителя моб."->holder.giValue.setText(order.giDirectorPhoneMob)
            "Принадлежность (ОР)"->holder.giValue.setText(order.gi_belong_or)
            "Принадлежность (УЭН)"->holder.giValue.setText(order.gi_belong_uen)
            "Управл. организация (УЭН)"->holder.giValue.setText(order.giManagingOrganizationUen)
            "Подрядная организация"->holder.giValue.setText(order.giContractingOrganization)
            "Принадлежность трассы"->holder.giValue.setText(order.giTrackOwnership)
            "Принадлежность узла"->holder.giValue.setText(order.giNodeOwnership)
            "РТС"->holder.giValue.setText(order.giRtc)
            "Мастер РТС"->holder.giValue.setText(order.giMasterRtc)
            else -> holder.giValue.setText("Значение")
        }

    }

    class GiItemsViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        override fun onClick(v: View?) {
            Timber.d("generalInformation_recyclerViewListClicked")
            //listener.recyclerViewListClicked(v, this.layoutPosition)
        }

        var giName:TextView=itemView.gi_name
        var giValue:TextInputEditText=itemView.gi_value_edit
    }
}