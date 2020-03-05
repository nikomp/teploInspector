package ru.bingosoft.mapquestapp2.ui.checkuplist_bottom

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.CheckupGuide.CheckupGuide
import ru.bingosoft.mapquestapp2.util.Toaster
import timber.log.Timber
import javax.inject.Inject


class CheckupListBottomSheet: BottomSheetDialogFragment(), CheckupListBottomSheetContractView, View.OnClickListener  {
    private lateinit var mbSpinner: MaterialBetterSpinner

    @Inject
    lateinit var clbsPresenter: CheckupListBottomSheetPresenter
    @Inject
    lateinit var toaster: Toaster

    private lateinit var checkupGuideCurrent: CheckupGuide
    private lateinit var rootView: View

    override fun setupDialog(dialog: Dialog, style: Int) {
        AndroidSupportInjection.inject(this)
        super.setupDialog(dialog, style)

        val view=LayoutInflater.from(context).inflate(R.layout.checkuplist_bottom_sheet,null)
        this.rootView=view
        // Заполним комбобокс Тип Объекта
        mbSpinner=view.findViewById(R.id.spinner_kindobject)
        dialog.setContentView(view)

        val btnSave = view.findViewById(R.id.btnSaveNewObject) as MaterialButton
        btnSave.setOnClickListener(this)

        clbsPresenter.attachView(this)
        clbsPresenter.getKindObject()

    }

    override fun showKindObject(checkupGuides: List<CheckupGuide>) {
        Timber.d("Выводим список объектов")

        val dataSpinner=arrayListOf<String>()
        checkupGuides.forEach {
            dataSpinner.add(it.kindCheckup)
        }

        Timber.d(dataSpinner.toString())

        val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
            this.requireContext(),
            R.layout.template_multiline_spinner_item,
            dataSpinner
        )

        mbSpinner.setAdapter(spinnerArrayAdapter)
        // Вешаем обработчик на spinner последним, иначе сбрасывается цвет шага
        mbSpinner.setOnItemClickListener { adapterView: AdapterView<*>, view1: View, position: Int, l: Long ->
            Timber.d("position $position")
            checkupGuideCurrent=checkupGuides[position]

        }
    }

    override fun saveNewObjectOk() {
        Timber.d("Сохранили объект обследования")
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.btnSaveNewObject -> {
                    val stNameObject=rootView.findViewById<TextInputEditText>(R.id.tietNameObject).text
                    if (::clbsPresenter.isInitialized && !stNameObject.isNullOrEmpty()) {
                        clbsPresenter.saveObject(checkupGuideCurrent, stNameObject.toString())

                        val params =
                            (rootView.parent as View).layoutParams as CoordinatorLayout.LayoutParams
                        val behavior = params.behavior
                        if (behavior!=null && behavior is BottomSheetBehavior) {
                            behavior.state=BottomSheetBehavior.STATE_HIDDEN
                        }

                    } else {
                        toaster.showToast(R.string.btmSheetSaveInvalid)
                    }

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clbsPresenter.onDestroy()
    }


}