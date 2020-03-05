package ru.bingosoft.mapquestapp2.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Orders.Orders
import ru.bingosoft.mapquestapp2.ui.checkup.CheckupFragment
import ru.bingosoft.mapquestapp2.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.mapquestapp2.util.Const.Location.MAPQUEST_HEADQUARTERS
import ru.bingosoft.mapquestapp2.util.Const.LogTags.LOGTAG
import ru.bingosoft.mapquestapp2.util.Const.RequestCodes.PERMISSION
import ru.bingosoft.mapquestapp2.util.Toaster
import timber.log.Timber
import javax.inject.Inject


class MapFragment : Fragment(), MapContractView {


    private var mapView: MapView? = null
    lateinit var mapboxMap: MapboxMap

    @Inject
    lateinit var mapPresenter: MapPresenter

    @Inject
    lateinit var toaster: Toaster

    var addCoordinatesTag: Boolean=false
    var checkupId: Long?=0
    var controlId: Int=0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)

        Mapbox.getInstance(this.context!!,getString(R.string.access_token))
        val root = inflater.inflate(R.layout.fragment_map, container, false)

        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.menu_map)


        mapView=root.findViewById(R.id.mapView)
        
        val mv=mapView
        if (mv!=null) {
            mv.onCreate(savedInstanceState)
            mv.getMapAsync(OnMapReadyCallback{mapboxMap ->
                mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
                    enableLocationComponent(style)
                }

                mapboxMap.addOnMapClickListener{point ->
                    addMarker(mapboxMap,point)
                }

                this.mapboxMap = mapboxMap

                this.mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MAPQUEST_HEADQUARTERS,
                    12.0
                ))

            })
        }

        Timber.d("MapFragment onCreateView")
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

        return root
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
        mapPresenter.onDestroy()
    }

    override fun showMarkers(orders: List<Orders>) {
        mapView?.getMapAsync { it ->
            orders.forEach{
                importOrdersOnMap(mapboxMap, it)
            }

        }
    }


    private fun importOrdersOnMap(mapboxMap: MapboxMap, order: Orders) {
        val markerOptions = MarkerOptions()

        markerOptions.position(LatLng(order.lat,order.lon))
        markerOptions.title(order.number)
        markerOptions.snippet(order.name)
        mapboxMap.addMarker(markerOptions)

    }

    private fun addMarker(mapboxMap: MapboxMap, point: LatLng): Boolean {
        val markerOptions = MarkerOptions()

        markerOptions.position(point)
        mapboxMap.addMarker(markerOptions)

        if (addCoordinatesTag) {
            val bundle = Bundle()
            bundle.putBoolean("returnFromMap", true)
            val idCheckup=checkupId
            if (idCheckup!=null) {
                bundle.putLong("checkupId",idCheckup)
            }

            val fragmentCheckup= CheckupFragment()
            fragmentCheckup.arguments=bundle
            val fragmentManager=this.requireActivity().supportFragmentManager

            fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragmentCheckup, "checkup_list_fragment_tag")
                .addToBackStack(null)
                .commit()

            fragmentManager.executePendingTransactions()

            (this.requireActivity() as FragmentsContractActivity).setCoordinates(point,controlId)

        }


        return true
    }

    private fun enableLocationComponent(loadedMapStyle: Style) { // Check if permissions are enabled and if not request
        Timber.d("enableLocationComponent")
        // Проверим разрешения
        if (ContextCompat.checkSelfPermission(this.context!!,(Manifest.permission.ACCESS_FINE_LOCATION)) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                PERMISSION
            )

        } else {
            getUserLocation(loadedMapStyle)
        }

    }

    private fun getUserLocation(loadedMapStyle: Style) {
        Timber.d("Permissions ok")
        val locationComponent = mapboxMap.locationComponent
        // Set the LocationComponent activation options
        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(this.context!!, loadedMapStyle)
                .useDefaultLocationEngine(true)
                .build()
        // Activate with the LocationComponentActivationOptions object
        locationComponent.activateLocationComponent(locationComponentActivationOptions)
        // Enable to make component visible
        locationComponent.isLocationComponentEnabled = true
        // Set the component's camera mode
        locationComponent.cameraMode = CameraMode.TRACKING
        // Set the component's render mode
        locationComponent.renderMode = RenderMode.COMPASS
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(LOGTAG,"onRequestPermissionsResult")
        when (requestCode) {
            PERMISSION -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // Разрешения выданы, повторим попытку авторизации
                    Log.d(LOGTAG,"enableLocationComponent")
                    //enableLocationComponent(mapboxMap?.getStyle()!!)
                    getUserLocation(mapboxMap.style!!)
                } else {
                    // Разрешения не выданы оповестим юзера
                    toaster.showToast(R.string.not_permissions)
                }
            }
            else -> Timber.d("Неизвестный PERMISSION_REQUEST_CODE")
        }

    }


}