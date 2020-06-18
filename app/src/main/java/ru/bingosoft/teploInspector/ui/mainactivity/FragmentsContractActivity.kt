package ru.bingosoft.teploInspector.ui.mainactivity

import com.yandex.mapkit.geometry.Point
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Orders.Orders

interface FragmentsContractActivity {
    fun setCheckup(checkup: Checkup)
    fun setChecupListOrder(order: Orders)
    fun setCoordinates(point: Point, controlId: Int)
    //fun setDataForRouteDetail(order: Orders, mapFragment: MapFragment)
    fun setOrder(order: Orders)
}