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
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.geometry.LatLng
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.ui_view.ViewProvider
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.map_bottom.MapBottomSheet
import ru.bingosoft.teploInspector.util.Const.RequestCodes.PERMISSION
import ru.bingosoft.teploInspector.util.Toaster
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class MapFragment : Fragment(), MapContractView, IOnBackPressed, MapLoadedListener,
    UserLocationObjectListener,InputListener {


    private lateinit var mapView: MapView
    lateinit var map: Map
    lateinit var userLocationLayer: UserLocationLayer

    @Inject
    lateinit var mapPresenter: MapPresenter

    @Inject
    lateinit var toaster: Toaster

    private var addCoordinatesTag: Boolean=false
    private var checkupId: Long?=0
    private var controlId: Int=0


    private val callback = LocationListeningCallback(/*this.requireActivity() as MainActivity*/)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)

        MapKitFactory.setApiKey("bdca3c95-b709-476c-99ba-b0f6c9548c0a")
        MapKitFactory.initialize(this.context)
        MapKitFactory.setLocale(Locale("ru","RU").toString())


        val root = inflater.inflate(R.layout.fragment_map, container, false)
        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.menu_map)

        mapView=root.findViewById(R.id.mapView)
        mapView.map.move(CameraPosition(Point(56.3287,44.002),12.0f,0.0f,0.0f), Animation(Animation.Type.SMOOTH,0.0f),null)

        map=mapView.map

        map.setMapLoadedListener(this) // Обработчик когда карта загружена
        map.addInputListener(this) // Обработчик клика на карте


        return root
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

        orders.forEach{
            importOrdersOnMap(it)
        }
    }


    private fun importOrdersOnMap(order: Orders) {
        Timber.d("importOrdersOnMap")
        val view=layoutInflater.inflate(R.layout.template_marker,null)
        view.findViewById<TextView>(R.id.markerText).text=order.number
        map.mapObjects.addPlacemark(Point(order.lat,order.lon),ViewProvider(view))
            .addTapListener{mapObject, point ->
                Timber.d("tapPoint=${point.latitude}_${point.longitude}")
                Timber.d("mapObject=${(mapObject as PlacemarkMapObject).geometry.latitude}_${(mapObject).geometry.longitude}")

                val mapBottomSheet = MapBottomSheet(order)
                mapBottomSheet.show(this.requireActivity().supportFragmentManager,"BOTTOM_SHEET")

                true
            }

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
            getUserLocation()
        }

    }

    private fun getUserLocation() {
        Timber.d("getUserLocation")
        val mapKit=MapKitFactory.getInstance()
        userLocationLayer=mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible=true
        userLocationLayer.isHeadingEnabled=true
        userLocationLayer.setObjectListener(this)

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

    override fun onMapLoaded(p0: MapLoadStatistics) {
        mapPresenter.attachView(this)

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

    override fun onMapLongTap(p0: Map, p1: Point) {
        Timber.d("onMapLongTap")
    }

    override fun onMapTap(p0: Map, point: Point) {
        Timber.d("onMapTap")
        if (addCoordinatesTag) {
            val view=layoutInflater.inflate(R.layout.template_marker,null)
            //view.findViewById<TextView>(R.id.markerText).text=order.number
            map.mapObjects.addPlacemark(point,ViewProvider(view))

            val bundle = Bundle()
            bundle.putBoolean("loadCheckupById", true)
            val idCheckup=checkupId
            if (idCheckup!=null) {
                bundle.putLong("checkupId",idCheckup)
            }

            val fragmentCheckup= CheckupFragment()
            fragmentCheckup.arguments=bundle

            val fragmentManager=this.requireActivity().supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragmentCheckup, "checkup_fragment_tag")
                .addToBackStack(null)
                .commit()

            fragmentManager.executePendingTransactions()

            (this.requireActivity() as FragmentsContractActivity).setCoordinates(point,controlId)
        }
    }


}