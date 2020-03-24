package ru.bingosoft.teploInspector.ui.order

import ru.bingosoft.teploInspector.db.Orders.Orders

interface OrderContractView {
    fun showOrders(orders: List<Orders>)
    fun showMessageOrders(msg: String)
}