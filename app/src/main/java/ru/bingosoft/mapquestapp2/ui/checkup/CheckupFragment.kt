package ru.bingosoft.mapquestapp2.ui.checkup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.util.UICreator
import timber.log.Timber
import javax.inject.Inject

class CheckupFragment : Fragment(), CheckupContractView {

    @Inject
    lateinit var checkupPresenter: CheckupPresenter

    lateinit var root: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)

        root = inflater.inflate(R.layout.fragment_gallery, container, false)

        checkupPresenter.attachView(this)
        checkupPresenter.loadCheckup(1)

        return root
    }

    override fun dataIsLoaded(checkup: Checkup) {
        Timber.d("Checkup готов к работе")
        Timber.d(checkup.toString())

        val ui=UICreator(root,checkup)
        ui.create()

    }
}