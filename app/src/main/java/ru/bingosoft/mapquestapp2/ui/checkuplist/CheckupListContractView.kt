package ru.bingosoft.mapquestapp2.ui.checkuplist

import ru.bingosoft.mapquestapp2.db.Checkup.Checkup

interface CheckupListContractView {
    fun showCheckups(checkups: List<Checkup>)
}