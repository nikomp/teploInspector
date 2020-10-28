package ru.bingosoft.teploInspector.ui.mainactivity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.yandex.mapkit.geometry.Point
import dagger.android.AndroidInjection
import retrofit2.HttpException
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.login.LoginActivity
import ru.bingosoft.teploInspector.ui.login.LoginPresenter
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.ui.order.OrderFragment
import ru.bingosoft.teploInspector.ui.order.OrderListAdapter
import ru.bingosoft.teploInspector.util.*
import ru.bingosoft.teploInspector.util.Const.MessageCode.REFUSED_PERMISSION
import ru.bingosoft.teploInspector.util.Const.MessageCode.REPEATEDLY_REFUSED
import ru.bingosoft.teploInspector.util.Const.RequestCodes.AUTH
import ru.bingosoft.teploInspector.util.Const.RequestCodes.PHOTO
import timber.log.Timber
import java.net.UnknownHostException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
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
    lateinit var userLocationReceiver: UserLocationReceiver

    private lateinit var appBarConfiguration: AppBarConfiguration
    lateinit var navController: NavController

    private var mapPoint: Point=Point(0.0,0.0)
    private var controlMapId: Int=0
    var photoDir: String=""
    var lastKnownFilenamePhoto=""
    var photoStep: Models.TemplateControl?=null
    lateinit var currentOrder: Orders
    lateinit var techParams: List<TechParams>
    private lateinit var locationManager: LocationManager
    lateinit var dialogView: View
    lateinit var dialogDateFilterView: View
    lateinit var filterView: ConstraintLayout

    var isMapFragmentShow=false
    lateinit var orders: List<Orders>
    var isBackPressed=false
    var isSearchView=false

    var filteredOrders: List<Orders> = listOf()

    //var doubleBackToExitPressedOnce: Boolean=false
    private var doubleBackToExitCounter=0


    lateinit var menu: Menu
    lateinit var tietFilterDate: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("MainActivity_onCreate")
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        mainPresenter.attachView(this)

        // Запросим разрешение на геолокацию, нужны для сервиса
        requestPermission()

        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) /*&&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)*/) {
            //toaster.showToast("buildAlertMessageNoGps")
            buildAlertMessageNoGps()
        }


        // Регистрируем широковещательный слушатель для получения данных от фонового сервиса
        LocalBroadcastManager.getInstance(this).registerReceiver(userLocationReceiver, IntentFilter("userLocationUpdates"))

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)

        drawerLayout.addDrawerListener(object: DrawerLayout.DrawerListener {
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
            //clearBackstack()
            // Запустим активити с авторизацией
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, AUTH)
            drawerLayout.closeDrawer(Gravity.LEFT)
        }

    }

    fun isInitCurrentOrder() :Boolean {
        return ::currentOrder.isInitialized
    }


    private fun buildAlertMessageNoGps() {
        val builder= AlertDialog.Builder(this)
        builder.setMessage("Датчик GPS выключен, включить?").setCancelable(false)
            .setPositiveButton("Да"
            ) { _, _ -> startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton("Нет"
            ) { dialog, _ ->
                Timber.d("Сообщение администратору")
                mainPresenter.sendMessageToAdmin(REPEATEDLY_REFUSED)
                dialog?.cancel()
            }
        val alert=builder.create()

        alert.setOnShowListener { dialog ->
            val posButton=(dialog as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
            val params=LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin=20
            posButton.layoutParams=params
        }
        alert.show()
    }

    private fun requestPermission() {
        // Проверим разрешения
        Timber.d("requestPermission")
        if (ContextCompat.checkSelfPermission(this,(Manifest.permission.ACCESS_FINE_LOCATION)) != PackageManager.PERMISSION_GRANTED) {
            Timber.d("requestPermission1")
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                Timber.d("requestPermission2")
                requestPermissions(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                    Const.RequestCodes.PERMISSION
                )
            }
        } else {
            Timber.d("startService_MainActivity")
            // Стартуем фоновый сервис для отслеживания пользователя
            // Сервис стартуем сразу (до авторизации), чтоб можно было локацию для фоток получить
            startService(Intent(this,UserLocationService::class.java))
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
                    startService(Intent(this,UserLocationService::class.java))
                    //enableLocationComponent()
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

            lateinit var dialogFilterDateOrder: AlertDialog
            val layoutInflater = LayoutInflater.from(this)
            dialogDateFilterView =
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
            /*tietFilterDate =dialogDateFilterView.findViewById(R.id.tietFilterDate)
            tietFilterDate.setOnClickListener(this)*/

            builder.setView(dialogDateFilterView)
            builder.setCancelable(true)
            dialogFilterDateOrder=builder.create()
            dialogFilterDateOrder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            //Меняем позицию диалога
            dialogFilterDateOrder.window?.setGravity(Gravity.TOP or Gravity.RIGHT)

            dialogFilterDateOrder.setOnCancelListener(dialogDateFilterCancelListener)

            dialogFilterDateOrder.show()
            // Установим ширину диалогового окна
            val width=resources.getDimension(R.dimen.dialog_filter_date_width).toInt()
            dialogFilterDateOrder.window?.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)

            // Отступы диалога от границ экрана
            val wlp=dialogFilterDateOrder.window?.attributes
            wlp?.y=50
            wlp?.x=50
            dialogFilterDateOrder.window?.attributes=wlp

            true
        }

        /*val btnDateFilter=filterView.findViewById<Button>(R.id.)
        btnFilter.setOnClickListener(this)*/

        val searchItem=menu.findItem(R.id.action_search)
        if (searchItem!=null) {
            val searchView=searchItem.actionView as SearchView
            searchView.setPadding(0,0,-30,0)

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


            searchView.setOnQueryTextListener(object:SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean {
                    Timber.d("onQueryTextSubmit")
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    Timber.d(newText)

                    isSearchView=true
                    //#SearchView_close
                    if (newText=="") {
                        Timber.d("Закроем")
                        searchView.isIconified=true
                    }

                    Timber.d("_isSearchView=true")


                    if (!isMapFragmentShow) {
                        val rcv=findViewById<RecyclerView>(R.id.orders_recycler_view)
                        (rcv.adapter as OrderListAdapter).filter.filter(newText)
                    } else {
                        Timber.d("Включена карта11 $newText")

                        val filteredList=orders.filter { it.address!=null && it.address!!.contains(newText!!,true) }

                        Timber.d("Отфильтровано=${filteredList.size}")
                        val currentNavHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                        val currentFragmentClassName = (navController.currentDestination as FragmentNavigator.Destination).className
                        val mf= currentNavHost?.childFragmentManager?.fragments?.filterNotNull()?.find { it.javaClass.name==currentFragmentClassName } as MapFragment
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

                    val navHostFragment: NavHostFragment? =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val of =navHostFragment!!.childFragmentManager.fragments[0] as? OrderFragment
                    of?.doAuthorization()
                    if (of==null) {
                        doAuthorization()
                    }

                }
                else -> {
                    Timber.d("Неизвестный requestCode")
                }
            }
        }
    }

    private fun doAuthorization() {
        Timber.d("doAuthorization")
        // Получим логин и пароль из настроек
        val sharedPref = this.getSharedPreferences(Const.SharedPrefConst.APP_PREFERENCES, Context.MODE_PRIVATE)
        if (sharedPref!!.contains(Const.SharedPrefConst.LOGIN) && sharedPref.contains(Const.SharedPrefConst.PASSWORD)) {

            val login = this.sharedPref.getLogin()
            val password = this.sharedPref.getPassword()

            mainPresenter.attachView(this)
            mainPresenter.authorization(login, password) // Проверим есть ли авторизация
        } else {
            Timber.d("логин/пароль=ОТСУТСТВУЮТ")
            // Запустим активити с настройками
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, AUTH)
        }
    }



    override fun onDestroy() {
        Timber.d("MainAct_destroy")
        super.onDestroy()
        stopService(Intent(this,UserLocationService::class.java))
        mainPresenter.onDestroy()
        //unregisterReceiver(userLocationReceiver)
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(userLocationReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        userLocationReceiver.onDestroy()
        sharedPref.clearAuthData() // Очистим информацию об авторизации

    }


    override fun onBackPressed() {
        super.onBackPressed()
        isBackPressed=true
        doubleBackToExitCounter += 1
        Timber.d("onBackPressed")
        Timber.d("backStackEntryCount=${supportFragmentManager.backStackEntryCount}")
        Timber.d("fragments.size=${supportFragmentManager.fragments.size}")

        val currentFragmentClassName = (navController.currentDestination as FragmentNavigator.Destination).className
        Timber.d("currentFragmentClassName=$currentFragmentClassName")
        if (supportFragmentManager.fragments.size>1) {
            if (currentFragmentClassName==getString(R.string.order_fragment_className)) {
                Timber.d("navController_navigate_nav_home")
                navController.navigate(R.id.nav_home)
            }
        }


        if (supportFragmentManager.backStackEntryCount==0) {
            Timber.d("onBackPressed_Заявки")
            // Сбросим текущую заявку
            currentOrder= Orders(guid = "")
            supportActionBar?.setTitle(R.string.menu_orders)
            setMode(false) // Включены Заявки, а не карта
            // Выделим кнопку Список
            findViewById<Button>(R.id.btnList)?.isEnabled=false
            findViewById<Button>(R.id.btnMap)?.isEnabled=true

            Timber.d("navController_navigate_nav_home2")
            navController.navigate(R.id.nav_home)
            if (isBackPressed) {
                isBackPressed=false
            }
            if (isSearchView) {
                isSearchView=false
            }

            // Выходим из приложения при повторном клике на кнопку Назад
            if (doubleBackToExitCounter==2) {
                toaster.showToast(R.string.double_back)
                Handler().postDelayed({ doubleBackToExitCounter = 0 }, 4000)
            }
            if (doubleBackToExitCounter==3) {
                finish()
            }


            /*val rv=findViewById<RecyclerView>(R.id.orders_recycler_view)
            if (rv!=null) {
                Timber.d("rv!=null")
                if (filteredOrders.isNotEmpty()) {
                    (rv.adapter as OrderListAdapter).ordersFilterList=this.filteredOrders
                }

                //#Recyclerview_binding_finish
                rv.addOnLayoutChangeListener(object: View.OnLayoutChangeListener{
                    override fun onLayoutChange(
                        v: View?,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int
                    ) {
                        rv.removeOnLayoutChangeListener(this)
                        Timber.d("onLayoutChange")
                        if (isBackPressed) {
                            isBackPressed=false
                        }
                        if (isSearchView) {
                            isSearchView=false
                        }
                    }

                })
                rv.adapter?.notifyDataSetChanged()
            } else {
                Timber.d("navController_navigate_nav_home2")
                navController.navigate(R.id.nav_home)
                if (isBackPressed) {
                    isBackPressed=false
                }
                if (isSearchView) {
                    isSearchView=false
                }
            }*/

        }

    }


    override fun setCoordinates(point: Point, controlId: Int) {
        Timber.d("setCoordinates from Activity")
        mapPoint=point
        this.controlMapId=controlId
    }


    override fun setMode(isMap: Boolean) {
        Timber.d("setMode=$isMap")
        isMapFragmentShow=isMap
    }



    override fun showMarkers(orders: List<Orders>) {
        /*val rcv=findViewById<RecyclerView>(R.id.orders_recycler_view)
        val mf=supportFragmentManager.findFragmentByTag("fragment_map") as? MapFragment
        mf?.showMarkers((rcv.adapter as OrderListAdapter).ordersFilterList)*/
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
            childFragment.showMarkers((rcv.adapter as OrderListAdapter).ordersFilterList)
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
        OtherUtil().saveExifLocation(lastKnownFilenamePhoto,photoLocation)


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
                navController.navigate(R.id.nav_home)
                setMode(false) //Включены заявки
                true
            }
            R.id.nav_slideshow -> {
                navController.navigate(R.id.nav_slideshow)
                setMode() //Включена карта
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

    override fun dataSyncOK() {
        Timber.d("dataSyncOK")
        mainPresenter.updData()
    }

    override fun updDataOK() {
        Timber.d("updDataOK")
        //Передаем маршрут пользователя
        mainPresenter.sendUserRoute()
    }

    override fun filesSend(countFiles: Int, indexCurrentFile: Int) {
        if (countFiles==indexCurrentFile) {
            mainPresenter.updData()
            toaster.showToast(R.string.data_and_files_sends)
        }
    }

     override fun saveLoginPasswordToSharedPreference(stLogin: String, stPassword: String) {
         sharedPref.saveLogin(stLogin)
         sharedPref.savePassword(stPassword)
     }

     override fun saveToken(token: String) {
         sharedPref.saveToken(token)
     }


    override fun errorReceived(throwable: Throwable) {
        Timber.d("MainACT_errorReceived_$throwable")
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

    fun invalidateNavigationDrawer() {
        Timber.d("invalidateNavigationDrawer")
        val user=sharedPref.getUser()

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
                Timber.d("Открываем окно с фильтром")

                Timber.d("dialogFilterOrder")
                lateinit var dialogFilterOrder: AlertDialog
                val layoutInflater = LayoutInflater.from(this)
                dialogView =
                    layoutInflater.inflate(R.layout.alert_filter_order, null, false)

                val builder = AlertDialog.Builder(this)

                setCountFiltersGroup()

                builder.setView(dialogView)
                builder.setCancelable(true)
                dialogFilterOrder=builder.create()
                dialogFilterOrder.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                //Меняем позицию диалога
                dialogFilterOrder.window?.setGravity(Gravity.TOP or Gravity.RIGHT)

                dialogFilterOrder.setOnCancelListener(dialogCancelListener)

                dialogFilterOrder.show()
                // Установим ширину диалогового окна
                val width=resources.getDimension(R.dimen.dialog_filter_width).toInt()
                dialogFilterOrder.window?.setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT)

                // Отступы диалога от границ экрана
                val wlp=dialogFilterOrder.window?.attributes
                wlp?.y=50
                wlp?.x=50
                dialogFilterOrder.window?.attributes=wlp

            }
            R.id.rbAll -> {
                Timber.d("Фильтр_по_дате_Все")
                filteredOrders= listOf()
                filterOrderByDate("all")
            }
            R.id.rbToday -> {
                Timber.d("Фильтр_по_дате_Сегодня")

                // Текущее время
                val currentDate = Date()
                val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateText: String = dateFormat.format(currentDate)
                Timber.d("dateText=$dateText")
                filterOrderByDate(dateText)

            }
            R.id.rbTomorrow -> {
                Timber.d("Фильтр_по_дате_Завтра")
                val calendar=Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR,1)
                val tomorrowDate=calendar.time

                val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateText: String = dateFormat.format(tomorrowDate)
                Timber.d("dateText=$dateText")
                filterOrderByDate(dateText)
            }
            R.id.rbDate -> {
                Timber.d("Фильтр_по_дате_Дата")
                val date =Calendar.getInstance()
                val dateListener =
                    DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                        date.set(Calendar.YEAR, year)
                        date.set(Calendar.MONTH, month)
                        date.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        /*tietFilterDate.setText(
                            SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).format(date.time)
                        )*/

                        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val dateText: String = dateFormat.format(date.time)
                        Timber.d("dateText=$dateText")
                        filterOrderByDate(dateText)
                    }

                DatePickerDialog(this,
                    dateListener,
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH),
                    date.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            /*R.id.tietFilterDate -> {
                Timber.d("Фильтр_по_дате_Дата")

            }*/
            /*R.id.btnSearch -> {
                Timber.d("Открываем окно с поиском")
            }*/
        }
    }

    private fun setCountFiltersGroup() {
        Timber.d("setCountFiltersGroup")

        if (::orders.isInitialized) {
            dialogView.findViewById<TextView>(R.id.countType1).text=orders.filter { it.groupOrder==this.getString(R.string.orderType1).toLowerCase()}.size.toString()
            dialogView.findViewById<TextView>(R.id.countType2).text=orders.filter { it.groupOrder==this.getString(R.string.orderType2).toLowerCase() }.size.toString()
            dialogView.findViewById<TextView>(R.id.countType3).text=orders.filter { it.groupOrder==this.getString(R.string.orderType3).toLowerCase() }.size.toString()
            dialogView.findViewById<TextView>(R.id.countType4).text=orders.filter { it.groupOrder==this.getString(R.string.orderType4).toLowerCase() }.size.toString()
        }

    }

    private val dialogCancelListener=object: DialogInterface.OnCancelListener{
        override fun onCancel(dialog: DialogInterface?) {
            Timber.d("setOnCancelListener")
            // Считаем сколько фильтров поставили
            val cb1=dialogView.findViewById<CheckBox>(R.id.cbType1)
            val cb2=dialogView.findViewById<CheckBox>(R.id.cbType2)
            val cb3=dialogView.findViewById<CheckBox>(R.id.cbType3)
            val cb4=dialogView.findViewById<CheckBox>(R.id.cbType4)

            val filterGroupList= mutableListOf<String>()
            var filterCount=0
            if (cb1.isChecked) {
                filterCount+=1
                filterGroupList.add(dialogView.context.getString(R.string.orderType1).toLowerCase())
            }
            if (cb2.isChecked) {
                filterCount+=1
                filterGroupList.add(dialogView.context.getString(R.string.orderType2).toLowerCase())
            }
            if (cb3.isChecked) {
                filterCount+=1
                filterGroupList.add(dialogView.context.getString(R.string.orderType3).toLowerCase())
            }
            if (cb4.isChecked) {
                filterCount+=1
                filterGroupList.add(dialogView.context.getString(R.string.orderType4).toLowerCase())
            }

            Timber.d("filterCount=$filterCount")
            Timber.d("filterGroupList=$filterGroupList")

            filterView.findViewById<TextView>(R.id.badge_count).text=filterCount.toString()
            if (filterCount==4) {
                filteredOrders= listOf()
            }

            // Фильтруем заявки
            val navHostFragment=(dialogView.context as MainActivity).supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            val childFragment=navHostFragment?.childFragmentManager?.fragments?.get(0)
            if (childFragment is OrderFragment) {
                childFragment.filteredOrderByGroup(filterGroupList)
            }
            if (childFragment is MapFragment){
                Timber.d("Включена карта")
                val filteredOrderByGroup=orders.filter { it.groupOrder in filterGroupList }

                childFragment.showMarkers(filteredOrderByGroup)
            }


        }

    }

     private val dialogDateFilterCancelListener=object: DialogInterface.OnCancelListener{
         override fun onCancel(dialog: DialogInterface?) {
             Timber.d("dialogDateFilterCancelListener")
         }

     }

     private fun filterOrderByDate(dateText: String) {
         // Фильтруем заявки
         val navHostFragment=(dialogDateFilterView.context as MainActivity).supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
         val childFragment=navHostFragment?.childFragmentManager?.fragments?.get(0)
         if (childFragment is OrderFragment) {
             childFragment.filteredOrderByDate(dateText)
         }
         if (childFragment is MapFragment){
             Timber.d("Включена карта")
             var filteredOrderByGroup= listOf<Orders>()
             if (dateText=="all") {
                 filteredOrderByGroup=orders.filter { it.dateVisit !=null }
             } else {
                 filteredOrderByGroup=orders.filter { it.dateVisit ==dateText }
             }


             childFragment.showMarkers(filteredOrderByGroup)
         }
     }


}
