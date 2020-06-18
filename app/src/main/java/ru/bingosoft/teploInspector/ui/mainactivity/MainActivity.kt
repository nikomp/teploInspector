package ru.bingosoft.teploInspector.ui.mainactivity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.navigation.NavigationView
import com.yandex.mapkit.geometry.Point
import dagger.android.AndroidInjection
import ru.bingosoft.teploInspector.BuildConfig
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.api.ApiService
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.ui.checkuplist.CheckupListFragment
import ru.bingosoft.teploInspector.ui.login.LoginActivity
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.util.*
import ru.bingosoft.teploInspector.util.Const.MessageCode.REFUSED_PERMISSION
import ru.bingosoft.teploInspector.util.Const.MessageCode.REPEATEDLY_REFUSED
import ru.bingosoft.teploInspector.util.Const.RequestCodes.PHOTO
import ru.bingosoft.teploInspector.util.Const.RequestCodes.QR_SCAN
import timber.log.Timber
import javax.inject.Inject


class MainActivity : AppCompatActivity(), FragmentsContractActivity,
    NavigationView.OnNavigationItemSelectedListener, MainActivityContractView, View.OnClickListener {

    @Inject
    lateinit var mainPresenter: MainActivityPresenter
    @Inject
    lateinit var toaster: Toaster
    @Inject
    lateinit var sharedPref: SharedPrefSaver
    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var userLocationReceiver: UserLocationReceiver

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    var mapPoint: Point=Point(0.0,0.0)
    var controlMapId: Int=0
    var photoDir: String=""
    var lastKnownFilenamePhoto=""
    var photoStep: Models.TemplateControl?=null
    lateinit var currentOrder: Orders
    private lateinit var locationManager: LocationManager
    lateinit var dialogView: View
    lateinit var filterView: RelativeLayout

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
        Timber.d("startService_MainActivity")
        // Стартуем фоновый сервис для отслеживания пользователя
        // Сервис стартуем сразу (до авторизации), чтоб можно было локацию для фоток получить
        // Сейчас данные уходят так как стоит заглушка на проверку сессии
        startService(Intent(this,UserLocationService::class.java))
        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            buildAlertMessageNoGps()
            Timber.d("startService_MainActivity22")
        }

        /*if (!sharedPref.isLocationTracking()) {
            Timber.d("zzz=${sharedPref.getLogin()}_${sharedPref.getPassword()}")
            // Проверим авторизован ли пользователь
            if (sharedPref.getLogin()!="" && sharedPref.getPassword()!="") {
                Timber.d("startService_MainActivity")
                // Стартуем фоновый сервис для отслеживания пользователя
                startService(Intent(this,UserLocationService::class.java))
            }
        } else {
            val locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
            Timber.d("GPS_PROVIDER=${locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)}")
            Timber.d("NETWORK_PROVIDER=${locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)}")
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                buildAlertMessageNoGps()
                Timber.d("startService_MainActivity22")
                // Стартуем фоновый сервис для отслеживания пользователя
                startService(Intent(this,UserLocationService::class.java))
            }
        }*/
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
                R.id.nav_gallery,
                R.id.nav_slideshow,
                R.id.nav_checkup
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setNavigationItemSelectedListener(this)
        //navView.setupWithNavController(navController) // Переключалка фрагментов по-умолчанию

        val header=navView.getHeaderView(0)
        val imgButtonAuth=header.findViewById<ImageButton>(R.id.imgbAuth)
        imgButtonAuth.setOnClickListener {
            Timber.d("Auth")
            // Запустим активити с настройками
            val intent = Intent(this, LoginActivity::class.java)
            startActivityForResult(intent, Const.RequestCodes.AUTH)
        }
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
        if (ContextCompat.checkSelfPermission(this,(Manifest.permission.ACCESS_FINE_LOCATION)) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                    Const.RequestCodes.PERMISSION
                )
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
                    Timber.d("startService_Permission")
                    startService(Intent(this,UserLocationService::class.java))
                    //enableLocationComponent()
                } else {
                    // Разрешения не выданы оповестим юзера
                    toaster.showToast(R.string.not_permissions)
                    Timber.d("ОТКАЗАЛСЯ ОТ ГЕОЛОКАЦИИ")
                    mainPresenter.sendMessageToAdmin(REFUSED_PERMISSION)
                }
            }
            else -> Timber.d("Неизвестный PERMISSION_REQUEST_CODE")
        }

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val item = menu.findItem(R.id.menu_buttons)
        item.setActionView(R.layout.filter_count)
        filterView=item.actionView as RelativeLayout

        val btnFilter=filterView.findViewById<Button>(R.id.btnFilter)
        btnFilter.setOnClickListener(this)

        val searchView=item.actionView as RelativeLayout
        val btnSearch=searchView.findViewById<Button>(R.id.btnSearch)
        btnSearch.setOnClickListener(this)

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
                QR_SCAN ->{
                    Timber.d(data?.getStringExtra("SCAN_RESULT"))
                    val scanResult=data?.getStringExtra("SCAN_RESULT")
                    val orderId=scanResult?.toLongOrNull()
                    Timber.d(orderId.toString())
                    if (orderId!=null) {
                        mainPresenter.openCheckup(this.supportFragmentManager,orderId)
                    } else {
                        toaster.showToast(R.string.not_checkup)
                    }


                }
                else -> {
                    Timber.d("Неизвестный requestCode")
                }
            }
        }
    }



    override fun onDestroy() {
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

    }


    override fun onBackPressed() {
        super.onBackPressed()
        Timber.d("onBackPressed")
        Timber.d("backStackEntryCount=${supportFragmentManager.backStackEntryCount}")

        if (supportFragmentManager.backStackEntryCount==0) {
            supportActionBar?.setTitle(R.string.menu_orders)
            // Выделим кнопку Список
            findViewById<Button>(R.id.btnList).isEnabled=false
            findViewById<Button>(R.id.btnMap).isEnabled=true
        }

        val fragment=supportFragmentManager.findFragmentByTag("checkup_fragment_tag")
        if (fragment!=null) {
            Timber.d("onBackPressed_checkup_fragment_tag")
            val checkupId= fragment.arguments?.getLong("checkupId")
            Timber.d("checkupId=$checkupId")
            if (checkupId!=null) {
                (fragment as CheckupFragment).checkupPresenter.loadCheckup(checkupId)
            }
        }

        val fragment2=supportFragmentManager.findFragmentByTag("checkup_list_fragment_tag")
        if (fragment2!=null) {
            Timber.d("onBackPressed_checkup_list_fragment_tag")
            val idOrder = fragment2.arguments?.getLong("idOrder")
            if (idOrder!=null) {
                (fragment2 as CheckupListFragment).checkupListPresenter.loadCheckupListByOrder(idOrder) // Грузим объекты только выбранной заявки
            } else {
                (fragment2 as CheckupListFragment).checkupListPresenter.loadCheckupList() // Грузим все объекты
            }
        }
    }

    override fun setCheckup(checkup: Checkup) {
        Timber.d("setCheckup from Activity")
        val cf=this.supportFragmentManager.findFragmentByTag("checkup_fragment_tag") as? CheckupFragment
        cf?.dataIsLoaded(checkup)
    }

    override fun setChecupListOrder(order: Orders) {
        Timber.d("setChecupListOrder from Activity")
        val clf=this.supportFragmentManager.findFragmentByTag("checkup_list_fragment_tag") as? CheckupListFragment
        clf?.showCheckupListOrder(order)
    }

    override fun setCoordinates(point: Point, controlId: Int) {
        Timber.d("setCoordinates from Activity")
        mapPoint=point
        this.controlMapId=controlId
    }

    override fun setOrder(order: Orders) {
        Timber.d("setOrderForRouteDetail from Activity")
        val mf=this.supportFragmentManager.findFragmentByTag("map_fragment_from_orders_tag") as? MapFragment
        mf?.showRouteDialog(order)
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


        Timber.d("setPhotoResult from Activity")
        val cf=this.supportFragmentManager.findFragmentByTag("checkup_fragment_tag") as? CheckupFragment
        cf?.setPhotoResult(photoStep?.id, photoDir)
        photoStep?.resvalue=photoDir

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)

        Timber.d("onNavigationItemSelected")

        when (item.itemId) {
            R.id.nav_home -> {
                navController.navigate(R.id.nav_home)
                return true
            }
            R.id.nav_slideshow -> {
                navController.navigate(R.id.nav_slideshow)
                return true
            }
            R.id.nav_send_data -> {
                Timber.d("Отправляем данные на сервер")
                mainPresenter.sendData()
                //mainPresenter.isCheckupWithResult()
                return true
            }
            else -> {
                return false
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
            R.id.btnSearch -> {
                Timber.d("Открываем окно с поиском")
            }
        }
    }

    val dialogCancelListener=object: DialogInterface.OnCancelListener{
        override fun onCancel(dialog: DialogInterface?) {
            Timber.d("setOnCancelListener")
            // Считаем сколько фильтров поставили
            val cb1=dialogView.findViewById<CheckBox>(R.id.cbType1)
            val cb2=dialogView.findViewById<CheckBox>(R.id.cbType2)
            val cb3=dialogView.findViewById<CheckBox>(R.id.cbType3)
            val cb4=dialogView.findViewById<CheckBox>(R.id.cbType4)

            var filterCount=0
            if (cb1.isChecked) { filterCount+=1 }
            if (cb2.isChecked) { filterCount+=1 }
            if (cb3.isChecked) { filterCount+=1 }
            if (cb4.isChecked) { filterCount+=1 }

            Timber.d("filterCount=$filterCount")

            filterView.findViewById<TextView>(R.id.badge_count).text=filterCount.toString()

            // Фильтруем заявки
            Timber.d("Фильтруем заявки")
        }

    }

}
