package ru.bingosoft.teploInspector.ui.checkuplist_bottom

import ru.bingosoft.teploInspector.db.CheckupGuide.CheckupGuide

interface CheckupListBottomSheetContractView {
    fun showKindObject(checkupGuides: List<CheckupGuide>)
    fun saveNewObjectOk()
}