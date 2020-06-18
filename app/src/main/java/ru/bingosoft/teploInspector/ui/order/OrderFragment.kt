package ru.bingosoft.teploInspector.ui.order

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.alert_not_internet.view.*
import kotlinx.android.synthetic.main.alert_syncdb.view.*
import kotlinx.android.synthetic.main.fragment_order.*
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.checkuplist.CheckupListFragment
import ru.bingosoft.teploInspector.ui.login.LoginActivity
import ru.bingosoft.teploInspector.ui.login.LoginContractView
import ru.bingosoft.teploInspector.ui.login.LoginPresenter
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.util.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class OrderFragment : Fragment(), LoginContractView, OrderContractView, OrdersRVClickListeners, View.OnClickListener {

    @Inject
    lateinit var loginPresenter: LoginPresenter

    @Inject
    lateinit var orderPresenter: OrderPresenter

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var sharedPref: SharedPrefSaver

    @Inject
    lateinit var otherUtil: OtherUtil

    @Inject
    lateinit var userLocationNative: UserLocationNative

    private lateinit var currentOrder: Orders
    private lateinit var root: View


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Timber.d("OrderFragment.onCreateView")

        root = inflater.inflate(R.layout.fragment_order, container, false)

        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.menu_orders)

        val btnList=root.findViewById<Button>(R.id.btnList)
        btnList.setOnClickListener(this)
        val btnMap=root.findViewById<Button>(R.id.btnMap)
        btnMap.setOnClickListener(this)

        // Авторизуемся всегда иначе данные не будут уходить на сервер
        /*doAuthorization()
        Timber.d("sharedPref.getLogin()=${sharedPref.getLogin()}")
        orderPresenter.attachView(this)
        orderPresenter.loadOrders()

        val pb=root.findViewById<ProgressBar>(R.id.progressBar)
        pb.visibility= View.INVISIBLE*/


        // Если логин/пароль есть не авторизуемся
        if (sharedPref.getLogin()=="" && sharedPref.getPassword()=="") {
            doAuthorization()
        } else {
            Timber.d("sharedPref.getLogin()=${sharedPref.getLogin()}")
            orderPresenter.attachView(this)
            orderPresenter.loadOrders()

            val pb=root.findViewById<ProgressBar>(R.id.progressBar)
            pb.visibility= View.INVISIBLE
        }

        return root
    }


    override fun onDestroy() {
        super.onDestroy()
        orderPresenter.onDestroy()
        loginPresenter.onDestroy()
    }

    private fun doAuthorization() {
        Timber.d("doAuthorization")
        // Получим логин и пароль из настроек
        val sharedPref = this.activity?.getSharedPreferences(Const.SharedPrefConst.APP_PREFERENCES, Context.MODE_PRIVATE)
        if (sharedPref!!.contains(Const.SharedPrefConst.LOGIN) && sharedPref.contains(Const.SharedPrefConst.PASSWORD)) {

            val login = this.sharedPref.getLogin()
            val password = this.sharedPref.getPassword()

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


    override fun onCreate(savedInstanceState: Bundle?) {
       AndroidSupportInjection.inject(this)
       super.onCreate(savedInstanceState)

       val locationManager=this.requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
       locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 10f, userLocationNative.locationListener)
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

    override fun showMessageLogin(msg: String) {
        toaster.showToast(msg)
    }

    override fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String) {
        sharedPref.saveLogin(stLogin)
        sharedPref.savePassword(stPassword)
    }

    override fun showFailureTextView() {
        val tv=textfailure
        tv.visibility=View.VISIBLE
        val pb=progressBar
        pb.visibility=View.INVISIBLE
        alertNotInternet()
    }

    /**
     * Метод, проверяющий доступность БД
     * @return - true/false
     */
    override fun alertRepeatSync() {
        Timber.d("doSync")
        val pb=progressBar
        pb.visibility= View.INVISIBLE

        val dbFile = this.requireContext().getDatabasePath("mydatabase.db")
        lateinit var alertDialog: AlertDialog
        if (dbFile.exists()) {
            val layoutInflater = LayoutInflater.from(this.requireContext())
            val dialogView: View =
                layoutInflater.inflate(R.layout.alert_syncdb, (root.parent as ViewGroup), false)

            if (sharedPref.getDateSyncDB()!="") {
                dialogView.stMsgAlert.text=getString(R.string.syncdb, sharedPref.getDateSyncDB())
            } else {
                dialogView.stMsgAlert.text=getString(R.string.syncdb2)
            }

            val builder = AlertDialog.Builder(this.context)

            dialogView.buttonOK.setOnClickListener{
                Timber.d("dialogView.buttonOK")
                loginPresenter.syncDB()
                alertDialog.dismiss()

            }

            dialogView.buttonNo.setOnClickListener{
                showMessageLogin(R.string.auth_ok)
                orderPresenter.loadOrders() // Грузим данные из локальной БД
                alertDialog.dismiss()
            }

            builder.setView(dialogView)
            builder.setCancelable(true)
            alertDialog=builder.create()
            alertDialog.show()
        } else {
            loginPresenter.syncDB()
        }

    }

    private fun alertNotInternet() {
        Timber.d("alertNotInternet")
        lateinit var alertDialogNotInternet: AlertDialog
        val layoutInflater = LayoutInflater.from(this.requireContext())
        val dialogView: View =
            layoutInflater.inflate(R.layout.alert_not_internet, (root.parent as ViewGroup), false)

        val builder = AlertDialog.Builder(this.context)

        dialogView.buttonInetOK.setOnClickListener{
            Timber.d("dialogView.buttonInetOK")
            //TODO возможно тут потребуется вывести окно авторизации
            showMessageLogin(R.string.auth_ok)
            alertDialogNotInternet.dismiss()

        }

        dialogView.buttonInetNo.setOnClickListener{
            alertDialogNotInternet.dismiss()
        }

        builder.setView(dialogView)
        builder.setCancelable(true)
        alertDialogNotInternet=builder.create()
        alertDialogNotInternet.show()


    }

    override fun saveDateSyncToSharedPreference(date: Date) {
        Timber.d("saveDateSyncToSharedPreference")
        sharedPref.saveDateSyncDB(date)
    }

    override fun saveInfoUserToSharedPreference(user: Models.User) {
        sharedPref.saveUser(user)
    }

    override fun startLocationService() {
        // Стартуем фоновый сервис для отслеживания пользователя
        activity?.startService(Intent(activity, UserLocationService::class.java))
    }

    override fun showOrders(orders: List<Orders>) {

        // инициализируем контейнер SwipeRefreshLayout
        val swipeRefreshLayout = root.findViewById(R.id.srl_container) as SwipeRefreshLayout

        // указываем слушатель свайпов пользователя
        swipeRefreshLayout.setOnRefreshListener {
            loginPresenter.syncDB()
            swipeRefreshLayout.isRefreshing = false
        }

        Timber.d("userLocation2342342=${userLocationNative.userLocation}")

        val ordersRecyclerView = root.findViewById(R.id.orders_recycler_view) as RecyclerView
        ordersRecyclerView.layoutManager = LinearLayoutManager(this.activity)
        val adapter = OrderListAdapter(orders,this, this, userLocationNative.userLocation)
        ordersRecyclerView.adapter = adapter
    }

    override fun showMessageOrders(msg: String) {
        //TODO реализую позже
    }

    override fun recyclerViewListClicked(v: View?, position: Int) {
        Timber.d("recyclerViewListClicked")

        currentOrder=(orders_recycler_view.adapter as OrderListAdapter).getOrder(position)

        currentOrder.checked=!currentOrder.checked


        (activity as MainActivity).currentOrder=this.currentOrder

        //Включаем фрагмент со списком Обследований для конкретной заявки
        val bundle = Bundle()
        bundle.putBoolean("checkUpForOrder", true)
        bundle.putLong("idOrder",currentOrder.id)

        val fragmentCheckupList= CheckupListFragment()
        fragmentCheckupList.arguments=bundle
        val fragmentManager=this.requireActivity().supportFragmentManager

        fragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragmentCheckupList, "checkup_list_fragment_tag")
            .addToBackStack(null)
            .commit()

        fragmentManager.executePendingTransactions()

        (this.requireActivity() as FragmentsContractActivity).setChecupListOrder(currentOrder)

    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.btnList -> {
                    v.isEnabled=false
                    (v.parent as View).findViewById<Button>(R.id.btnMap).isEnabled=true
                }

                R.id.btnMap -> {
                    v.isEnabled=false
                    (v.parent as View).findViewById<Button>(R.id.btnList).isEnabled=true

                    val fragmentMap= MapFragment()
                    //fragmentMap.arguments=bundle
                    val fragmentManager=this.requireActivity().supportFragmentManager

                    fragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, fragmentMap, "")
                        .addToBackStack(null)
                        .commit()

                    fragmentManager.executePendingTransactions()

                }


            }
        }
    }


}