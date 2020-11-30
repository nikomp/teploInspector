package ru.bingosoft.teploInspector.util

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.material.button.MaterialButton
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
import ru.bingosoft.teploInspector.util.Const.Photo.DCIM_DIR
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Класс, который создает интерфейс Фрагмента Обследования
 */
//private val rootView: View, val checkup: Checkup, private val photoHelper: PhotoHelper, private val checkupPresenter: CheckupPresenter
@Suppress("unused")
class UICreator(private val parentFragment: CheckupFragment, val checkup: Checkup) {
    lateinit var controlList: List<Models.TemplateControl>
    private var enabledControls: MutableList<View> = mutableListOf()
    private val dateAndTime: Calendar =Calendar.getInstance()

    private val photoHelper=parentFragment.photoHelper
    private lateinit var rootView:View
    private var enabled: Boolean=true
    private var llGroupNodes: LinearLayout?=null
    private var listllNode: List<LinearLayout> = listOf()
    private var listllArchHour: List<LinearLayout> = listOf()
    private var listllArchDaily: List<LinearLayout> = listOf()
    private var listllGroupOther: MutableList<LinearLayout> = mutableListOf()

    fun create(rootView: View) {
        this.rootView=rootView
        enabled= !(parentFragment.currentOrder.status==parentFragment.getString(R.string.status_IN_WAY)||
                parentFragment.currentOrder.status==parentFragment.getString(R.string.status_OPEN))

        // Возможно чеклист был ранее сохранен, тогда берем сохраненный и восстанавливаем его
        val listType: Type = object : TypeToken<List<Models.TemplateControl?>?>() {}.type

        controlList = if (checkup.textResult != null) {
            Gson().fromJson(checkup.textResult, listType)
        } else {
            Timber.d("checkup.text=${checkup.text}")
            Gson().fromJson(checkup.text, listType)
        }

        controlList.forEach controls@ {
            when (it.type) {
                "combobox","date","time","textinput","numeric","photo" -> {
                    Timber.d("генерим ${it.type}")
                    createWithGrouping(it)
                    return@controls
                }
                else -> {
                    Timber.d("Неизвестный элемент интерфейса")
                    return@controls
                }
            }

        }

    }

    private fun createWithGrouping(it: Models.TemplateControl) {
        if (it.replication_nodes==true) {
            // Создаем группу Узлы
            if (llGroupNodes==null) {
                llGroupNodes=createGroupNodes("Узлы")
            }
            // Создаем Узлы в группе
            if (listllNode.isEmpty()) {
                listllNode=createNodes(llGroupNodes!!)
            }

            if (it.group_checklist==null) {
                // Тиражируем по узлам
                if (it.node!=null) {
                    if (listllNode.isNotEmpty()) {
                        val llCurrentNode=listllNode[it.node-1]
                        when (it.type) {
                            "combobox" -> createCombobox(it,llCurrentNode)
                            "textinput" -> createTextInput(it,llCurrentNode)
                            "numeric" -> createNumeric(it,llCurrentNode)
                            "date" -> createDate(it,llCurrentNode)
                            "time" -> createTime(it,llCurrentNode)
                            "photo" -> createPhoto(it,llCurrentNode)
                        }
                    }
                }
            } else {
                Timber.d("it.group_checklist=${it.group_checklist}")
                // Есть еще и группа
                if (it.node!=null) {
                    if (listllNode.isNotEmpty()) {
                        val llCurrentNode=listllNode[it.node-1]
                        val llGroup=createGroup(it.group_checklist,llCurrentNode,it.node.toString())
                        when (it.type) {
                            "combobox" -> createCombobox(it, llGroup)
                            "textinput" -> createTextInput(it, llGroup)
                            "numeric" -> createNumeric(it, llGroup)
                            "date" -> createDate(it, llGroup)
                            "time" -> createTime(it, llGroup)
                            "photo" -> createPhoto(it, llGroup)
                        }
                    }
                }
            }

        } else {
            if (it.replicating_archival_records==true) {
                Timber.d("Тиражирование_Арх_Зап")
                // Создаем группу Архивные записи
                if (llGroupNodes==null) {
                    llGroupNodes=createGroupNodes("Архивные записи")
                }
                // Создаем группы Часовые, Суточные по 5 штук
                /*if (listllNode.isEmpty()) {
                    listllArchHour=createArchGroup(llGroupNodes!!,"Часовые")
                    listllArchDaily=createArchGroup(llGroupNodes!!,"Суточные")
                }*/

                if (it.group_checklist==null) {
                    // Тиражируем по узлам
                    /*if (it.archival_records!=null) {
                        if (listllArchHour.isNotEmpty()) {
                            val llCurrentNode=listllArchHour[it.archival_records-1]
                            when (it.type) {
                                "combobox" -> createCombobox(it,llCurrentNode)
                                "textinput" -> createTextInput(it,llCurrentNode)
                                "numeric" -> createNumeric(it,llCurrentNode)
                                "date" -> createDate(it,llCurrentNode)
                                "time" -> createTime(it,llCurrentNode)
                                "photo" -> createPhoto(it,llCurrentNode)
                            }
                        }
                    }*/
                } else {
                    Timber.d("it.group_checklist=${it.group_checklist}")
                    if (listllArchHour.isEmpty() && it.group_checklist=="Часовые") {
                        listllArchHour=createArchGroup(llGroupNodes!!,"Часовые")
                    }
                    if (listllArchDaily.isEmpty() && it.group_checklist=="Суточные") {
                        listllArchDaily=createArchGroup(llGroupNodes!!,"Суточные")
                    }
                    // Есть еще и группа
                    if (it.archival_records!=null) {
                        if (listllArchHour.isNotEmpty()) {
                            val llCurrentNode=listllArchHour[it.archival_records-1]
                            //val llGroup=createGroup(it.group_checklist,llCurrentNode,it.archival_records.toString())
                            when (it.type) {
                                "combobox" -> createCombobox(it, llCurrentNode)
                                "textinput" -> createTextInput(it, llCurrentNode)
                                "numeric" -> createNumeric(it, llCurrentNode)
                                "date" -> createDate(it, llCurrentNode)
                                "time" -> createTime(it, llCurrentNode)
                                "photo" -> createPhoto(it, llCurrentNode)
                            }
                        }
                        if (listllArchDaily.isNotEmpty()) {
                            val llCurrentNode=listllArchDaily[it.archival_records-1]
                            //val llGroup=createGroup(it.group_checklist,llCurrentNode,it.archival_records.toString())
                            when (it.type) {
                                "combobox" -> createCombobox(it, llCurrentNode)
                                "textinput" -> createTextInput(it, llCurrentNode)
                                "numeric" -> createNumeric(it, llCurrentNode)
                                "date" -> createDate(it, llCurrentNode)
                                "time" -> createTime(it, llCurrentNode)
                                "photo" -> createPhoto(it, llCurrentNode)
                            }
                        }
                    }
                }
            } else {
                // Тиражирования по узлам и по архивным записям нет
                if (it.group_checklist!=null) {
                    //..., но есть Группа
                    var llGroup=createGroup(it.group_checklist,rootView.findViewById(R.id.llMain))
                    // Проверим, возможно эта группа тиражируется по узлам (как ИТП(общий ввод))
                    Timber.d("_it=$it")
                    if (it.replicated_on!=null) {
                        Timber.d("it.replicated_on=${it.replicated_on}")
                        if (it.node_itp!=null) {
                            Timber.d("it.node_itp=${it.node_itp}")
                            llGroup=createGroup("Узел ${it.group_checklist} ${it.node_itp}",llGroup,it.node_itp)
                        }
                    }

                    when (it.type) {
                        "combobox" -> createCombobox(it, llGroup)
                        "textinput" -> createTextInput(it, llGroup)
                        "numeric" -> createNumeric(it, llGroup)
                        "date" -> createDate(it, llGroup)
                        "time" -> createTime(it, llGroup)
                        "photo" -> createPhoto(it, llGroup)
                    }

                } else {
                    // Ничего нет, ни тиражирования, ни группы
                    when (it.type) {
                        "combobox" -> createCombobox(it, rootView.findViewById(R.id.llMain))
                        "textinput" -> createTextInput(it, rootView.findViewById(R.id.llMain))
                        "numeric" -> createNumeric(it, rootView.findViewById(R.id.llMain))
                        "date" -> createDate(it, rootView.findViewById(R.id.llMain))
                        "time" -> createTime(it, rootView.findViewById(R.id.llMain))
                        "photo" -> createPhoto(it, rootView.findViewById(R.id.llMain))
                    }
                }
            }


        }
    }

    private fun createCombobox(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_material_spinner, rootView.parent as ViewGroup?, false) as LinearLayout

        //templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question
        setQuestionColor(it,templateStep)

        val materialSpinner=templateStep.findViewById<MaterialBetterSpinner>(R.id.android_material_design_spinner)

        doAssociateParent(templateStep, parent)

        //#Перенос_строк #Combobox
        // Проставим переносы в строку. Длина строки 40 символов
        val listLinesWithBreak= mutableListOf<String>()
        it.value.forEach {
            var item=it
            var newitem=""
            while (item.length > 40) {
                val spacepos = item.lastIndexOf(" ", 40) // ищем первый пробел начиная с 40 символа к началу строки
                val strtemp = item.substring(0, spacepos)
                newitem = newitem + strtemp+"\n"

                // Удалим из item подстроку
                item=item.replace(strtemp, "")
            }
            newitem += item
            listLinesWithBreak.add(newitem)
        }

        val spinnerArrayAdapter: ArrayAdapter<String> = ArrayAdapter(
            rootView.context,
            R.layout.template_multiline_spinner_item,
            listLinesWithBreak //it.value
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
        materialSpinner.addTextChangedListener(TextWatcherHelper(it, templateStep))
        enabledControls.add(materialSpinner)

    }

    private fun createTextInput(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_textinput, rootView.parent as ViewGroup?, false) as LinearLayout


        //attachListenerToFab(templateStep,it)

        templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question
        setQuestionColor(it,templateStep)


        val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint=it.hint
        textInputLayout.isEnabled = enabled


        val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)


        doAssociateParent(templateStep, parent)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            textInputEditText.setText(it.resvalue)
        }
        // Вешаем обработчик на textInputEditText последним, иначе сбрасывается цвет шага
        textInputEditText.addTextChangedListener(TextWatcherHelper(it, templateStep))

        enabledControls.add(textInputLayout)
    }

    private fun createNumeric(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep = LayoutInflater.from(rootView.context).inflate(
            R.layout.template_numeric, rootView.parent as ViewGroup?, false
        ) as LinearLayout


        templateStep.id = it.id
        templateStep.findViewById<TextView>(R.id.question).text = it.question
        setQuestionColor(it,templateStep)

        val textInputLayout = templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint = it.hint
        textInputLayout.isEnabled = enabled

        val textInputEditText = templateStep.findViewById<TextInputEditText>(R.id.tiet)

        doAssociateParent(templateStep, parent)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()) {
            textInputEditText.setText(it.resvalue)
        }
        // Вешаем обработчик на textInputEditText последним, иначе сбрасывается цвет шага
        textInputEditText.addTextChangedListener(
            TextWatcherHelper(
                it,
                templateStep
            )
        )

        enabledControls.add(textInputLayout)
    }

    private fun createDate(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_date, rootView.parent as ViewGroup?, false) as LinearLayout

        templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question
        setQuestionColor(it,templateStep)

        val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint=it.hint
        textInputLayout.isEnabled = enabled

        val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)

        textInputEditText.setOnClickListener {
            showDialog(DIALOG_DATE, textInputEditText)
        }

        doAssociateParent(templateStep, parent)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            textInputEditText.setText(it.resvalue)
        }

        //#Проверка_формата_даты
        textInputEditText.addTextChangedListener(TextWatcherHelper(it, templateStep))
        textInputEditText.setOnFocusChangeListener { v, hasFocus ->
            Timber.d("фокус_даты=$hasFocus")
            val strDate=(v as TextInputEditText).text.toString()
            if (!hasFocus && strDate.isNotEmpty()) {
                Timber.d("strDate=$strDate")
                if (!strDate.matches("[0-3]\\d.[01]\\d.\\d{4}".toRegex())) {
                    Timber.d("Ошибка_даты")
                    textInputEditText.error=parentFragment.getString(R.string.error_date)
                    parentFragment.errorControls.add(textInputEditText)
                } else {
                    val df=SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU"))
                    df.isLenient=false
                    try {
                        df.parse(strDate)
                        if (parentFragment.errorControls.contains(textInputEditText)) {
                            parentFragment.errorControls.remove(textInputEditText)
                        }
                    } catch (e: ParseException) {
                        e.printStackTrace()
                        textInputEditText.error=parentFragment.getString(R.string.error_date)
                        parentFragment.errorControls.add(textInputEditText)
                    }
                }
            }
        }

        enabledControls.add(textInputLayout)
    }

    private fun createTime(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_time, rootView.parent as ViewGroup?, false) as LinearLayout

        templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question
        setQuestionColor(it,templateStep)

        val textInputLayout=templateStep.findViewById<TextInputLayout>(R.id.til)
        textInputLayout.hint=it.hint
        textInputLayout.isEnabled = enabled

        val textInputEditText=templateStep.findViewById<TextInputEditText>(R.id.tiet)

        textInputEditText.setOnClickListener {
            showDialog(DIALOG_TIME, textInputEditText)
        }

        doAssociateParent(templateStep, parent)

        // Если шаг чеклиста был ранее сохранен восстановим значение
        if (!it.resvalue.isNullOrEmpty()){
            textInputEditText.setText(it.resvalue)
        }

        textInputEditText.addTextChangedListener(TextWatcherHelper(it, templateStep))

        enabledControls.add(textInputLayout)
    }


    private fun createPhoto(it: Models.TemplateControl, parent: LinearLayout) {
        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_photo2, rootView.parent as ViewGroup?, false) as LinearLayout

        templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=it.question
        setQuestionColor(it,templateStep)

        templateStep.tag=it

        doAssociateParent(templateStep, parent)

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
            // Сбрасываем признак Checked
            val curOrder=(parentFragment.activity as MainActivity).currentOrder
            (parentFragment.requireActivity() as MainActivity).photoStep=stepCheckup // Сохраним id контрола для которого делаем фото
            (parentFragment.requireActivity() as MainActivity).photoDir="${curOrder.guid}/${stepCheckup.guid}" // Сохраним id контрола для которого делаем фото
            photoHelper.createPhoto(curOrder.guid, stepCheckup)
        }

        val btnDeletePhoto = templateStep.findViewById<MaterialButton>(R.id.btnDeletePhoto)
        btnDeletePhoto.tag=templateStep
        btnDeletePhoto.setOnClickListener {
            Timber.d("Удалим фото")
            val ts=it.tag
            val tc=((ts as View).tag as Models.TemplateControl)

            val pager = templateStep.findViewById(R.id.pager) as ViewPager

            // Получим текущую страницу
            val indexPhoto=pager.currentItem
            Timber.d("pager.adapter.count=${pager.adapter?.count}")
            if (pager.adapter?.count!! >0) {
                // Получим список фоток из папки
                val imagesPhoto: List<String>
                imagesPhoto = if (!tc.resvalue.isNullOrEmpty()) {
                    OtherUtil().getFilesFromDir("${DCIM_DIR}/PhotoForApp/${tc.resvalue}")
                } else {
                    val curOrder=(parentFragment.activity as MainActivity).currentOrder
                    val photoDirectory="${curOrder.guid}/${stepCheckup.guid}"
                    OtherUtil().getFilesFromDir("${DCIM_DIR}/PhotoForApp/$photoDirectory")
                }
                val photoForDelete=imagesPhoto[indexPhoto]
                Timber.d("photoForDelete=$photoForDelete")
                if (photoHelper.deletePhoto(photoForDelete)) {
                    val imagesNew= mutableListOf<String>()
                    imagesNew.addAll(imagesPhoto)
                    imagesNew.removeAt(indexPhoto)
                    parentFragment.refreshPhotoViewer(templateStep, imagesNew, rootView.context)

                    // Проверим папку, может она пуста
                    val curOrder=(parentFragment.activity as MainActivity).currentOrder
                    if (photoHelper.checkDirAndEmpty("${curOrder.guid}/${stepCheckup.guid}")) {
                        Timber.d("Папка есть, она не пуста")
                    } else {
                        Timber.d("Удалчем_папку")
                        // Удалим папку, очистим photoStep?.resvalue
                        val dir=File("$DCIM_DIR/PhotoForApp/${curOrder.guid}/${stepCheckup.guid}")
                        dir.delete()
                        tc.answered=false
                        tc.resvalue=null
                        (parentFragment.activity as MainActivity).photoDir=""
                    }
                }
            }
        }

        val images: List<String>
        images = if (!it.resvalue.isNullOrEmpty()) {
            Timber.d("Фото_${it.resvalue}")
            // Обновим список с фото
            val curOrder=(parentFragment.activity as MainActivity).currentOrder
            val stDir = "PhotoForApp/${curOrder.guid}/${stepCheckup.guid}"
            val storageDir =
                File(
                    //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    DCIM_DIR,
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

        enabledControls.add(btnPhoto)
    }



    @SuppressLint("UseRequireInsteadOfGet")
    private fun createNodes(llContainer: LinearLayout): List<LinearLayout> {
        Timber.d("генерим узлы")
        val listNodesView= mutableListOf<LinearLayout>()
        if (parentFragment.currentOrder.countNode!=null) {
            for (i in 1..parentFragment.currentOrder.countNode!!) {
                val templateStep=LayoutInflater.from(rootView.context).inflate(
                    R.layout.template_group, rootView.parent as ViewGroup?, false) as LinearLayout

                Timber.d("Узел $i")
                templateStep.findViewById<TextView>(R.id.question).text=parentFragment.requireContext().getString(R.string.name_node,i)

                val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
                val llNode=templateStep.findViewById<LinearLayout>(R.id.container)
                val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

                clTitle.setOnClickListener {
                    if (llNode.visibility==View.VISIBLE) {
                        llNode.visibility=View.GONE
                        ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_up))
                    } else {
                        llNode.visibility=View.VISIBLE
                        ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_down))
                    }
                }

                listNodesView.add(llNode)

                doAssociateParent(templateStep, llContainer)
            }
        } else {
            Timber.d("parentFragment.currentOrder.countNode==null")
            parentFragment.toaster.showToast(R.string.not_count_node)
        }

        return listNodesView

    }

    private fun createArchGroup(llContainer: LinearLayout, name: String): List<LinearLayout> {
        Timber.d("генерим группы $name")
        val listNodesView= mutableListOf<LinearLayout>()
        for (i in 1..5) {
            val templateStep=LayoutInflater.from(rootView.context).inflate(
                R.layout.template_group, rootView.parent as ViewGroup?, false) as LinearLayout

            Timber.d("$name $i")
            templateStep.findViewById<TextView>(R.id.question).text=parentFragment.requireContext().getString(R.string.question,name,i)

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llNode=templateStep.findViewById<LinearLayout>(R.id.container)
            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

            clTitle.setOnClickListener {
                if (llNode.visibility==View.VISIBLE) {
                    llNode.visibility=View.GONE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_up))
                } else {
                    llNode.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_down))
                }
            }

            listNodesView.add(llNode)

            doAssociateParent(templateStep, llContainer)
        }


        return listNodesView

    }

    private fun createGroupNodes(name: String) :LinearLayout {
        Timber.d("генерим группу для Узлов")

        val templateStep=LayoutInflater.from(rootView.context).inflate(
            R.layout.template_group, rootView.parent as ViewGroup?, false) as LinearLayout

        //attachListenerToFab(templateStep,it)

        //templateStep.id=it.id
        templateStep.findViewById<TextView>(R.id.question).text=name//"Узлы"

        val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
        val llGroup=templateStep.findViewById<LinearLayout>(R.id.container)
        val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

        clTitle.setOnClickListener {
            if (llGroup.visibility==View.VISIBLE) {
                llGroup.visibility=View.GONE
                ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_up))
            } else {
                llGroup.visibility=View.VISIBLE
                ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_down))
            }
        }

        doAssociateParent(templateStep, rootView.findViewById(R.id.llMain))
        Timber.d("llGroup=$llGroup")

        return llGroup
    }

    private fun createGroup(name: String, parent: LinearLayout, node: String=""): LinearLayout {
        Timber.d("генерим группу $name$node")

        // Проверим, возможно группа уже создана
        val ll=listllGroupOther.filter { it.tag=="$name$node" }
        if (ll.isNotEmpty()) {
            return ll[0]
        } else {
            val templateStep=LayoutInflater.from(rootView.context).inflate(
                R.layout.template_group, rootView.parent as ViewGroup?, false) as LinearLayout

            templateStep.findViewById<TextView>(R.id.question).text=name

            val ivExpand=templateStep.findViewById<ImageView>(R.id.ivExpand)
            val llGroup=templateStep.findViewById<LinearLayout>(R.id.container)
            llGroup.tag="$name$node" // Сохраним имя группы
            val clTitle=templateStep.findViewById<ConstraintLayout>(R.id.titleGroup)

            clTitle.setOnClickListener {
                if (llGroup.visibility==View.VISIBLE) {
                    llGroup.visibility=View.GONE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_up))
                } else {
                    llGroup.visibility=View.VISIBLE
                    ivExpand.setImageDrawable(ContextCompat.getDrawable(rootView.context,R.drawable.arrow_down))
                }
            }

            doAssociateParent(templateStep, parent)
            Timber.d("llGroup=$llGroup")

            listllGroupOther.add(llGroup)

            return llGroup
        }


    }

    private fun setQuestionColor(it: Models.TemplateControl, templateStep: LinearLayout) {
        var questionColor=Color.BLACK
        if (it.question.indexOf("подающий")!=-1) {
            Timber.d("Color.RED")
            questionColor=Color.RED
        }
        if (it.question.indexOf("обратный")!=-1) {
            Timber.d("Color.BLUE")
            questionColor=Color.BLUE
        }
        templateStep.findViewById<TextView>(R.id.question).setTextColor(questionColor)
    }

    fun refresh() {
        val llMain= rootView.findViewById(R.id.llMain) as LinearLayout
        if(llMain.childCount>0) {
            llGroupNodes= null
            listllGroupOther.clear()
            listllNode=listOf()
            llMain.removeAllViews()
            create(rootView)
        }

    }

    fun checkEnabled() {
        Timber.d("checkEnabled")
        Timber.d("parentFragment_currentOrder_status=${parentFragment.currentOrder.status}")
        enabled= !(parentFragment.currentOrder.status==parentFragment.getString(R.string.status_IN_WAY)||
                parentFragment.currentOrder.status==parentFragment.getString(R.string.status_OPEN))

        Timber.d("enabledControls_size=${enabledControls.size}")
        Timber.d("enabled=${enabled}")

        enabledControls.forEach {
            if (it is MaterialBetterSpinner) {
                if (enabled) {
                    it.dropDownHeight = WindowManager.LayoutParams.WRAP_CONTENT
                } else {
                    it.dropDownHeight = 0
                }
            }
            it.isEnabled=enabled
        }
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


    private fun showDialog(id: Int, textInputEditText: TextInputEditText) {
        //Если потребуется вводить дату вручную, нужно поставить флаг focusableInTouchMode в XML
        if (id== DIALOG_DATE) {
            val dateListener =
                DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                    dateAndTime.set(Calendar.YEAR, year)
                    dateAndTime.set(Calendar.MONTH, month)
                    dateAndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    textInputEditText.setText(
                        SimpleDateFormat("dd.MM.yyyy", Locale("ru","RU")).format(dateAndTime.time)
                    )
                    textInputEditText.error=null
                    Timber.d("errorControls=${parentFragment.errorControls}")
                    Timber.d("textInputEditText=${textInputEditText.id}")
                    if (parentFragment.errorControls.contains(textInputEditText)) {
                        parentFragment.errorControls.remove(textInputEditText)
                    }
                }

            DatePickerDialog(parentFragment.requireContext(),
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
                    textInputEditText.setText(
                        SimpleDateFormat("HH:mm", Locale("ru","RU")).format(dateAndTime.time)
                    )
                }

            TimePickerDialog(parentFragment.requireContext(),
                timeListener,
                dateAndTime.get(Calendar.HOUR_OF_DAY),
                dateAndTime.get(Calendar.MINUTE), true
            ).show()
        }
    }



}