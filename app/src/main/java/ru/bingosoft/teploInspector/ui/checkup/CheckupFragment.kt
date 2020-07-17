package ru.bingosoft.teploInspector.ui.checkup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.mapbox.mapboxsdk.geometry.LatLng
import dagger.android.support.AndroidSupportInjection
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.mainactivity.FragmentsContractActivity
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.map.MapFragment
import ru.bingosoft.teploInspector.util.*
import ru.bingosoft.teploInspector.util.photoSliderHelper.GalleryPagerAdapter
import ru.bingosoft.teploInspector.util.photoSliderHelper.HorizontalAdapter
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


class CheckupFragment : Fragment(), CheckupContractView, View.OnClickListener {

    @Inject
    lateinit var checkupPresenter: CheckupPresenter

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var otherUtil: OtherUtil

    @Inject
    lateinit var userLocationNative: UserLocationNative

    @Inject
    lateinit var photoHelper: PhotoHelper

    lateinit var rootView: View
    private lateinit var uiCreator: UICreator

    lateinit var controlList: Models.ControlList
    var savedcontrolList: Models.ControlList?=null
    lateinit var checkup: Checkup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        AndroidSupportInjection.inject(this)
        Timber.d("CheckupFragment.onCreateView")

        val view = inflater.inflate(R.layout.fragment_gallery2, container, false)

        // Устанавливаем заголовок фрагмента
        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.title_checkup_fragment)

        this.rootView=view

        val checkupSteps: ArrayList<String> = ArrayList()
        checkupSteps.add("Общие сведения об абоненте")
        checkupSteps.add("Технические характеристики объекта")
        checkupSteps.add("Промывка ГВС Узел №1")
        checkupSteps.add("Промывка ГВС Узел №2")

        val stepsRecyclerView = rootView.findViewById(R.id.steps_recycler_view) as RecyclerView
        stepsRecyclerView.layoutManager = LinearLayoutManager(this.activity)
        val adapter=StepsAdapter(checkupSteps)
        stepsRecyclerView.adapter = adapter

        val titleOrder=rootView.findViewById<LinearLayout>(R.id.titleOrder)
        titleOrder.setOnClickListener {
            val dataOrder=rootView.findViewById<ConstraintLayout>(R.id.dataOrder)
            if (dataOrder.visibility==View.VISIBLE) {
                dataOrder.visibility=View.GONE
            } else {
                dataOrder.visibility=View.VISIBLE
            }
        }

        fillOrderData()


        /*val btnSave = view.findViewById(R.id.mbSaveCheckup) as MaterialButton
        btnSave.setOnClickListener(this)

        val btnSend = view.findViewById(R.id.mbSendCheckup) as MaterialButton
        btnSend.setOnClickListener(this)

        checkupPresenter.attachView(this)

        val tag = arguments?.getBoolean("loadCheckupById")
        if (tag!=null && tag==true) {
            Timber.d("loadCheckupById")
            val checkupId= arguments?.getLong("checkupId")
            Timber.d("checkupId=$checkupId")
            if (checkupId!=null) {
                checkupPresenter.loadCheckup(checkupId)
            }
        }

        checkPhotoPermission() // Проверим разрешения для фото*/
        return view
    }


    /*override fun onStop() {
        Timber.d("CheckupFragment_onStop")
        super.onStop()
    }*/

    fun fillOrderData() {
        val order=(rootView.context as MainActivity).currentOrder

        rootView.findViewById<TextView>(R.id.number).text=order.number
        rootView.findViewById<TextView>(R.id.order_type).text=order.typeOrder
        if (order.typeOrder.isNullOrEmpty()){
            rootView.findViewById<TextView>(R.id.order_type).text="Тип заявки"
        } else {
            rootView.findViewById<TextView>(R.id.order_type).text=order.typeOrder
        }
        when (order.state) {
            "1" -> rootView.findViewById<TextView>(R.id.order_state).text="В работе"
        }
        rootView.findViewById<TextView>(R.id.date).text=
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(order.dateCreate)
        rootView.findViewById<TextView>(R.id.name).text=order.name
        rootView.findViewById<TextView>(R.id.adress).text=order.address
        rootView.findViewById<TextView>(R.id.fio).text=order.contactFio
        if (order.typeTransportation.isNullOrEmpty()) {
            rootView.findViewById<TextView>(R.id.type_transportation).text="Нет данных"
        } else {
            rootView.findViewById<TextView>(R.id.type_transportation).text=order.typeTransportation
        }
        val btnPhone=rootView.findViewById<Button>(R.id.btnPhone)
        if (order.phone.isNullOrEmpty()) {
            btnPhone.text=btnPhone.context.getString(R.string.no_contact)
            btnPhone.isEnabled=false
            btnPhone.setTextColor(R.color.enabledText)
        } else {
            btnPhone.text=order.phone
        }

        val btnRoute=rootView.findViewById<Button>(R.id.btnRoute)

        val distance=otherUtil.getDistance(userLocationNative.userLocation, order)
        btnRoute.text=rootView.context.getString(R.string.distance, distance.toString())//"Маршрут 3.2 км"

        btnPhone.setOnClickListener { _ ->
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phone}"))
            if (intent.resolveActivity(btnPhone.context.packageManager) != null) {
                ContextCompat.startActivity(btnPhone.context, intent, null)
            }
        }

        btnRoute.setOnClickListener { _ ->
            Timber.d("btnRoute.setOnClickListener")

            //Включаем фрагмент со списком Маршрутов для конкретной заявки
            /*parentFragment.showRouteDialog(order)
            hideBottomSheet()*/

            Timber.d("btnRoute.setOnClickListener")

            //Включаем фрагмент со списком Маршрутов для конкретной заявки
            val bundle = Bundle()
            bundle.putBoolean("isOrderFragment",true)
            val fragmentMap= MapFragment()
            fragmentMap.arguments=bundle
            val fragmentManager=(rootView.context as MainActivity).supportFragmentManager

            fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragmentMap, "map_fragment_from_orders_tag")
                .addToBackStack(null)
                .commit()

            fragmentManager.executePendingTransactions()


            (rootView.context as FragmentsContractActivity).setOrder(order)

        }
    }



    override fun dataIsLoaded(checkup: Checkup) {
        Timber.d("Checkup готов к работе")
        Timber.d(checkup.toString())

        this.checkup=checkup
        photoHelper.parentFragment=this
        /*if (savedcontrolList!=null) {
            uiCreator= UICreator(this, checkup)
            controlList=uiCreator.create(root, controls = savedcontrolList)
        } else {
           uiCreator= UICreator(this, checkup)
            controlList=uiCreator.create(root)
        }*/
        uiCreator= UICreator(this, checkup)
        controlList=uiCreator.create(rootView)


    }

    fun setResultMapPoint(point: LatLng, controlId: Int) {
        Timber.d("CheckupFragment setResultMapPoint $point, $controlId")
        /*val templateStep=root.findViewById<LinearLayout>(controlId)
        val mapResultTextView=templateStep.findViewById<TextView>(R.id.mapCoordinatesResult)
        mapResultTextView.text=point.toString()*/

    }


    override fun showCheckupMessage(resID: Int) {
        toaster.showToast(resID)
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.mbSaveCheckup -> {
                    checkupPresenter.saveCheckup(uiCreator)
                    //checkupPresenter.saveCheckup(controlList,this.checkup)
                }
                R.id.mbSendCheckup -> {
                    Timber.d("Отправляем данные на сервер")
                    (this.requireActivity() as MainActivity).mainPresenter.sendData()
                }
            }
        }
    }

    override fun onDestroy() {
        Timber.d("CheckupFragment_onDestroy")
        super.onDestroy()
        checkupPresenter.onDestroy()
    }


    fun checkPhotoPermission() {
        // Проверим разрешения
        if (ContextCompat.checkSelfPermission(this.requireContext(),(Manifest.permission.READ_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this.requireContext(),(Manifest.permission.WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this.requireContext(),(Manifest.permission.CAMERA)) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ),
                Const.RequestCodes.PERMISSION
            )

        }
    }

    /*override fun onPause() {
        Timber.d("CheckupFragment_onPause")
        super.onPause()
    }*/

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(Const.LogTags.LOGTAG,"onRequestPermissionsResult")
        when (requestCode) {
            Const.RequestCodes.PERMISSION -> {
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED
                ) {
                    // Разрешения выданы, повторим попытку
                    Timber.d("enableLocationComponent")
                    //enableLocationComponent(mapboxMap?.getStyle()!!)
                    //getUserLocation(mapboxMap.style!!)
                } else {
                    // Разрешения не выданы оповестим юзера
                    toaster.showToast(R.string.not_permissions)
                }
            }
            else -> Timber.d("Неизвестный PERMISSION_REQUEST_CODE")
        }

    }

    fun setPhotoResult(controlId: Int?, photoDir: String) {
        Timber.d("setPhotoResult from fragment $controlId")
        if (controlId!=null) {
            val linearLayout=rootView.findViewById<LinearLayout>(controlId)
            //linearLayout.findViewById<TextView>(R.id.photoResult).text=this.getString(R.string.photoResult,photoDir)
            // Обновим список с фото
            val stDir = "PhotoForApp/$photoDir"
            val storageDir =
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    stDir
                )

            Timber.d("$storageDir")

            val images = OtherUtil().getFilesFromDir("$storageDir")
            refreshPhotoViewer(linearLayout, images, rootView.context)
        }
    }

    fun refreshPhotoViewer(v: View, images: List<String>, ctx: Context) {
        val pager = v.findViewById(R.id.pager) as ViewPager
        val myList = v.findViewById(R.id.recyclerviewFrag) as RecyclerView
        val photoCount = v.findViewById(R.id.photoCount) as TextView
        photoCount.text = images.size.toString()

        val adapter =
            GalleryPagerAdapter(
                images,
                pager,
                ctx
            )
        pager.adapter = adapter

        pager.offscreenPageLimit = 4 // сколько фоток загружать в память

        adapter.notifyDataSetChanged()
        val horizontalAdapter = HorizontalAdapter(images, pager, ctx)
        val horizontalLayoutManagaer =
            LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        myList.layoutManager = horizontalLayoutManagaer
        myList.adapter = horizontalAdapter
        horizontalAdapter.notifyDataSetChanged()
    }

}