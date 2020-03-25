package ru.bingosoft.teploInspector.ui.map_bottom

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.geometry.Point
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import javax.inject.Inject

//val mapView: MapView, val directions: Directions
class MapBottomSheet(val order: Orders, val userLocation: Point, val parentFragment: MapFragment): BottomSheetDialogFragment(), MapBottomSheetContractView, View.OnClickListener {

    private lateinit var drivingRouter: DrivingRouter
    private lateinit var rootView: View

    @Inject
    lateinit var mbsPresenter: MapBottomSheetPresenter

    @Inject
    lateinit var toaster: Toaster

    private var drivingSession: DrivingSession? = null

    val drivingRouteListener=object:DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutesError(error: Error) {
            var errorMessage = getString(R.string.unknown_error_message)
            if (error is RemoteError) {
                errorMessage = getString(R.string.remote_error_message)
            } else if (error is NetworkError) {
                errorMessage = getString(R.string.network_error_message)
            }

            toaster.showToast(errorMessage)
        }

        override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
            for (route in routes) {
                parentFragment.mapView.map.mapObjects.addPolyline(route.geometry)
            }
        }
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        AndroidSupportInjection.inject(this)
        super.setupDialog(dialog, style)

        Timber.d("MapBottomSheet_setupDialog")

        val view=
            LayoutInflater.from(context).inflate(R.layout.map_bottom_sheet,null)
        this.rootView=view
        dialog.setContentView(view)

        val btn = view.findViewById(R.id.btnCar) as Button
        btn.setOnClickListener(this)

        mbsPresenter.attachView(this)

        // Заполним текстовые поля
        val tvNumber=rootView.findViewById<TextView>(R.id.symbolNumber)
        tvNumber?.text=order.number

        val tvName=rootView.findViewById<TextView>(R.id.symbolName)
        tvName?.text=order.name
    }

    override fun onDestroy() {
        super.onDestroy()
        mbsPresenter.onDestroy()
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.btnCar -> {
                    Timber.d("MapBottomSheet_onClick")
                    Timber.d("orderPoint=${order.lat}_${order.lon}")
                    Timber.d("userLocation=${userLocation.latitude}_${userLocation.longitude}")

                    drivingRouter=parentFragment.directions.createDrivingRouter()
                    submitRequest()

                    val params =
                        (rootView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
                    val behavior = params.behavior
                    if (behavior!=null && behavior is BottomSheetBehavior) {
                        behavior.state= BottomSheetBehavior.STATE_HIDDEN
                    }
                }
            }
        }
    }



    private fun submitRequest() {
        val options = DrivingOptions()
        val requestPoints: ArrayList<RequestPoint> = ArrayList()
        requestPoints.add(
            RequestPoint(
                Point(order.lat,order.lon),
                RequestPointType.WAYPOINT,
                null
            )
        )
        requestPoints.add(
            RequestPoint(
                Point(userLocation.latitude,userLocation.longitude),
                RequestPointType.WAYPOINT,
                null
            )
        )
        drivingSession = drivingRouter.requestRoutes(requestPoints, options, drivingRouteListener)
    }

}