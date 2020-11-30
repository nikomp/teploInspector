package ru.bingosoft.teploInspector.util

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import ru.bingosoft.teploInspector.R
import timber.log.Timber


class Toaster(private val ctx: Context) {
    fun showToast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        showToast(ctx.getString(resId), duration) // Используется перегрузка метода
    }

    fun showToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        Timber.d("showToast")
        val toaster=View.inflate(ctx,R.layout.toaster,null)

        val text=toaster.findViewById<TextView>(R.id.textToast)
        text.text=msg

        val toast=Toast(ctx)
        toast.duration=duration
        toast.view=toaster
        toast.show()
    }

    fun showErrorToast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        showErrorToast(ctx.getString(resId), duration) // Используется перегрузка метода
    }

    fun showErrorToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        Timber.d("showToast")
        val toaster=View.inflate(ctx,R.layout.toaster,null)
        val llToast=toaster.findViewById<LinearLayout>(R.id.llToast)
        llToast.background=ContextCompat.getDrawable(ctx, R.drawable.toasterror)

        val text=toaster.findViewById<TextView>(R.id.textToast)
        text.setTextColor(ContextCompat.getColor(ctx, R.color.error))
        text.text=msg

        val toast=Toast(ctx)
        toast.duration=duration
        toast.view=toaster
        toast.show()
    }
}