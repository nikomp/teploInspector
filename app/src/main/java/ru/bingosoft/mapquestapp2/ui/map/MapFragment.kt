package ru.bingosoft.mapquestapp2.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapquest.mapping.MapQuest
import com.mapquest.mapping.maps.MapView
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Orders.Orders
import ru.bingosoft.mapquestapp2.util.Const.Location.MAPQUEST_HEADQUARTERS
import timber.log.Timber
import javax.inject.Inject

class MapFragment : Fragment(), MapContractView {

    private var mMapView: MapView? = null
    private var mMapboxMap: MapboxMap? = null

    @Inject
    lateinit var mapPresenter: MapPresenter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        /*slideshowViewModel =
            ViewModelProviders.of(this).get(SlideshowViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_map, container, false)
        val textView: TextView = root.findViewById(R.id.text_slideshow)
        slideshowViewModel.text.observe(this, Observer {
            textView.text = it
        })*/
        AndroidSupportInjection.inject(this)

        val root = inflater.inflate(R.layout.fragment_map, container, false)

        MapQuest.start(this.context)

        mMapView = root.findViewById(R.id.mapquestMapView)


        val mv=mMapView
        if (mv!=null) {
            mv.onCreate(savedInstanceState)
            mv.getMapAsync(OnMapReadyCallback { mapboxMap ->
                mMapboxMap = mapboxMap

                val mbm=mMapboxMap
                if (mbm!=null) {
                    mbm.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(MAPQUEST_HEADQUARTERS,
                            11.0
                        ))
                }

                mv.setStreetMode()
            })
        }

        Timber.d(this.activity?.getDatabasePath("mydatabase")?.absolutePath)

        mapPresenter.attachView(this)
        mapPresenter.viewIsReady()

        return root
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mMapView?.onSaveInstanceState(outState)
    }

    override fun showMarkers(orders: List<Orders>) {
        mMapView?.getMapAsync { it ->
            val mapboxMap = it
            mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MAPQUEST_HEADQUARTERS,
                12.0
            ))

            orders.forEach{
                addMarker(mapboxMap, it)
            }

        }
    }

    private fun addMarker(mapboxMap: MapboxMap, order: Orders) {
        val markerOptions = MarkerOptions()

        markerOptions.position(LatLng(order.lat,order.lon))
        markerOptions.title(order.number)
        markerOptions.snippet(order.name)
        mapboxMap.addMarker(markerOptions)
    }


}