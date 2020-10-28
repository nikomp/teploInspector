package ru.bingosoft.teploInspector.ui.order

import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.TechParams.TechParams

interface OrderContractView {
    fun showOrders(orders: List<Orders>)
    fun showMessageOrders(msg: String)
    fun techParamsLoaded(techParams:List<TechParams>)

    fun errorReceived(throwable: Throwable)
}