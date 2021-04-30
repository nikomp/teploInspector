package ru.bingosoft.teploInspector.util

import android.content.Context
import android.graphics.Rect
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner

/*Подробности тут https://github.com/Lesilva/BetterSpinner/issues/53#issuecomment-262440059
При возникновении ошибки com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner.onFocusChanged (MaterialBetterSpinner.java:49)
нужно переопределить метод onFocusChanged, из-за того что потребуется вносить много правок в код
решил пока ограничиться вот этим https://github.com/Lesilva/BetterSpinner/issues/15#issuecomment-295115943,
https://github.com/Lesilva/BetterSpinner/issues/15#issuecomment-142473736
Не тестировал!
*/
class MyMaterialBetterSpinner(val ctx: Context): MaterialBetterSpinner(ctx) {
    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        try {
            super.onFocusChanged(focused, direction, previouslyFocusedRect)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}