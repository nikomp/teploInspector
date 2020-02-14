package ru.bingosoft.mapquestapp2.ui.mainactivity

import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.db.Orders.Orders

interface FragmentsContractActivity {
    fun setCheckup(checkup: Checkup)
    fun setChecupListOrder(order: Orders)
}