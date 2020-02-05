package ru.bingosoft.mapquestapp2.ui.order

import ru.bingosoft.mapquestapp2.db.Orders.Orders

interface OrderContractView {
    fun showOrders(orders: List<Orders>)
    fun showMessageOrders(msg: String)
}