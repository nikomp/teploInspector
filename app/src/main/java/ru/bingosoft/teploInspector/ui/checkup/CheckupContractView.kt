package ru.bingosoft.teploInspector.ui.checkup

import ru.bingosoft.teploInspector.db.Checkup.Checkup

interface CheckupContractView {
    fun dataIsLoaded(checkup: Checkup)
    fun showCheckupMessage(resID: Int)
}