package ru.bingosoft.teploInspector.ui.mainactivity

import com.yandex.mapkit.geometry.Point
import ru.bingosoft.teploInspector.db.Orders.Orders

interface FragmentsContractActivity {
    fun setCoordinates(point: Point, controlId: Int)
    fun setMode(isMap:Boolean=true)

    fun showMarkers(orders: List<Orders>)
    //fun showSearchedOrders(orders: List<Orders>)
}