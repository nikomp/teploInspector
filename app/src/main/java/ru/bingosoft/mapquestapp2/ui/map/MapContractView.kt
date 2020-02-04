package ru.bingosoft.mapquestapp2.ui.map

import ru.bingosoft.mapquestapp2.db.Orders.Orders

interface MapContractView {
    fun showMarkers(orders: List<Orders>)
}