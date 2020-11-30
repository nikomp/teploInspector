package ru.bingosoft.teploInspector.ui.checkup

import android.text.Editable
import android.text.TextWatcher
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
            // Заблокированные поля
            "Номер договора"->{
                holder.giValue.setText(order.giContractNumber)
                holder.giValue.isFocusable=false
            }
            "Дата договора"->
            {
                if (order.giContractDate!=null) {
                    holder.giValue.setText(SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).format(order.giContractDate!!))
                }
                holder.giValue.isFocusable=false
            }
            "Юридический адрес"->{
                holder.giValue.setText(order.giLegalAddress)
                holder.giValue.isFocusable=false
            }
            "Телефон"->{
                holder.giValue.setText(order.giPhone)
                holder.giValue.isFocusable=false
            }
            "Почтовый адрес"->{
                holder.giValue.setText(order.giPostAddress)
                holder.giValue.isFocusable=false
            }
            "Контрагент"-> {
                holder.giValue.setText(order.giContractor)
                holder.giValue.isFocusable=false
            }
            "Принадлежность трассы"->{
                holder.giValue.setText(order.giTrackOwnership)
                holder.giValue.isFocusable=false
            }
            "Принадлежность узла"->{
                holder.giValue.setText(order.giNodeOwnership)
                holder.giValue.isFocusable=false
            }
            "РТС"->{
                holder.giValue.setText(order.giRtc)
                holder.giValue.isFocusable=false
            }
            "Принадлежность (ОР)"->{
                holder.giValue.setText(order.gi_belong_or)
                holder.giValue.isFocusable=false
            }
            "Принадлежность (УЭН)"->{
                holder.giValue.setText(order.gi_belong_uen)
                holder.giValue.isFocusable=false
            }

            // Поля, которые можно менять
            "Управл. организация (УЭН)"-> holder.giValue.setText(order.giManagingOrganizationUen)
            "Подрядная организация"->holder.giValue.setText(order.giContractingOrganization)
            "Ответственный за ТХ"->holder.giValue.setText(order.gi_responsible_tx)
            "Руководитель"->holder.giValue.setText(order.giDirector)
            "Телефон ответственного гор."->holder.giValue.setText(order.giResponsiblePhoneCity)
            "Телефон ответственного моб."->holder.giValue.setText(order.giResponsiblePhoneMob)
            "Телефон руководителя гор."->holder.giValue.setText(order.giDirectorPhoneCity)
            "Телефон руководителя моб."->holder.giValue.setText(order.giDirectorPhoneMob)
            "Электронная почта"->holder.giValue.setText(order.giEmail)
            "Мастер РТС"->holder.giValue.setText(order.giMasterRtc)
            else -> holder.giValue.setText("Значение")
        }

        holder.giValue.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }

            override fun afterTextChanged(s: Editable?) {
                Timber.d("GeneralInformationAdapter_beforeTextChanged")
                when (holder.giName.text) {
                    "Управл. организация (УЭН)"-> order.giManagingOrganizationUen=s.toString()
                    "Подрядная организация"->order.giContractingOrganization=s.toString()
                    "Ответственный за ТХ"->order.gi_responsible_tx=s.toString()
                    "Руководитель"->order.giDirector=s.toString()
                    "Телефон ответственного гор."->order.giResponsiblePhoneCity=s.toString()
                    "Телефон ответственного моб."->order.giResponsiblePhoneMob=s.toString()
                    "Телефон руководителя гор."->order.giDirectorPhoneCity=s.toString()
                    "Телефон руководителя моб."->order.giDirectorPhoneMob=s.toString()
                    "Электронная почта"->order.giEmail=s.toString()
                    "Мастер РТС"->order.giMasterRtc=s.toString()
                }
            }

        })

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