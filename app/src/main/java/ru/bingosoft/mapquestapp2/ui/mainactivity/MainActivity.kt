package ru.bingosoft.mapquestapp2.ui.mainactivity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import com.mapbox.mapboxsdk.geometry.LatLng
import dagger.android.AndroidInjection
import ru.bingosoft.mapquestapp2.BuildConfig
import ru.bingosoft.mapquestapp2.R
import ru.bingosoft.mapquestapp2.db.Checkup.Checkup
import ru.bingosoft.mapquestapp2.db.Orders.Orders
import ru.bingosoft.mapquestapp2.models.Models
import ru.bingosoft.mapquestapp2.ui.checkup.CheckupFragment
import ru.bingosoft.mapquestapp2.ui.checkuplist.CheckupListFragment
import ru.bingosoft.mapquestapp2.ui.login.LoginActivity
import ru.bingosoft.mapquestapp2.util.Const
import ru.bingosoft.mapquestapp2.util.Const.RequestCodes.PHOTO
import ru.bingosoft.mapquestapp2.util.Const.RequestCodes.QR_SCAN
import ru.bingosoft.mapquestapp2.util.SharedPrefSaver
import ru.bingosoft.mapquestapp2.util.Toaster
import timber.log.Timber
import javax.inject.Inject


class MainActivity : AppCompatActivity(), FragmentsContractActivity,
    NavigationView.OnNavigationItemSelectedListener, MainActivityContractView {

    @Inject
    lateinit var mainPresenter: MainActivityPresenter
    @Inject
    lateinit var toaster: Toaster
    @Inject
    lateinit var sharedPref: SharedPrefSaver

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    var mapPoint: LatLng= LatLng(0.0,0.0)
    var controlMapId: Int=0
    var photoDir: String=""
    var photoStep: Models.TemplateControl?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("MainActivity_onCreate")
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

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

        mainPresenter.attachView(this)



    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
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
                    //toaster.showToast("Фото сохранено в папке DCIM\\PhotoForApp\\")
                    //photoDir="DCIM\\PhotoForApp\\"
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
        mainPresenter.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Timber.d("onBackPressed")
        Timber.d("backStackEntryCount=${supportFragmentManager.backStackEntryCount}")

        if (supportFragmentManager.backStackEntryCount==0) {
            supportActionBar?.setTitle(R.string.menu_orders)
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

    override fun setCoordinates(point: LatLng, controlId: Int) {
        Timber.d("setCoordinates from Activity")
        mapPoint=point
        this.controlMapId=controlId
    }

    private fun setPhotoResult() {
        Timber.d("setPhotoResult from Activity")
        val cf=this.supportFragmentManager.findFragmentByTag("checkup_fragment_tag") as? CheckupFragment
        cf?.setPhotoResult(photoStep?.id,"DCIM\\PhotoForApp\\$photoDir")
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
            R.id.nav_gallery -> {
                navController.navigate(R.id.nav_gallery)
                return true
            }
            R.id.nav_slideshow -> {
                navController.navigate(R.id.nav_slideshow)
                return true
            }
            R.id.nav_send_data -> {
                Timber.d("Отправляем данные на сервер")
                mainPresenter.sendData()
                return true
            }
            R.id.nav_auth -> {
                // Запустим активити с настройками
                val intent = Intent(this, LoginActivity::class.java)
                startActivityForResult(intent, Const.RequestCodes.AUTH)
                return true
            }
            R.id.nav_qr_scan -> {
                // Запустим сканер QR
                try {
                    val intent = Intent("com.google.zxing.client.android.SCAN")
                    intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
                    startActivityForResult(intent, QR_SCAN)
                    return true
                } catch (e: Exception) {

                    val marketUri = Uri.parse("market://details?id=com.google.zxing.client.android")
                    val marketIntent = Intent(Intent.ACTION_VIEW,marketUri)
                    startActivity(marketIntent)
                    return false

                }

            }
            else -> {
                return false
            }
        }
    }

    override fun showMainActivityMsg(resID: Int) {
        toaster.showToast(resID)
    }

    override fun showMainActivityMsg(msg: String) {
        toaster.showToast(msg)
    }

    override fun dataSyncOK() {
        Timber.d("dataSyncOK")
        mainPresenter.updData()
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


}
