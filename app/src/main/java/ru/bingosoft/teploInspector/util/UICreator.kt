package ru.bingosoft.teploInspector.util

import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner
import ru.bingosoft.teploInspector.R
import ru.bingosoft.teploInspector.db.Checkup.Checkup
import ru.bingosoft.teploInspector.models.Models
import ru.bingosoft.teploInspector.ui.checkup.CheckupFragment
import ru.bingosoft.teploInspector.ui.mainactivity.MainActivity
import ru.bingosoft.teploInspector.ui.map.MapFragment
import timber.log.Timber
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Класс, который создает интерфейс Фрагмента Обследования
 */
//private val rootView: View, val checkup: Checkup, private val photoHelper: PhotoHelper, private val checkupPresenter: CheckupPresenter
class UICreator(val parentFragment: CheckupFragment, val checkup: Checkup) {
    lateinit var controlList: Models.ControlList

    private val photoHelper=parentFragment.photoHelper

    fun create(rootView: View, controls:Models.ControlList?=null, parent: Models.TemplateControl?=null): Models.ControlList {
        // Возможно чеклист был ранее сохранен, тогда берем сохраненный и восстанавливаем его
        controlList = if (controls==null) {
            //Timber.d("checkup22=${checkup}")
            //Timber.d("checkup.textResult11=${checkup.textResult}")
            if (checkup.textResult!=null){
                //Timber.d("сюда")
                Gson().fromJson(checkup.textResult, Models.ControlList::class.java)
            } else {
                Timber.d("checkup=$checkup")
                if (checkup.text!=null) {
                    Gson().fromJson(checkup.text, Models.ControlList::class.java)
                } else {
                    Gson().fromJson("", Models.ControlList::class.java)
                }
            }
        } else {
            controls
        }


        //val rootView=parentFragment.root
        val checkupPresenter=parentFragment.checkupPresenter

        controlList.list.forEach controls@ { it ->
            // Для всех зависимых контролов сохраним его родителя
            if (parent!=null) {
                Timber.d("parent!=null")
                Timber.d("parent=${parent.id}_${parent.checked}_${parent.type}")
                it.parent=parent
            }
            when (it.type) {
                // Выпадающий список
                "combobox"->{
                    Timber.d("генерим combobox")

                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_material_spinner, rootView.parent as ViewGroup?, false) as LinearLayout

                    attachListenerToFab(templateStep,it)

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val materialSpinner=templateStep.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)

                    doAssociateParent(templateStep, rootView.findViewById(R.id.mainControl))

                    val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
                        rootView.context,
                        R.layout.template_multiline_spinner_item,
                        it.value
                    )

                    // Заполним spinner
                    materialSpinner.setAdapter(spinnerArrayAdapter)

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    Timber.d("it.checked=${it.checked}")
                    if (it.checked) {
                        changeChecked(templateStep,it) // Установим цвет шага
                    }
                    if (it.resvalue.isNotEmpty()){
                        materialSpinner.setText(it.resvalue)
                    }
                    // Вешаем обработчик на spinner последним, иначе сбрасывается цвет шага
                    materialSpinner.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    return@controls

                }
                // Выпадающий список
                "multilevel_combobox"->{
                    Timber.d("генерим multilevel_combobox")

                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_multilevel_spinner, rootView.parent as ViewGroup?, false) as LinearLayout


                    attachListenerToFab(templateStep,it)

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val materialSpinner=templateStep.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)
                    val materialSubSpinner=templateStep.findViewById<MaterialBetterSpinner>(R.id.subspinner)

                    doAssociateParent(templateStep, rootView.findViewById(R.id.mainControl))

                    val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
                        rootView.context,
                        R.layout.template_multiline_spinner_item,
                        it.value
                    )

                    // Заполним основной spinner
                    materialSpinner.setAdapter(spinnerArrayAdapter)


                    val subspinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
                        rootView.context,
                        R.layout.template_multiline_spinner_item,
                        arrayListOf()
                    )
                    // Запоним пустым значением, иначе краш
                    materialSubSpinner.setAdapter(subspinnerArrayAdapter)


                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    Timber.d("it.checked=${it.checked}")
                    if (it.checked) {
                        changeChecked(templateStep,it) // Установим цвет шага
                    }
                    if (it.resvalue.isNotEmpty()){
                        materialSpinner.setText(it.resvalue)
                    }
                    // Вешаем обработчик на spinner последним, иначе сбрасывается цвет шага
                    //materialSpinner.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    materialSpinner.setOnItemClickListener(object:AdapterView.OnItemClickListener {
                        override fun onItemClick(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val spinnerSubArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
                                rootView.context,
                                R.layout.template_multiline_spinner_item,
                                it.subvalue[position].value
                            )
                            // Запоним пустым значением, иначе краш
                            materialSubSpinner.setAdapter(spinnerSubArrayAdapter)
                        }

                    })


                    // Вешаем обработчик на второй spinner его значение уйдет на сервер
                    materialSubSpinner.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    return@controls

                }
                // Строковое поле ввода однострочное
                "textinput"->{
                    // Строковое поле однострочное
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_textinput, rootView.parent as ViewGroup?, false) as LinearLayout


                    attachListenerToFab(templateStep,it)

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
                    textInputLayout.hint=it.hint


                    val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)


                    doAssociateParent(templateStep, rootView.findViewById(R.id.mainControl))

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    Timber.d("it.checked=${it.checked}")
                    if (it.checked) {
                        changeChecked(templateStep,it) // Установим цвет шага
                        Timber.d("it.resvalue=${it.resvalue}")

                    }
                    if (it.resvalue.isNotEmpty()){
                        textInputEditText.setText(it.resvalue)
                    }
                    // Вешаем обработчик на textInputEditText последним, иначе сбрасывается цвет шага
                    textInputEditText.addTextChangedListener(TextWatcherHelper(it,this,templateStep))

                    return@controls
                }
                "photo"->{
                    // контрол с кнопкой для фото
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_photo2, rootView.parent as ViewGroup?, false) as LinearLayout

                    attachListenerToFab(templateStep,it)

                    templateStep.id=it.id
                    templateStep.findViewById<TextView>(R.id.question).text=it.question
                    templateStep.tag=it

                    doAssociateParent(templateStep, rootView.findViewById(R.id.mainControl))

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    if (it.checked) {
                        changeChecked(templateStep,it) // Установим цвет шага
                    }

                    Timber.d("Фото11 ${it.resvalue}")
                    /*if (it.resvalue.isNotEmpty()){
                        Timber.d("Фото ${it.resvalue}")
                        templateStep.findViewById<TextView>(R.id.photoResult).text=parentFragment.getString(R.string.photoResult,"DCIM\\PhotoForApp\\${it.resvalue}")
                    }*/

                    // Обработчик для кнопки "Добавить фото"
                    val btnPhoto=templateStep.findViewById<MaterialButton>(R.id.btnPhoto)
                    val stepCheckup=it
                    btnPhoto.tag=templateStep
                    btnPhoto.setOnClickListener{
                        Timber.d("Добавляем фото")
                        val ts=it.tag
                        val tc=((ts as View).tag as Models.TemplateControl)

                        // Сбрасываем признак Checked
                        if (tc.checked==true) {
                            tc.checked=!tc.checked
                            changeChecked(ts,tc)
                        }

                        val curOrder=(parentFragment.activity as MainActivity).currentOrder
                        (parentFragment.requireActivity() as MainActivity).photoStep=stepCheckup // Сохраним id контрола для которого делаем фото
                        (parentFragment.requireActivity() as MainActivity).photoDir="${curOrder.guid}/${checkup.guid}/${stepCheckup.guid}" // Сохраним id контрола для которого делаем фото
                        photoHelper.createPhoto("${curOrder.guid}/${checkup.guid}", stepCheckup)
                    }

                    val btnClearAll =
                        templateStep.findViewById<MaterialButton>(R.id.btnPhotoDeleteAll)
                    btnClearAll.setOnClickListener {
                        Timber.d("Удалим все фото")
                    }

                    val images: List<String>
                    images = if (it.resvalue.isNotEmpty()) {
                        Timber.d("Фото ${it.resvalue}")
                        // Обновим список с фото
                        val curOrder=(parentFragment.activity as MainActivity).currentOrder
                        val stDir = "PhotoForApp/${curOrder.guid}/${checkup.guid}/${stepCheckup.guid}"
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
                "map_coordinates"->{
                    // контрол с кнопкой, открываем карту и забираем с нее координаты клика
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_map_coordinates, rootView.parent as ViewGroup?, false) as LinearLayout

                    attachListenerToFab(templateStep,it)

                    Timber.d("it.id=${it.id}")
                    templateStep.id=it.id
                    Timber.d("templateStep.id=${templateStep.id}")
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    doAssociateParent(templateStep, rootView.findViewById(R.id.mainControl))

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    if (it.checked) {
                        changeChecked(templateStep,it) // Установим цвет шага
                    }
                    if (it.resvalue.isNotEmpty()){
                        Timber.d("map_coordinates=${it.resvalue}")
                        val point=Gson().fromJson(it.resvalue, Models.MapPoint::class.java)
                        templateStep.findViewById<TextView>(R.id.mapCoordinatesResult).text=parentFragment.getString(R.string.coordinates,BigDecimal(point.lat!!).setScale(5,RoundingMode.HALF_EVEN),BigDecimal(point.lon!!).setScale(5,RoundingMode.HALF_EVEN))

                    } else {
                        // Возьмем координаты от Activity
                        val controlId=(parentFragment.requireActivity() as MainActivity).controlMapId
                        if (it.id==controlId) {
                            val point=(parentFragment.requireActivity() as MainActivity).mapPoint
                            templateStep.findViewById<TextView>(R.id.mapCoordinatesResult).text=parentFragment.getString(R.string.coordinates,BigDecimal(point.latitude).setScale(5,RoundingMode.HALF_EVEN),BigDecimal(point.longitude).setScale(5,RoundingMode.HALF_EVEN))

                            val mapPoint=Models.MapPoint(point.latitude,point.longitude)

                            it.resvalue=Gson().toJson(mapPoint)
                            Timber.d("mapPoint=${it.resvalue}")
                        }
                    }

                    // Обработчик для кнопки "Добавить координаты"
                    val btnMap=templateStep.findViewById<MaterialButton>(R.id.btnMap)
                    btnMap.setOnClickListener{
                        Timber.d("Добавляем координаты")

                        // Сохраним текущее состояние чеклиста
                        checkupPresenter.saveCheckup(this)

                        //Открываем карту
                        val bundle = Bundle()
                        bundle.putBoolean("addCoordinates", true)
                        bundle.putLong("checkupId", checkup.id)
                        bundle.putInt("controlId", templateStep.id)

                        val fragmentMap= MapFragment()
                        fragmentMap.arguments=bundle
                        val fragmentManager=parentFragment.requireActivity().supportFragmentManager

                        fragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, fragmentMap, "fragment_map_tag")
                            .addToBackStack(null)
                            .commit()

                        fragmentManager.executePendingTransactions()

                    }

                    return@controls
                }
                "group_questions"->{
                    // контрол с зависимым чеклистом
                    val templateStep=LayoutInflater.from(rootView.context).inflate(
                        R.layout.template_subcheckup, rootView.parent as ViewGroup?, false) as LinearLayout

                    attachListenerToFab(templateStep,it)

                    Timber.d("it.id=${it.id}")
                    templateStep.id=it.id
                    templateStep.tag=it

                    Timber.d("templateStep.id=${templateStep.id}")
                    templateStep.findViewById<TextView>(R.id.question).text=it.question

                    val clCheckupsPager=templateStep.findViewById<ConstraintLayout>(R.id.clCheckupsPager)

                    // Если шаг чеклиста был ранее сохранен восстановим значение
                    if (it.checked) {
                        changeChecked(templateStep,it) // Установим цвет шага
                    }

                    doAssociateParent(templateStep, rootView.findViewById(R.id.mainControl))

                    if (it.resvalue.isNotEmpty()){
                        val controlsForPages=Gson().fromJson(it.resvalue, Models.CommonControlList::class.java)
                        val subcheckup= mutableListOf<Checkup>()
                        controlsForPages.list.forEach{controls ->
                            val gson= GsonBuilder()
                                .excludeFieldsWithoutExposeAnnotation()
                                .create()
                            val resValue=gson.toJson(controls,Models.ControlList::class.java)
                            val checkup=Checkup(textResult = Gson().fromJson(resValue, JsonObject::class.java))
                            subcheckup.add(checkup)
                        }

                        it.subcheckup=subcheckup
                        refreshCheckupViewer(clCheckupsPager, it)

                    } else {
                        Timber.d("Нет сохраненных результатов")
                        refreshCheckupViewer(clCheckupsPager, it)
                    }


                    val leftBtn = templateStep.findViewById(R.id.left_nav) as ImageButton
                    val rightBtn = templateStep.findViewById(R.id.right_nav) as ImageButton

                    val pager = templateStep.findViewById(R.id.viewPager) as ViewPager

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


                    // Обработчик для кнопки "Добавлям новый чеклист"
                    Timber.d("it=${it.multiplicity}")
                    if (it.multiplicity==1) {
                        val btnNewStep=templateStep.findViewById<MaterialButton>(R.id.addNewStep)
                        //Покажим панель с кнопками
                        val layoutBtn=templateStep.findViewById<LinearLayout>(R.id.llbtnConteiner)
                        val params=layoutBtn.layoutParams
                        params.height=LinearLayout.LayoutParams.WRAP_CONTENT
                        layoutBtn.layoutParams=params

                        btnNewStep.tag=templateStep
                        btnNewStep.setOnClickListener{
                            Timber.d("Добавлям новый чеклист")

                            val ts=it.tag
                            val tc=((ts as View).tag as Models.TemplateControl)

                            // Сбрасываем признак Checked
                            if (tc.checked==true) {
                                tc.checked=!tc.checked
                                changeChecked(ts,tc)
                            }


                            val subcheckupnew= mutableListOf<Checkup>()
                            val controlsForPages=tc.groupControlList
                            controlsForPages?.list?.forEach{ controls ->
                                val gson= GsonBuilder()
                                    .excludeFieldsWithoutExposeAnnotation()
                                    .create()
                                val resValue=gson.toJson(controls,Models.ControlList::class.java)
                                val checkup=Checkup(textResult = Gson().fromJson(resValue, JsonObject::class.java))
                                subcheckupnew.add(checkup)
                            }

                            val controls=Gson().fromJson(checkup.text,Models.ControlList::class.java)
                            val control=controls.list.filter { it.id==ts.id }

                            val newCheckup=control[0].subcheckup[0]
                            subcheckupnew.add(newCheckup) // Добавим еще один такой же
                            tc.subcheckup=subcheckupnew

                            refreshCheckupViewer(clCheckupsPager, tc)
                        }


                        val btnDeleteStep=templateStep.findViewById<MaterialButton>(R.id.deleteStep)
                        btnDeleteStep.tag=templateStep
                        btnDeleteStep.setOnClickListener{
                            Timber.d("Удалим чеклист")

                            // Получим текущую страницу
                            val indexPage=pager.currentItem
                            Timber.d("pager.adapter.count=${pager.adapter?.count}")
                            if (pager.adapter?.count!! >1) {
                                val ts=it.tag
                                val tc=((ts as View).tag as Models.TemplateControl)

                                val subcheckupnew= mutableListOf<Checkup>()
                                val controlsForPages=tc.groupControlList
                                Timber.d("tc.groupControlList=${tc.groupControlList?.list}")
                                controlsForPages?.list?.removeAt(indexPage)
                                controlsForPages?.list?.forEach{ controls ->
                                    val gson= GsonBuilder()
                                        .excludeFieldsWithoutExposeAnnotation()
                                        .create()
                                    val resValue=gson.toJson(controls,Models.ControlList::class.java)
                                    val checkup=Checkup(textResult = Gson().fromJson(resValue, JsonObject::class.java))
                                    subcheckupnew.add(checkup)
                                }

                                tc.subcheckup=subcheckupnew

                                refreshCheckupViewer(clCheckupsPager, tc)
                            }


                        }

                    } else {
                        //Скроем панель с кнопками
                        val layoutBtn=templateStep.findViewById<LinearLayout>(R.id.llbtnConteiner)
                        val params=layoutBtn.layoutParams
                        params.height=0
                        layoutBtn.layoutParams=params
                    }


                    return@controls
                }
                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
                    return@controls
                }
            }
        }

        return controlList

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

        val checkups=control.subcheckup
        if (checkups!=null) {
            checkupCount.text = checkups.size.toString()
        } else {
            checkupCount.text ="0"
        }


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
                "photo"->{
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
                }
                "map_coordinates"->{
                    val controlView=v.findViewById<TextView>(R.id.mapCoordinatesResult)
                    val controlViewError=v.findViewById<TextView>(R.id.errorMap)
                    if (!controlView.text.equals(parentFragment.resources.getString(R.string.not_coordinates))) {
                        Timber.d("координаты заданы")
                        controlViewError.visibility=View.INVISIBLE
                    } else {
                        Timber.d("координаты не заданы")
                        controlViewError.visibility=View.VISIBLE
                        isEmpty=true
                    }
                }
                "group_questions"->{
                    // Получим общий лиcт контролов, для фильтрации
                    val commonListControls= Models.ControlList()
                    Timber.d("control.controlList=${control.groupControlList}")
                    control.groupControlList?.list?.forEach {
                        commonListControls.list.addAll(it.list)
                    }

                    Timber.d("commonListControls=${commonListControls.list}")


                    //val notcheckedcontrol=control.controlList?.list?.filter { !it.checked }
                    val notcheckedcontrol= commonListControls.list.filter { !it.checked }
                    Timber.d("notcheckedcontrol=${notcheckedcontrol}")
                    //Timber.d("notcheckedcontrol=${notcheckedcontrol[0].resvalue}")
                    if (notcheckedcontrol.isNotEmpty()) {
                        Timber.d("isEmpty")
                        val tvError=v.findViewById<TextView>(R.id.errorSubcheckup)
                        tvError.visibility=View.VISIBLE
                        isEmpty=true
                    } else {
                        val tvError=v.findViewById<TextView>(R.id.errorSubcheckup)
                        tvError.visibility=View.INVISIBLE
                        // Сохраняем результат
                        val controlList2 = control.groupControlList
                        Timber.d("все отмечено!")
                        Timber.d("${controlList2?.list?.get(0)?.list?.get(0)?.resvalue}")
                        val gson= GsonBuilder()
                            .excludeFieldsWithoutExposeAnnotation()
                            .create()
                        val resCheckup =gson.toJson(controlList2)
                        control.resvalue=resCheckup.toString()
                    }

                }
                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
                }

            }

            if (!isEmpty) {
                control.checked=!control.checked
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
            }
        }
    }


    /**
     * Сменим цвет шага
     */
    fun changeChecked(v: View, control: Models.TemplateControl) {
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
    }

    /*private fun parseLatLng(str: String): LatLng {
        Timber.d("parseLatLng $str")
        val lat=str.substring(str.indexOf("latitude=")+9,str.indexOf(", longitude")).toDouble()
        val lon=str.substring(str.indexOf("longitude=")+10,str.indexOf(", altitude")).toDouble()
        return LatLng(lat, lon)
    }*/



}