package ru.bingosoft.teploInspector.ui.checkup

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.alert_change_date_time.view.*
import kotlinx.android.synthetic.main.fragment_gallery2.*
import retrofit2.HttpException
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.db.Orders.Orders
import ru.bingosoft.teploInspector.db.TechParams.TechParams
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.mainactivity.UserLocationReceiver
import ru.bingosoft.teploInspector.ui.order.OrderPresenter
import ru.bingosoft.teploInspector.util.*
import ru.bingosoft.teploInspector.util.Const.Photo.DCIM_DIR
import ru.bingosoft.teploInspector.util.photoSliderHelper.GalleryPagerAdapter
import ru.bingosoft.teploInspector.util.photoSliderHelper.HorizontalAdapter
import timber.log.Timber
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


class CheckupFragment : Fragment(), CheckupContractView, View.OnClickListener {

    var uiCreator: UICreator?=null
    var llMainUi= mutableListOf<View>()

    @Inject
    lateinit var checkupPresenter: CheckupPresenter

    @Inject
    lateinit var orderPresenter: OrderPresenter

    @Inject
    lateinit var toaster: Toaster

    @Inject
    lateinit var otherUtil: OtherUtil

    /*@Inject
    lateinit var userLocationNative: UserLocationNative*/

    @Inject
    lateinit var userLocationReceiver: UserLocationReceiver

    @Inject
    lateinit var photoHelper: PhotoHelper

    lateinit var rootView: View

    var pw: PopupWindow? =null

    lateinit var checkup: Checkup
    lateinit var currentOrder: Orders
    var techParams: List<TechParams> = listOf()

    var errorControls= mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        AndroidSupportInjection.inject(this)
        Timber.d("CheckupFragment.onCreateView")
        Timber.d("MainActivity_orders_CheckupFragment=${(requireContext() as MainActivity).orders}")

        rootView = inflater.inflate(R.layout.fragment_gallery2, container, false)

        // Устанавливаем заголовок фрагмента
        (this.requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.title_checkup_fragment)

        val btnSave = rootView.findViewById(R.id.mbSaveCheckup) as MaterialButton
        btnSave.setOnClickListener(this)


        val checkupSteps: ArrayList<String> = ArrayList()
        checkupSteps.add("Общие сведения об абоненте")
        checkupSteps.add("Технические характеристики объекта")

        val typeOrderTag = arguments?.getString("typeOrder")
        if (typeOrderTag!=null) {
            checkupSteps.add("$typeOrderTag")
        }


        checkupPresenter.attachView(this)

        if ((requireActivity() as MainActivity).isInitCurrentOrder()) {
            setCheckup((requireActivity() as MainActivity).currentOrder)
            setTechParams((requireActivity() as MainActivity).currentOrder.id)
            if ((requireActivity() as MainActivity).photoStep!=null &&
                (requireActivity() as MainActivity).photoDir!="" ) {
                setPhotoResult((requireActivity() as MainActivity).photoStep?.id,
                    (requireActivity() as MainActivity).photoDir)
            }
        }


        val ivExpand=rootView.findViewById<ImageView>(R.id.ivExpand)
        ivExpand.visibility=View.VISIBLE

        val stepsRecyclerView = rootView.findViewById(R.id.steps_recycler_view) as RecyclerView
        //(stepsRecyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations=false
        stepsRecyclerView.layoutManager = LinearLayoutManager(this.activity)
        val adapter=StepsAdapter(checkupSteps,this)
        stepsRecyclerView.adapter = adapter

        val titleOrder=rootView.findViewById<LinearLayout>(R.id.titleOrder)
        titleOrder.setOnClickListener {
            val dataOrder=rootView.findViewById<ConstraintLayout>(R.id.dataOrder)
            if (dataOrder.visibility==View.VISIBLE) {
                dataOrder.visibility=View.GONE
                ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_up))
            } else {
                dataOrder.visibility=View.VISIBLE
                ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_down))
            }
        }

        fillOrderData()


        checkPermission()
        return rootView
    }

    private fun setTechParams(idOrder: Long) {
        Timber.d("setTechParams")
        checkupPresenter.getTechParams(idOrder)
    }


    @SuppressLint("ResourceAsColor", "SetTextI18n")
    private fun fillOrderData() {
        Timber.d("fillOrderData")
        if (!(rootView.context as MainActivity).isInitCurrentOrder()) {
            return
        }
        val order=(rootView.context as MainActivity).currentOrder

        rootView.findViewById<TextView>(R.id.number).text="№ ${order.number}"
        rootView.findViewById<TextView>(R.id.order_type).text=order.typeOrder
        if (order.typeOrder.isNullOrEmpty()){
            rootView.findViewById<TextView>(R.id.order_type).text="Тип заявки"
        } else {
            rootView.findViewById<TextView>(R.id.order_type).text=order.typeOrder
        }
        val mbsOrderState=rootView.findViewById<MaterialBetterSpinner>(R.id.order_state)
        mbsOrderState.setText(order.status?.toUpperCase(Locale.ROOT))
        changeColorMBSState(mbsOrderState, order.status)

        val btnChangeDateTime=rootView.findViewById<Button>(R.id.btnChangeDateTime)
        btnChangeDateTime.setOnClickListener {
            Timber.d("btnChangeDateTime_setOnClickListener")
            lateinit var alertDialog: AlertDialog
            val layoutInflater = LayoutInflater.from(requireContext())
            val dialogView: View =
                layoutInflater.inflate(R.layout.alert_change_date_time, (rootView as ViewGroup), false)

            val builder = AlertDialog.Builder(requireContext())

            dialogView.btnOk.setOnClickListener{
                Timber.d("dialogView.buttonOK")
                val newDate=dialogView.findViewById<TextView>(R.id.newDate)
                val newTime=dialogView.findViewById<TextView>(R.id.newTime)
                val strDateTimeVisit="${newDate.text} ${newTime.text}"

                Timber.d(strDateTimeVisit)
                try {
                    val date=SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).parse(strDateTimeVisit)
                    if (date!=null) {
                        btnChangeDateTime.text=
                            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(date)
                        order.dateVisit=SimpleDateFormat("yyyy-MM-dd", Locale("ru","RU")).format(date)
                        order.timeVisit=SimpleDateFormat("HH:mm:ss", Locale("ru","RU")).format(date)
                        Timber.d("order=$order")
                        orderPresenter.saveDateTime(order)
                        currentOrder=order

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }


                alertDialog.dismiss()

            }

            builder.setView(dialogView)
            builder.setCancelable(true)
            alertDialog=builder.create()
            alertDialog.show()
        }

        val adapterStatus: ArrayAdapter<String> = ArrayAdapter(
            rootView.context,
            R.layout.template_multiline_spinner_item_state_order,
            R.id.text1,
            Const.StatusOrder.list
        )

        mbsOrderState.setAdapter(adapterStatus)

        mbsOrderState.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {

                // Если статус меняется на Выполнена, а чек лист пуст, выдаем сообщение
                if (order.answeredCount==0 && s.toString()=="Выполнена") {
                    toaster.showToast(R.string.checklist_not_changed_status)
                    mbsOrderState.removeTextChangedListener(this)
                    mbsOrderState.setText(order.status?.toUpperCase(Locale.ROOT))
                    mbsOrderState.addTextChangedListener(this)
                }

                if (s.toString().toUpperCase(Locale.ROOT) != order.status?.toUpperCase(Locale.ROOT)) {
                    order.status=s.toString().toLowerCase(Locale.ROOT).capitalize()
                    currentOrder=order
                    changeColorMBSState(mbsOrderState, order.status)
                    try {
                        orderPresenter.addHistoryState(order)
                    } catch (e: Throwable) {
                        errorReceived(e)
                    }
                }

                uiCreator?.checkEnabled()

                mbsOrderState.removeTextChangedListener(this)
                mbsOrderState.setText(s.toString().toUpperCase(Locale.ROOT))
                mbsOrderState.addTextChangedListener(this)


                if (s.toString()!="Открыта" && s.toString()!="В пути") {
                    pw?.dismiss()
                } else {
                    checkStateOrder(order)
                }

                //Фильтруем по статусу
                if (s.toString()=="Выполнена" || s.toString()=="Отменена") {
                    (activity as MainActivity).filteredOrders=(activity as MainActivity).filteredOrders.filter {it.id!=currentOrder.id }
                    Timber.d("фильтранули=${(activity as MainActivity).filteredOrders}")
                }

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }

        })

        if (order.dateVisit!=null && order.timeVisit!=null) {
            val strDateTimeVisit="${order.dateVisit} ${order.timeVisit}"
            try {
                val date=SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ru","RU")).parse(strDateTimeVisit)
                if (date!=null) {
                    btnChangeDateTime.text=
                        SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru","RU")).format(date)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }


        } else {
            btnChangeDateTime.text=getString(R.string.not_date_visit)
        }

        rootView.findViewById<TextView>(R.id.name).text=order.purposeObject
        rootView.findViewById<TextView>(R.id.adress).text=order.address
        rootView.findViewById<TextView>(R.id.fio).text=order.contactFio

        if (!order.typeTransportation.isNullOrEmpty()) {
            rootView.findViewById<MaterialBetterSpinner>(R.id.type_transportation).setText(order.typeTransportation)
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

        /*val distance=otherUtil.getDistance(userLocationNative.userLocation, order)
        btnRoute.text=rootView.context.getString(R.string.distance, distance.toString())//"Маршрут 3.2 км"*/

        if (userLocationReceiver.lastKnownLocation.latitude!=0.0 && userLocationReceiver.lastKnownLocation.longitude!=0.0) {
            val distanceRoute=otherUtil.getDistance(userLocationReceiver.lastKnownLocation, order)
            btnRoute.text=rootView.context.getString(R.string.distance, distanceRoute.toString())//"Маршрут 3.2 км"
        } else {
            btnRoute.text=rootView.context.getString(R.string.route)
        }

        btnPhone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.phone}"))
            if (intent.resolveActivity(btnPhone.context.packageManager) != null) {
                ContextCompat.startActivity(btnPhone.context, intent, null)
            }
        }

        btnRoute.setOnClickListener {
            Timber.d("btnRoute.setOnClickListener")

            (activity as MainActivity).currentOrder=this.currentOrder

            //Включаем фрагмент со списком Маршрутов для конкретной заявки
            val bundle = Bundle()
            bundle.putBoolean("isOrderFragment",true)

            Navigation.findNavController(rootView).navigate(R.id.nav_slideshow,bundle)

        }

        val adapter: ArrayAdapter<String> = ArrayAdapter(
            rootView.context,
            R.layout.template_multiline_spinner_item,
            Const.TypeTransportation.list
        )

        val mbsTypeTransportation=rootView.findViewById<MaterialBetterSpinner>(R.id.type_transportation)
        mbsTypeTransportation.setAdapter(adapter)

        mbsTypeTransportation.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btnRoute.isEnabled = s.toString() != rootView.context.getString(R.string.strTypeTransportationClient)
                if (btnRoute.isEnabled) {
                    btnRoute.setTextColor(Color.parseColor("#2D3239"))
                } else {
                    btnRoute.setTextColor(Color.parseColor("#C7CCD1"))
                }


                if (s.toString().toUpperCase(Locale.ROOT) != order.typeTransportation?.toUpperCase(
                        Locale.ROOT
                    )
                ) {
                    order.typeTransportation=s.toString()
                    orderPresenter.changeTypeTransortation(order)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //TODO("Not yet implemented")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //TODO("Not yet implemented")
            }

        })

        //Скрываем пункты меню
        setVisibleMenuItems(false)

        // Вызываем всплавающую подсказку
        checkStateOrder(order)

    }

    private fun checkStateOrder(order: Orders) {
        Timber.d("checkStateOrder")
        Timber.d("order.status=${order.status}")
        if (order.status=="Открыта" || order.status=="В пути") {

            val mbsOrderState=rootView.findViewById<MaterialBetterSpinner>(R.id.order_state)

            Timber.d("pw=$pw")
            if (pw==null) {
                val viewPopupWindow = View.inflate(rootView.context,R.layout.popup_orders_state,null)
                pw=PopupWindow(viewPopupWindow, LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT, false)
                pw?.showAsDropDown(mbsOrderState,0,-20)
            } else {
                pw?.showAsDropDown(mbsOrderState,0,-20)
            }
        }
    }

    private fun setVisibleMenuItems(visible: Boolean) {
        val searchItem=(this.activity as MainActivity).menu.findItem(R.id.action_search)
        if (searchItem!=null) {
            searchItem.isVisible=visible
        }
        val filterItem=(this.activity as MainActivity).menu.findItem(R.id.menu_buttons)
        if (filterItem!=null) {
            filterItem.isVisible=visible
        }
        val filterDate=(this.activity as MainActivity).menu.findItem(R.id.date_filter)
        if (filterDate!=null) {
            filterDate.isVisible=visible
        }
        val filterState=(this.activity as MainActivity).menu.findItem(R.id.status_filter)
        if (filterState!=null) {
            filterState.isVisible=visible
        }
    }


    fun changeColorMBSState(view: MaterialBetterSpinner, status:String?) {
        when (status) {
            getString(R.string.status_CANCEL) -> {
                view.setTextColor(Color.parseColor("#E94435"))
            }
            getString(R.string.status_TESTED) -> {
                view.setTextColor(Color.parseColor("#B370D7"))
            }
            getString(R.string.status_DONE) -> {
                view.setTextColor(Color.parseColor("#3DB650"))
            }
            getString(R.string.status_PAUSED) -> {
                view.setTextColor(Color.parseColor("#A5AEB6"))
            }
            getString(R.string.status_IN_WORK) -> {
                view.setTextColor(Color.parseColor("#309EFD"))
            }
            getString(R.string.status_IN_WAY) -> {
                view.setTextColor(Color.parseColor("#F28D17"))
            }
            getString(R.string.status_OPEN) -> {
                view.setTextColor(Color.parseColor("#56D5BE"))
            }

        }
    }


    override fun dataIsLoaded(checkup: Checkup) {
        this.checkup=checkup
        photoHelper.parentFragment=this
    }



    override fun showCheckupMessage(resID: Int) {
        toaster.showToast(resID)
        // Отправляем данные на сервер
        (this.activity as MainActivity).mainPresenter.sendData3(currentOrder.id)
    }

    override fun setAnsweredCount(count: Int) {
        Timber.d("setAnsweredCount")

        //rootView.findViewWithTag<TextView>("countQuestionChecklist").text="${currentOrder.questionCount}/${count}"
        rootView.findViewWithTag<TextView>("countQuestionChecklist").text=getString(R.string.count_question_checklist,currentOrder.questionCount,count)
        currentOrder.answeredCount=count

    }

    override fun techParamsLoaded(techParams: List<TechParams>) {
        Timber.d("techParamsLoaded $techParams")
        this.techParams=techParams
    }


    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.mbSaveCheckup -> {
                    Timber.d("mbSaveCheckup")
                    mbSaveCheckup.isEnabled=false // Блокируем кнопку пока данные не уйдут
                    // Всегда сохраняем основные данные по заявке
                    if (errorControls.isEmpty()) {
                        checkupPresenter.saveGeneralInformationOrder(currentOrder)
                        //doSaveCheckup()
                    } else {
                        toaster.showErrorToast(R.string.error_checklist_contains)
                    }

                }
            }
        }
    }

    override fun onDestroy() {
        Timber.d("CheckupFragment_onDestroy")
        super.onDestroy()
        checkupPresenter.onDestroy()
    }


    private fun checkPermission() {
        Timber.d("checkPermission")
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

    override fun onPause() {
        Timber.d("CheckupFragment_onPause")
        super.onPause()
        pw?.dismiss()
        //(activity as MainActivity).currentOrder=currentOrder включать нельзя иначе в onBackPressed не отрабатывает сброс currentOrder
    }

    override fun onDetach() {
        Timber.d("CheckupFragment_onDetach")
        super.onDetach()
    }

    override fun onDestroyView() {
        Timber.d("CheckupFragment_onDestroyView")
        super.onDestroyView()
        setVisibleMenuItems(true) // Возвращаем видимость меню
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

    override fun onStop() {
        Timber.d("CheckupFragment_onStop")
        super.onStop()
    }


    fun setPhotoResult(controlId: Int?, photoDir: String) {
        Timber.d("setPhotoResult_from_fragment $controlId")
        if (controlId!=null) {
            var linearLayout: LinearLayout?=null
            try {
                linearLayout=rootView.findViewById(controlId)
            } catch (e: Throwable) {
                e.printStackTrace()
                errorReceived(e)
            }

            // Обновим список с фото
            if (linearLayout!=null) {
                val images = OtherUtil().getFilesFromDir("$DCIM_DIR/PhotoForApp/$photoDir")
                refreshPhotoViewer(linearLayout, images, rootView.context)
            }
        }
    }

    fun refreshPhotoViewer(v: View, images: List<String>, ctx: Context) {
        Timber.d("refreshPhotoViewer__")
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

    private fun setCheckup(order: Orders) {
        Timber.d("showCheckupOrder ${order.status}")
        currentOrder=order
        checkupPresenter.loadCheckupByOrder(order.id)
    }

    fun isCheckupInitialized() =::checkup.isInitialized

    override fun errorReceived(throwable: Throwable) {
        Timber.d("errorReceived222")
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
                toaster.showErrorToast("${throwable.message}")
            }
        }

    }

    override fun sendGiOrder() {
        (this.activity as MainActivity).mainPresenter.sendGiOrder(currentOrder.id)
    }

    override fun doSaveCheckup() {
        if (uiCreator!=null) {
            Timber.d("checkupPresenter.saveCheckup")

            val changedCheckupCount = uiCreator!!.controlList.filter { it.answered }.size
            if (changedCheckupCount!=0) {
                checkupPresenter.saveCheckup(uiCreator!!)
            } else {
                toaster.showToast(R.string.checklist_not_changed)
                (parentFragment?.requireActivity() as MainActivity).enabledSaveButton()
            }

        } else {
            toaster.showToast(R.string.checklist_not_loaded)
        }
    }

}