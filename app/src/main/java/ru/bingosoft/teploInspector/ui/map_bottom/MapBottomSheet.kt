package ru.bingosoft.teploInspector.ui.map_bottom

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yandex.mapkit.geometry.Point
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.map.MapFragment
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

        view.findViewById<TextView>(R.id.number).text=order.number
        view.findViewById<TextView>(R.id.order_type).text=order.typeOrder
        if (order.typeOrder.isNullOrEmpty()){
            view.findViewById<TextView>(R.id.order_type).text="Нет данных"
        } else {
            view.findViewById<TextView>(R.id.order_type).text=order.typeOrder
        }
        when (order.state) {
            "1" -> view.findViewById<TextView>(R.id.order_state).text="В работе"
        }
        view.findViewById<TextView>(R.id.date).text=SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(order.dateCreate)
        view.findViewById<TextView>(R.id.name).text=order.name
        view.findViewById<TextView>(R.id.adress).text=order.address
        view.findViewById<TextView>(R.id.fio).text=order.contactFio
        if (order.typeTransportation.isNullOrEmpty()) {
            view.findViewById<TextView>(R.id.type_transportation).text="Нет данных"
        } else {
            view.findViewById<TextView>(R.id.type_transportation).text=order.typeTransportation
        }
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


        val distance=otherUtil.getDistance(userLocationNative.userLocation, order)
        btnRoute.text=requireContext().getString(R.string.distance, distance.toString())//"Маршрут 3.2 км"

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

        mbsPresenter.attachView(this)

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