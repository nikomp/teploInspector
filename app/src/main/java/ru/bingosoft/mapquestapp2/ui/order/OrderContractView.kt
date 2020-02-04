package ru.bingosoft.mapquestapp2.ui.order

import ru.bingosoft.mapquestapp2.db.Orders.Orders

interface OrderContractView {
    fun showOrders(targets: List<Orders>)
    fun showMessageOrders(msg: String)
}