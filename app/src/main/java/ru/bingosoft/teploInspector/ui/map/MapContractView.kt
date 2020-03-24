package ru.bingosoft.teploInspector.ui.map

import ru.bingosoft.teploInspector.db.Orders.Orders

interface MapContractView {
    fun showMarkers(orders: List<Orders>)
}