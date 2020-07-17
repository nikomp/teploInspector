package ru.bingosoft.teploInspector.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.yandex.mapkit.Animation
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
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.map_bottom.MapBottomSheet
import ru.bingosoft.teploInspector.ui.order.OrderFragment
import ru.bingosoft.teploInspector.ui.route_detail.RouteDetailFragment
import ru.bingosoft.teploInspector.util.Const.Location.DESIRED_ACCURACY
import ru.bingosoft.teploInspector.util.Const.Location.MINIMAL_DISTANCE
import ru.bingosoft.teploInspector.util.Const.Location.MINIMAL_TIME
import ru.bingosoft.teploInspector.util.Const.Location.TARGET_POINT
import ru.bingosoft.teploInspector.util.Const.Location.USE_IN_BACKGROUND
import ru.bingosoft.teploInspector.util.Const.Location.ZOOM_LEVEL
import ru.bingosoft.teploInspector.util.Const.RequestCodes.PERMISSION
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import javax.inject.Inject


class MapFragment : Fragment(), MapContractView, IOnBackPressed, View.OnClickListener {

    val fragment=this
    lateinit var mapView: MapView
    lateinit var map: Map
    lateinit var userLocationLayer: UserLocationLayer
    lateinit var locationManager:LocationManager

    @Inject
    lateinit var mapPresenter: MapPresenter

    @Inject
    lateinit var toaster: Toaster

    private var addCoordinatesTag: Boolean=false
    private var checkupId: Long?=0
    private var controlId: Int=0

    lateinit var directions: Directions
    lateinit var transports: Transport
    var lastCarRouter=mutableListOf<PolylineMapObject>()
    var prevMapObjectMarker: MapObject?=null

    lateinit var order: Orders

    private val mapLoadedListener=object:MapLoadedListener{
        override fun onMapLoaded(p0: MapLoadStatistics) {
            mapPresenter.attachView(fragment)

            val tag = arguments?.getBoolean("addCoordinates")
            checkupId= arguments?.getLong("checkupId")
            val control= arguments?.getInt("controlId")
            if (control!=null) {
                controlId=control
            }
            Timber.d("checkupId=$checkupId")

            if (tag==null || tag==false) {
                mapPresenter.loadMarkers() // Грузим все маркеры Заявок
            } else {
                addCoordinatesTag=true
            }

            enableLocationComponent() // Включим разрешения на работу с картой
        }
    }

    fun showRouteDialog(order: Orders) {
        val routeDetailFragment = RouteDetailFragment(order, this)
        routeDetailFragment.show(fragment.requireActivity().supportFragmentManager,"BOTTOM_SHEET_ROUTE_DETAIL")
    }

    private val inputListener=object:InputListener {
        override fun onMapLongTap(p0: Map, p1: Point) {
            Timber.d("onMapLongTap")
        }

        override fun onMapTap(p0: Map, point: Point) {
            Timber.d("onMapTap")
            if (addCoordinatesTag) {
                val view=layoutInflater.inflate(R.layout.template_marker,null)
                map.mapObjects.addPlacemark(point,ViewProvider(view))

                val bundle = Bundle()
                bundle.putBoolean("loadCheckupById", true)
                val idCheckup=checkupId
                if (idCheckup!=null) {
                    bundle.putLong("checkupId",idCheckup)
                }

                val fragmentCheckup= CheckupFragment()
                fragmentCheckup.arguments=bundle

                val fragmentManager=fragment.requireActivity().supportFragmentManager
                fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragmentCheckup, "checkup_fragment_tag")
                    .addToBackStack(null)
                    .commit()

                fragmentManager.executePendingTransactions()

                (fragment.requireActivity() as FragmentsContractActivity).setCoordinates(point,controlId)
            }

            if (lastCarRouter.isNotEmpty()) {
                removeRouter(lastCarRouter)
            }

            if (prevMapObjectMarker!=null) {
                // выключим предыдущий маркер
                val prevTvMarker=(prevMapObjectMarker!!.userData as Models.CustomMarker).markerView
                prevTvMarker.isEnabled=!prevTvMarker.isEnabled
                (prevMapObjectMarker as PlacemarkMapObject).setView(ViewProvider(prevTvMarker))
                prevMapObjectMarker=null
            }
        }
    }

    fun removeRouter(list: MutableList<PolylineMapObject>) {
        list.forEach{
            map.mapObjects.remove(it)
        }
        list.clear()
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
            userLocationLayer.setAnchor(
                PointF((mapView.width * 0.5).toFloat(), ((mapView.height * 0.5)).toFloat()) ,
                PointF((mapView.width * 0.5).toFloat(), (mapView.height * 0.83).toFloat()))


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

    val mapObjectTapListener=object:MapObjectTapListener{
        override fun onMapObjectTap(mapObject: MapObject, p1: Point): Boolean {
            Timber.d("onMapObjectTap")

            val order=(mapObject.userData as Models.CustomMarker).order
            val tvMarker=(mapObject.userData as Models.CustomMarker).markerView
            if (prevMapObjectMarker!=null) {
                // выключим предыдущий маркер
                val prevTvMarker=(prevMapObjectMarker!!.userData as Models.CustomMarker).markerView
                prevTvMarker.isEnabled=!prevTvMarker.isEnabled
                (prevMapObjectMarker as PlacemarkMapObject).setView(ViewProvider(prevTvMarker))
            }

            tvMarker.isEnabled=!tvMarker.isEnabled
            (mapObject as PlacemarkMapObject).setView(ViewProvider(tvMarker))
            prevMapObjectMarker=mapObject

            if (locationListener.lastLocation!=null) {
                val mapBottomSheet = MapBottomSheet(order, locationListener.lastLocation!!.position,fragment)
                mapBottomSheet.show(fragment.requireActivity().supportFragmentManager,"BOTTOM_SHEET")

            } else {
                Timber.d("locationListener.lastLocation==null")
                locationManager.requestSingleUpdate(locationListener)
                if (locationListener.lastLocation!=null) {
                    val mapBottomSheet = MapBottomSheet(order, locationListener.lastLocation!!.position,fragment)
                    mapBottomSheet.show(fragment.requireActivity().supportFragmentManager,"BOTTOM_SHEET")
                }
            }

            return true
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)

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
        super.onCreate(savedInstanceState)

        Timber.d("MapFragment_onCreate")

        MapKitFactory.setApiKey(BuildConfig.yandex_mapkit_api)
        MapKitFactory.setLocale("ru_RU")
        MapKitFactory.initialize(this.context)
        DirectionsFactory.initialize(this.context)
        TransportFactory.initialize(this.context)

        directions=DirectionsFactory.getInstance()
        transports=TransportFactory.getInstance()
    }


    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun showMarkers(orders: List<Orders>) {
        Timber.d("showMarkers=$orders")

        map.mapObjects.clear()
        orders.forEach{
            importOrdersOnMap(it)
        }
    }


    private fun importOrdersOnMap(order: Orders) {
        Timber.d("importOrdersOnMap=$order")
        val view=layoutInflater.inflate(R.layout.template_marker,null)
        val tvMarker=view.findViewById<TextView>(R.id.tvMarker)
        tvMarker.text=order.number

        val customMarker=Models.CustomMarker(order=order,markerView = tvMarker)

        val placemarkMapObject=map.mapObjects.addPlacemark(Point(order.lat,order.lon),ViewProvider(view))
        placemarkMapObject.userData=customMarker
        placemarkMapObject.addTapListener(mapObjectTapListener)

    }


    private fun enableLocationComponent() {
        Timber.d("enableLocationComponent")
        // Проверим разрешения
        if (ContextCompat.checkSelfPermission(this.context!!,(Manifest.permission.ACCESS_FINE_LOCATION)) != PackageManager.PERMISSION_GRANTED) {
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
        val mapKit=MapKitFactory.getInstance()
        userLocationLayer=mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible=true
        userLocationLayer.isHeadingEnabled=true
        userLocationLayer.setObjectListener(userLocationObjectListener)

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
        Timber.d("MapFragment onBackPressed")
        return true
    }


    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
        var drawable = ContextCompat.getDrawable(this.context!!, drawableId) ?: return null

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

                    val fragmentOrder= OrderFragment()
                    //fragmentMap.arguments=bundle
                    val fragmentManager=this.requireActivity().supportFragmentManager

                    fragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, fragmentOrder, "")
                        .addToBackStack(null)
                        .commit()

                    fragmentManager.executePendingTransactions()

                    (fragmentOrder.requireActivity() as FragmentsContractActivity).setMode(isMap = false)
                }

                R.id.btnMap -> {
                    v.isEnabled=false
                    (v.parent as View).findViewById<Button>(R.id.btnList).isEnabled=true

                }


            }
        }
    }


}