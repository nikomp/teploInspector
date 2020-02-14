package ru.bingosoft.mapquestapp2.ui.order

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order.*
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Orders.Orders
import ru.bingosoft.mapquestapp2.ui.checkuplist.CheckupListFragment
import ru.bingosoft.mapquestapp2.ui.login.LoginActivity
import ru.bingosoft.mapquestapp2.ui.login.LoginContractView
import ru.bingosoft.mapquestapp2.ui.login.LoginPresenter
import ru.bingosoft.mapquestapp2.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.mapquestapp2.util.Const
import ru.bingosoft.mapquestapp2.util.SharedPrefSaver
import ru.bingosoft.mapquestapp2.util.Toaster
import timber.log.Timber
import javax.inject.Inject

class OrderFragment : Fragment(), LoginContractView, OrderContractView, OrdersRVClickListeners {

    @Inject
    lateinit var loginPresenter: LoginPresenter

    @Inject
    lateinit var orderPresenter: OrderPresenter

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var sharedPref: SharedPrefSaver

    lateinit var currentOrder: Orders
    lateinit var root: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)

        root = inflater.inflate(R.layout.fragment_order, container, false)

        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.menu_orders)

        doAuthorization()
        return root
    }


    override fun onDestroy() {
        super.onDestroy()
        orderPresenter.onDestroy()
        loginPresenter.onDestroy()
    }

    private fun doAuthorization() {
        // Получим логин и пароль из настроек
        val sharedpref = this.activity?.getSharedPreferences(Const.SharedPrefConst.APP_PREFERENCES, Context.MODE_PRIVATE)
        if (sharedpref!!.contains(Const.SharedPrefConst.LOGIN) && sharedpref.contains(Const.SharedPrefConst.PASSWORD)) {

            val login = sharedPref.getLogin()
            val password = sharedPref.getPassword()

            loginPresenter.attachView(this)
            loginPresenter.authorization(login, password) // Проверим есть ли авторизация
        } else {
            Timber.d("логин/пароль=ОТСУТСТВУЮТ")
            // Запустим активити с настройками
            val intent = Intent(this.activity, LoginActivity::class.java)
            startActivityForResult(intent, Const.RequestCodes.AUTH)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Timber.d( "resultCode OK")
            when (requestCode) {
                Const.RequestCodes.AUTH -> {
                    Timber.d( "Авторизуемся")
                    if (data != null) {
                        val login = data.getStringExtra("login")
                        val password = data.getStringExtra("password")

                        loginPresenter.attachView(this)
                        loginPresenter.authorization(login, password)
                    }
                }
                else -> {
                    //toaster.showToast(R.string.unknown_requestCode)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun showMessageLogin(resID: Int) {
        toaster.showToast(resID)

        orderPresenter.attachView(this)
        //orderPresenter.importData()
        orderPresenter.loadOrders()
    }

    override fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String) {
        sharedPref.saveLogin(stLogin)
        sharedPref.savePassword(stPassword)
    }

    override fun showFailureTextView() {
        val tv=textfailure
        tv.visibility=View.VISIBLE
    }

    override fun showOrders(orders: List<Orders>) {
        val orders_recycler_view = root.findViewById(R.id.orders_recycler_view) as RecyclerView
        orders_recycler_view.layoutManager = LinearLayoutManager(this.activity)
        val adapter = OrderListAdapter(orders,this, this.requireContext())
        orders_recycler_view.adapter = adapter
    }

    override fun showMessageOrders(msg: String) {
        //TODO реализую позже
    }

    override fun recyclerViewListClicked(v: View?, position: Int) {
        Timber.d("recyclerViewListClicked")

        currentOrder=(orders_recycler_view.adapter as OrderListAdapter).getOrder(position)

        currentOrder.checked=!currentOrder.checked

        Timber.d(currentOrder.checked.toString())
        if (currentOrder.checked) {
            val cardView = v?.findViewById<CardView>(R.id.cv)
            cardView?.setCardBackgroundColor(
                ContextCompat.getColor(
                    v.context,
                    R.color.colorCardSelect
                )
            )
        } else {
            val cardView = v?.findViewById<CardView>(R.id.cv)
            cardView?.setCardBackgroundColor(
                ContextCompat.getColor(
                    v.context,
                    R.color.colorCardItem
                )
            )
        }

        //Включаем фрагмент со списком Обследований для конкретной заявки
        val fragmentCheckupList= CheckupListFragment()
        val fragmentManager=this.requireActivity().supportFragmentManager

        fragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragmentCheckupList, "checkup_list_fragment_tag")
            .addToBackStack(null)
            .commit()

        fragmentManager.executePendingTransactions()

        (this.requireActivity() as FragmentsContractActivity).setChecupListOrder(currentOrder)


    }


}