package ru.bingosoft.mapquestapp2.ui.checkuplist_bottom

import ru.bingosoft.mapquestapp2.db.CheckupGuide.CheckupGuide

interface CheckupListBottomSheetContractView {
    fun showKindObject(checkupGuides: List<CheckupGuide>)
    fun saveNewObjectOk()
}