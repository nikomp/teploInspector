package ru.bingosoft.mapquestapp2.ui.checkup

import ru.bingosoft.mapquestapp2.db.Checkup.Checkup

interface CheckupContractView {
    fun dataIsLoaded(checkup: Checkup)
    fun showCheckupMessage(resID: Int)
}