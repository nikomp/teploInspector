package ru.bingosoft.teploInspector.ui.order

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.alert_not_internet.view.*
import kotlinx.android.synthetic.main.alert_syncdb.view.*
import kotlinx.android.synthetic.main.fragment_order.*
import retrofit2.HttpException
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.login.LoginActivity
import ru.bingosoft.teploInspector.ui.login.LoginContractView
import ru.bingosoft.teploInspector.ui.login.LoginPresenter
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivityPresenter
import ru.bingosoft.teploInspector.util.*
import timber.log.Timber
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject


class OrderFragment : Fragment(), LoginContractView, OrderContractView, OrdersRVClickListeners, View.OnClickListener {

    @Inject
    lateinit var loginPresenter: LoginPresenter

    @Inject
    lateinit var mainPresenter: MainActivityPresenter

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
    lateinit var root: View
    lateinit var orders: List<Orders>
    //var filteredOrdersOrderFragment: List<Orders> = listOf()


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


        Timber.d("root=$root")
        return root
    }


    override fun onDestroy() {
        super.onDestroy()
        orderPresenter.onDestroy()
        loginPresenter.onDestroy()
    }

    fun doAuthorization(factivity: FragmentActivity=this.requireActivity()) {
        Timber.d("doAuthorization")
        // Получим логин и пароль из настроек
        val sharedPref = factivity.getSharedPreferences(Const.SharedPrefConst.APP_PREFERENCES, Context.MODE_PRIVATE)
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
        Timber.d("OrderFragment onCreate")
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)

        if (sharedPref.getLogin()=="" && sharedPref.getPassword()=="") {
            doAuthorization()
        } else {
            Timber.d("sharedPref.getLogin()=${sharedPref.getLogin()}")
            orderPresenter.attachView(this)
            orderPresenter.loadOrders()

        }


       /*val locationManager=this.requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
       locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 10f, userLocationNative.locationListener)*/
   }

    private fun requestGPSPermission() {
        // Проверим разрешения
        Timber.d("requestGPSPermission")
        if (ContextCompat.checkSelfPermission(this.requireContext(),(Manifest.permission.ACCESS_FINE_LOCATION)) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("requestPermission1")
            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.M) {
                Timber.d("requestPermission2")
                requestPermissions(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                    Const.RequestCodes.PERMISSION
                )
            }
        } else {
            activity?.startService(Intent(this.requireContext(),UserLocationService::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Timber.d("onRequestPermissionsResult")
        when (requestCode) {
            Const.RequestCodes.PERMISSION -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    // Разрешения выданы
                    Timber.d("startService_Permission")
                    val locationManager=this.requireContext().getSystemService(LOCATION_SERVICE) as LocationManager
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 10f, userLocationNative.locationListener)

                    activity?.startService(Intent(this.requireContext(),UserLocationService::class.java))
                } else {
                    // Разрешения не выданы оповестим юзера
                    toaster.showToast(R.string.not_permissions)
                    Timber.d("ОТКАЗАЛСЯ ОТ ГЕОЛОКАЦИИ")
                    mainPresenter.sendMessageToAdmin(Const.MessageCode.REFUSED_PERMISSION)
                }
            }
            else -> Timber.d("Неизвестный PERMISSION_REQUEST_CODE")
        }

    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun showMessageLogin(resID: Int) {
        toaster.showToast(resID)
    }

    override fun showMessageLogin(msg: String) {
        toaster.showToast(msg)
    }

    override fun showOrders() {
        Timber.d("showOrders")
        orderPresenter.attachView(this)
        //orderPresenter.importData()
        orderPresenter.loadOrders()
    }

    override fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String) {
        sharedPref.saveLogin(stLogin)
        sharedPref.savePassword(stPassword)
        requestGPSPermission()
    }

    override fun saveToken(token: String) {
        sharedPref.saveToken(token)
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
        longInfo("showOrdersVV=${orders}")

        val pb=root.findViewById<ProgressBar>(R.id.progressBar)
        pb.visibility= View.INVISIBLE

        this.orders=orders
        (activity as MainActivity).orders=orders
        //Timber.d("filteredOrders=$filteredOrdersOrderFragment")
        Timber.d("filteredOrders=${(activity as MainActivity).filteredOrders}")

        // инициализируем контейнер SwipeRefreshLayout
        val swipeRefreshLayout = root.findViewById(R.id.srl_container) as SwipeRefreshLayout

        // указываем слушатель свайпов пользователя
        swipeRefreshLayout.setOnRefreshListener {
            loginPresenter.attachView(this)
            loginPresenter.syncDB()
            swipeRefreshLayout.isRefreshing = false
        }

        val ordersRecyclerView = root.findViewById(R.id.orders_recycler_view) as RecyclerView
        ordersRecyclerView.layoutManager = LinearLayoutManager(this.activity)
        val adapter=

        if ((activity as MainActivity).filteredOrders.isEmpty()) {
            OrderListAdapter(orders,this, this, userLocationNative.userLocation)
        } else {
            OrderListAdapter((activity as MainActivity).filteredOrders,this, this, userLocationNative.userLocation)
        }


        ordersRecyclerView.adapter = adapter
    }

    //#Android_Studio #длинный_лог
    // Функция для вывода длинных строк в лог. Использовать вместо Timber.d
    private fun longInfo(str: String) {
        if (str.length > 3000) {
            Timber.d( str.substring(0, 3000))
            longInfo(str.substring(3000))
        } else Timber.d(str)
    }

    override fun showMessageOrders(msg: String) {
        //TODO реализую позже
    }

    fun filteredOrderByGroup(filterGroupList: List<String>) {
        Timber.d("filteredOrderByGroup")
        val rcv=root.findViewById(R.id.orders_recycler_view) as RecyclerView

        Timber.d("orders=${orders}")
        val filteredOrderByGroup=orders.filter { it.groupOrder in filterGroupList }

        Timber.d("test=$filteredOrderByGroup")

        val adapter = OrderListAdapter(filteredOrderByGroup,this, this, userLocationNative.userLocation)
        rcv.adapter = adapter

        adapter.ordersFilterList=filteredOrderByGroup
    }

    fun filteredOrderByDate(strDate: String) {
        Timber.d("filteredOrderByDate")
        val rcv=root.findViewById(R.id.orders_recycler_view) as RecyclerView
        var filteredOrderByDate= listOf<Orders>()
        if (strDate=="all") {
            filteredOrderByDate=orders.filter { it.dateVisit !=null }
        } else {
            filteredOrderByDate=orders.filter { it.dateVisit ==strDate }
        }


        val adapter = OrderListAdapter(filteredOrderByDate,this, this, userLocationNative.userLocation)
        rcv.adapter = adapter

        adapter.ordersFilterList=filteredOrderByDate
    }

    override fun techParamsLoaded(techParams: List<TechParams>) {
        Timber.d("techParamsLoaded $techParams")
        (activity as MainActivity).techParams=techParams
    }

    override fun errorReceived(throwable: Throwable) {
        Timber.d("errorReceived")
        when (throwable) {
            is HttpException -> {
                Timber.d("throwable.code()=${throwable.code()}")
                when (throwable.code()) {
                    401 -> toaster.showToast(R.string.unauthorized)
                    else -> toaster.showToast("Ошибка! ${throwable.message}")
                }
            }
            is UnknownHostException ->{
                toaster.showToast(R.string.no_address_hostname)
            }
            else -> {
                toaster.showToast("Ошибка! ${throwable.message}")
            }
        }

    }


    override fun recyclerViewListClicked(v: View?, position: Int) {
        Timber.d("recyclerViewListClicked")


        currentOrder=(orders_recycler_view.adapter as OrderListAdapter).getOrder(position)
        Timber.d("currentOrder.questionCount=${currentOrder.questionCount}")
        currentOrder.checked=!currentOrder.checked
        (activity as MainActivity).currentOrder=this.currentOrder

        val bundle = Bundle()
        bundle.putBoolean("checkUpForOrder", true)
        bundle.putLong("idOrder",currentOrder.id)
        bundle.putString("typeOrder",currentOrder.typeOrder)

        Navigation.findNavController(root).navigate(R.id.nav_checkup,bundle)

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

                    val rcv=root.findViewById(R.id.orders_recycler_view) as RecyclerView

                    (this.requireActivity() as MainActivity).filteredOrders=(rcv.adapter as OrderListAdapter).ordersFilterList
                    (this.requireActivity() as MainActivity).navController.navigate(R.id.nav_slideshow)
                    (this.requireActivity() as FragmentsContractActivity).setMode()

                }
            }
        }
    }

}