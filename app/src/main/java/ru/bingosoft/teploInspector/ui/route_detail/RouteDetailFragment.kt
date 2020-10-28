package ru.bingosoft.teploInspector.ui.route_detail

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.Directions
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.SubpolylineHelper
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.transport.Transport
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.*
import com.yandex.runtime.Error
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import com.yandex.runtime.ui_view.ViewProvider
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.util.Const
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import javax.inject.Inject

class RouteDetailFragment(val order: Orders, val parentFragment: MapFragment): BottomSheetDialogFragment(), View.OnClickListener {

    @Inject
    lateinit var toaster: Toaster


    private lateinit var root: View
    lateinit var mapView: MapView

    private lateinit var carRouter: DrivingRouter
    private lateinit var busRouter: MasstransitRouter
    private var isPedestrianRouter: Boolean=false

    //private val userLocation: Point=Point()

    private val requestPoints: ArrayList<RequestPoint> = ArrayList()

    var lastLocation: com.yandex.mapkit.location.Location?=null

    lateinit var foundingRoutes: MutableList<Route>
    private lateinit var pedestrianRouter: PedestrianRouter

    private var drivingSession: DrivingSession? = null

    lateinit var directions: Directions
    lateinit var transports: Transport


    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey(BuildConfig.yandex_mapkit_api)
        MapKitFactory.setLocale("ru_RU")
        MapKitFactory.initialize(this.context)
        DirectionsFactory.initialize(this.context)
        TransportFactory.initialize(this.context)

        directions= DirectionsFactory.getInstance()
        transports= TransportFactory.getInstance()


    }

    override fun onClick(v: View?) {
        Timber.d("onClick_ROUTE")
        if (v != null) {
            //Очистим предыдущий маршрут
            /*if (parentFragment.lastCarRouter.isNotEmpty()) {
                parentFragment.removeRouter(parentFragment.lastCarRouter)
            }*/

            when (v.id) {
                R.id.btnCar -> {
                    Timber.d("CLICK_CAR")
                    v.isEnabled=false
                    v.setBackgroundColor(ContextCompat.getColor(this.context!!, R.color.colorCardItem))

                    val btnFoot=(v.parent as View).findViewById<Button>(R.id.btnFoot)
                    btnFoot.setBackgroundColor(ContextCompat.getColor(this.context!!, R.color.routeButtonPanel))
                    btnFoot.isEnabled=true

                    val btnBus=(v.parent as View).findViewById<Button>(R.id.btnBus)
                    btnBus.setBackgroundColor(ContextCompat.getColor(this.context!!, R.color.routeButtonPanel))
                    btnBus.isEnabled=true


                    carRouter=directions.createDrivingRouter()
                    if (requestPoints.size>1) {
                        drivingSession = carRouter.requestRoutes(requestPoints, DrivingOptions(), drivingRouteListener)
                        Timber.d("carRouter_created")
                    } else {
                        toaster.showToast(R.string.points_less_than_2)
                    }


                    hideBottomSheet()
                }

                R.id.btnBus -> {
                    Timber.d("bus_clicked")
                    (v as Button).isEnabled=false
                    v.setBackgroundColor(ContextCompat.getColor(this.context!!, R.color.colorCardItem))

                    val btnCar=(v.parent as View).findViewById<Button>(R.id.btnCar)
                    btnCar.setBackgroundColor(ContextCompat.getColor(this.context!!, R.color.routeButtonPanel))
                    btnCar.isEnabled=true

                    val btnFoot=(v.parent as View).findViewById<Button>(R.id.btnFoot)
                    btnFoot.setBackgroundColor(ContextCompat.getColor(this.context!!, R.color.routeButtonPanel))
                    btnFoot.isEnabled=true

                    busRouter = TransportFactory.getInstance().createMasstransitRouter()
                    Timber.d("busRouter=$busRouter")
                    //Timber.d("requestPoints=${requestPoints[0].point.latitude} ${requestPoints[1].point.latitude}")
                    val options = MasstransitOptions(ArrayList<String>(), ArrayList<String>(), TimeOptions())
                    if (requestPoints.size>1) {
                        busRouter.requestRoutes(requestPoints, options, routeListener)
                    } else {
                        toaster.showToast(R.string.points_less_than_2)
                    }

                    isPedestrianRouter=false
                }

                R.id.btnFoot -> {
                    (v as Button).isEnabled=false
                    v.setBackgroundColor(ContextCompat.getColor(this.context!!, R.color.colorCardItem))

                    val btnBus=(v.parent as View).findViewById<Button>(R.id.btnBus)
                    btnBus.setBackgroundColor(ContextCompat.getColor(this.context!!, R.color.routeButtonPanel))
                    btnBus.isEnabled=true

                    val btnCar=(v.parent as View).findViewById<Button>(R.id.btnCar)
                    btnCar.setBackgroundColor(ContextCompat.getColor(this.context!!, R.color.routeButtonPanel))
                    btnCar.isEnabled=true

                    pedestrianRouter=transports.createPedestrianRouter()

                    if (requestPoints.size>1) {
                        pedestrianRouter.requestRoutes(requestPoints,TimeOptions(),routeListener)
                    } else {
                        toaster.showToast(R.string.points_less_than_2)
                    }


                    isPedestrianRouter=true

                }
            }

        }
    }

    //https://github.com/yandex/mapkit-android-demo/blob/master/src/main/java/com/yandex/mapkitdemo/MasstransitRoutingActivity.java
    private val routeListener=object: Session.RouteListener{
        override fun onMasstransitRoutesError(error: Error) {
            Timber.d(error.toString())
            var errorMessage = getString(R.string.unknown_error_message)
            if (error is RemoteError) {
                errorMessage = getString(R.string.remote_error_message)
            } else if (error is NetworkError) {
                errorMessage = getString(R.string.network_error_message)
            }

            toaster.showToast(errorMessage)
        }

        override fun onMasstransitRoutes(routes: MutableList<Route>) {
            Timber.d("onMasstransitRoutes $routes")
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
            Timber.d("showRoutersList=${routes.size}")
            val routesRecyclerView = root.findViewById(R.id.routers_recycler_view) as RecyclerView

            routesRecyclerView.layoutManager = LinearLayoutManager(root.context)
            val adapter = RouterListAdapter(routes,routerRVClickListeners, isPedestrianRouter)
            routesRecyclerView.adapter = adapter
        }
    }

    private val drivingRouteListener=object: DrivingSession.DrivingRouteListener {
        override fun onDrivingRoutesError(error: Error) {
            var yandexMapError = getString(R.string.unknown_error_message)
            if (error is RemoteError) {
                yandexMapError = getString(R.string.remote_error_message)
            } else if (error is NetworkError) {
                yandexMapError = getString(R.string.network_error_message)
            }

            toaster.showToast(yandexMapError)
        }

        override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
            if (routes.isNotEmpty()) {
                routes.forEach {
                    Timber.d("saveRoute")
                    //parentFragment.lastCarRouter.add(parentFragment.mapView.map.mapObjects.addPolyline(it.geometry)) // Сохраним маршрут, чтоб потом можно было его удалить
                    parentFragment.lastCarRouterPolyline.add(it)
                }
            }
        }

    }

    private val routerRVClickListeners=object: RouterRVClickListeners {
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

            parentFragment.map.mapObjects.traverse(visitor)

        }

    }

    val visitor=object: MapObjectVisitor {
        override fun onPolygonVisited(p0: PolygonMapObject) {
            //TODO("Not yet implemented")
        }

        override fun onCircleVisited(p0: CircleMapObject) {
            //TODO("Not yet implemented")
        }

        override fun onPolylineVisited(p0: PolylineMapObject) {
            //TODO("Not yet implemented")
        }

        override fun onColoredPolylineVisited(p0: ColoredPolylineMapObject) {
            //TODO("Not yet implemented")
        }

        override fun onPlacemarkVisited(p0: PlacemarkMapObject) {
            val o=(p0.userData as Models.CustomMarker).order
            val tvMarker=(p0.userData as Models.CustomMarker).markerView
            if (o==order) {
                if (parentFragment.prevMapObjectMarker!=null) {
                    // выключим предыдущий маркер
                    val prevTvMarker=(parentFragment.prevMapObjectMarker!!.userData as Models.CustomMarker).markerView
                    prevTvMarker.isEnabled=!prevTvMarker.isEnabled
                    (parentFragment.prevMapObjectMarker as PlacemarkMapObject).setView(ViewProvider(prevTvMarker))
                }

                tvMarker.isEnabled=!tvMarker.isEnabled
                p0.setView(ViewProvider(tvMarker))
                parentFragment.prevMapObjectMarker=p0
            }
        }

        override fun onCollectionVisitEnd(p0: MapObjectCollection) {
            //TODO("Not yet implemented")
        }

        override fun onCollectionVisitStart(p0: MapObjectCollection): Boolean {
            //TODO("Not yet implemented")
            return true
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

    private fun getVehicleType(transport: com.yandex.mapkit.transport.masstransit.Transport, knownVehicleTypes:HashSet<String>):String? {
        val type=transport.line.vehicleTypes
        Timber.d("transport=${transport.line.name}_${transport.line.vehicleTypes}")
        type.forEach{
            if (knownVehicleTypes.contains(it)) {
                return it
            }
        }
        return null
    }

    override fun onStart() {
        super.onStart()
        //mapView.onStart()
        MapKitFactory.getInstance().onStart()
        //parentFragment.mkfInstatnce.onStart()
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        Timber.d("MapBottomSheet_setupDialog")

        val view=
            LayoutInflater.from(context).inflate(R.layout.fragment_route_detail,null)
        this.root=view
        dialog.setContentView(view)

        //(this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle("")

        val btnCar = root.findViewById(R.id.btnCar) as Button
        btnCar.setOnClickListener(this)

        val btnBus = root.findViewById(R.id.btnBus) as Button
        btnBus.setOnClickListener(this)

        val btnFoot = root.findViewById(R.id.btnFoot) as Button
        btnFoot.setOnClickListener(this)



        root.findViewById<TextView>(R.id.symbolNumber).text="Заявка №${order.number}"
        root.findViewById<TextView>(R.id.address).text=order.address

        Timber.d("order=$order")
        //Сохраним маршрут с точками
        requestPoints.add(
            RequestPoint(
                Point(order.lat,order.lon),
                RequestPointType.WAYPOINT,
                null
            )
        )

        Timber.d("requestPoints=${requestPoints[0].point.latitude}_${requestPoints[0].point.longitude}")

        if ((this.activity as MainActivity).userLocationReceiver.isInitLocation()) {
            Timber.d("userLocationReceiver_isInitLocation")
            val userLocation=(this.activity as MainActivity).userLocationReceiver.lastKnownLocation


            Timber.d("userLocation=${userLocation.latitude} =${userLocation.longitude}")
            requestPoints.add(
                RequestPoint(
                    Point(userLocation.latitude, userLocation.longitude),
                    RequestPointType.WAYPOINT,
                    null
                )
            )
        }

        Timber.d("AUTOCLICK!")
        if (order.typeTransportation==null) {
            order.typeTransportation="Самостоятельно на общественном транспорте"
        }
        Timber.d("order.typeTransportation=${order.typeTransportation}")
        val indexTypeTransport=Const.TypeTransportation.list.indexOf(order.typeTransportation)
        Timber.d("indexTypeTransport=$indexTypeTransport")
        when (indexTypeTransport) {
            0 -> {
                Timber.d("Самостоятельно на общественном транспорте")
                btnBus.performClick()
            }
            1 -> {
                Timber.d("Самостоятельно на личном транспорте")
                btnCar.performClick()
            }
            2 -> {
                Timber.d("Самостоятельно пешком")
                btnFoot.performClick()
            }

        }


    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog= super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { bottomDialog ->
            val bottomSheetDialog = bottomDialog as BottomSheetDialog
            //setupFullHeight(bottomSheetDialog)
        }
        return dialog
    }

    private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet: FrameLayout? =
            bottomSheetDialog.findViewById(R.id.design_bottom_sheet) as FrameLayout?
        val behavior: BottomSheetBehavior<*> =
            BottomSheetBehavior.from(bottomSheet!!)
        val layoutParams: ViewGroup.LayoutParams = bottomSheet.layoutParams

        layoutParams.height = getWindowHeight()
        bottomSheet.layoutParams = layoutParams
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }


    private fun getWindowHeight(): Int {
        val displayMetrics = DisplayMetrics()
        this.activity?.windowManager?.defaultDisplay?.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    override fun onStop() {
        super.onStop()
        //mapView.onStop()
        MapKitFactory.getInstance().onStop()
        //parentFragment.mkfInstatnce.onStop()
    }


    fun hideBottomSheet() {
        // Закроем BottomSheetDialog
        val params =(root.parent as View).layoutParams as CoordinatorLayout.LayoutParams
        val behavior = params.behavior
        if (behavior!=null && behavior is BottomSheetBehavior) {
            behavior.state= BottomSheetBehavior.STATE_HIDDEN
        }
    }

}