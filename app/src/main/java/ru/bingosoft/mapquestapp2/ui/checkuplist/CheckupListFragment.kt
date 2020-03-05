package ru.bingosoft.mapquestapp2.ui.checkuplist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_checkuplist.*
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.db.Orders.Orders
import ru.bingosoft.mapquestapp2.ui.checkup.CheckupFragment
import ru.bingosoft.mapquestapp2.ui.checkuplist_bottom.CheckupListBottomSheet
import ru.bingosoft.mapquestapp2.ui.mainactivity.FragmentsContractActivity
import timber.log.Timber
import javax.inject.Inject


class CheckupListFragment: Fragment(), CheckupListContractView, CheckupListRVClickListeners {

    @Inject
    lateinit var checkupListPresenter: CheckupListPresenter

    private lateinit var root: View
    private lateinit var currentCheckup: Checkup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)
        Timber.d("CheckupListFragment.onCreateView")

        root = inflater.inflate(R.layout.fragment_checkuplist, container, false)

        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.menu_checkups)

        setupViewBottomScreen()

        val tag = arguments?.getBoolean("checkUpForOrder")
        Timber.d("tag=$tag")

        checkupListPresenter.attachView(this)
        if (tag==null || tag==false) {
            checkupListPresenter.loadCheckupList() // Грузим все объекты
        }


        return root
    }

    private fun setupViewBottomScreen() {

        val fab: FloatingActionButton = root.findViewById(R.id.fabAddCheckup)
        fab.setOnClickListener {
            Timber.d("Добавляем новый чекап, параллельно создаем заявку, если открыли чекаплист без привязки к заявке")
            val bottomSheetDialogFragment = CheckupListBottomSheet()
            bottomSheetDialogFragment.show(this.requireActivity().supportFragmentManager,"BOTTOM_SHEET")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        checkupListPresenter.onDestroy()
    }

    override fun showCheckups(checkups: List<Checkup>) {
        Timber.d("Список обследований")
        val checkuplist_recycler_view = root.findViewById(R.id.checkuplist_recycler_view) as RecyclerView
        checkuplist_recycler_view.layoutManager = LinearLayoutManager(this.activity)
        val adapter = CheckupsListAdapter(checkups,this, this.requireContext())
        Timber.d("В адаптере строк=${adapter.itemCount}")
        checkuplist_recycler_view.adapter = adapter
    }


    override fun recyclerViewListClicked(v: View?, position: Int) {
        Timber.d("Открываем выбранное обследование")

        currentCheckup=(checkuplist_recycler_view.adapter as CheckupsListAdapter).getCheckup(position)
        Timber.d(currentCheckup.toString())

        val fragmentCheckup=CheckupFragment()
        val fragmentManager=this.requireActivity().supportFragmentManager

        fragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragmentCheckup, "checkup_fragment_tag")
            .addToBackStack(null)
            .commit()

        fragmentManager.executePendingTransactions()

        (this.requireActivity() as FragmentsContractActivity).setCheckup(currentCheckup)

    }

    fun showCheckupListOrder(order: Orders) {
        checkupListPresenter.loadCheckupListByOrder(order.id)
    }


}