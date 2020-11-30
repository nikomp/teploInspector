package ru.bingosoft.teploInspector.ui.checkup

import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.TechParams.TechParams

interface CheckupContractView {
    fun dataIsLoaded(checkup: Checkup)
    fun showCheckupMessage(resID: Int)
    fun setAnsweredCount(count: Int)
    fun techParamsLoaded(techParams: List<TechParams>)

    fun errorReceived(throwable: Throwable)

    fun sendGiOrder()
    fun doSaveCheckup()
}