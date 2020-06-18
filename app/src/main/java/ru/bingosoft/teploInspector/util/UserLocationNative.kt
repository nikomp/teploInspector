package ru.bingosoft.teploInspector.util

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import timber.log.Timber

class UserLocationNative {
    var userLocation: Location= Location(LocationManager.GPS_PROVIDER)

    val locationListener=object: LocationListener {
        override fun onLocationChanged(location: Location?) {
            Timber.d("onLocationChanged1111 $location")
            userLocation.set(location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            //TODO("Not yet implemented")
        }

        override fun onProviderEnabled(provider: String?) {
            //TODO("Not yet implemented")
        }

        override fun onProviderDisabled(provider: String?) {
            //TODO("Not yet implemented")
        }

    }
}