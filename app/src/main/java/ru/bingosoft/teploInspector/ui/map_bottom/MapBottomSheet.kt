package ru.bingosoft.teploInspector.ui.map_bottom

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import com.yandex.mapkit.geometry.Point
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.ui.order.OrderPresenter
import ru.bingosoft.teploInspector.util.Const
import ru.bingosoft.teploInspector.util.OtherUtil
import ru.bingosoft.teploInspector.util.Toaster
import ru.bingosoft.teploInspector.util.UserLocationNative
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class MapBottomSheet(val order: Orders, private val userLocation: Point, private val parentFragment: MapFragment): BottomSheetDialogFragment(), MapBottomSheetContractView, View.OnClickListener {

    private lateinit var rootView: View

    @Inject
    lateinit var mbsPresenter: MapBottomSheetPresenter

    @Inject
    lateinit var orderPresenter: OrderPresenter

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var otherUtil: OtherUtil

    @Inject
    lateinit var userLocationNative: UserLocationNative

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)

        val locationManager=this.requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 10f, userLocationNative.locationListener)
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        Timber.d("MapBottomSheet_setupDialog")

        val view=
            LayoutInflater.from(context).inflate(R.layout.map_bottom_sheet,null)
        this.rootView=view
        dialog.setContentView(view)

        view.findViewById<TextView>(R.id.number).text="№ ${order.number}"
        view.findViewById<TextView>(R.id.order_type).text=order.typeOrder
        if (order.typeOrder.isNullOrEmpty()){
            view.findViewById<TextView>(R.id.order_type).text="Тип заявки"
        } else {
            view.findViewById<TextView>(R.id.order_type).text=order.typeOrder
        }

        val mbsOrderState=rootView.findViewById<MaterialBetterSpinner>(R.id.order_state)
        mbsOrderState.setText(order.status?.toUpperCase())
        changeColorMBSState(mbsOrderState, order.status)

        val adapterStatus: ArrayAdapter<String> = ArrayAdapter<String>(
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
                    mbsOrderState.setText(order.status?.toUpperCase())
                    mbsOrderState.addTextChangedListener(this)
                    return
                }

                if (s.toString().toUpperCase()!=order.status?.toUpperCase()) {
                    order.status=s.toString().toLowerCase().capitalize()
                    changeColorMBSState(mbsOrderState, order.status)
                    orderPresenter.addHistoryState(order)
                }

                mbsOrderState.removeTextChangedListener(this)
                mbsOrderState.setText(s.toString().toUpperCase())
                mbsOrderState.addTextChangedListener(this)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }

        })

        if (order.dateVisit!=null && order.timeVisit!=null) {
            val strDateTimeVisit="${order.dateVisit} ${order.timeVisit}"
            val date=SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ru","RU")).parse(strDateTimeVisit)
            view.findViewById<TextView>(R.id.date).text=SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(date)
        } else {
            view.findViewById<TextView>(R.id.date).text=getString(R.string.not_date_visit)
        }

        view.findViewById<TextView>(R.id.name).text=order.purposeObject
        view.findViewById<TextView>(R.id.adress).text=order.address
        view.findViewById<TextView>(R.id.fio).text=order.contactFio
        /*if (order.typeTransportation.isNullOrEmpty()) {
            view.findViewById<TextView>(R.id.type_transportation).text="Нет данных"
        } else {
            view.findViewById<TextView>(R.id.type_transportation).text=order.typeTransportation
        }*/

        val btnPhone=view.findViewById<Button>(R.id.btnPhone)
        if (order.phone.isNullOrEmpty()) {
            btnPhone.text=btnPhone.context.getString(R.string.no_contact)
            btnPhone.isEnabled=false
            btnPhone.setTextColor(R.color.enabledText)
        } else {
            btnPhone.text=order.phone
        }

        val btnRoute=view.findViewById<Button>(R.id.btnRoute)
        //btnRoute.text="Маршрут 3.2 км"


        /*val distance=otherUtil.getDistance(userLocationNative.userLocation, order)
        btnRoute.text=requireContext().getString(R.string.distance, distance.toString())//"Маршрут 3.2 км"*/

        if (userLocationNative.userLocation.latitude!=0.0 && userLocationNative.userLocation.longitude!=0.0) {
            val distance=otherUtil.getDistance(userLocationNative.userLocation, order)
            btnRoute.text=rootView.context.getString(R.string.distance, distance.toString())//"Маршрут 3.2 км"
        } else {
            btnRoute.text=rootView.context.getString(R.string.route)
        }

        btnPhone.setOnClickListener { _ ->
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phone}"))
            if (intent.resolveActivity(btnPhone.context.packageManager) != null) {
                ContextCompat.startActivity(btnPhone.context, intent, null)
            }
        }

        btnRoute.setOnClickListener { _ ->
            Timber.d("btnRoute.setOnClickListener")

            //Включаем фрагмент со списком Маршрутов для конкретной заявки
            parentFragment.showRouteDialog(order)
            hideBottomSheet()

        }

        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
            parentFragment.requireContext(),
            R.layout.template_multiline_spinner_item,
            Const.TypeTransportation.list
        )

        val mbsTypeTransportation=view.findViewById<MaterialBetterSpinner>(R.id.type_transportation)
        mbsTypeTransportation.setAdapter(adapter)

        mbsTypeTransportation.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btnRoute.isEnabled = s.toString() != parentFragment.requireContext().getString(R.string.strTypeTransportationClient)
                if (btnRoute.isEnabled) {
                    btnRoute.setTextColor(Color.parseColor("#2D3239"))
                } else {
                    btnRoute.setTextColor(Color.parseColor("#C7CCD1"))
                }

                if (s.toString().toUpperCase()!=order.typeTransportation?.toUpperCase()) {
                    order.typeTransportation=s.toString()
                    orderPresenter.changeTypeTransortation(order)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }

        })

        mbsPresenter.attachView(this)

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

    override fun onDestroy() {
        super.onDestroy()
        mbsPresenter.onDestroy()
    }


    private fun hideBottomSheet() {
        // Закроем BottomSheetDialog
        val params =(rootView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior
        if (behavior!=null && behavior is BottomSheetBehavior) {
            behavior.state= BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun onClick(v: View?) {

    }


}