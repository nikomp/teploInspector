package ru.bingosoft.teploInspector.ui.map

import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import timber.log.Timber

class LocationListeningCallback /*internal constructor(activity: MainActivity)*/:
    LocationEngineCallback<LocationEngineResult> {

    /*private val activityWeakReference: WeakReference<MainActivity>

    init {this.activityWeakReference = WeakReference(activity)}*/

    override fun onSuccess(result: LocationEngineResult?) {
        Timber.d("LocationListeningCallback onSuccess ${result?.lastLocation}")
        result?.lastLocation

    }

    override fun onFailure(exception: Exception) {
        exception.printStackTrace()
    }
}