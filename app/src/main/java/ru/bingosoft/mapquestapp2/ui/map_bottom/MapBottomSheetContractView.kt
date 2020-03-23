package ru.bingosoft.mapquestapp2.ui.map_bottom

import ru.bingosoft.mapquestapp2.db.Orders.Orders

interface MapBottomSheetContractView {
    fun showOrder(order: Orders)
}