package ru.bingosoft.teploInspector.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.directions.Directions
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.location.*
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.transport.Transport
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.ui_view.ViewProvider
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.map_bottom.MapBottomSheet
import ru.bingosoft.teploInspector.ui.route_detail.RouteDetailFragment
import ru.bingosoft.teploInspector.util.Const.Location.DESIRED_ACCURACY
import ru.bingosoft.teploInspector.util.Const.Location.MINIMAL_DISTANCE
import ru.bingosoft.teploInspector.util.Const.Location.MINIMAL_TIME
import ru.bingosoft.teploInspector.util.Const.Location.TARGET_POINT
import ru.bingosoft.teploInspector.util.Const.Location.USE_IN_BACKGROUND
import ru.bingosoft.teploInspector.util.Const.Location.ZOOM_LEVEL
import ru.bingosoft.teploInspector.util.Const.RequestCodes.PERMISSION
import ru.bingosoft.teploInspector.util.OtherUtil
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class MapFragment : Fragment(), MapContractView, IOnBackPressed, View.OnClickListener {

    private val fragment=this
    var orders: List<Orders> = listOf()
    lateinit var mapView: MapView
    lateinit var map: Map
    var userLocationLayer: UserLocationLayer?=null
    private lateinit var locationManager:LocationManager

    @Inject
    lateinit var mapPresenter: MapPresenter

    @Inject
    lateinit var toaster: Toaster

    private var checkupId: Long?=0
    private var controlId: Int=0

    private lateinit var directions: Directions
    private lateinit var transports: Transport
    private lateinit var mkInstances: MapKit
    var lastCarRouter=mutableListOf<PolylineMapObject>()
    var lastStopTransferMarkers=mutableListOf<PlacemarkMapObject>()
    var prevMapObjectMarker: MapObject?=null

    @Inject
    lateinit var otherUtil: OtherUtil


    private val mapLoadedListener= MapLoadedListener {

        mapPresenter.attachView(fragment)

        checkupId= arguments?.getLong("checkupId")
        val control= arguments?.getInt("controlId")
        if (control!=null) {
            controlId=control
        }

        if (isAdded) {
            Timber.d("MapFragment_filteredOrders=${(this.requireActivity() as MainActivity).filteredOrders}")
            showMarkers((this.requireActivity() as MainActivity).filteredOrders)

            if ((this.requireActivity() as MainActivity).isInitCurrentOrder()) {
                Timber.d("isInitCurrentOrder_${(this.requireActivity() as MainActivity).currentOrder}")
                if ((this.requireActivity() as MainActivity).currentOrder.id!=0L) {
                    showRouteDialog((this.requireActivity() as MainActivity).currentOrder)
                }
            }
        } else {
            otherUtil.writeToFile("Logger_MapFragment.isAdded==false")
        }


    }

    fun showRouteDialog(order: Orders) {
        Timber.d("showRouteDialog")
        val routeDetailFragment = RouteDetailFragment(order, this)
        // Handler().post попытка решить Fatal Exception: java.lang.RuntimeException
        //java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState androidx
        // подробнее тут https://stackoverflow.com/questions/14177781/java-lang-illegalstateexception-can-not-perform-this-action-after-onsaveinstanc
        Handler().post {
            routeDetailFragment.show(
                fragment.requireActivity().supportFragmentManager,
                "BOTTOM_SHEET_ROUTE_DETAIL"
            )
        }


    }

    private val inputListener=object:InputListener {
        override fun onMapLongTap(p0: Map, p1: Point) {
            Timber.d("onMapLongTap")
        }

        override fun onMapTap(p0: Map, point: Point) {
            Timber.d("onMapTap")
            // Чистим карту от старых маршрутов
            if (lastCarRouter.isNotEmpty()) {
                removeRouter()
            }

            clearPreviousMarker()
        }
    }

    fun removeRouter() {
        Timber.d("removeRouter")
        lastCarRouter.forEach{
            try {
                map.mapObjects.remove(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        lastCarRouter.clear()

        if (!lastStopTransferMarkers.isNullOrEmpty()){
            lastStopTransferMarkers.forEach{
                try {
                    map.mapObjects.remove(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            lastStopTransferMarkers.clear()
        }

    }

    private val userLocationObjectListener=object:UserLocationObjectListener{
        override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {
            Timber.d("onObjectUpdated")
        }

        override fun onObjectRemoved(p0: UserLocationView) {
            Timber.d("onObjectRemoved")
        }

        override fun onObjectAdded(userLocationView: UserLocationView) {
            Timber.d("onObjectAdded")
            /*следить за локацией пользователя, карта будет перепрыгивать на локацию пользователя
            userLocationLayer?.setAnchor(
                PointF((mapView.width * 0.5).toFloat(), ((mapView.height * 0.5)).toFloat()) ,
                PointF((mapView.width * 0.5).toFloat(), (mapView.height * 0.83).toFloat()))*/


            val bitmap=getBitmapFromVectorDrawable(R.drawable.ic_navigation_24dp)
            userLocationView.arrow.setIcon(
                ImageProvider.fromBitmap(bitmap))



            val pinIcon = userLocationView.pin.useCompositeIcon()

            pinIcon.setIcon(
                "icon",
                ImageProvider.fromBitmap(bitmap),
                IconStyle().setAnchor(PointF(0f, 0f))
                    .setRotationType(RotationType.ROTATE)
                    .setZIndex(0f)
                    .setScale(1f)
            )

            pinIcon.setIcon(
                "pin",
                ImageProvider.fromBitmap(bitmap),
                IconStyle().setAnchor(PointF(0.5f, 0.5f))
                    .setRotationType(RotationType.ROTATE)
                    .setZIndex(1f)
                    .setScale(0.5f)
            )

            userLocationView.accuracyCircle.fillColor = Color.BLUE


        }
    }

    fun clearPreviousMarker() {
        if (prevMapObjectMarker!=null) {
            // выключим предыдущий маркер
            try {
                val prevTvMarker=(prevMapObjectMarker!!.userData as Models.CustomMarker).markerView
                prevTvMarker.isEnabled=!prevTvMarker.isEnabled
                (prevMapObjectMarker as PlacemarkMapObject).setView(ViewProvider(prevTvMarker))
                prevMapObjectMarker=null
            } catch (e:Exception) {
                e.printStackTrace()
            }

        }
    }

    private val locationListener=object:LocationListener {
        var lastLocation: Location?=null

        override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
            if (locationStatus == LocationStatus.NOT_AVAILABLE) {
                Timber.d("LocationStatus.NOT_AVAILABLE")
            }
        }

        override fun onLocationUpdated(location: Location) {
            if (lastLocation == null) {
                moveCamera(location.position)
            }
            lastLocation = location
        }

        private fun moveCamera(point: Point) {
            mapView.map.move(
                CameraPosition(point, ZOOM_LEVEL, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 1f),
                null
            )
        }
    }

    private val mapObjectTapListener= MapObjectTapListener { mapObject, _ ->
        Timber.d("onMapObjectTap")

        val order=(mapObject.userData as Models.CustomMarker).order
        val tvMarker=(mapObject.userData as Models.CustomMarker).markerView

        // #Unchecked_cast
        val listOrders =
        if (tvMarker.tag!=null) {
            if (tvMarker.tag is List<*>) {
                (tvMarker.tag as List<*>).filterIsInstance<Orders>()
            } else {
                listOf(order)
            }
            //tvMarker.tag as List<Orders>
        } else {
             listOf(order)
        }

        clearPreviousMarker()

        tvMarker.isEnabled=!tvMarker.isEnabled
        (mapObject as PlacemarkMapObject).setView(ViewProvider(tvMarker))
        prevMapObjectMarker=mapObject

        if (locationListener.lastLocation!=null) {
            val mapBottomSheet = MapBottomSheet(listOrders, fragment)
            mapBottomSheet.show(fragment.requireActivity().supportFragmentManager,"BOTTOM_SHEET")
        } else {
            Timber.d("locationListener.lastLocation==null")
            locationManager.requestSingleUpdate(locationListener)
            if (locationListener.lastLocation!=null) {
                val mapBottomSheet = MapBottomSheet(listOrders, fragment)
                mapBottomSheet.show(fragment.requireActivity().supportFragmentManager,"BOTTOM_SHEET")
            }
        }

        true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //AndroidSupportInjection.inject(this)
        Timber.d("MapFragment_onCreateView")

        locationManager=MapKitFactory.getInstance().createLocationManager()

        val root = inflater.inflate(R.layout.fragment_map, container, false)
        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.menu_orders)

        mapView=root.findViewById(R.id.mapView)
        mapView.map.move(CameraPosition(TARGET_POINT,ZOOM_LEVEL,0.0f,0.0f), Animation(Animation.Type.SMOOTH,0.0f), null)

        locationManager.subscribeForLocationUpdates(DESIRED_ACCURACY, MINIMAL_TIME, MINIMAL_DISTANCE, USE_IN_BACKGROUND,FilteringMode.ON,locationListener)

        map=mapView.map

        map.setMapLoadedListener(mapLoadedListener) // Обработчик, когда карта загружена
        map.addInputListener(inputListener) // Обработчик клика на карте

        val btnList=root.findViewById<Button>(R.id.btnList)
        btnList.setOnClickListener(this)
        val btnMap=root.findViewById<Button>(R.id.btnMap)
        btnMap.setOnClickListener(this)

        return root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)


        //MapKitFactory.setApiKey(BuildConfig.yandex_mapkit_api)
        //MapKitFactory.setLocale("ru_RU")
        MapKitFactory.initialize(this.context)
        DirectionsFactory.initialize(this.context)
        TransportFactory.initialize(this.context)

        mkInstances=MapKitFactory.getInstance()

        directions=DirectionsFactory.getInstance()
        transports=TransportFactory.getInstance()

        /*mkInstances=App.appInstance.mkInstances
        directions=App.appInstance.directions
        transports=App.appInstance.transports*/

    }


    override fun onStart() {
        super.onStart()
        mapView.onStart()
        enableLocationComponent() // Включим разрешения на работу с картой
        MapKitFactory.getInstance().onStart()
        //mkfInstatnce.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        //mkfInstatnce.onStop()
    }

    override fun showMarkers(orders: List<Orders>) {
        Timber.d("showMarkers=$orders")
        this.orders=orders

        prevMapObjectMarker=null // Очистим предыдущий маркер
        map.mapObjects.clear()
        orders.forEach{
            importOrdersOnMap(it)
        }
    }


    private fun importOrdersOnMap(order: Orders) {
        Timber.d("importOrdersOnMap=$order")
        //val view=layoutInflater.inflate(R.layout.template_marker,null)
        val view=View.inflate(fragment.requireContext(),R.layout.template_marker,null)
        val tvMarker=view.findViewById<TextView>(R.id.tvMarker)

        // Проверим возможно маркеры накладываются друг на друга
        val ordersAddress=orders.filter { it.address==order.address }
        if (ordersAddress.size>1) {
            var stOrdersName=""
            ordersAddress.forEach{
                stOrdersName += "${it.number},\n"
            }
            stOrdersName=stOrdersName.substring(0,stOrdersName.length-2)

            tvMarker.tag=ordersAddress
            tvMarker.text=stOrdersName
            tvMarker.setCompoundDrawablesWithIntrinsicBounds(null,null,null, ContextCompat.getDrawable(fragment.requireContext(),R.drawable.ic_marker_selector5))
        } else {
            tvMarker.text=order.number

            when (order.groupOrder) {
                this.getString(R.string.orderType1).toLowerCase(Locale.ROOT) -> {
                    Timber.d("R.string.orderType1")
                    tvMarker.setCompoundDrawablesWithIntrinsicBounds(null,null,null,ContextCompat.getDrawable(fragment.requireContext(),R.drawable.ic_marker_selector1))
                }
                this.getString(R.string.orderType2).toLowerCase(Locale.ROOT) -> {
                    tvMarker.setCompoundDrawablesWithIntrinsicBounds(null,null,null,ContextCompat.getDrawable(fragment.requireContext(),R.drawable.ic_marker_selector2))
                }
                this.getString(R.string.orderType3).toLowerCase(Locale.ROOT) -> {
                    Timber.d("R.string.orderType3")
                    tvMarker.setCompoundDrawablesWithIntrinsicBounds(null,null,null,ContextCompat.getDrawable(fragment.requireContext(),R.drawable.ic_marker_selector3))
                }
                this.getString(R.string.orderType4).toLowerCase(Locale.ROOT) -> {
                    tvMarker.setCompoundDrawablesWithIntrinsicBounds(null,null,null,ContextCompat.getDrawable(fragment.requireContext(),R.drawable.ic_marker_selector4))
                }
            }
        }

        val customMarker=Models.CustomMarker(order=order,markerView = tvMarker)

        val placemarkMapObject=map.mapObjects.addPlacemark(Point(order.lat,order.lon),ViewProvider(view))
        placemarkMapObject.userData=customMarker
        placemarkMapObject.addTapListener(mapObjectTapListener)

    }


    private fun enableLocationComponent() {
        Timber.d("enableLocationComponent")
        // Проверим разрешения
        if (ContextCompat.checkSelfPermission(this.requireContext(),(Manifest.permission.ACCESS_FINE_LOCATION)) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
                PERMISSION
            )

        } else {
            initUserLocationLayer()
        }

    }

    private fun initUserLocationLayer() {
        Timber.d("initUserLocationLayer")
        if (userLocationLayer==null) {
            userLocationLayer=mkInstances.createUserLocationLayer(mapView.mapWindow)
            userLocationLayer!!.isVisible=true
            userLocationLayer!!.isHeadingEnabled=true
            userLocationLayer!!.setObjectListener(userLocationObjectListener)

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Timber.d("onRequestPermissionsResult")
        when (requestCode) {
            PERMISSION -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // Разрешения выданы
                    Timber.d("enableLocationComponent")
                    enableLocationComponent()
                } else {
                    // Разрешения не выданы оповестим юзера
                    toaster.showToast(R.string.not_permissions)
                }
            }
            else -> Timber.d("Неизвестный PERMISSION_REQUEST_CODE")
        }

    }

    override fun onBackPressed(): Boolean {
        Timber.d("MapFragment_onBackPressed")
        return true
    }


    @Suppress("SameParameterValue")
    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
        var drawable = ContextCompat.getDrawable(this.requireContext(), drawableId) ?: return null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = DrawableCompat.wrap(drawable).mutate()
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888) ?: return null
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.btnList -> {
                    v.isEnabled=false
                    (v.parent as View).findViewById<Button>(R.id.btnMap).isEnabled=true

                    //this.requireActivity().onBackPressed()
                    Navigation.findNavController(fragment.requireView()).navigate(R.id.nav_home)


                    //(this.requireActivity() as FragmentsContractActivity).setMode(isMap = false)
                    (this.requireActivity() as MainActivity).filteredOrders=this.orders

                }

                R.id.btnMap -> {
                    v.isEnabled=false
                    (v.parent as View).findViewById<Button>(R.id.btnList).isEnabled=true

                }

            }
        }
    }




}