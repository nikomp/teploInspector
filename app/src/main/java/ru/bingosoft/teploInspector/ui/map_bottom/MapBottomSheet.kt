package ru.bingosoft.teploInspector.ui.map_bottom

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import timber.log.Timber
import javax.inject.Inject

class MapBottomSheet(val order: Orders): BottomSheetDialogFragment(), MapBottomSheetContractView, View.OnClickListener {

    private lateinit var rootView: View

    @Inject
    lateinit var mbsPresenter: MapBottomSheetPresenter

    override fun setupDialog(dialog: Dialog, style: Int) {
        AndroidSupportInjection.inject(this)
        super.setupDialog(dialog, style)

        Timber.d("MapBottomSheet_setupDialog")

        val view=
            LayoutInflater.from(context).inflate(R.layout.map_bottom_sheet,null)
        this.rootView=view
        dialog.setContentView(view)

        val btn = view.findViewById(R.id.btnDriveway) as Button
        btn.setOnClickListener(this)

        mbsPresenter.attachView(this)

        // Заполним текстовые поля
        val tvNumber=rootView.findViewById<TextView>(R.id.symbolNumber)
        tvNumber?.text=order.number

        val tvName=rootView.findViewById<TextView>(R.id.symbolName)
        tvName?.text=order.name
    }

    override fun onDestroy() {
        super.onDestroy()
        mbsPresenter.onDestroy()
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.btnDriveway -> {
                    Timber.d("MapBottomSheet_onClick")
                }
            }
        }
    }

}