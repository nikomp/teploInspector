package ru.bingosoft.mapquestapp2.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)

        Mapbox.getInstance(this.context!!,getString(R.string.access_token))
        val root = inflater.inflate(R.layout.fragment_map, container, false)


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

            })
        }


        mapPresenter.attachView(this)
        mapPresenter.viewIsReady()

        return root
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }



    override fun showMarkers(orders: List<Orders>) {
        mapView?.getMapAsync { it ->
            val mapboxMap = it
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MAPQUEST_HEADQUARTERS,
                12.0
            ))

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