package ru.bingosoft.teploInspector.ui.map

import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import ru.bingosoft.teploInspector.util.Const.Location.ZOOM_LEVEL
import timber.log.Timber

class LocationListener(val mapView: MapView) :LocationListener {
    var lastLocation: Location?=null

    override fun onLocationStatusUpdated(locationStatus: LocationStatus) {
        if (locationStatus == LocationStatus.NOT_AVAILABLE) {
            Timber.d("LocationStatus.NOT_AVAILABLE")
        }
    }

    override fun onLocationUpdated(location: Location) {
        if (lastLocation == null) {
            moveCamera(location.getPosition(), ZOOM_LEVEL)
        }
        lastLocation = location
    }

    private fun moveCamera(point: Point, zoom: Float) {
        mapView.getMap().move(
            CameraPosition(point, zoom, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1f),
            null
        )
    }
}