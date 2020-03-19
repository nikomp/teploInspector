package ru.bingosoft.mapquestapp2.ui.mainactivity

import com.mapbox.mapboxsdk.geometry.LatLng
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.db.Orders.Orders

interface FragmentsContractActivity {
    fun setCheckup(checkup: Checkup)
    fun setChecupListOrder(order: Orders)
    fun setCoordinates(point: LatLng, controlId: Int)
}