package ru.bingosoft.teploInspector.ui.order

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.alert_not_internet.view.*
import kotlinx.android.synthetic.main.alert_syncdb.view.*
import kotlinx.android.synthetic.main.fragment_order.*
import retrofit2.HttpException
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.login.LoginActivity
import ru.bingosoft.teploInspector.ui.login.LoginContractView
import ru.bingosoft.teploInspector.ui.login.LoginPresenter
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivityPresenter
import ru.bingosoft.teploInspector.ui.mainactivity.UserLocationReceiver
import ru.bingosoft.teploInspector.util.*
import ru.bingosoft.teploInspector.util.Const.FinishTime.FINISH_HOURS
import ru.bingosoft.teploInspector.util.Const.FinishTime.FINISH_MINUTES
import ru.bingosoft.teploInspector.util.Const.MessageCode.USER_LOGIN
import ru.bingosoft.teploInspector.util.Const.SharedPrefConst.ENTER_TYPE
import ru.bingosoft.teploInspector.wsnotification.NotificationService
import timber.log.Timber
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit
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

    /*@Inject
    lateinit var userLocationNative: UserLocationNative*/

    @Inject
    lateinit var userLocationReceiver: UserLocationReceiver

    private lateinit var currentOrder: Orders
    lateinit var root: View
    //lateinit var alertDialogRepeatSync: AlertDialog


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("OrderFragment_onCreateView")
        root = inflater.inflate(R.layout.fragment_order, container, false)

        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.menu_orders)

        val btnList=root.findViewById<Button>(R.id.btnList)
        btnList.setOnClickListener(this)
        val btnMap=root.findViewById<Button>(R.id.btnMap)
        btnMap.setOnClickListener(this)

        // инициализируем контейнер SwipeRefreshLayout
        val swipeRefreshLayout = root.findViewById(R.id.srl_container) as SwipeRefreshLayout

        // указываем слушатель свайпов пользователя
        swipeRefreshLayout.setOnRefreshListener {
            Timber.d("swipeRefreshLayout.setOnRefreshListener")
            loginPresenter.attachView(this)
            loginPresenter.syncDB()
            swipeRefreshLayout.isRefreshing = false
            (activity as MainActivity).isDefaultFilter=false
        }

        Timber.d("filteredOrders=${(requireContext() as MainActivity).filteredOrders}")
        if ((requireContext() as MainActivity).filteredOrders.isNotEmpty()) {
            showFilterOrders((requireContext() as MainActivity).filteredOrders)
        }

        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("OrderFragment_onDestroy")
        if (this::orderPresenter.isInitialized) {
            orderPresenter.onDestroy()
        }
        if (this::loginPresenter.isInitialized) {
            loginPresenter.onDestroy()
        }
    }

    fun doAuthorization(factivity: FragmentActivity=this.requireActivity(),data:Models.RepeatAuthData?= null) {
        Timber.d("doAuthorization")
        // Получим логин и пароль из настроек
        val sharedPref = factivity.getSharedPreferences(Const.SharedPrefConst.APP_PREFERENCES, Context.MODE_PRIVATE)
        if (this.sharedPref.isAuth()) {
            if (sharedPref!!.contains(Const.SharedPrefConst.LOGIN) && sharedPref.contains(Const.SharedPrefConst.PASSWORD)) {

                val login = this.sharedPref.getLogin()
                val password = this.sharedPref.getPassword()

                // Для презентации
                val url = if (BuildConfig.BUILD_TYPE=="presentation") {
                    // Для презентации
                    if (this.sharedPref.getEnterType()=="directory_service") {
                        "http://teplomi.bingosoft-office.ru/ldapauthentication/auth/login"
                    } else {
                        "http://teplomi.bingosoft-office.ru/defaultauthentication/auth/login"
                    }
                } else {
                    if (this.sharedPref.getEnterType()=="directory_service") {
                        "https://mi.teploenergo-nn.ru/ldapauthentication/auth/login"
                    } else {
                        "https://mi.teploenergo-nn.ru/defaultauthentication/auth/login"
                    }
                }

                loginPresenter.attachView(this)
                loginPresenter.authorization(url, login, password) // Проверим есть ли авторизация
            } else {
                Timber.d("логин/пароль=ОТСУТСТВУЮТ")
                if (data!=null) {
                    // Для презентации
                    val url = if (BuildConfig.BUILD_TYPE=="presentation") {

                        // Для презентации
                        if (data.enter_type=="directory_service") {
                            "http://teplomi.bingosoft-office.ru/ldapauthentication/auth/login"
                        } else {
                            "http://teplomi.bingosoft-office.ru/defaultauthentication/auth/login"
                        }
                    } else {
                        if (data.enter_type=="directory_service") {
                            "https://mi.teploenergo-nn.ru/ldapauthentication/auth/login"
                        } else {
                            "https://mi.teploenergo-nn.ru/defaultauthentication/auth/login"
                        }
                    }
                    loginPresenter.attachView(this)
                    loginPresenter.authorization(url, data.login, data.password) // Проверим есть ли авторизация

                } else {
                    // Запустим активити с настройками
                    val intent = Intent(this.activity, LoginActivity::class.java)
                    startActivityForResult(intent, Const.RequestCodes.AUTH)
                }

            }
        } else {
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
                        val enterType = data.getStringExtra(ENTER_TYPE)


                        // Для презентации
                        val url = if (BuildConfig.BUILD_TYPE=="presentation") {
                            // Для презентации
                            if (enterType=="directory_service") {
                                "http://teplomi.bingosoft-office.ru/ldapauthentication/auth/login"
                            } else {
                                "http://teplomi.bingosoft-office.ru/defaultauthentication/auth/login"
                            }
                        } else {
                            if (enterType=="directory_service") {
                                "https://mi.teploenergo-nn.ru/ldapauthentication/auth/login"
                            } else {
                                "https://mi.teploenergo-nn.ru/defaultauthentication/auth/login"
                            }
                        }

                        loginPresenter.attachView(this)
                        loginPresenter.authorization(url,login, password)

                    }
                }
                else -> {
                    //toaster.showToast(R.string.unknown_requestCode)
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("OrderFragment_onCreate")
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)

        if (!sharedPref.isAuth()) { //sharedPref.getLogin()=="" && sharedPref.getPassword()==""
            doAuthorization()
        } else {
            Timber.d("sharedPref.getLogin()=${sharedPref.getLogin()}")
            Timber.d("MainActivity_orders=${(requireContext() as MainActivity).orders}")
            if ((requireContext() as MainActivity).orders.isEmpty()) {
                orderPresenter.attachView(this)
                orderPresenter.loadOrders()
            }
        }
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
        }
    }

    @SuppressLint("MissingPermission")
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
                    Timber.d("startService_OrderFragment2")
                    otherUtil.writeToFile("Logger_стартуем_сервисы_OrderFragment_UserLocationService_MapkitLocationService")

                    activity?.startService(Intent(this.requireContext(),UserLocationService::class.java))
                    activity?.startService(Intent(this.requireContext(),MapkitLocationService::class.java))
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
        Timber.d("showOrders1")
        orderPresenter.attachView(this)
        //orderPresenter.importData()
        orderPresenter.loadOrders()
    }

    override fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String) {
        sharedPref.saveLogin(stLogin)
        sharedPref.savePassword(stPassword)
        sharedPref.saveAuthFlag()
        requestGPSPermission()
    }

    override fun saveToken(token: String) {
        sharedPref.sptoken=token
        sharedPref.saveToken(token)
    }

    override fun showFailureTextView(failureMessage: String) {
        if (failureMessage.isNotEmpty()) {
            textfailure.text=failureMessage
        }
        textfailure.visibility=View.VISIBLE
        progressBar.visibility=View.INVISIBLE
    }

    override fun showAlertNotInternet() {
        textfailure.visibility=View.VISIBLE
        progressBar.visibility=View.INVISIBLE
        alertNotInternet()
    }

    /**
     * Метод, проверяющий доступность БД
     * @return - true/false
     */
    override fun alertRepeatSync() {
        Timber.d("doSync")
        /*val pb=progressBar
        pb.visibility= View.INVISIBLE*/
        loginPresenter.attachView(this)

        val dbFile = this.requireContext().getDatabasePath("mydatabase.db")
        lateinit var alertDialogRepeatSync: AlertDialog

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
                alertDialogRepeatSync.dismiss()
                loginPresenter.syncDB()
            }

            dialogView.buttonNo.setOnClickListener{
                alertDialogRepeatSync.dismiss()
                showMessageLogin(R.string.auth_ok)
                showOrders()
            }

            builder.setView(dialogView)
            builder.setCancelable(false)
            alertDialogRepeatSync=builder.create()
            alertDialogRepeatSync.show()
            (requireActivity() as MainActivity).alertDialogRepeatSync=alertDialogRepeatSync
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
            showMessageLogin(R.string.working_offline)
            alertDialogNotInternet.dismiss()

            /*textfailure.visibility=View.INVISIBLE
            Navigation.findNavController(root).navigate(R.id.nav_home)*/
        }

        dialogView.buttonInetNo.setOnClickListener{
            alertDialogNotInternet.dismiss()
        }

        builder.setView(dialogView)
        builder.setCancelable(false)
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

    override fun startNotificationService(token: String) {
        // Старутем сервис отслеживания уведомлений
        Timber.d("tokenToWS_OrdeFragment=$token")
        activity?.startService(Intent(activity, NotificationService::class.java).putExtra("Token",token))
    }

    override fun checkMessageId() {
        Timber.d("OrderFragment_checkMessageId")
        (activity as MainActivity).checkMessageId()
    }

    override fun getAllMessage() {
        Timber.d("OF_LoginPresenter_getAllMessage")
        // Получим все уведомления с сервера
        (activity as MainActivity).getAllMessage()

    }

    private fun showFilterOrders(orders: List<Orders>) {
        longInfo("showFilterOrders=${orders}")

        val pb=root.findViewById<ProgressBar>(R.id.progressBar)
        pb.visibility= View.INVISIBLE

        //this.orders=orders
        (activity as MainActivity).filteredOrders=orders


        val ordersRecyclerView = root.findViewById(R.id.orders_recycler_view) as RecyclerView
        ordersRecyclerView.layoutManager = LinearLayoutManager(this.activity)

        Timber.d("filteredOrders=${(activity as MainActivity).filteredOrders}")
        val adapter=OrderListAdapter((activity as MainActivity).filteredOrders,this, this, userLocationReceiver.lastKnownLocation)
        ordersRecyclerView.adapter = adapter

    }

    override fun showOrders(orders: List<Orders>) {
        longInfo("showOrdersVV=${orders}")

        val pb=root.findViewById<ProgressBar>(R.id.progressBar)
        pb.visibility= View.INVISIBLE
        val tf=root.findViewById<TextView>(R.id.textfailure)
        tf.visibility=View.INVISIBLE

        //this.orders=orders
        (activity as MainActivity).orders=orders
        (activity as MainActivity).filteredOrders=orders

        val ordersRecyclerView = root.findViewById(R.id.orders_recycler_view) as RecyclerView
        ordersRecyclerView.layoutManager = LinearLayoutManager(this.activity)
        /*val adapter=
        if ((activity as MainActivity).filteredOrders.isEmpty()) {
            OrderListAdapter(orders,this, this, userLocationNative.userLocation)
        } else {
            OrderListAdapter((activity as MainActivity).filteredOrders,this, this, userLocationNative.userLocation)
        }*/
        Timber.d("filteredOrders=${(activity as MainActivity).filteredOrders}")
        val adapter=OrderListAdapter((activity as MainActivity).filteredOrders,this, this, userLocationReceiver.lastKnownLocation)
        ordersRecyclerView.adapter = adapter

        //По-умолчанию показываем все кроме выполненных и отмененных
        Timber.d("isDefaultFilter=${(activity as MainActivity).isDefaultFilter}")
        if (!(activity as MainActivity).isDefaultFilter) {
            (activity as MainActivity).isDefaultFilter=true // Фильтр срабатывает только при первой загрузке, дальше данные берутся из filteredOrders
            (activity as MainActivity).filterOrderByState("all_without_Done_and_Cancel")
            (activity as MainActivity).filterOrderByGroup()
        }

    }

    //#Android_Studio #длинный_лог
    // Функция для вывода длинных строк в лог. Использовать вместо Timber.d
    private fun longInfo(str: String) {
        if (str.length > 3000) {
            Timber.d(str.substring(0, 3000))
            longInfo(str.substring(3000))
        } else Timber.d(str)
    }

    override fun showMessageOrders(msg: String) {}

    fun filteredOrderByGroup(filterGroupList: List<String>) {
        Timber.d("filteredOrderByGroup")
        val rcv=root.findViewById(R.id.orders_recycler_view) as RecyclerView
        val filteredOrderByGroup=(requireContext() as MainActivity).orders.filter { it.groupOrder in filterGroupList }
        (requireContext() as MainActivity).filteredOrders=filteredOrderByGroup

        val adapter = OrderListAdapter(filteredOrderByGroup,this, this, userLocationReceiver.lastKnownLocation)
        rcv.adapter = adapter

        adapter.ordersFilterList=filteredOrderByGroup
    }

    fun filteredOrderByDate(strDate: String) {
        Timber.d("filteredOrderByDate")
        val rcv=root.findViewById(R.id.orders_recycler_view) as RecyclerView
        val filteredOrderByDate = if (strDate=="all") {
            (requireContext() as MainActivity).orders.filter { it.dateVisit !=null }
        } else {
            (requireContext() as MainActivity).orders.filter { it.dateVisit ==strDate }
        }
        (requireContext() as MainActivity).filteredOrders=filteredOrderByDate

        Timber.d("MainActivity_orders_filteredOrderByDate=${(requireContext() as MainActivity).orders}")

        val adapter = OrderListAdapter(filteredOrderByDate,this, this, userLocationReceiver.lastKnownLocation)
        rcv.adapter = adapter

        adapter.ordersFilterList=filteredOrderByDate
    }

    fun filteredOrderByState(filter: String) {
        Timber.d("filteredOrderByState")
        val rcv=root.findViewById(R.id.orders_recycler_view) as RecyclerView
        val filteredOrderByState: List<Orders>
        if (filter=="all") {
            filteredOrderByState=(requireContext() as MainActivity).orders.filter { it.status !=null }
        } else {
            filteredOrderByState=(requireContext() as MainActivity).orders.filter { it.status !="Выполнена" && it.status !="Отменена" }
            if ((root.context as MainActivity).isDialogFilterStateOrderInit()) {
                val rbWithoutDone=(root.context as MainActivity).dialogFilterStateOrder.findViewById<RadioButton>(R.id.rbWithoutDone)
                rbWithoutDone.isChecked=true
            }
        }
        (requireContext() as MainActivity).filteredOrders=filteredOrderByState


        val adapter = OrderListAdapter(filteredOrderByState,this, this, userLocationReceiver.lastKnownLocation)
        rcv.adapter = adapter

        adapter.ordersFilterList=filteredOrderByState
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
                    401 -> {
                        //toaster.showToast(R.string.unauthorized)
                        (activity as MainActivity).doAuthorization(msgId = R.string.auth_restored)
                    }
                    else -> toaster.showErrorToast("Ошибка! ${throwable.message}")
                }
            }
            is UnknownHostException ->{
                toaster.showErrorToast(R.string.no_address_hostname)
            }
            else -> {
                toaster.showErrorToast("Ошибка! ${throwable.message}")
                progressBar.visibility=View.INVISIBLE
            }
        }

    }

    override fun showFailure(idMsg: Int) {
        showFailureTextView(getString(idMsg))
    }

    override fun registerReceiverMainActivity() {
        (activity as MainActivity).registerReceiver()
    }

    override fun sendMessageUserLogged() {
        val pInfo=requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        val currentVersion= pInfo.versionName
        mainPresenter.sendMessageToAdmin(USER_LOGIN,currentVersion)


    }

    override fun saveAppVersionName() {
        val pInfo=requireContext().packageManager.getPackageInfo(requireContext().packageName,0)
        sharedPref.saveVersionName(pInfo.versionName)
    }

    override fun startFinishWorker() {
        Timber.d("startFinishWorker")
        val date=Calendar.getInstance()
        val calendar = Calendar.getInstance()
        calendar.set(date.get(Calendar.YEAR),date.get(Calendar.MONTH),date.get(Calendar.DATE),FINISH_HOURS,
            FINISH_MINUTES,0)

        Timber.d("duration=$calendar")
        var duration =otherUtil.getDifferenceTime(date.timeInMillis, calendar.timeInMillis)
        // Если зашли после 18.00 приложение будет работать 4 часа
        if (duration<0) {
            duration=240
        }
        Timber.d("duration=$duration")
        otherUtil.writeToFile("Logger_startFinishWorker_$duration")

        val finishAppWorkerRequest: WorkRequest =
            OneTimeWorkRequestBuilder<FinishAppWorker>()
                .addTag("auto_finish")
                .setInitialDelay(duration, TimeUnit.MINUTES)
                .build()
        WorkManager.getInstance(requireContext()).enqueue(finishAppWorkerRequest)
    }

    override fun finishAppDoubler() {
        Timber.d("finishAppDoubler")
        val sp=requireContext().getSharedPreferences(Const.SharedPrefConst.APP_PREFERENCES, Context.MODE_PRIVATE)
        val login=sp.getString(Const.SharedPrefConst.LOGIN, "") ?: ""

        if (login!="") {
            otherUtil.writeToFile("Logger_FINISH_FROM_finishAppDoubler_${Date()}")
            val intent = Intent("EXIT")
            (requireContext() as MainActivity).checkFinish(intent)
        }
    }

    override fun recyclerViewListClicked(v: View?, position: Int) {
        Timber.d("recyclerViewListClicked")
        Timber.d("MainActivity_orders_recyclerViewListClicked=${(requireContext() as MainActivity).orders}")

        val rcv=root.findViewById(R.id.orders_recycler_view) as RecyclerView
        /*Timber.d("notifyItemChanged_1")
        (rcv.adapter as OrderListAdapter).notifyItemChanged(position)
        Timber.d("notifyItemChanged_2")*/

        currentOrder = if (rcv.adapter!=null) {
            //(rcv.adapter as OrderListAdapter).getOrder(position)

            (rcv.adapter as OrderListAdapter).getOrder(position)
        } else {
            Orders(guid="")
        }

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

                    if (rcv.adapter!=null) {
                        (this.requireActivity() as MainActivity).filteredOrders=(rcv.adapter as OrderListAdapter).ordersFilterList
                    } else {
                        (this.requireActivity() as MainActivity).filteredOrders=(this.requireActivity() as MainActivity).orders
                    }

                    (this.requireActivity() as MainActivity).navController.navigate(R.id.nav_slideshow)
                    //(this.requireActivity() as FragmentsContractActivity).setMode()

                }
            }
        }
    }

}