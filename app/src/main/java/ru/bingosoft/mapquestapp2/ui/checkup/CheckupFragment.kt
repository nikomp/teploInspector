package ru.bingosoft.mapquestapp2.ui.checkup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.util.Toaster
import ru.bingosoft.mapquestapp2.util.UICreator
import timber.log.Timber
import javax.inject.Inject


class CheckupFragment : Fragment(), CheckupContractView, View.OnClickListener {

    @Inject
    lateinit var checkupPresenter: CheckupPresenter

    @Inject
    lateinit var toaster: Toaster

    private lateinit var root: View
    private lateinit var uiCreator: UICreator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)

        val view = inflater.inflate(R.layout.fragment_gallery, container, false)
        this.root=view

        val btnSave = view.findViewById(R.id.mbSaveCheckup) as MaterialButton
        btnSave.setOnClickListener(this)

        checkupPresenter.attachView(this)
        //checkupPresenter.loadCheckup(1)

        // Устанавливаем заголовок фрагмента
        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.title_checkup_fragment)

        Timber.d("CheckupFragment.onCreateView")

        return view
    }

    override fun dataIsLoaded(checkup: Checkup) {
        Timber.d("Checkup готов к работе")
        Timber.d(checkup.toString())

        uiCreator=UICreator(root,checkup)
        uiCreator.create()

    }

    override fun showCheckupMessage(resID: Int) {
        toaster.showToast(resID)
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.mbSaveCheckup -> {
                    checkupPresenter.saveCheckup(uiCreator)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        checkupPresenter.onDestroy()
    }


}