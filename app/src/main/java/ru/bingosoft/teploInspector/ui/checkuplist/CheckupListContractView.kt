package ru.bingosoft.teploInspector.ui.checkuplist

import ru.bingosoft.teploInspector.db.Checkup.Checkup

interface CheckupListContractView {
    fun showCheckups(checkups: List<Checkup>)
}