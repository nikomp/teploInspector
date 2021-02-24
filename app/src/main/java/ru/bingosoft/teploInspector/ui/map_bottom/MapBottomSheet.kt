package ru.bingosoft.teploInspector.ui.map_bottom

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.alert_change_date_time.view.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.mainactivity.UserLocationReceiver
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.ui.order.OrderPresenter
import ru.bingosoft.teploInspector.util.Const
import ru.bingosoft.teploInspector.util.OtherUtil
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class MapBottomSheet(val orders: List<Orders>, private val parentFragment: MapFragment): BottomSheetDialogFragment(),
    MapBottomOrdersRVClickListeners {

    private lateinit var rootView: View

    @Inject
    lateinit var orderPresenter: OrderPresenter

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var otherUtil: OtherUtil

    @Inject
    lateinit var userLocationReceiver: UserLocationReceiver

    /*@Inject
    lateinit var userLocationNative: UserLocationNative*/

    private var currentOrder: Orders?=null
    private var rcv: RecyclerView?=null
    private var cv: CardView?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)

    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        Timber.d("MapBottomSheet_setupDialog")
        rootView=View.inflate(parentFragment.requireContext(),R.layout.map_bottom_sheet,null)

        dialog.setContentView(rootView)

        rcv=rootView.findViewById(R.id.rvListOrders) as RecyclerView
        rcv?.layoutManager = LinearLayoutManager(this.activity)
        cv=rootView.findViewById(R.id.cv) as CardView
        if (orders.size>1) {
            cv?.visibility=View.GONE
            rcv?.visibility=View.VISIBLE
            val adapter = MapBottomOrdersListAdapter(orders,this)
            rcv?.adapter = adapter

        } else {
            cv?.visibility=View.VISIBLE
            rcv?.visibility=View.GONE
            fillOrderData(orders.last())
            currentOrder=orders.last()
        }
    }

    private fun fillOrderData(order: Orders) {
        rootView.findViewById<TextView>(R.id.number).text=parentFragment.getString(R.string.order_number,order.number)
        rootView.findViewById<TextView>(R.id.order_type).text= order.typeOrder
        if (order.typeOrder.isNullOrEmpty()){
            rootView.findViewById<TextView>(R.id.order_type).text="Тип заявки"
        } else {
            rootView.findViewById<TextView>(R.id.order_type).text=order.typeOrder
        }

        val mbsOrderState=rootView.findViewById<MaterialBetterSpinner>(R.id.order_state)
        mbsOrderState.setText(order.status?.toUpperCase(Locale.ROOT))
        changeColorMBSState(mbsOrderState, order.status)

        val btnChangeDateTime=rootView.findViewById<Button>(R.id.btnChangeDateTime)
        btnChangeDateTime.setOnClickListener {
            Timber.d("btnChangeDateTime_setOnClickListener")
            lateinit var alertDialog: AlertDialog
            val layoutInflater = LayoutInflater.from(parentFragment.requireContext())
            val dialogView: View =
                layoutInflater.inflate(R.layout.alert_change_date_time, (rootView as ViewGroup), false)

            val builder = AlertDialog.Builder(parentFragment.requireContext())

            dialogView.btnOk.setOnClickListener{
                Timber.d("dialogView.buttonOK")
                val newDate=dialogView.findViewById<TextView>(R.id.newDate)
                val newTime=dialogView.findViewById<TextView>(R.id.newTime)
                val strDateTimeVisit="${newDate.text} ${newTime.text}"

                Timber.d(strDateTimeVisit)
                try {
                    val date=SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).parse(strDateTimeVisit)
                    if (date!=null) {
                        btnChangeDateTime.text=
                            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(date)
                        order.dateVisit=SimpleDateFormat("yyyy-MM-dd", Locale("ru","RU")).format(date)
                        order.timeVisit=SimpleDateFormat("HH:mm:ss", Locale("ru","RU")).format(date)

                        orderPresenter.saveDateTime(order)
                        (parentFragment.requireActivity() as MainActivity).currentOrder=order

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }


                alertDialog.dismiss()

            }

            builder.setView(dialogView)
            builder.setCancelable(true)
            alertDialog=builder.create()
            alertDialog.show()
        }

        val adapterStatus: ArrayAdapter<String> = ArrayAdapter(
            rootView.context,
            R.layout.template_multiline_spinner_item_state_order,
            R.id.text1,
            Const.StatusOrder.list
        )

        mbsOrderState.setAdapter(adapterStatus)

        mbsOrderState.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Если статус меняется на Выполнена, а чек лист пуст, выдаем сообщение
                if (order.answeredCount==0 && s.toString()=="Выполнена") {
                    parentFragment.toaster.showToast(R.string.checklist_not_changed_status)
                    mbsOrderState.removeTextChangedListener(this)
                    mbsOrderState.setText(order.status?.toUpperCase(Locale.ROOT))
                    mbsOrderState.addTextChangedListener(this)
                    return
                }

                if (s.toString().toUpperCase(Locale.ROOT) != order.status?.toUpperCase(Locale.ROOT)) {
                    order.status=s.toString().toLowerCase(Locale.ROOT).capitalize()
                    changeColorMBSState(mbsOrderState, order.status)
                    try {
                        orderPresenter.addHistoryState(order)
                    } catch (e: Throwable) {
                        (activity as MainActivity).errorReceived(e)
                    }
                }

                mbsOrderState.removeTextChangedListener(this)
                mbsOrderState.setText(s.toString().toUpperCase(Locale.ROOT))
                mbsOrderState.addTextChangedListener(this)

                //Фильтруем по статусу
                if (s.toString()=="Выполнена" || s.toString()=="Отменена") {
                    (activity as MainActivity).filteredOrders=(activity as MainActivity).filteredOrders.filter {it.id!=currentOrder?.id }
                    Timber.d("фильтранули=${(activity as MainActivity).filteredOrders}")
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })

        if (order.dateVisit!=null && order.timeVisit!=null) {
            val strDateTimeVisit="${order.dateVisit} ${order.timeVisit}"
            try {
                val date=SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ru","RU")).parse(strDateTimeVisit)
                if (date!=null) {
                    rootView.findViewById<TextView>(R.id.btnChangeDateTime).text=SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(date)
                }
            } catch (e:Exception) {
                e.printStackTrace()
            }

        } else {
            rootView.findViewById<TextView>(R.id.btnChangeDateTime).text=getString(R.string.not_date_visit)
        }

        rootView.findViewById<TextView>(R.id.name).text=order.purposeObject
        rootView.findViewById<TextView>(R.id.adress).text=order.address
        rootView.findViewById<TextView>(R.id.fio).text=order.contactFio

        val btnPhone=rootView.findViewById<Button>(R.id.btnPhone)
        if (order.phone.isNullOrEmpty()) {
            btnPhone.text=btnPhone.context.getString(R.string.no_contact)
            btnPhone.isEnabled=false
            btnPhone.setTextColor(ContextCompat.getColor(parentFragment.requireContext(),R.color.enabledText))
        } else {
            btnPhone.text=order.phone
        }

        val btnRoute=rootView.findViewById<Button>(R.id.btnRoute)

        if (userLocationReceiver.lastKnownLocation.latitude!=0.0 && userLocationReceiver.lastKnownLocation.longitude!=0.0) {
            val distance=otherUtil.getDistance(userLocationReceiver.lastKnownLocation, order)
            btnRoute.text=rootView.context.getString(R.string.distance, distance.toString())//"Маршрут 3.2 км"
        } else {
            btnRoute.text=rootView.context.getString(R.string.route)
        }

        btnPhone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phone}"))
            if (intent.resolveActivity(btnPhone.context.packageManager) != null) {
                ContextCompat.startActivity(btnPhone.context, intent, null)
            }
        }

        btnRoute.setOnClickListener {
            Timber.d("btnRoute.setOnClickListener")

            //Включаем фрагмент со списком Маршрутов для конкретной заявки
            parentFragment.showRouteDialog(order)
            hideBottomSheet()

        }

        val adapter: ArrayAdapter<String> = ArrayAdapter(
            parentFragment.requireContext(),
            R.layout.template_multiline_spinner_item,
            Const.TypeTransportation.list
        )

        val mbsTypeTransportation=rootView.findViewById<MaterialBetterSpinner>(R.id.type_transportation)
        mbsTypeTransportation.setAdapter(adapter)

        mbsTypeTransportation.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btnRoute.isEnabled = s.toString() != parentFragment.requireContext().getString(R.string.strTypeTransportationClient)
                if (btnRoute.isEnabled) {
                    btnRoute.setTextColor(Color.parseColor("#2D3239"))
                } else {
                    btnRoute.setTextColor(Color.parseColor("#C7CCD1"))
                }

                if (s.toString().toUpperCase(Locale.ROOT) != order.typeTransportation?.toUpperCase(
                        Locale.ROOT
                    )
                ) {
                    order.typeTransportation=s.toString()
                    orderPresenter.changeTypeTransortation(order)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        })
    }

    fun changeColorMBSState(view: MaterialBetterSpinner, status:String?) {
        when (status) {
            getString(R.string.status_CANCEL) -> {
                view.setTextColor(Color.parseColor("#E94435"))
            }
            getString(R.string.status_TESTED) -> {
                view.setTextColor(Color.parseColor("#B370D7"))
            }
            getString(R.string.status_DONE) -> {
                view.setTextColor(Color.parseColor("#3DB650"))
            }
            getString(R.string.status_PAUSED) -> {
                view.setTextColor(Color.parseColor("#A5AEB6"))
            }
            getString(R.string.status_IN_WORK) -> {
                view.setTextColor(Color.parseColor("#309EFD"))
            }
            getString(R.string.status_IN_WAY) -> {
                view.setTextColor(Color.parseColor("#F28D17"))
            }
            getString(R.string.status_OPEN) -> {
                view.setTextColor(Color.parseColor("#56D5BE"))
            }

        }
    }


    private fun hideBottomSheet() {
        Timber.d("MapBottomSheet_hideBottomSheet")
        // Закроем BottomSheetDialog
        val params =(rootView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior
        if (behavior!=null && behavior is BottomSheetBehavior) {
            behavior.state= BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun recyclerViewListClicked(v: View?, position: Int) {
        cv?.visibility=View.VISIBLE
        rcv?.visibility=View.GONE
        fillOrderData(orders[position])
        currentOrder=orders[position]
    }

}