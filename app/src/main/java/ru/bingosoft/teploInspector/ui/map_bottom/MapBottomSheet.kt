package ru.bingosoft.teploInspector.ui.map_bottom

import android.app.Dialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.SubpolylineHelper
import com.yandex.mapkit.transport.masstransit.*
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

class MapBottomSheet(val order: Orders, private val userLocation: Point, val parentFragment: MapFragment): BottomSheetDialogFragment(), MapBottomSheetContractView, View.OnClickListener {

    private lateinit var carRouter: DrivingRouter
    private lateinit var busRouter: MasstransitRouter

    private lateinit var rootView: View

    @Inject
    lateinit var mbsPresenter: MapBottomSheetPresenter

    @Inject
    lateinit var toaster: Toaster

    private var drivingSession: DrivingSession? = null
    private val requestPoints: ArrayList<RequestPoint> = ArrayList()
    lateinit var foundingRoutes: MutableList<Route>

    private val routerRVClickListeners=object:RouterRVClickListeners{
        override fun routerRVListClicked(v: View?, position: Int) {
            Timber.d("routerRVListClicked")
            // Строим маршрут
            val route=foundingRoutes[position]
            val sections= route.sections
            Timber.d("sections=${sections.size}")
            sections.forEach{
                Timber.d("section=${it.metadata.data.transports?.get(0)?.line?.name}")
                drawSection(
                    it.metadata.data,
                    SubpolylineHelper.subpolyline(
                        route.geometry, it.geometry
                    ))
            }

            hideBottomSheet()
        }

    }


    fun drawSection(data: SectionMetadata.SectionData, geometry: Polyline) {
        val polylineMapObject = parentFragment.mapView.map.mapObjects.addPolyline(geometry)
        parentFragment.lastCarRouter.add(polylineMapObject) // Сохраним маршрут, чтоб потом можно было его удалить

        if (data.transports != null) {
            val transports=data.transports
            transports!!.forEach {
                if (it.line.style != null) {
                    polylineMapObject.strokeColor = it.line.style!!.color!!
                    return
                }
            }

            val knownVehicleTypes = HashSet<String>()
            knownVehicleTypes.add("bus")
            knownVehicleTypes.add("tramway")

            transports.forEach {
                val sectionVehicleType = getVehicleType(it, knownVehicleTypes)
                if (sectionVehicleType.equals("bus")) {
                    polylineMapObject.strokeColor = Color.GREEN
                    return
                } else if (sectionVehicleType.equals("tramway")) {
                    polylineMapObject.strokeColor = Color.RED
                    return
                }
            }
            polylineMapObject.strokeColor = Color.BLUE
        } else {
            polylineMapObject.strokeColor = Color.BLACK
        }
    }

    //https://github.com/yandex/mapkit-android-demo/blob/master/src/main/java/com/yandex/mapkitdemo/MasstransitRoutingActivity.java
    private val routeListener=object:Session.RouteListener{
        override fun onMasstransitRoutesError(error: Error) {
            var errorMessage = getString(R.string.unknown_error_message)
            if (error is RemoteError) {
                errorMessage = getString(R.string.remote_error_message)
            } else if (error is NetworkError) {
                errorMessage = getString(R.string.network_error_message)
            }

            toaster.showToast(errorMessage)
        }

        override fun onMasstransitRoutes(routes: MutableList<Route>) {
            showRoutersList(routes)
            if (routes.isNotEmpty()) {
                foundingRoutes=routes
            }


            /*if (routes.isNotEmpty()) {
                val sections= routes[0].sections
                Timber.d("sections=${sections.size}")
                sections.forEach{
                    Timber.d("section=${it.metadata.data.transports?.get(0)?.line?.name}")
                    drawSection(
                        it.metadata.data,
                        SubpolylineHelper.subpolyline(
                            routes[0].geometry, it.geometry
                        ))
                }
            }*/
        }



        fun showRoutersList(routes: MutableList<Route>) {
            val routes_recycler_view = rootView.findViewById(R.id.routers_recycler_view) as RecyclerView

            val params=routes_recycler_view.layoutParams
            params.height=330
            routes_recycler_view.layoutParams=params

            routes_recycler_view.layoutManager = LinearLayoutManager(rootView.context)
            val adapter = RouterListAdapter(routes,routerRVClickListeners)
            routes_recycler_view.adapter = adapter
        }
    }


    fun getVehicleType(transport: Transport, knownVehicleTypes:HashSet<String>):String? {
        val type=transport.line.vehicleTypes
        Timber.d("transport=${transport.line.name}_${transport.line.vehicleTypes}")
        type.forEach{
            if (knownVehicleTypes.contains(it)) {
                return it
            }
        }
        return null
    }

    private val drivingRouteListener=object:DrivingSession.DrivingRouteListener {
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
            if (routes.isNotEmpty()) {
                routes.forEach {
                    Timber.d("saveRoute")
                    val polylineMapObject=parentFragment.mapView.map.mapObjects.addPolyline(it.geometry)
                    parentFragment.lastCarRouter.add(polylineMapObject) // Сохраним маршрут, чтоб потом можно было его удалить
                }

                Timber.d("lastCarRouter?.size=${parentFragment.lastCarRouter.size}")
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

        val btnCar = view.findViewById(R.id.btnCar) as Button
        btnCar.setOnClickListener(this)

        val btnBus = view.findViewById(R.id.btnBus) as Button
        btnBus.setOnClickListener(this)

        mbsPresenter.attachView(this)

        // Заполним текстовые поля
        val tvNumber=rootView.findViewById<TextView>(R.id.symbolNumber)
        tvNumber?.text=order.number

        val tvName=rootView.findViewById<TextView>(R.id.symbolName)
        tvName?.text=order.name

        //Сохраним маршрут с точками
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
    }

    override fun onDestroy() {
        super.onDestroy()
        mbsPresenter.onDestroy()
    }

    override fun onClick(v: View?) {
        if (v != null) {
            //Очистим предыдущий маршрут
            if (parentFragment.lastCarRouter.isNotEmpty()) {
                parentFragment.removeRouter(parentFragment.lastCarRouter)
            }

            when (v.id) {
                R.id.btnCar -> {
                    carRouter=parentFragment.directions.createDrivingRouter()
                    val options = DrivingOptions()
                    drivingSession = carRouter.requestRoutes(requestPoints, options, drivingRouteListener)

                    hideBottomSheet()
                }

                R.id.btnBus -> {
                    busRouter = parentFragment.transports.createMasstransitRouter()
                    val options = MasstransitOptions(ArrayList<String>(), ArrayList<String>(), TimeOptions())
                    busRouter.requestRoutes(requestPoints, options, routeListener)
                }
            }


        }
    }

    fun hideBottomSheet() {
        // Закроем BottomSheetDialog
        val params =(rootView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior
        if (behavior!=null && behavior is BottomSheetBehavior) {
            behavior.state= BottomSheetBehavior.STATE_HIDDEN
        }
    }


}