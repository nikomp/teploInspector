package ru.bingosoft.teploInspector.ui.checkup

import ru.bingosoft.teploInspector.db.AddLoad.AddLoad
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.TechParams.TechParams

interface CheckupContractView {
    fun dataIsLoaded(checkup: Checkup)
    fun showCheckupMessage(resID: Int)
    fun setAnsweredCount(count: Int)
    fun techParamsLoaded(techParams: List<TechParams>)
    fun addLoadsLoaded(addLoads: List<AddLoad>)

    fun errorReceived(throwable: Throwable)

    fun sendGiOrder()
    fun doSaveCheckup()
}