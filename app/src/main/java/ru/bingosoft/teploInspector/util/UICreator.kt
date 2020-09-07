package ru.bingosoft.teploInspector.util

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Environment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.util.Const.Dialog.DIALOG_DATE
import ru.bingosoft.teploInspector.util.Const.Dialog.DIALOG_TIME
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

/**
 * Класс, который создает интерфейс Фрагмента Обследования
 */
//private val rootView: View, val checkup: Checkup, private val photoHelper: PhotoHelper, private val checkupPresenter: CheckupPresenter
class UICreator(val parentFragment: CheckupFragment, val checkup: Checkup) {
    lateinit var controlList: List<Models.TemplateControl>
    val dateAndTime=Calendar.getInstance()

    private val photoHelper=parentFragment.photoHelper
    private var rootView:View? =null

    fun create(rootView: View) {
        this.rootView=rootView
        val enabled= !(parentFragment.currentOrder.status==parentFragment.getString(R.string.status_IN_WAY)||
                parentFragment.currentOrder.status==parentFragment.getString(R.string.status_OPEN))

        // Возможно чеклист был ранее сохранен, тогда берем сохраненный и восстанавливаем его
        val listType: Type = object : TypeToken<List<Models.TemplateControl?>?>() {}.type

        controlList = if (checkup.textResult != null) {
            Gson().fromJson(checkup.textResult, listType)
        } else {
            Timber.d("checkup.text=${checkup.text}")
            Gson().fromJson(checkup.text, listType)
        }


        controlList.forEach controls@ { it ->
            // Для всех зависимых контролов сохраним его родителя
            /*if (parent!=null) {
                Timber.d("parent!=null")
                Timber.d("parent=${parent.id}_${parent.checked}_${parent.type}")
                it.parent=parent
            }*/
            when (it.type) {
                // Выпадающий список
                "combobox"->{
                    Timber.d("генерим combobox")

                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_material_spinner, rootView.parent as ViewGroup?, false) as LinearLayout

                    //attachListenerToFab(templateStep,it)

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val materialSpinner=templateStep.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)

                    doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))

                    val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
                        rootView.context,
                        R.layout.template_multiline_spinner_item,
                        it.value
                    )

                    materialSpinner.isEnabled = enabled
                    if (enabled) {
                        materialSpinner.dropDownHeight = WindowManager.LayoutParams.WRAP_CONTENT
                    } else {
                        materialSpinner.dropDownHeight = 0
                    }

                    // Заполним spinner
                    materialSpinner.setAdapter(spinnerArrayAdapter)

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    if (!it.resvalue.isNullOrEmpty()){
                        materialSpinner.setText(it.resvalue)
                    }
                    // Вешаем обработчик на spinner последним, иначе сбрасывается цвет шага
                    materialSpinner.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    return@controls

                }
                "date"->{
                    Timber.d("генерим date")
                    // Поле Дата
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_date, rootView.parent as ViewGroup?, false) as LinearLayout

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
                    textInputLayout.hint=it.hint
                    textInputLayout.isEnabled = enabled

                    val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)

                    textInputEditText.setOnClickListener {
                        showDialog(DIALOG_DATE, textInputEditText)
                    }

                    doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    if (!it.resvalue.isNullOrEmpty()){
                        textInputEditText.setText(it.resvalue)
                    }

                    textInputEditText.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    return@controls
                }
                "time"->{
                    Timber.d("генерим time")
                    // Поле Дата
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_time, rootView.parent as ViewGroup?, false) as LinearLayout

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
                    textInputLayout.hint=it.hint
                    textInputLayout.isEnabled = enabled

                    val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)

                    textInputEditText.setOnClickListener {
                        showDialog(DIALOG_TIME, textInputEditText)
                    }

                    doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    if (!it.resvalue.isNullOrEmpty()){
                        textInputEditText.setText(it.resvalue)
                    }

                    textInputEditText.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    return@controls
                }
                // Строковое поле ввода однострочное
                "textinput"->{
                    // Строковое поле однострочное
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_textinput, rootView.parent as ViewGroup?, false) as LinearLayout


                    //attachListenerToFab(templateStep,it)

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
                    textInputLayout.hint=it.hint
                    textInputLayout.isEnabled = enabled


                    val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)


                    doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    if (!it.resvalue.isNullOrEmpty()){
                        textInputEditText.setText(it.resvalue)
                    }
                    // Вешаем обработчик на textInputEditText последним, иначе сбрасывается цвет шага
                    textInputEditText.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    return@controls
                }
                // Числовое поле
                "numeric" -> {
                    Timber.d("генерим numeric")
                    val templateStep = LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_numeric, rootView.parent as ViewGroup?, false
                    ) as LinearLayout


                    templateStep.id = it.id
                    templateStep.findViewById<TextView>(R.id.question).text = it.question

                    val textInputLayout = templateStep.findViewById<TextInputLayout>(R.id.til)
                    textInputLayout.hint = it.hint
                    textInputLayout.isEnabled = enabled

                    val textInputEditText = templateStep.findViewById<TextInputEditText>(R.id.tiet)

                    doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    //Timber.d("it.checked=${it.answered}")
                    /*if (it.error) {
                        changeChecked(templateStep, it) // Установим цвет шага
                    }*/
                    if (!it.resvalue.isNullOrEmpty()) {
                        textInputEditText.setText(it.resvalue)
                    }
                    // Вешаем обработчик на textInputEditText последним, иначе сбрасывается цвет шага
                    textInputEditText.addTextChangedListener(
                        TextWatcherHelper(
                            it,
                            this,
                            templateStep
                        )
                    )

                    return@controls
                }

                "photo"->{
                    // контрол с кнопкой для фото
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_photo2, rootView.parent as ViewGroup?, false) as LinearLayout

                    //attachListenerToFab(templateStep,it)

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question
                    templateStep.tag=it

                    doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    Timber.d("Фото11 ${it.resvalue}")
                    /*if (it.resvalue.isNotEmpty()){
                        Timber.d("Фото ${it.resvalue}")
                        templateStep.findViewById<TextView>(R.id.photoResult).text=parentFragment.getString(R.string.photoResult,"DCIM\\PhotoForApp\\${it.resvalue}")
                    }*/

                    // Обработчик для кнопки "Добавить фото"
                    val btnPhoto=templateStep.findViewById<MaterialButton>(R.id.btnPhoto)
                    btnPhoto.isEnabled = enabled
                    val stepCheckup=it
                    btnPhoto.tag=templateStep
                    btnPhoto.setOnClickListener{
                        Timber.d("Добавляем фото")
                        val ts=it.tag
                        //val tc=((ts as View).tag as Models.TemplateControl)

                        // Сбрасываем признак Checked
                        val curOrder=(parentFragment.activity as MainActivity).currentOrder
                        (parentFragment.requireActivity() as MainActivity).photoStep=stepCheckup // Сохраним id контрола для которого делаем фото
                        (parentFragment.requireActivity() as MainActivity).photoDir="${curOrder.guid}/${stepCheckup.guid}" // Сохраним id контрола для которого делаем фото
                        photoHelper.createPhoto(curOrder.guid, stepCheckup)
                    }

                    val btnClearAll =
                        templateStep.findViewById<MaterialButton>(R.id.btnPhotoDeleteAll)
                    btnClearAll.isEnabled = enabled
                    btnClearAll.setOnClickListener {
                        Timber.d("Удалим все фото")
                    }

                    val images: List<String>
                    images = if (!it.resvalue.isNullOrEmpty()) {
                        Timber.d("Фото ${it.resvalue}")
                        // Обновим список с фото
                        val curOrder=(parentFragment.activity as MainActivity).currentOrder
                        val stDir = "PhotoForApp/${curOrder.guid}/${stepCheckup.guid}"
                        val storageDir =
                            File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                                stDir
                            )

                        OtherUtil().getFilesFromDir("$storageDir")

                    } else {
                        listOf()
                    }

                    val leftBtn = templateStep.findViewById(R.id.left_nav) as ImageButton
                    val rightBtn = templateStep.findViewById(R.id.right_nav) as ImageButton

                    // Обновим вьювер с фотками
                    parentFragment.refreshPhotoViewer(templateStep, images, rootView.context)

                    val pager = templateStep.findViewById(R.id.pager) as ViewPager
                    val myList = templateStep.findViewById(R.id.recyclerviewFrag) as RecyclerView

                    leftBtn.setOnClickListener {
                        var tab = pager.currentItem
                        if (tab > 0) {
                            tab--
                            pager.currentItem = tab
                        } else if (tab == 0) {
                            pager.currentItem = tab
                        }
                    }

                    rightBtn.setOnClickListener {
                        var tab = pager.currentItem
                        tab++
                        pager.currentItem = tab
                    }

                    pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                        override fun onPageScrollStateChanged(state: Int) {

                        }

                        override fun onPageScrolled(
                            position: Int,
                            positionOffset: Float,
                            positionOffsetPixels: Int
                        ) {

                        }

                        override fun onPageSelected(position: Int) {
                            myList.scrollToPosition(position)
                        }

                    })

                    return@controls
                }


                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
                    return@controls
                }
            }
        }

    }

    fun refresh() {
        val llMain= rootView?.findViewById(R.id.llMain) as LinearLayout
        if(llMain.childCount>0) {
            llMain.removeAllViews()
            rootView?.let { create(it) }
        }

    }

    /*private fun clearCheckup(checkup: Checkup): Checkup {
        val newcheckup=checkup
        checkup.
        return
    }*/

    private fun refreshCheckupViewer(v: View, control:Models.TemplateControl) {
        Timber.d("refreshCheckupViewer")
        val pager = v.findViewById(R.id.viewPager) as ViewPager
        val checkupCount = (v.parent as LinearLayout).findViewById(R.id.countCheckup) as TextView

        /*val checkups=control.subcheckup
        if (checkups!=null) {
            checkupCount.text = checkups.size.toString()
        } else {
            checkupCount.text ="0"
        }*/


        val adapter =
            CheckupPagerAdapter(
                control,
                parentFragment
            )
        pager.adapter = adapter


        pager.offscreenPageLimit = 4 // сколько чеклистов загружать в память
    }

    /**
     * Метод, в котором осуществляется привязка дочернего View к родительскому
     */
    private fun doAssociateParent(v: View, mainView: View, index: Int?=null){
        if (mainView is LinearLayout) {
            if (index!=null) {
                mainView.addView(v,index)
            } else {
                mainView.addView(v)
            }

        }
    }

    /**
     * Найдем кнопку Fab и добавим для нее обработчик
     */
    private fun attachListenerToFab(v: View, control: Models.TemplateControl) {
        Timber.d("attachListenerToFab")

        val fab: FloatingActionButton = v.findViewById(R.id.fabCheck)
        fab.setOnClickListener {
            Timber.d("Прошли шаг чеклиста")

            var isEmpty=false

            // Проверим введено ли значение
            when (control.type) {
                "combobox"->{
                    val controlView=v.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)
                    if (TextUtils.isEmpty(controlView.text.toString())) {
                        controlView.error = "Нужно выбрать значение из списка"
                        isEmpty=true
                    }
                }
                "multilevel_combobox"->{
                    val controlView=v.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)
                    if (TextUtils.isEmpty(controlView.text.toString())) {
                        controlView.error = "Нужно выбрать значение из списка"
                        isEmpty=true
                    }
                    val controlView2=v.findViewById<MaterialBetterSpinner>(R.id.subspinner)
                    if (TextUtils.isEmpty(controlView2.text.toString())) {
                        controlView2.error = "Нужно выбрать значение из списка"
                        isEmpty=true
                    }
                }
                "textinput"->{
                    val controlView=v.findViewById<TextInputEditText>(R.id.tiet)
                    if (TextUtils.isEmpty(controlView.text.toString())) {
                        controlView.error = "Нужно заполнить поле"
                        isEmpty=true
                    }
                }
                /*"photo"->{
                    // Проверим были ли добавлены фото
                    val curOrder=(parentFragment.activity as MainActivity).currentOrder
                    val controlViewError=v.findViewById<TextView>(R.id.errorPhoto)
                    if (photoHelper.checkDirAndEmpty("${curOrder.guid}/${checkup.guid}/${control.guid}")) {
                        Timber.d("Папка есть, она не пуста")
                        controlViewError.visibility=View.INVISIBLE
                        // Сохраним filemap в resValue
                        //control.resvalue="${checkup.guid}/${control.guid}"

                        Timber.d(control.resvalue)

                    } else {
                        Timber.d("Папка с фото отсутствует")
                        controlViewError.visibility=View.VISIBLE
                        isEmpty=true
                    }
                }*/

                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
                }

            }

            /*if (!isEmpty) {
                //control.checked=!control.checked
                changeChecked(v,control)

                // Обновим контрол для зависимого чеклиста
                val parent=control.parent
                if (parent!=null) {
                    Timber.d("control.parent=${parent}")
                    Timber.d("group_${parent.groupControlList?.list?.get(0)?.list}")
                    parent.groupControlList?.list?.forEach {
                        val index=it.list.indexOf(control)
                        if (index>-1) {
                            Timber.d("НАШЛИ!!")
                            it.list[index] = control
                        }
                    }
                } else {
                    Timber.d("parent==null")
                }
            }*/
        }
    }

    private fun showDialog(id: Int, textInputEditText: TextInputEditText) {
        if (id== DIALOG_DATE) {
            val dateListener = object :DatePickerDialog.OnDateSetListener{
                override fun onDateSet(
                    view: DatePicker?,
                    year: Int,
                    month: Int,
                    dayOfMonth: Int
                ) {
                    dateAndTime.set(Calendar.YEAR, year)
                    dateAndTime.set(Calendar.MONTH, month)
                    dateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    textInputEditText.setText(
                        SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).format(dateAndTime.time)
                    )
                }

            }

            DatePickerDialog(parentFragment.requireContext(),
                dateListener,
                dateAndTime.get(Calendar.YEAR),
                dateAndTime.get(Calendar.MONTH),
                dateAndTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        } else {
            val timeListener = object : TimePickerDialog.OnTimeSetListener{
                override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                    dateAndTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    dateAndTime.set(Calendar.MINUTE, minute)
                    textInputEditText.setText(
                        SimpleDateFormat("HH:mm", Locale("ru","RU")).format(dateAndTime.time)
                    )
                }
            }

            TimePickerDialog(parentFragment.requireContext(),
                timeListener,
                dateAndTime.get(Calendar.HOUR_OF_DAY),
                dateAndTime.get(Calendar.MINUTE), true
            ).show()
        }
    }


    /**
     * Сменим цвет шага
     */
    /*fun changeChecked(v: View, control: Models.TemplateControl) {
        val cardView = v.findViewById<CardView>(R.id.cv)
        if (control.checked) {
            cardView?.setCardBackgroundColor(
                ContextCompat.getColor(
                    v.context,
                    R.color.colorCardSelect
                )
            )
        } else {
            cardView?.setCardBackgroundColor(
                ContextCompat.getColor(
                    v.context,
                    R.color.colorCardItem
                )
            )
        }
    }*/

    /*private fun parseLatLng(str: String): LatLng {
        Timber.d("parseLatLng $str")
        val lat=str.substring(str.indexOf("latitude=")+9,str.indexOf(", longitude")).toDouble()
        val lon=str.substring(str.indexOf("longitude=")+10,str.indexOf(", altitude")).toDouble()
        return LatLng(lat, lon)
    }*/



}