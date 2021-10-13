package ru.bingosoft.teploInspector.ui.mainactivity

import android.Manifest
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.os.PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.yandex.mapkit.geometry.Point
import dagger.android.AndroidInjection
import retrofit2.HttpException
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.statereceivers.CustomNetworkCallback
import ru.bingosoft.teploInspector.statereceivers.DozeReceiver
import ru.bingosoft.teploInspector.statereceivers.ShutdownReceiver
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.ui.login.LoginActivity
import ru.bingosoft.teploInspector.ui.login.LoginPresenter
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.ui.order.OrderFragment
import ru.bingosoft.teploInspector.ui.order.OrderListAdapter
import ru.bingosoft.teploInspector.util.*
import ru.bingosoft.teploInspector.util.Const.MessageCode.REFUSED_PERMISSION
import ru.bingosoft.teploInspector.util.Const.MessageCode.REPEATEDLY_REFUSED
import ru.bingosoft.teploInspector.util.Const.MessageCode.USER_LOGOUT
import ru.bingosoft.teploInspector.util.Const.RequestCodes.AUTH
import ru.bingosoft.teploInspector.util.Const.RequestCodes.PHOTO
import ru.bingosoft.teploInspector.wsnotification.NotificationService
import timber.log.Timber
import java.io.File
import java.net.UnknownHostException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeoutException
import javax.inject.Inject


class MainActivity : AppCompatActivity(), FragmentsContractActivity,
    NavigationView.OnNavigationItemSelectedListener, MainActivityContractView, View.OnClickListener
{

    @Inject
    lateinit var mainPresenter: MainActivityPresenter
    @Inject
    lateinit var loginPresenter: LoginPresenter

    @Inject
    lateinit var toaster: Toaster
    @Inject
    lateinit var sharedPref: SharedPrefSaver
    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var otherUtil: OtherUtil

    @Inject
    lateinit var userLocationReceiver: UserLocationReceiver
    @Inject
    lateinit var updateFromNotificationReceiver: UpdateOrderFromNotificationReceiver

    private val dateAndTime: Calendar =Calendar.getInstance()

    private val shutdownReceiver= ShutdownReceiver()
    private val airplaneModeReceiver=AirplaneModeReceiver()
    private val dozeReceiver= DozeReceiver()


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val defaultNetworkCallback= CustomNetworkCallback()

    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController: NavController

    private var mapPoint: Point=Point(0.0, 0.0)
    private var controlMapId: Int=0
    var photoDir: String=""
    var lastKnownFilenamePhoto=""
    var photoStep: Models.TemplateControl?=null
    lateinit var currentOrder: Orders
    lateinit var techParams: List<TechParams>
    private lateinit var locationManager: LocationManager

    private lateinit var dialogFilterGroupOrder: AlertDialog
    lateinit var dialogFilterStateOrder: AlertDialog
    private lateinit var dialogFilterDateOrder: AlertDialog
    private lateinit var filterView: ConstraintLayout

    //var isMapFragmentShow=false
    //lateinit var orders: List<Orders>
    var orders= listOf<Orders>()
    var isBackPressed=false
    var isSearchView=false

    var filteredOrders: List<Orders> = listOf()
    var isDefaultFilter=false

    private var doubleBackToExitCounter=0


    lateinit var menu: Menu

    private var messageId: Int=0
    var ordersIdsNotSync: MutableList<Long> = mutableListOf()

    var alertDialogRepeatSync: AlertDialog?=null
    var routeIntervalFlag=false
    var registerReceiverFlag=false


    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("MainActivity_onCreate")
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        if (isAirplaneModeOn(this)) {
            Timber.d("Logger_isAirplaneModeOn")
            otherUtil.writeToFile("Logger_isAirplaneModeOn")
        }

        setCustomDefaultUncaughtExceptionHandler()

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        mainPresenter.attachView(this)
        mainPresenter.getAllOrderNotSync()

        requestPermission() // Запросим разрешения
        // Проверим возможно приложение обновили
        if (!checkUpdateApp()) {
            return
        }


        // Авторизуемся сразу при входе в приложение, чтоб уходили GPS данные
        // Авторизуемся так только если есть логин и пароль в настройках
        if (sharedPref.getLogin().isNotEmpty() && sharedPref.getPassword().isNotEmpty() && sharedPref.isAuth()) {
            Timber.d("Первая_авторизация")
            doAuthorization() // Сразу пробуем авторизоваться
        }


        //checkIntent() // Возможно Activity открыта из уведомления

        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) /*&&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)*/) {
            //toaster.showToast("buildAlertMessageNoGps")
            buildAlertMessageNoGps()
        }


        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {
                Timber.d("onDrawerStateChanged")
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                Timber.d("onDrawerSlide")

            }

            override fun onDrawerClosed(drawerView: View) {
                Timber.d("onDrawerClosed")

            }

            override fun onDrawerOpened(drawerView: View) {
                Timber.d("onDrawerOpened")
                invalidateNavigationDrawer()
            }

        })


        val navView: NavigationView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_slideshow,
                R.id.nav_checkup
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setNavigationItemSelectedListener(this)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Timber.d("DestinationChangedListener_${destination}")
            when (destination.id) {
                R.id.nav_home -> {
                    Timber.d("DestinationChangedListener_OrderFragment")
                }
                R.id.nav_checkup -> {
                    Timber.d("DestinationChangedListener_CheckupFragment")
                }
                R.id.nav_slideshow -> {
                    Timber.d("DestinationChangedListener_MapFragment")
                }
                else -> {
                }
            }
        }
        //navView.setupWithNavController(navController) // Переключалка фрагментов по-умолчанию

        val header=navView.getHeaderView(0)
        val imgButtonAuth=header.findViewById<ImageButton>(R.id.imgbAuth)
        imgButtonAuth.setOnClickListener {
            Timber.d("Auth")
            // Запустим активити с авторизацией
            doAuthorization()
            drawerLayout.closeDrawer(GravityCompat.START)
        }

    }

    fun isAirplaneModeOn(context: Context): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
    }

    // #Необрабатываемые_исключения
    // Попытка решить ошибку Fatal Exception: java.util.concurrent.TimeoutException
    // com.yandex.runtime.NativeObject.finalize() timed out after 10 seconds
    // Подробнее см. тут
    // https://stackoverflow.com/questions/24021609/how-to-handle-java-util-concurrent-timeoutexception-android-os-binderproxy-fin
    private fun setCustomDefaultUncaughtExceptionHandler() {
        val defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            if (t.name == "FinalizerWatchdogDaemon" && e is TimeoutException) {
                e.printStackTrace()
            } else {
                defaultUncaughtExceptionHandler?.uncaughtException(t, e)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        otherUtil.writeToFile("Logger_onNewIntent")
        Timber.d("onNewIntent_${intent?.extras}")
        Timber.d("onNewIntent_${intent?.extras?.getBoolean("EXIT", false)}")
        Timber.d("onNewIntent_${intent?.extras?.getInt("messageId")}")
        checkFinish(intent)
        checkIntent() // Возможно Activity открыта из уведомления
    }

    override fun onPause() {
        super.onPause()
        Timber.d("MainActivity_onPause")

    }

    fun checkFinish(intent: Intent?) {
        otherUtil.writeToFile("Logger_checkFinish")
        if (sharedPref.isAuth()) { //sharedPref.getLogin().isNotEmpty() && sharedPref.getPassword().isNotEmpty()
            if (intent?.extras != null) {
                Timber.d("EXIT_${intent.extras!!.getBoolean("EXIT", false)}")
                if (intent.extras!!.getBoolean("EXIT", false)) {
                    finish()
                }
            }
            otherUtil.writeToFile("Logger_action_${intent?.action}")
            if (intent?.action!=null && intent.action =="EXIT") {
                finish()
            }
        } else {
            otherUtil.writeToFile("Logger_checkFinish_login_${sharedPref.getLogin()}_password_${sharedPref.getPassword()}")
            // Проверим логин и пароль после сворачивания приложения
            if (!sharedPref.isAuth()) { //sharedPref.getLogin().isEmpty() || sharedPref.getPassword().isEmpty()
                // Запустим активити с настройками
                val intent1 = Intent(this, LoginActivity::class.java)
                startActivityForResult(intent1, AUTH)
            }
        }

    }

    private fun checkUpdateApp(): Boolean {
        val pInfo=packageManager.getPackageInfo(packageName, 0)
        val currentVersion= pInfo.versionName
        Timber.d("versionName_App$currentVersion")
        val oldVersion=sharedPref.getVersionName()
        Timber.d("oldVersion$oldVersion")
        if (oldVersion.isNotEmpty()) {
            if ( oldVersion != currentVersion) {
                otherUtil.writeToFile("Logger_Версии не совпадают старая_$oldVersion, новая $currentVersion")
                buildAlertClearUserData()
                return false
            }
        }
        return true

    }

    private fun buildAlertClearUserData() {
        lateinit var alertDialog: AlertDialog
        val layoutInflater = LayoutInflater.from(this)
        val dialogView: View =
            layoutInflater.inflate(R.layout.alert_clear_userdata, null, false)

        val builder = AlertDialog.Builder(this)

        dialogView.findViewById<MaterialButton>(R.id.buttonOK).setOnClickListener{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            } else {
                // Код работает, если что, можно использовать его в Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }


            alertDialog.dismiss()
        }

        builder.setView(dialogView)
        builder.setCancelable(false)
        alertDialog=builder.create()
        alertDialog.show()
    }

    private val unauthorizedReceiver=object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("unauthorizedReceiver_onReceive")
            //otherUtil.writeToFile("unauthorizedReceiver_onReceive")
            doAuthorization(msgId = R.string.auth_restored)
        }
    }


    private fun checkIntent() {
        Timber.d("checkIntent")

        messageId=intent.getIntExtra("messageId", 0)
        Timber.d("messageId=$messageId")
        if (messageId!=0) {
            refreshOrderListFromMA()
            mainPresenter.markMessageAsRead(messageId)
        }
        val messageAll=intent.getBooleanExtra("allMessageRead", false)
        Timber.d("messageAll=$messageAll")
        if (messageAll) {
            refreshOrderListFromMA()
            mainPresenter.markAllMessageAsRead()
        }
    }

    override fun refreshOrderListFromMA() {
        val navHostFragment: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        Timber.d("VCVC23_${navHostFragment.childFragmentManager.fragments.size}")
        Timber.d("VCVC23_${navHostFragment.childFragmentManager.fragments}")
        val of = navHostFragment.childFragmentManager.fragments[0] as? OrderFragment
        of?.refreshOrdersList()
    }


    fun isInitCurrentOrder() :Boolean {
        return ::currentOrder.isInitialized
    }

    fun isInitMenu() :Boolean {
        return ::menu.isInitialized
    }


    private fun buildAlertMessageNoGps() {

        lateinit var alertDialog: AlertDialog
        val layoutInflater = LayoutInflater.from(this)
        val dialogView: View =
            layoutInflater.inflate(R.layout.alert_gps_off, null, false)

        val builder = AlertDialog.Builder(this)

        dialogView.findViewById<MaterialButton>(R.id.buttonOK).setOnClickListener{
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            alertDialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.buttonNo).setOnClickListener{
            Timber.d("Сообщение администратору")
            mainPresenter.sendMessageToAdmin(REPEATEDLY_REFUSED)
            alertDialog.dismiss()
        }

        builder.setView(dialogView)
        builder.setCancelable(false)
        alertDialog=builder.create()
        alertDialog.show()
    }

    private fun requestPermission() {
        // Проверим разрешения
        Timber.d("requestPermission")
        if (ContextCompat.checkSelfPermission(this, (Manifest.permission.ACCESS_FINE_LOCATION)) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                (Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, (Manifest.permission.READ_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, (WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        WRITE_EXTERNAL_STORAGE
                    ),
                    Const.RequestCodes.PERMISSION
                )
            }
        } else {
            Timber.d("startService_MainActivity1")
            // Стартуем фоновый сервис для отслеживания пользователя
            // Сервис стартуем сразу (до авторизации), чтоб можно было локацию для фоток получить
            otherUtil.writeToFile("Logger_стартуем_сервисы_MainActivity_UserLocationService_MapkitLocationService")
            startService(Intent(this, UserLocationService::class.java)) // Отслеживаем состояние GPS
            startService(Intent(this, MapkitLocationService::class.java)) // Отслеживаем координаты
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            Timber.d(packageName)
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
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
                    Timber.d("startService_MainActivity2")
                    otherUtil.writeToFile("Logger_стартуем_сервисы_MainActivity_UserLocationService_MapkitLocationService_from_onRequestPermissionsResult")
                    startService(Intent(this, UserLocationService::class.java))
                    startService(Intent(this, MapkitLocationService::class.java))
                } else {
                    // Разрешения не выданы оповестим юзера
                    toaster.showToast(R.string.not_permissions)
                    Timber.d("ОТКАЗАЛСЯ ОТ ГЕОЛОКАЦИИ MainActivity")
                    mainPresenter.sendMessageToAdmin(REFUSED_PERMISSION)
                }
            }
            else -> Timber.d("Неизвестный PERMISSION_REQUEST_CODE")
        }

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu=menu
        menuInflater.inflate(R.menu.main, menu)

        val item = menu.findItem(R.id.menu_buttons)
        item.setActionView(R.layout.filter_count)
        filterView=item.actionView as ConstraintLayout

        val btnFilter=filterView.findViewById<Button>(R.id.btnFilter)
        btnFilter.setOnClickListener(this)

        val dateFilterItem=menu.findItem(R.id.date_filter)
        dateFilterItem?.setOnMenuItemClickListener {
            Timber.d("dateFilterItem")
            Timber.d("Открываем окно с фильтром по дате")

            if (!::dialogFilterDateOrder.isInitialized) {
                val layoutInflater = LayoutInflater.from(this)
                val dialogDateFilterView =
                    layoutInflater.inflate(R.layout.alert_filter_date_order, null, false)

                val builder = AlertDialog.Builder(this)

                val rbAll=dialogDateFilterView.findViewById<RadioButton>(R.id.rbAll)
                rbAll.setOnClickListener(this)
                val rbToday=dialogDateFilterView.findViewById<RadioButton>(R.id.rbToday)
                rbToday.setOnClickListener(this)
                val rbTomorrow=dialogDateFilterView.findViewById<RadioButton>(R.id.rbTomorrow)
                rbTomorrow.setOnClickListener(this)
                val rbDate=dialogDateFilterView.findViewById<RadioButton>(R.id.rbDate)
                rbDate.setOnClickListener(this)

                builder.setView(dialogDateFilterView)
                builder.setCancelable(true)
                dialogFilterDateOrder=builder.create()
                dialogFilterDateOrder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                //Меняем позицию диалога
                dialogFilterDateOrder.window?.setGravity(Gravity.TOP or Gravity.END)

                dialogFilterDateOrder.setOnCancelListener(dialogDateFilterCancelListener)

                dialogFilterDateOrder.show()
                // Установим ширину диалогового окна
                val width=resources.getDimension(R.dimen.dialog_filter_date_width).toInt()
                dialogFilterDateOrder.window?.setLayout(
                    width,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // Отступы диалога от границ экрана
                val wlp=dialogFilterDateOrder.window?.attributes
                wlp?.y=50
                wlp?.x=50
                dialogFilterDateOrder.window?.attributes=wlp
            } else {
                dialogFilterDateOrder.show()
            }

            true
        }

        val statusFilterItem=menu.findItem(R.id.status_filter)
        statusFilterItem?.setOnMenuItemClickListener {
            Timber.d("statusFilterItem")
            Timber.d("Открываем окно с фильтром по статусу")

            if (!::dialogFilterStateOrder.isInitialized) {
                val layoutInflater = LayoutInflater.from(this)
                val dialogStateFilterView =
                    layoutInflater.inflate(R.layout.alert_filter_status_order, null, false)

                val builder = AlertDialog.Builder(this)

                val rbStateAll=dialogStateFilterView.findViewById<RadioButton>(R.id.rbStateAll)
                rbStateAll.setOnClickListener(this)
                val rbWithoutDone=dialogStateFilterView.findViewById<RadioButton>(R.id.rbWithoutDone)
                rbWithoutDone.setOnClickListener(this)
                val rbOnWay=dialogStateFilterView.findViewById<RadioButton>(R.id.rbOnWay)
                rbOnWay.setOnClickListener(this)
                val rbInProgress=dialogStateFilterView.findViewById<RadioButton>(R.id.rbInProgress)
                rbInProgress.setOnClickListener(this)
                val rbOpen=dialogStateFilterView.findViewById<RadioButton>(R.id.rbOpen)
                rbOpen.setOnClickListener(this)


                builder.setView(dialogStateFilterView)
                builder.setCancelable(true)
                dialogFilterStateOrder=builder.create()
                dialogFilterStateOrder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                //Меняем позицию диалога
                dialogFilterStateOrder.window?.setGravity(Gravity.TOP or Gravity.END)

                dialogFilterStateOrder.setOnCancelListener(dialogStateFilterCancelListener)

                dialogFilterStateOrder.show()
                // Установим ширину диалогового окна
                val width=resources.getDimension(R.dimen.dialog_filter_date_width).toInt()
                dialogFilterStateOrder.window?.setLayout(
                    width,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // Отступы диалога от границ экрана
                val wlp=dialogFilterStateOrder.window?.attributes
                wlp?.y=50
                wlp?.x=50
                dialogFilterStateOrder.window?.attributes=wlp
            } else {
                dialogFilterStateOrder.show()
            }


            true
        }

        /*val btnDateFilter=filterView.findViewById<Button>(R.id.)
        btnFilter.setOnClickListener(this)*/

        val searchItem=menu.findItem(R.id.action_search)
        if (searchItem!=null) {
            val searchView=searchItem.actionView as SearchView
            searchView.setPadding(0, 0, -30, 0)

            val searchPlate = searchView.findViewById(androidx.appcompat.R.id.search_src_text) as EditText
            searchPlate.hint = getString(R.string.search_by_address)

            searchPlate.setOnEditorActionListener { _, actionId, _ ->
                Timber.d("actionId=$actionId")
                false
            }


            searchView.setOnCloseListener {
                isSearchView=false
                false
            }


            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Timber.d("onQueryTextSubmit")
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    Timber.d(newText)

                    //isSearchView = true
                    //#SearchView_close
                    if (newText == "") {
                        Timber.d("Закроем")
                        searchView.isIconified = true
                    }

                    val currentFragmentClassName =
                        (navController.currentDestination as FragmentNavigator.Destination).className
                    Timber.d("CXCX=$currentFragmentClassName")

                    if (currentFragmentClassName == getString(R.string.order_fragment_className)) {
                        val rcv = findViewById<RecyclerView>(R.id.orders_recycler_view)
                        if (rcv.adapter != null) {
                            (rcv.adapter as OrderListAdapter).filter.filter(newText)
                        } else {
                            //toaster.showErrorToast(R.string.rcv_adapter_isnull)
                            // Обновляем список заявок
                            findViewById<Button>(R.id.btnList).performClick()
                        }


                    } else {
                        Timber.d("Включена карта11 $newText")

                        val filteredList = orders.filter {
                            it.address != null && it.address!!.contains(
                                newText!!,
                                true
                            )
                        }

                        filteredOrders = filteredList

                        Timber.d("Отфильтровано=${filteredList.size}")
                        val currentNavHost =
                            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

                        val mf = currentNavHost?.childFragmentManager?.fragments?.filterNotNull()
                            ?.find { it.javaClass.name == currentFragmentClassName } as MapFragment
                        mf.showMarkers(filteredList)
                    }

                    return true
                }

            })

            val searchManager=getSystemService(Context.SEARCH_SERVICE) as SearchManager
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PHOTO -> {
                    Timber.d("REQUEST_CODE_PHOTO")
                    setPhotoResult()

                }
                AUTH -> {
                    Timber.d("Авторизуемся_повторно")
                    otherUtil.writeToFile("Авторизуемся_повторно")

                    val login =
                        if (data?.getStringExtra("login") == null) "" else data.getStringExtra("login")
                    val password =
                        if (data?.getStringExtra("password") == null) "" else data.getStringExtra(
                            "password"
                        )
                    val url =
                        if (data?.getStringExtra("url") == null) "" else data.getStringExtra("url")
                    val enterType =
                        if (data?.getStringExtra("enter_type") == null) "" else data.getStringExtra(
                            "enter_type"
                        )

                    val dataAuth = Models.RepeatAuthData(
                        login, password, url, enterType
                    )


                    val navHostFragment: NavHostFragment =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val of = navHostFragment.childFragmentManager.fragments[0] as? OrderFragment
                    of?.doAuthorization(data = dataAuth)
                    if (of == null) {
                        otherUtil.writeToFile("Logger_MainActivity_OrderFragment == null")
                        doAuthorization(data = dataAuth)
                    }


                }
                else -> {
                    Timber.d("Неизвестный requestCode")
                }
            }
        }
    }

    fun doAuthorization(msgId: Int = R.string.auth_ok, data: Models.RepeatAuthData? = null) {
        Timber.d("doAuthorization_MA")
        // Получим логин и пароль из настроек
        val sharedPref = this.getSharedPreferences(
            Const.SharedPrefConst.APP_PREFERENCES,
            Context.MODE_PRIVATE
        )
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

                mainPresenter.authorization(url, login, password, msgId) // Проверим есть ли авторизация
            } else {
                Timber.d("логин/пароль=ОТСУТСТВУЮТ_в_настройках")
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
                    mainPresenter.authorization(url, data.login, data.password, msgId) // Проверим есть ли авторизация

                } else {
                    // Запустим активити с настройками
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivityForResult(intent, AUTH)
                }

            }
        } else {
            // Запустим активити с настройками
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, AUTH)
        }

    }


    override fun onDestroy() {
        Timber.d("MainAct_destroy")
        otherUtil.writeToFile("Logger_MainAct_destroy_${Date()}")
        if (alertDialogRepeatSync?.isShowing == true) {
            alertDialogRepeatSync?.dismiss()
        }

        // Отправляем сообщение о выходе, только если был совершен вход
        if (sharedPref.isAuth()) { //sharedPref.getLogin().isNotEmpty() && sharedPref.getPassword().isNotEmpty()
            mainPresenter.sendMessageToAdmin(USER_LOGOUT)
        }
        sharedPref.clearAuthData() // Очистим информацию об авторизации


        stopService(Intent(this, UserLocationService::class.java))
        stopService(Intent(this, MapkitLocationService::class.java))
        stopService(Intent(this, NotificationService::class.java))

        if (this::mainPresenter.isInitialized) {
            mainPresenter.onDestroy()
        }

        try {
            if (registerReceiverFlag) {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(userLocationReceiver)
                LocalBroadcastManager.getInstance(this).unregisterReceiver(unauthorizedReceiver)
                unregisterReceiver(shutdownReceiver)
                unregisterReceiver(airplaneModeReceiver)
                unregisterReceiver(dozeReceiver)
                unregisterCustomNetworkCallback()
                unregisterReceiver(updateFromNotificationReceiver)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        userLocationReceiver.onDestroy()

        super.onDestroy()

    }

    private fun unregisterCustomNetworkCallback() {
        val connectivityManager  = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.unregisterNetworkCallback(defaultNetworkCallback)
        }
    }


    override fun onBackPressed() {
        isBackPressed=true
        Timber.d("onBackPressed")
        Timber.d("backStackEntryCount=${supportFragmentManager.backStackEntryCount}")
        Timber.d("fragments.size=${supportFragmentManager.fragments.size}")
        Timber.d("MainActivity_orders_onBackPressed=${orders}")



        val currentFragmentClassName = (navController.currentDestination as FragmentNavigator.Destination).className
        Timber.d("currentFragmentClassName=$currentFragmentClassName")
        /*if (supportFragmentManager.fragments.size>1) {
            if (currentFragmentClassName==getString(R.string.order_fragment_className)) {
                Timber.d("navController_navigate_nav_home")
                navController.navigate(R.id.nav_home)
            }
        }*/

        if (currentFragmentClassName==getString(R.string.map_fragment_className)) {
            Timber.d("уходим_с_карты")
            doubleBackToExitCounter=0
            super.onBackPressed()
            //return
        }

        if (currentFragmentClassName==getString(R.string.checkup_fragment_className)) {
            Timber.d("navController_navigate_nav_1home")
            doubleBackToExitCounter=0
            super.onBackPressed()
            //return
        }


        val newCurrentFragmentClassName = (navController.currentDestination as FragmentNavigator.Destination).className
        Timber.d("VVV_$newCurrentFragmentClassName}")
        // Если попали на экран Заявки
        if (newCurrentFragmentClassName==getString(R.string.order_fragment_className)){ //(supportFragmentManager.backStackEntryCount==0)
            Timber.d("onBackPressed_Заявки")

            // Сбросим текущую заявку, только когда переходим к Списку
            currentOrder= Orders(guid = "")
            photoDir=""
            photoStep=null

            supportActionBar?.setTitle(R.string.menu_orders)

            // Выделим кнопку Список
            findViewById<Button>(R.id.btnList)?.isEnabled=false
            findViewById<Button>(R.id.btnMap)?.isEnabled=true

            //navController.navigate(R.id.nav_home)
            if (isBackPressed) {
                isBackPressed=false
            }
            if (isSearchView) {
                isSearchView=false
            }


            doubleBackToExitCounter += 1
            Timber.d("doubleBackToExitCounter=$doubleBackToExitCounter")
            if (doubleBackToExitCounter>1) {
                alertExit()
            }


        }
    }

    private fun alertExit() {
        lateinit var alertDialog: AlertDialog
        val layoutInflater = LayoutInflater.from(this)
        val dialogView: View =
            layoutInflater.inflate(R.layout.alert_exit, null, false)

        val builder = AlertDialog.Builder(this)

        dialogView.findViewById<MaterialButton>(R.id.buttonOK).setOnClickListener{
            Timber.d("alertExit_buttonOK")
            otherUtil.writeToFile("Logger_Нажата_кнопка_Да_в_диалоге_выхода")
            alertDialog.dismiss()
            finish()
        }

        dialogView.findViewById<MaterialButton>(R.id.buttonNo).setOnClickListener{
            otherUtil.writeToFile("Logger_Нажата_кнопка_Нет_в_диалоге_выхода")
            alertDialog.dismiss()
        }

        builder.setView(dialogView)
        builder.setCancelable(true)
        alertDialog=builder.create()
        alertDialog.show()
    }


    override fun setCoordinates(point: Point, controlId: Int) {
        Timber.d("setCoordinates from Activity")
        mapPoint=point
        this.controlMapId=controlId
    }

    /*override fun setMode(isMap: Boolean) {
        Timber.d("setMode=$isMap")
        isMapFragmentShow=isMap
    }*/



    override fun showMarkers(orders: List<Orders>) {
        Timber.d("MainAct_showMarkers")

        // Фильтруем заявки
        val navHostFragment=supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val childFragment=navHostFragment?.childFragmentManager?.fragments?.get(0)
        if (childFragment is OrderFragment) {
            val rcv=findViewById<RecyclerView>(R.id.orders_recycler_view)
            if (rcv!=null) {
                rcv.adapter?.notifyDataSetChanged()
            }
        }
        if (childFragment is MapFragment){
            Timber.d("Включена карта")
            val rcv=findViewById<RecyclerView>(R.id.orders_recycler_view)
            if (rcv.adapter!=null) {
                childFragment.showMarkers((rcv.adapter as OrderListAdapter).ordersFilterList)
            } else {
                toaster.showErrorToast(R.string.rcv_adapter_isnull)
            }

        }
    }

    private fun setPhotoResult() {
        var photoLocation:Location?=Location(LocationManager.GPS_PROVIDER)
        try {
            photoLocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) {
            Timber.d("Не удается запросить обновление местоположения, игнорировать ${e.printStackTrace()}")
        } catch (e: IllegalArgumentException) {
            Timber.d("GPS провайдер не существует ${e.printStackTrace()}")
        }
        Timber.d("locationPhoto=${photoLocation?.latitude}_${photoLocation?.longitude}")

        Timber.d("lastKnownFilenamePhoto=${lastKnownFilenamePhoto}")
        otherUtil.saveExifLocation(lastKnownFilenamePhoto, photoLocation)

        val navHostFragment=supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val childFragment=navHostFragment?.childFragmentManager?.fragments?.get(0)
        if (childFragment is CheckupFragment) {
            childFragment.setPhotoResult(photoStep?.results_id, photoDir)
        }


        Timber.d("setPhotoResult_from_Activity")
        photoStep?.answered = true
        photoStep?.resvalue=photoDir
        photoStep?.datetime= Date().time

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)

        Timber.d("onNavigationItemSelected")

        return when (item.itemId) {
            R.id.nav_home -> {
                Timber.d("nav_home")
                navController.navigate(R.id.nav_home)
                //setMode(false) //Включены заявки
                true
            }
            R.id.nav_slideshow -> {
                navController.navigate(R.id.nav_slideshow)
                //setMode() //Включена карта
                true
            }
            R.id.repeat_sync -> {
                Timber.d("Повторная_синхронизация")
                Timber.d("ordersIdsNotSync_size=${ordersIdsNotSync.size}")
                mainPresenter.repeatSendData()

                true
            }
            R.id.exit -> {
                Timber.d("Выход")
                alertExit()
                true
            }
            else -> {
                false
            }
        }
    }

    override fun showMainActivityMsg(resID: Int) {
        Timber.d("showMainActivityMsg")
        toaster.showToast(resID)
    }

    override fun showMainActivityMsg(msg: String) {
        Timber.d("showMainActivityMsg")
        toaster.showToast(msg)
    }

    override fun dataSyncOK(idOrder: Long?) {
        Timber.d("dataSyncOK")
        mainPresenter.updData()
        if (idOrder!=null) {
            ordersIdsNotSync.remove(idOrder)
        }

    }

    override fun dataNotSync(idOrder: Long, throwable: Throwable) {
        Timber.d("dataNotSync")
        if (!ordersIdsNotSync.contains(idOrder)) {
            ordersIdsNotSync.add(idOrder)
        }
        if (throwable is ThrowHelper) {
            mainPresenter.isCheckupWithResult("${throwable.message}")
        } else {
            errorReceived(throwable)
        }
        mainPresenter.updData(sync = 2)
    }

     /*override fun updDataOK() {
         Timber.d("updDataOK")
         //Передаем маршрут пользователя
         mainPresenter.sendUserRoute()
     }*/

    override fun filesSend(countFiles: Int, indexCurrentFile: Int) {
        if (countFiles==indexCurrentFile) {
            mainPresenter.updData()
            //toaster.showToast(R.string.data_and_files_sends) // Убрал т.к. есть задержка с появлением сообщения из-за передачи файлов
        }
    }

    override fun renameSyncedFiles(files: Array<File>?) {
        Timber.d("renameSyncedFiles ${files?.size}")
        files?.forEach {
            it.renameTo(File(it.parentFile, "${it.nameWithoutExtension}_synced.${it.extension}"))
        }
    }

     override fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String) {
         Timber.d("saveLoginPasswordToSharedPreference")
         sharedPref.saveLogin(stLogin)
         sharedPref.savePassword(stPassword)
         sharedPref.saveAuthFlag()
     }

     override fun saveToken(token: String) {
         Timber.d("saveToken")
         sharedPref.sptoken=token
         sharedPref.saveToken(token)

     }

     override fun saveInfoUserToSharedPreference(user: Models.User) {
         sharedPref.saveUser(user)
     }

     override fun startNotificationService(token: String) {
         // Старутем сервис отслеживания уведомлений
         Timber.d("tokenToWS=$token")
         startService(Intent(this, NotificationService::class.java).putExtra("Token", token))
     }

     override fun checkMessageId() {
         if (messageId!=0) {
             // Отправим запрос на сервер, что уведомление прочитано
             mainPresenter.markMessageAsRead(messageId)
         }
     }

     override fun setEmptyMessageId() {
         Timber.d("setEmptyMessageId")
         messageId=0
     }

     override fun getAllMessage() {
         mainPresenter.getAllMessage()
     }

     override fun showUnreadNotification(listNotification: List<Models.Notification>) {
         Timber.d("showUnreadNotification")
         var notificationContent=""
         listNotification.forEach {
             notificationContent += "\n ${it.content}"
         }

         Timber.d("notificationContent=$notificationContent")
         val handler = Handler()
         handler.postDelayed({
             createNotification(notificationContent, listNotification.size)
         }, 8000)
     }

     private fun createNotification(notificationContent: String, count: Int) {
         Timber.d("createNotification")
         val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             val notificationChannel = NotificationChannel(
                 Const.WebSocketConst.NOTIFICATION_CHANNEL_ID,
                 "Уведомления о получении заявок",
                 NotificationManager.IMPORTANCE_HIGH
             )

             val audioAttributes = AudioAttributes.Builder()
                 .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                 .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                 .build()

             notificationChannel.enableLights(true)
             notificationChannel.lightColor = Color.RED
             notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
             notificationChannel.setSound(
                 RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                 audioAttributes
             )
             notificationChannel.enableVibration(true)
             notificationManager.createNotificationChannel(notificationChannel)
         }

         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             val channel = notificationManager.getNotificationChannel(Const.WebSocketConst.NOTIFICATION_CHANNEL_ID)
             channel.canBypassDnd()
         }

         val notificationBuilder = NotificationCompat.Builder(
             this,
             Const.WebSocketConst.NOTIFICATION_CHANNEL_ID
         )

         val resultIntent= Intent(this, MainActivity::class.java).putExtra(
             "allMessageRead", true
         )
         /*val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
             addNextIntentWithParentStack(resultIntent)
             getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
         }*/

         //подробнее тут https://stackoverflow.com/questions/28258404/singletask-and-singleinstance-not-respected-when-using-pendingintent
         //+ переустановка приложения на эмуляторе
         val resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT)

         val customNotification=notificationBuilder.setAutoCancel(true)
             .setContentTitle(getString(R.string.notification_title, count))
             //.setContentText(notificationContent)
             .setStyle(
                 NotificationCompat.BigTextStyle()
                     .bigText(notificationContent)
             )
             .setContentIntent(resultPendingIntent)
             .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
             .setDefaults(Notification.DEFAULT_ALL)
             .setWhen(System.currentTimeMillis())
             .setSmallIcon(R.drawable.ic_new_notification)
             .setAutoCancel(true)
             .build()

         notificationManager.notify(1001, customNotification)
     }

     override fun setIdsOrdersNotSync(list: List<Long>) {
         ordersIdsNotSync.clear()
         ordersIdsNotSync.addAll(list)

     }


     override fun errorReceived(throwable: Throwable) {
        Timber.d("MainACT_errorReceived_$throwable")
        when (throwable) {
            is HttpException -> {
                Timber.d("throwable.code()=${throwable.code()}")
                when (throwable.code()) {
                    401 -> {
                        //toaster.showToast(R.string.unauthorized)
                        doAuthorization(msgId = R.string.auth_restored)
                    }
                    else -> toaster.showErrorToast("Ошибка! ${throwable.message}")
                }
            }
            is UnknownHostException -> {
                toaster.showErrorToast(R.string.no_address_hostname)
            }
            else -> {
                toaster.showErrorToast("Ошибка! ${throwable.message}")
            }
        }
    }

    override fun refreshRecyclerView() {
        ordersIdsNotSync.clear()
        val rv=findViewById<RecyclerView>(R.id.orders_recycler_view)
        Timber.d("$rv")
        if (rv!=null) {
            Timber.d("notifyDataSetChanged")
            rv.adapter?.notifyDataSetChanged()
        }
    }

     override fun registerReceiver() {
         Timber.d("registerReceiver")
         // Регистрируем широковещательный слушатель для получения данных от фонового сервиса MapkitLocationService
         LocalBroadcastManager.getInstance(this).registerReceiver(
             userLocationReceiver, IntentFilter(
                 "userLocationUpdates"
             )
         )
         LocalBroadcastManager.getInstance(this).registerReceiver(
             unauthorizedReceiver, IntentFilter(
                 "unauthorized"
             )
         )


         registerReceiver(shutdownReceiver, IntentFilter(Intent.ACTION_SHUTDOWN))
         registerReceiver(airplaneModeReceiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED))
         registerReceiver(dozeReceiver, IntentFilter(ACTION_DEVICE_IDLE_MODE_CHANGED))
         registerCustomDefaultNetworkCallback()
         updateFromNotificationReceiver.setMainActivity(this)
         LocalBroadcastManager.getInstance(this).registerReceiver(updateFromNotificationReceiver,
             IntentFilter("updateFromNotification")
         )

         registerReceiverFlag=true

     }

    fun registerCustomDefaultNetworkCallback() {
        val connectivityManager  = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            defaultNetworkCallback.otherUtil=otherUtil
            connectivityManager.registerDefaultNetworkCallback(defaultNetworkCallback)
        }
    }


     override fun enabledSaveButton() {
         findViewById<MaterialButton>(R.id.mbSaveCheckup).isEnabled=true
     }

     override fun repeatSync() {
         val navHostFragment: NavHostFragment =
             supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
         val of = navHostFragment.childFragmentManager.fragments[0] as? OrderFragment
         of?.alertRepeatSync()
     }

    override fun sendRoute() {
        // Из MainActivity стартуем если ранее еще не стартовали из LoginPresenter
        Timber.d("routeIntervalFlag_$routeIntervalFlag")
        otherUtil.writeToFile("Logger_routeIntervalFlag_$routeIntervalFlag")
        if (!routeIntervalFlag) {
            mainPresenter.sendRoute()
        }
    }

    override fun clearAuthData() {
         Timber.d("clearAuthData_MainActivity")
         sharedPref.clearAuthData()
     }

    fun invalidateNavigationDrawer() {
        Timber.d("invalidateNavigationDrawer")
        val user=sharedPref.getUser()
         Timber.d("user_$user")

        val navView: NavigationView = findViewById(R.id.nav_view)

        val headerLayout=navView.getHeaderView(0)

        if (user.photoUrl!="") {
            val ivAvatar: ImageView =
                headerLayout.findViewById(R.id.imageAvatar)

            val glideUrl = GlideUrl(
                "${BuildConfig.urlServer}/${user.photoUrl}", LazyHeaders.Builder()
                    .build()
            )

            Glide
                .with(this)
                .load(glideUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(ivAvatar)
        }

        if (user.fullname!="") {
            val tvName: TextView = headerLayout.findViewById(R.id.fullname)
            tvName.text = user.fullname
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnFilter -> {
                Timber.d("Открываем окно с фильтром по Группе")
                Timber.d("dialogFilterGroupOrder")

                if (!::dialogFilterGroupOrder.isInitialized) {
                    val layoutInflater = LayoutInflater.from(this)
                    val dialogView =
                        layoutInflater.inflate(R.layout.alert_filter_order, null, false)

                    val builder = AlertDialog.Builder(this)

                    builder.setView(dialogView)
                    builder.setCancelable(true)
                    dialogFilterGroupOrder = builder.create()
                    dialogFilterGroupOrder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                    //Меняем позицию диалога
                    dialogFilterGroupOrder.window?.setGravity(Gravity.TOP or Gravity.END)

                    dialogFilterGroupOrder.setOnCancelListener(dialogCancelListener)

                    dialogFilterGroupOrder.show()
                    // Установим ширину диалогового окна
                    val width = resources.getDimension(R.dimen.dialog_filter_width).toInt()
                    dialogFilterGroupOrder.window?.setLayout(
                        width,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    // Отступы диалога от границ экрана
                    val wlp = dialogFilterGroupOrder.window?.attributes
                    wlp?.y = 50
                    wlp?.x = 50
                    dialogFilterGroupOrder.window?.attributes = wlp

                    setCountFiltersGroup()
                } else {
                    dialogFilterGroupOrder.show()
                }

            }
            R.id.rbAll -> {
                Timber.d("Фильтр_по_дате_Все")
                filteredOrders = orders
                Timber.d("orders=$orders")
                filterOrderByDate("all")
                if (::dialogFilterDateOrder.isInitialized) {
                    dialogFilterDateOrder.dismiss()
                }
            }
            R.id.rbToday -> {
                Timber.d("Фильтр_по_дате_Сегодня")

                // Текущее время
                val currentDate = Date()
                val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateText: String = dateFormat.format(currentDate)
                Timber.d("dateText=$dateText")
                filterOrderByDate(dateText)
                if (::dialogFilterDateOrder.isInitialized) {
                    dialogFilterDateOrder.dismiss()
                }

            }
            R.id.rbTomorrow -> {
                Timber.d("Фильтр_по_дате_Завтра")
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowDate = calendar.time

                val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateText: String = dateFormat.format(tomorrowDate)
                Timber.d("dateText=$dateText")
                filterOrderByDate(dateText)
                if (::dialogFilterDateOrder.isInitialized) {
                    dialogFilterDateOrder.dismiss()
                }
            }
            R.id.rbDate -> {
                Timber.d("Фильтр_по_дате_Дата")
                Timber.d("MainActivity_orders_filteredOrderByDate=${orders}")
                val date = Calendar.getInstance()
                val dateListener =
                    DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                        date.set(Calendar.YEAR, year)
                        date.set(Calendar.MONTH, month)
                        date.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                        val dateFormat: DateFormat = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        )
                        val dateText: String = dateFormat.format(date.time)
                        Timber.d("dateText=$dateText")
                        filterOrderByDate(dateText)
                        if (::dialogFilterDateOrder.isInitialized) {
                            dialogFilterDateOrder.dismiss()
                        }
                    }

                DatePickerDialog(
                    this,
                    dateListener,
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH),
                    date.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.rbStateAll -> {
                Timber.d("Фильтр_по_Статусу_Все")
                filteredOrders = orders
                filterOrderByState("all")
                if (::dialogFilterStateOrder.isInitialized) {
                    dialogFilterStateOrder.dismiss()
                }
            }
            R.id.rbWithoutDone -> {
                Timber.d("Фильтр_по_Статусу_Кроме_Выполненных_и_Отмененных")
                filterOrderByState("all_without_Done_and_Cancel")
                if (::dialogFilterStateOrder.isInitialized) {
                    dialogFilterStateOrder.dismiss()
                }
            }
            R.id.rbOnWay -> {
                Timber.d("Фильтр_по_Статусу_В_пути")
                filterOrderByState("on_way")
                if (::dialogFilterStateOrder.isInitialized) {
                    dialogFilterStateOrder.dismiss()
                }
            }
            R.id.rbInProgress -> {
                Timber.d("Фильтр_по_Статусу_В_работе")
                filterOrderByState("in_progress")
                if (::dialogFilterStateOrder.isInitialized) {
                    dialogFilterStateOrder.dismiss()
                }
            }
            R.id.rbOpen -> {
                Timber.d("Фильтр_по_Статусу_Открыты")
                filterOrderByState("open")
                if (::dialogFilterStateOrder.isInitialized) {
                    dialogFilterStateOrder.dismiss()
                }
            }
        }
    }

    private fun setCountFiltersGroup() {
        Timber.d("setCountFiltersGroup")

        if (orders.isNotEmpty())  { //(::orders.isInitialized)
            dialogFilterGroupOrder.findViewById<TextView>(R.id.countType1).text=orders.filter { it.groupOrder== this.getString(
                R.string.orderType1
            ).lowercase(
                Locale.ROOT
            )
            }.size.toString()
            dialogFilterGroupOrder.findViewById<TextView>(R.id.countType2).text=orders.filter { it.groupOrder== this.getString(
                R.string.orderType2
            ).lowercase(
                Locale.ROOT
            )
            }.size.toString()
            dialogFilterGroupOrder.findViewById<TextView>(R.id.countType3).text=orders.filter { it.groupOrder== this.getString(
                R.string.orderType3
            ).lowercase(
                Locale.ROOT
            )
            }.size.toString()
            dialogFilterGroupOrder.findViewById<TextView>(R.id.countType4).text=orders.filter { it.groupOrder== this.getString(
                R.string.orderType4
            ).lowercase(
                Locale.ROOT
            )
            }.size.toString()
        }

    }

    private val dialogCancelListener= DialogInterface.OnCancelListener {
        Timber.d("setOnCancelListener")
        // Считаем сколько фильтров поставили
        val cb1=dialogFilterGroupOrder.findViewById<CheckBox>(R.id.cbType1)
        val cb2=dialogFilterGroupOrder.findViewById<CheckBox>(R.id.cbType2)
        val cb3=dialogFilterGroupOrder.findViewById<CheckBox>(R.id.cbType3)
        val cb4=dialogFilterGroupOrder.findViewById<CheckBox>(R.id.cbType4)

        val filterGroupList= mutableListOf<String>()
        var filterCount=0
        if (cb1.isChecked) {
            filterCount+=1
            filterGroupList.add(getString(R.string.orderType1).lowercase(Locale.ROOT))
        }
        if (cb2.isChecked) {
            filterCount+=1
            filterGroupList.add(getString(R.string.orderType2).lowercase(Locale.ROOT))
        }
        if (cb3.isChecked) {
            filterCount+=1
            filterGroupList.add(getString(R.string.orderType3).lowercase(Locale.ROOT))
        }
        if (cb4.isChecked) {
            filterCount+=1
            filterGroupList.add(getString(R.string.orderType4).lowercase(Locale.ROOT))
        }

        Timber.d("filterCount=$filterCount")

        filterView.findViewById<TextView>(R.id.badge_count).text=filterCount.toString()
        if (filterCount==4) {
            filteredOrders=orders
        }

        // Фильтруем заявки
        val navHostFragment=supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val childFragment=navHostFragment?.childFragmentManager?.fragments?.get(0)
        Timber.d("childFragment=$childFragment")
        if (childFragment is OrderFragment) {
            childFragment.filteredOrderByGroup(filterGroupList)
        }
        if (childFragment is MapFragment){
            Timber.d("Включена карта")
            val filteredOrderByGroup=orders.filter { it.groupOrder in filterGroupList }
            filteredOrders=filteredOrderByGroup

            childFragment.showMarkers(filteredOrderByGroup)
        }
    }

     private val dialogDateFilterCancelListener= DialogInterface.OnCancelListener { Timber.d("dialogDateFilterCancelListener") }

     private val dialogStateFilterCancelListener= DialogInterface.OnCancelListener { Timber.d("dialogStateFilterCancelListener") }

     private fun filterOrderByDate(dateText: String) {
         // Фильтруем заявки
         val navHostFragment=supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
         val childFragment=navHostFragment?.childFragmentManager?.fragments?.get(0)
         if (childFragment is OrderFragment) {
             childFragment.filteredOrderByDate(dateText)
         }
         if (childFragment is MapFragment){
             Timber.d("Включена карта")
             val filteredOrderByDate: List<Orders> = if (dateText=="all") {
                 orders.filter { it.dateVisit !=null }
             } else {
                 orders.filter { it.dateVisit ==dateText }
             }

             filteredOrders=filteredOrderByDate

             childFragment.showMarkers(filteredOrderByDate)
         }
     }

     fun filterOrderByState(filter: String) {
         Timber.d("filterOrderByState")
         // Фильтруем заявки
         val navHostFragment=supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
         val childFragment=navHostFragment?.childFragmentManager?.fragments?.get(0)
         if (childFragment is OrderFragment) {
             childFragment.filteredOrderByState(filter)
         }
         if (childFragment is MapFragment){
             Timber.d("Включена карта")
             var filteredOrderByState: List<Orders> = listOf()
             when (filter) {
                 "all" -> {
                     filteredOrderByState = orders.filter { it.status != null }
                 }
                 "all_without_Done_and_Cancel" -> {
                     filteredOrderByState =
                         orders.filter { it.status != "Выполнена" && it.status != "Отменена" }
                 }
                 "on_way" -> {
                     filteredOrderByState = orders.filter { it.status == "В пути" }
                 }
                 "in_progress" -> {
                     filteredOrderByState = orders.filter { it.status == "В работе" }
                 }
                 "open" -> {
                     filteredOrderByState = orders.filter { it.status == "Открыта" }
                 }
             }

             /*val filteredOrderByState: List<Orders> = if (filter=="all") {
                 orders.filter { it.status !=null }
             } else {
                 orders.filter { it.status !="Выполнена" && it.status !="Отменена" }
             }*/

             filteredOrders=filteredOrderByState

             childFragment.showMarkers(filteredOrderByState)
         }
     }

     fun filterOrderByGroup() {
         if (this::dialogFilterGroupOrder.isInitialized) {
             dialogCancelListener.onCancel(dialogFilterGroupOrder)
         }
     }

     fun isDialogFilterStateOrderInit(): Boolean {
         return ::dialogFilterStateOrder.isInitialized
     }


     fun showDateTimeDialog(id: Int, tv: TextView) {
         Timber.d("showDialog")
         //Если потребуется вводить дату вручную, нужно поставить флаг focusableInTouchMode в XML
         if (id== Const.Dialog.DIALOG_DATE) {
             val dateListener =
                 DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                     dateAndTime.set(Calendar.YEAR, year)
                     dateAndTime.set(Calendar.MONTH, month)
                     dateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                     tv.text = SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU")).format(dateAndTime.time)
                 }

             DatePickerDialog(
                 this,
                 dateListener,
                 dateAndTime.get(Calendar.YEAR),
                 dateAndTime.get(Calendar.MONTH),
                 dateAndTime.get(Calendar.DAY_OF_MONTH)
             ).show()
         } else {
             val timeListener =
                 TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                     dateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                     dateAndTime.set(Calendar.MINUTE, minute)
                     tv.text = SimpleDateFormat("HH:mm", Locale("ru", "RU")).format(dateAndTime.time)
                 }

             TimePickerDialog(
                 this,
                 timeListener,
                 dateAndTime.get(Calendar.HOUR_OF_DAY),
                 dateAndTime.get(Calendar.MINUTE), true
             ).show()
         }
     }

}
